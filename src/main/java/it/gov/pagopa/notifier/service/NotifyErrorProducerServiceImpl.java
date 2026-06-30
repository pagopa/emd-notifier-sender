package it.gov.pagopa.notifier.service;


import it.gov.pagopa.common.configuration.MongoRetrySpecs;
import it.gov.pagopa.notifier.enums.MessageState;
import it.gov.pagopa.notifier.model.Message;
import it.gov.pagopa.notifier.dto.NotifyErrorQueuePayload;
import it.gov.pagopa.notifier.dto.TppDTO;
import it.gov.pagopa.notifier.event.producer.NotifyErrorProducer;
import it.gov.pagopa.notifier.repository.MessageRepository;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

import static it.gov.pagopa.notifier.constants.NotifierSenderConstants.MessageHeader.*;

/**
 * <p>Implementation of {@link NotifyErrorProducerService} for publishing failed notifications to error queue.</p>
 */
@Slf4j
@Service
public class NotifyErrorProducerServiceImpl implements NotifyErrorProducerService {

    private final NotifyErrorProducer notifyErrorProducer;
    private final MessageRepository messageRepository;
    private final long maxTry;
    private final long initialDelaySeconds;
    private final long maxDelaySeconds;

    public NotifyErrorProducerServiceImpl(NotifyErrorProducer notifyErrorProducer,
                                          MessageRepository messageRepository,
                                          @Value("${app.retry.max-retry}") long maxRetry,
                                          @Value("${app.retry.initial-delay-seconds:5}") long initialDelaySeconds,
                                          @Value("${app.retry.max-delay-seconds:60}") long maxDelaySeconds) {
        this.notifyErrorProducer = notifyErrorProducer;
        this.messageRepository = messageRepository;
        this.maxTry = maxRetry;
        this.initialDelaySeconds = initialDelaySeconds;
        this.maxDelaySeconds = maxDelaySeconds;
    }


    /**
     * {@inheritDoc}
     *
     * <p>Flow:</p>
     * <ol>
     *   <li>Checks if retry count exceeds {@code maxTry}; if so, saves message as ERROR and returns empty</li>
     *   <li>Computes exponential backoff delay: {@code min(initialDelay * 2^(retry-1), maxDelay)}</li>
     *   <li>Publishes to error queue via {@link NotifyErrorProducer#scheduleMessage(org.springframework.messaging.Message)}</li>
     * </ol>
     *
     * <p>Messages exceeding max retries are persisted in state ERROR and remain queryable from the DB.</p>
     */
    @Override
    public Mono<String> enqueueNotify(Message message, TppDTO tppDTO, long retry) {
        String messageId = message.getMessageId();
        String entityId = tppDTO.getEntityId();

        if (retry > maxTry) {
            log.info("[NOTIFY-ERROR-PRODUCER-SERVICE][ENQUEUE-NOTIFY] Message ID: {} for TPP: {} exceeds max retry attempts ({}). Persisting state ERROR.", messageId, entityId, maxTry);
            message.setMessageState(MessageState.ERROR);
            return messageRepository.save(message)
                    .retryWhen(MongoRetrySpecs.cosmosDbThrottling())
                    .flatMap(messageWithError -> Mono.empty());
        }

        // Backoff esponenziale: initialDelay * 2^(retry-1), con cap a maxDelay.
        // Esempio con initialDelay=5s, maxDelay=60s:
        //   retry=1 →  5s, retry=2 → 10s, retry=3 → 20s, retry=4 → 40s, retry=5 → 60s (cap)
        // Il delay vive DENTRO la reactive chain: BaseKafkaConsumer attende il completamento
        // di questo Mono prima di committare l'offset Kafka.
        long delaySeconds = Math.min(initialDelaySeconds * (1L << (retry - 1)), maxDelaySeconds);
        log.info("[NOTIFY-ERROR-PRODUCER-SERVICE][ENQUEUE-NOTIFY] Enqueuing message ID: {} for TPP: {} with retry attempt: {}, backoff delay: {}s", messageId, entityId, retry, delaySeconds);

        return Mono.delay(Duration.ofSeconds(delaySeconds))
                .publishOn(Schedulers.boundedElastic())
                .flatMap(tick -> Mono.fromRunnable(() -> {
                    log.debug("[NOTIFY-ERROR-PRODUCER-SERVICE][ENQUEUE-NOTIFY] Sending message ID: {} for TPP: {} with retry: {} to notify error queue.", messageId, entityId, retry);
                    notifyErrorProducer.scheduleMessage(createMessage(message, tppDTO, retry));
                }))
                .then(Mono.just("enqueued"));
    }

    /**
     * <p>Creates a Spring Kafka message with payload and retry metadata.</p>
     *
     * <p>Constructs a {@link NotifyErrorQueuePayload} containing the notification and TPP details,
     * and adds {@code ERROR_MSG_HEADER_RETRY} header with current retry count.</p>
     *
     * @param message the notification message
     * @param tppDTO the TPP configuration
     * @param retry current retry count
     * @return Spring message ready for publishing
     */
    @NotNull
    private static org.springframework.messaging.Message<NotifyErrorQueuePayload> createMessage(Message message, TppDTO tppDTO, long retry) {
        log.debug("[NOTIFY-ERROR-PRODUCER-SERVICE][CREATE-MESSAGE] Creating message for ID: {} with retry: {}, entityId: {}",
                message.getMessageId(), retry, tppDTO.getEntityId());

        return MessageBuilder
                .withPayload(new NotifyErrorQueuePayload(tppDTO,message))
                .setHeader(ERROR_MSG_HEADER_RETRY, retry)
                .build();
    }

}
