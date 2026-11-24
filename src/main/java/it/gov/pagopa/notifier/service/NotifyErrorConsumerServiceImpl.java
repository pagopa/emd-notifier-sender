package it.gov.pagopa.notifier.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.common.reactive.kafka.consumer.BaseKafkaConsumer;
import it.gov.pagopa.common.reactive.kafka.exception.NonRetryableMessageException;
import it.gov.pagopa.notifier.model.Message;
import it.gov.pagopa.notifier.dto.NotifyErrorQueuePayload;
import it.gov.pagopa.notifier.dto.TppDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Consumer;

/**
 * <p>Implementation of {@link NotifyErrorConsumerService} extending {@link BaseKafkaConsumer}.</p>
 */
@Service
@Slf4j
public class NotifyErrorConsumerServiceImpl extends BaseKafkaConsumer<NotifyErrorQueuePayload,String> implements NotifyErrorConsumerService {

    private final ObjectReader objectReader;
    private final NotifyServiceImpl sendMessageService;
    public NotifyErrorConsumerServiceImpl(ObjectMapper objectMapper,
                                              NotifyServiceImpl sendMessageService,
                                              @Value("${spring.application.name}") String applicationName) {
        super(applicationName);
        this.objectReader = objectMapper.readerFor(NotifyErrorQueuePayload.class);
        this.sendMessageService = sendMessageService;
    }
    @Override
    protected ObjectReader getObjectReader() {
        return objectReader;
    }
    @Override
    protected Consumer<Throwable> onDeserializationError(org.springframework.messaging.Message<String> message) {
        return e -> log.info("[NOTIFY-ERROR-CONSUMER-SERVICE][DESERIALIZATION-ERROR] Unexpected JSON : {}", e.getMessage());
    }

    /**
     * <p>Processes a single error queue message by attempting to resend the notification to TPP.</p>
     *
     * <p>Flow:</p>
     * <ol>
     *   <li>Extracts {@code NotifyErrorQueuePayload} and retry count from headers</li>
     *   <li>Validates presence of {@code ERROR_MSG_HEADER_RETRY} header</li>
     *   <li>Delegates to {@link NotifyServiceImpl#sendNotify(Message, TppDTO, long)} asynchronously</li>
     *   <li>Returns processing status message</li>
     * </ol>
     *
     * <p>Invalid payloads and retry headers are logged and discarded by the base consumer.</p>
     *
     * @param payload the deserialized notification payload
     * @param message the original Spring message with headers
     * @param ctx context map for additional information
     * @return {@code Mono<String>} with processing status message
     */
    @Override
    protected Mono<String> execute(NotifyErrorQueuePayload payload, org.springframework.messaging.Message<String> message, Map<String, Object> ctx) {
        if (payload == null || payload.getMessage() == null || payload.getTppDTO() == null) {
            return Mono.error(new NonRetryableMessageException("Missing message or tppDTO in notification payload"));
        }
        TppDTO tppDTO = payload.getTppDTO();
        Message notification = payload.getMessage();
        if (!StringUtils.hasText(notification.getMessageId()) || !StringUtils.hasText(tppDTO.getEntityId())
                || tppDTO.getTokenSection() == null) {
            return Mono.error(new NonRetryableMessageException("Missing messageId, entityId or tokenSection in notification payload"));
        }
        String notificationId = notification.getMessageId();
        String entityId = tppDTO.getEntityId();
        long retry = readRetryHeader(message);
        log.info("[NOTIFY-ERROR-CONSUMER-SERVICE][EXECUTE] Queue message received with ID: {} and payload: {}", notificationId, notification);


        log.info("[NOTIFY-ERROR-CONSUMER-SERVICE][EXECUTE] Attempting to send message ID: {} to TPP: {} at retry attempt: {}", notificationId, entityId, retry);
        // FIX Graceful Shutdown: rimosso .subscribe() fire-and-forget.
        // BaseKafkaConsumer attende il completamento di questo Mono<String> prima di fare il commit
        // dell'offset Kafka. Con .subscribe() + Mono.just("in progress"), l'offset veniva committed
        // immediatamente mentre la business logic girava in background, causando perdita di messaggi
        // e duplicati in caso di crash/shutdown. Ora la chain è integra: il commit avviene solo
        // dopo che sendNotify() ha completato tutta la sua elaborazione.
        return sendMessageService.sendNotify(notification, tppDTO, retry)
                .thenReturn("[NOTIFY-ERROR-CONSUMER-SERVICE][EXECUTE] Processing attempt for message %s to TPP %s completed".formatted(notificationId, entityId));
    }

}
