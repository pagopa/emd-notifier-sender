package it.gov.pagopa.notifier.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.common.reactive.kafka.consumer.BaseKafkaConsumer;
import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.dto.NotifyErrorQueuePayload;
import it.gov.pagopa.notifier.dto.TppDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
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

    private final Duration commitDelay;
    private final Duration delayMinusCommit;
    private final ObjectReader objectReader;
    private final NotifyServiceImpl sendMessageService;
    public NotifyErrorConsumerServiceImpl(ObjectMapper objectMapper,
                                              NotifyServiceImpl sendMessageService,
                                              @Value("${spring.application.name}") String applicationName,
                                              @Value("${spring.cloud.stream.kafka.bindings.consumerNotify-in-0.consumer.ackTime}") long commitMillis,
                                              @Value("${app.message-core.build-delay-duration}") long delayMinusCommit) {
        super(applicationName);
        this.commitDelay = Duration.ofMillis(commitMillis);
        this.delayMinusCommit =Duration.ofMillis(delayMinusCommit);
        this.objectReader = objectMapper.readerFor(NotifyErrorQueuePayload.class);
        this.sendMessageService = sendMessageService;
    }
    @Override
    protected Duration getCommitDelay() {
        return commitDelay;
    }

    @Override
    protected Duration getDelayMinusCommit() {
        return delayMinusCommit;
    }
    @Override
    protected void subscribeAfterCommits(Flux<List<String>> afterCommits2subscribe) {
        afterCommits2subscribe
                .buffer(delayMinusCommit)
                .subscribe(r -> log.info("[NOTIFIER-ERROR-COMMANDS] Processed offsets committed successfully"));
    }
    @Override
    protected ObjectReader getObjectReader() {
        return objectReader;
    }
    @Override
    protected Consumer<Throwable> onDeserializationError(Message<String> message) {
        return e -> log.info("[NOTIFY-ERROR-CONSUMER-SERVICE][DESERIALIZATION-ERROR] Unexpected JSON : {}", e.getMessage());
    }

    @Override
    protected Mono<String> execute(NotifyErrorQueuePayload payload, Message<String> message, Map<String, Object> ctx) {
        TppDTO tppDTO = payload.getTppDTO();
        MessageDTO messageDTO = payload.getMessageDTO();
        String messageId = messageDTO.getMessageId();
        String entityId = tppDTO.getEntityId();
        log.info("[NOTIFY-ERROR-CONSUMER-SERVICE][EXECUTE]Queue message received with ID: {} and payload: {}", messageId, messageDTO);

        MessageHeaders headers = message.getHeaders();
        Long retry = (Long) headers.get(ERROR_MSG_HEADER_RETRY);

        if (retry == null){
            log.warn("[NOTIFY-ERROR-CONSUMER-SERVICE][EXECUTE]Missing header: ERROR_MSG_HEADER_RETRY for message ID: {}", messageId);
            return Mono.just("[NOTIFY-ERROR-CONSUMER-SERVICE][EXECUTE]Message %s not processed due to missing headers".formatted(messageId));
        }

        log.info("[NOTIFY-ERROR-CONSUMER-SERVICE][EXECUTE]Attempting to send message ID: {} to TPP: {} at retry attempt: {}", messageId, entityId, retry);

        sendMessageService.sendNotify(messageDTO, tppDTO, retry)
                .doOnSuccess(v -> log.info("[NOTIFY-ERROR-CONSUMER-SERVICE][EXECUTE]Successfully sent message ID: {} to TPP: {}", messageId, entityId))
                .doOnError(e -> log.error("[NOTIFY-ERROR-CONSUMER-SERVICE][EXECUTE]Error sending message ID: {} to TPP: {}. Error: {}", messageId, entityId, e.getMessage()))
                .subscribe();

        return Mono.just("[NOTIFY-ERROR-CONSUMER-SERVICE][EXECUTE]Processing attempt for message %s to TPP %s in progress".formatted(messageId, entityId));
    }

}
