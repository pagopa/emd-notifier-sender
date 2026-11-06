package it.gov.pagopa.notifier.service;


import it.gov.pagopa.common.reactive.kafka.exception.UncommittableError;
import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.event.producer.MessageCoreProducer;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import static it.gov.pagopa.notifier.constants.NotifierSenderConstants.MessageHeader.*;

/**
 * <p>Implementation of {@link MessageCoreProducerService}.</p>
 *
 * <p>Validates retry limits and delegates message scheduling to {@link MessageCoreProducer}.</p>
 */
@Slf4j
@Service
public class MessageCoreProducerServiceImpl implements MessageCoreProducerService {

    private final MessageCoreProducer messageCoreProducer;
    private final long maxTry;
    private final long initialDelaySeconds;
    private final long maxDelaySeconds;

    public MessageCoreProducerServiceImpl(MessageCoreProducer messageCoreProducer,
                                          @Value("${app.retry.max-retry}") long maxRetry,
                                          @Value("${app.retry.initial-delay-seconds:5}") long initialDelaySeconds,
                                          @Value("${app.retry.max-delay-seconds:60}") long maxDelaySeconds) {
        this.messageCoreProducer = messageCoreProducer;
        this.maxTry = maxRetry;
        this.initialDelaySeconds = initialDelaySeconds;
        this.maxDelaySeconds = maxDelaySeconds;
    }


    /**
     * <p>Enqueues a message for delayed retry if within retry limits.</p>
     *
     * <p>Flow:</p>
     * <ol>
     *   <li>Check if retry count exceeds maximum allowed attempts.</li>
     *   <li>If exceeded, fail without committing the source offset: dropping an unpersisted
     *       message is not an acceptable terminal outcome.</li>
     *   <li>Otherwise, compute exponential backoff delay: {@code min(initialDelay * 2^(retry-1), maxDelay)}.</li>
     *   <li>Schedule message via {@link MessageCoreProducer#scheduleMessage(Message)} after the delay.</li>
     * </ol>
     *
     * @param messageDTO the message to be enqueued
     * @param retry the current retry attempt count (incremented after each failure)
     * @return {@code Mono<Void>} completes when the retry is published; fails if retry attempts are exhausted
     */
    @Override
    public Mono<Void> enqueueMessage(MessageDTO messageDTO, long retry) {
        String messageId = messageDTO.getMessageId();

        if (retry > maxTry) {
            log.error("[MESSAGE-CORE-PRODUCER-SERVICE][ENQUEUE-MESSAGE] Message ID: {} exceeds max retry attempts ({}). Source offset must not be committed.", messageId, maxTry);
            return Mono.error(new UncommittableError("Message " + messageId + " exhausted retries before persistence"));
        }

        // Backoff esponenziale: initialDelay * 2^(retry-1), con cap a maxDelay.
        // Esempio con initialDelay=5s, maxDelay=60s:
        //   retry=1 →  5s, retry=2 → 10s, retry=3 → 20s, retry=4 → 40s, retry=5 → 60s (cap)
        // Il delay vive DENTRO la reactive chain: BaseKafkaConsumer attende il completamento
        // di questo Mono prima di committare l'offset Kafka, garantendo che il messaggio
        // sia pubblicato su Kafka prima che l'offset venga committed.
        long delaySeconds = Math.min(initialDelaySeconds * (1L << (retry - 1)), maxDelaySeconds);
        // Spread retries across pods without exceeding the configured backoff cap.
        long delayMillis = (long) (Duration.ofSeconds(delaySeconds).toMillis()
                * ThreadLocalRandom.current().nextDouble(0.8, 1.0));
        log.info("[MESSAGE-CORE-PRODUCER-SERVICE][ENQUEUE-MESSAGE] Enqueuing message ID: {} with retry attempt: {}, backoff delay: {}ms", messageId, retry, delayMillis);

        return Mono.delay(Duration.ofMillis(delayMillis))
                .publishOn(Schedulers.boundedElastic())
                .flatMap(tick -> Mono.fromRunnable(() -> {
                    log.debug("[MESSAGE-CORE-PRODUCER-SERVICE][ENQUEUE-MESSAGE] Sending message ID: {} with retry attempt: {} to message queue.", messageId, retry);
                    messageCoreProducer.scheduleMessage(createMessage(messageDTO, retry));
                }))
                .then();
    }

    @NotNull
    private static Message<MessageDTO> createMessage(MessageDTO messageDTO, long retry) {
        log.debug("[MESSAGE-CORE-PRODUCER-SERVICE][CREATE-MESSAGE] Creating message for ID: {} with retry attempt: {}", messageDTO.getMessageId(), retry);
        return MessageBuilder
                .withPayload(messageDTO)
                .setHeader(ERROR_MSG_HEADER_RETRY, retry)
                .build();
    }

}
