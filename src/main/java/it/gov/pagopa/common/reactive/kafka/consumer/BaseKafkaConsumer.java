package it.gov.pagopa.common.reactive.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.common.kafka.utils.KafkaConstants;
import it.gov.pagopa.common.reactive.kafka.exception.UncommittableError;
import it.gov.pagopa.common.reactive.utils.PerformanceLogger;
import it.gov.pagopa.common.utils.CommonUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;
import reactor.util.retry.Retry;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.time.Duration;

/**
 * Base class for processing and acknowledging one record at a time on the Kafka listener thread.
 * Other than extend this class, you should:
 * <ol>
 *     <li>Turn off the autoCommit (spring.cloud.stream.kafka.bindings.BINDINGNAME.consumer.autoCommitOffset=false)</li>
 *     <li>Set the ackMode to MANUAL_IMMEDIATE (spring.cloud.stream.kafka.bindings.BINDINGNAME.consumer.ackMode=MANUAL_IMMEDIATE)</li>
 * </ol>
 * @param <T> The type of the message to read and deserialize
 * @param <R> The type of the message resulted
 */
@Slf4j
public abstract class BaseKafkaConsumer<T, R> {

    /** Key used inside the {@link Context} to store the startTime */
    protected static final String CONTEXT_KEY_START_TIME = "START_TIME";
    /** Key used inside the {@link Context} to store a msg identifier used for logging purpose */
    protected static final String CONTEXT_KEY_MSG_ID = "MSG_ID";

    private final String applicationName;
    protected BaseKafkaConsumer(String applicationName) {
        this.applicationName = applicationName;
    }

    record KafkaAcknowledgeResult<T> (Acknowledgment ack, Integer partition, Long offset, T result){
        public KafkaAcknowledgeResult(Message<?> message, T result) {
            this(
                    (Acknowledgment)CommonUtilities.getHeaderValue(message, KafkaHeaders.ACKNOWLEDGMENT),
                    getMessagePartitionId(message),
                    getMessageOffset(message),
                    result
            );
        }
    }

    private static Integer getMessagePartitionId(Message<?> message) {
        return (Integer )CommonUtilities.getHeaderValue(message, KafkaHeaders.RECEIVED_PARTITION);
    }

    private static Long getMessageOffset(Message<?> message) {
        return (Long) CommonUtilities.getHeaderValue(message, KafkaHeaders.OFFSET);
    }

    /** Process and acknowledge one record before the Kafka listener can move to the next one. */
    public final void execute(Message<String> message) {
        KafkaAcknowledgeResult<R> processed = executeAcknowledgeAware(message).block();
        if (processed == null || processed.ack() == null) {
            throw new UncommittableError("Kafka processing produced no result or acknowledgment");
        }
        processed.ack().acknowledge();
        log.info("[KAFKA_COMMIT][{}] Acknowledged partition {} offset {} after processing",
                getFlowName(), processed.partition(), processed.offset());
    }


    private Mono<KafkaAcknowledgeResult<R>> executeAcknowledgeAware(Message<String> message) {
        KafkaAcknowledgeResult<R> defaultAck = new KafkaAcknowledgeResult<>(message, null);

        byte[] retryingApplicationName = message.getHeaders().get(KafkaConstants.ERROR_MSG_HEADER_APPLICATION_NAME, byte[].class);
        if(retryingApplicationName != null && !new String(retryingApplicationName, StandardCharsets.UTF_8).equals(this.applicationName)){
            log.info("[{}] Discarding message due to other application retry ({}): {}", getFlowName(), retryingApplicationName,  CommonUtilities.readMessagePayload(message));
            return Mono.just(defaultAck);
        }

        return Mono.defer(() -> {
                    Map<String, Object> ctx = new HashMap<>();
                    ctx.put(CONTEXT_KEY_START_TIME, System.currentTimeMillis());
                    ctx.put(CONTEXT_KEY_MSG_ID, CommonUtilities.readMessagePayload(message));
                    return execute(message, ctx)
                            .map(r -> new KafkaAcknowledgeResult<>(message, r))
                            .switchIfEmpty(Mono.error(new UncommittableError("Empty Kafka processing result; offset must not be committed")))
                            .doOnNext(r -> doFinally(message, r.result, ctx));
                })
                .onErrorMap(e -> e instanceof UncommittableError ? e
                        : new UncommittableError("Kafka message processing failed", e instanceof Exception ex ? ex : new RuntimeException(e)))
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(30))
                        .jitter(0.5)
                        .filter(UncommittableError.class::isInstance)
                        .doBeforeRetry(signal -> log.error("[KAFKA_COMMIT][{}] Processing failed; offset is not committed (attempt {}). Retrying.",
                                getFlowName(), signal.totalRetries() + 1, signal.failure())));
    }

    /** to perform some operation at the end of business logic execution, thus before to wait for commit. As default, it will perform an INFO logging with performance time */
    @SuppressWarnings({"sonar:S1172", "unused"}) // suppressing unused parameters
    protected void doFinally(Message<String> message, R r, Map<String, Object> ctx) {
        Long startTime = (Long)ctx.get(CONTEXT_KEY_START_TIME);
        String msgId = (String)ctx.get(CONTEXT_KEY_MSG_ID);
        if(startTime != null){
            PerformanceLogger.logTiming(getFlowName(), startTime,
                    "(partition: %s, offset: %s) %s".formatted(getMessagePartitionId(message), getMessageOffset(message), msgId));
        }
    }

    /** Name used for logging purpose */
    public String getFlowName() {
        return getClass().getSimpleName();
    }

    /** It will deserialize the message and then call the {@link #execute(Object, Message, Map)} method */
    protected Mono<R> execute(Message<String> message, Map<String, Object> ctx){
        return Mono.just(message)
                .mapNotNull(this::deserializeMessage)
                .flatMap(payload->execute(payload, message, ctx));
    }

    /** The {@link ObjectReader} to use in order to deserialize the input message */
    protected abstract ObjectReader getObjectReader();
    /** The action to take if the deserialization will throw an error */
    protected abstract Consumer<Throwable> onDeserializationError(Message<String> message);

    /** The function invoked in order to process the current message */
    protected abstract Mono<R> execute(T payload, Message<String> message, Map<String, Object> ctx);

    /** It will read and deserialize {@link Message#getPayload()} using the given {@link #getObjectReader()} */
    protected T deserializeMessage(Message<String> message) {
        T result = CommonUtilities.deserializeMessage(message, getObjectReader(), onDeserializationError(message));
        if (result == null) {
            throw new UncommittableError("Cannot deserialize Kafka message; offset must not be committed");
        }
        return result;
    }

}
