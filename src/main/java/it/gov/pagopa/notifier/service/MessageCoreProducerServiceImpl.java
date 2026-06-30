package it.gov.pagopa.notifier.service;


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
     *   <li>If exceeded, log and stop: the message is an upstream processing failure not yet persisted,
     *       so there is nothing to update — it is simply abandoned after exhausting retries.</li>
     *   <li>Otherwise, compute exponential backoff delay: {@code min(initialDelay * 2^(retry-1), maxDelay)}.</li>
     *   <li>Schedule message via {@link MessageCoreProducer#scheduleMessage(Message)} after the delay.</li>
     * </ol>
     *
     * @param messageDTO the message to be enqueued
     * @param retry the current retry attempt count (incremented after each failure)
     * @return {@code Mono<Void>} completes when the message is enqueued or abandoned
     */
    @Override
    public Mono<Void> enqueueMessage(MessageDTO messageDTO, long retry) {
        String messageId = messageDTO.getMessageId();

        if (retry > maxTry) {
            log.info("[MESSAGE-CORE-PRODUCER-SERVICE][ENQUEUE-MESSAGE] Message ID: {} exceeds max retry attempts ({}). Message will not be retried.", messageId, maxTry);
            return Mono.empty();
        }

        // Backoff esponenziale: initialDelay * 2^(retry-1), con cap a maxDelay.
        // Esempio con initialDelay=5s, maxDelay=60s:
        //   retry=1 →  5s, retry=2 → 10s, retry=3 → 20s, retry=4 → 40s, retry=5 → 60s (cap)
        // Il delay vive DENTRO la reactive chain: BaseKafkaConsumer attende il completamento
        // di questo Mono prima di committare l'offset Kafka, garantendo che il messaggio
        // sia pubblicato su Kafka prima che l'offset venga committed.
        long delaySeconds = Math.min(initialDelaySeconds * (1L << (retry - 1)), maxDelaySeconds);
        log.info("[MESSAGE-CORE-PRODUCER-SERVICE][ENQUEUE-MESSAGE] Enqueuing message ID: {} with retry attempt: {}, backoff delay: {}s", messageId, retry, delaySeconds);

        return Mono.delay(Duration.ofSeconds(delaySeconds))
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
