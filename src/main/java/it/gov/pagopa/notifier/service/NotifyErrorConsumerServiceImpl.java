package it.gov.pagopa.notifier.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.common.reactive.kafka.consumer.BaseKafkaConsumer;
import it.gov.pagopa.notifier.model.Message;
import it.gov.pagopa.notifier.dto.NotifyErrorQueuePayload;
import it.gov.pagopa.notifier.dto.TppDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static it.gov.pagopa.notifier.constants.NotifierSenderConstants.MessageHeader.*;


@Service
@Slf4j
public class NotifyErrorConsumerServiceImpl extends BaseKafkaConsumer<NotifyErrorQueuePayload,String> implements NotifyErrorConsumerService {

    private final ObjectReader objectReader;
    private final NotifyServiceImpl sendMessageService;
    public NotifyErrorConsumerServiceImpl(ObjectMapper objectMapper,
                                              NotifyServiceImpl sendMessageService,
                                              @Value("${spring.application.name}") String applicationName,
                                              @Value("${spring.cloud.stream.kafka.bindings.consumerNotify-in-0.consumer.ackTime}") long commitDelay,
                                              @Value("${app.message-core.build-delay-duration}") long delayMinusCommit) {
        super(applicationName, Duration.ofMillis(commitDelay),Duration.ofMillis(delayMinusCommit));
        this.objectReader = objectMapper.readerFor(NotifyErrorQueuePayload.class);
        this.sendMessageService = sendMessageService;
    }
  
    @Override
    protected void subscribeAfterCommits(Flux<List<String>> afterCommits2subscribe) {
        afterCommits2subscribe
                .subscribe(r -> log.info("[NOTIFIER-ERROR-COMMANDS] Processed offsets committed successfully"));
    }
    @Override
    protected ObjectReader getObjectReader() {
        return objectReader;
    }
    @Override
    protected Consumer<Throwable> onDeserializationError(org.springframework.messaging.Message<String> message) {
        return e -> log.info("[NOTIFY-ERROR-CONSUMER-SERVICE][DESERIALIZATION-ERROR] Unexpected JSON : {}", e.getMessage());
    }

    @Override
    protected Mono<String> execute(NotifyErrorQueuePayload payload, org.springframework.messaging.Message<String> message, Map<String, Object> ctx) {
        TppDTO tppDTO = payload.getTppDTO();
        Message notification = payload.getMessage();
        String notificationId = notification.getMessageId();
        String entityId = tppDTO.getEntityId();
        log.info("[NOTIFY-ERROR-CONSUMER-SERVICE][EXECUTE] Queue message received with ID: {} and payload: {}", notificationId, notification);

        MessageHeaders headers = message.getHeaders();
        Long retry = (Long) headers.get(ERROR_MSG_HEADER_RETRY);

        if (retry == null){
            log.warn("[NOTIFY-ERROR-CONSUMER-SERVICE][EXECUTE]Missing header: ERROR_MSG_HEADER_RETRY for message ID: {}", notificationId);
            return Mono.just("[NOTIFY-ERROR-CONSUMER-SERVICE][EXECUTE] Message %s not processed due to missing headers".formatted(notificationId));
        }

        log.info("[NOTIFY-ERROR-CONSUMER-SERVICE][EXECUTE] Attempting to send message ID: {} to TPP: {} at retry attempt: {}", notificationId, entityId, retry);
        sendMessageService.sendNotify(notification, tppDTO, retry)
                .subscribe();

        return Mono.just("[NOTIFY-ERROR-CONSUMER-SERVICE][EXECUTE]Processing attempt for message %s to TPP %s in progress".formatted(notificationId, entityId));
    }

}
