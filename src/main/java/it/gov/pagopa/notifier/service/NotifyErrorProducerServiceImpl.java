package it.gov.pagopa.notifier.service;


import it.gov.pagopa.notifier.model.Message;
import it.gov.pagopa.notifier.dto.NotifyErrorQueuePayload;
import it.gov.pagopa.notifier.dto.TppDTO;
import it.gov.pagopa.notifier.event.producer.NotifyErrorProducer;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.gov.pagopa.notifier.constants.NotifierSenderConstants.MessageHeader.*;

/**
 * <p>Implementation of {@link NotifyErrorProducerService} for publishing failed notifications to error queue.</p>
 */
@Slf4j
@Service
public class NotifyErrorProducerServiceImpl implements NotifyErrorProducerService {

    private final NotifyErrorProducer notifyErrorProducer;

    private final Long maxTry;

    public NotifyErrorProducerServiceImpl(NotifyErrorProducer notifyErrorProducer,
                                          @Value("${app.retry.max-retry}") long maxRetry){
        this.notifyErrorProducer = notifyErrorProducer;
        this.maxTry = maxRetry;
    }


    /**
     * {@inheritDoc}
     *
     * <p>Flow:</p>
     * <ol>
     *   <li>Checks if retry count exceeds {@code maxTry}; if so, returns empty</li>
     *   <li>Constructs Kafka message via {@link #createMessage(Message, TppDTO, long)}</li>
     *   <li>Publishes to error queue via {@link NotifyErrorProducer#scheduleMessage(org.springframework.messaging.Message)}</li>
     * </ol>
     *
     * <p>Messages exceeding max retries are logged and discarded.</p>
     */
    @Override
    public Mono<String> enqueueNotify(Message message, TppDTO tppDTO, long retry) {
        String messageId = message.getMessageId();
        String entityId = tppDTO.getEntityId();

        if (retry > maxTry) {
            log.info("[NOTIFY-ERROR-PRODUCER-SERVICE][ENQUEUE-NOTIFY] Message ID: {} for TPP: {} exceeds max retry attempts ({}). Not retryable.", messageId, entityId, maxTry);
            return Mono.empty();
        }

        log.info("[NOTIFY-ERROR-PRODUCER-SERVICE][ENQUEUE-NOTIFY] Enqueuing message ID: {} for TPP: {} with retry attempt: {}", messageId, entityId, retry);

        return Mono.fromRunnable(() -> {
            log.debug("[NOTIFY-ERROR-PRODUCER-SERVICE][ENQUEUE-NOTIFY] Sending message ID: {} for TPP: {} with retry: {} to notify error queue.", messageId, entityId, retry);
            notifyErrorProducer.scheduleMessage(createMessage(message, tppDTO, retry));
        });
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
