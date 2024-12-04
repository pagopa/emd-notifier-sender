package it.gov.pagopa.notifier.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.common.reactive.kafka.consumer.BaseKafkaConsumer;
import it.gov.pagopa.notifier.connector.tpp.TppConnectorImpl;
import it.gov.pagopa.notifier.dto.MessageDTO;
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

import static it.gov.pagopa.notifier.constants.NotifierSenderConstants.MessageHeader.ERROR_MSG_HEADER_RETRY;
import static it.gov.pagopa.notifier.constants.NotifierSenderConstants.MessageHeader.ERROR_MSG_HEADER_TPP_ID;


@Service
@Slf4j
public class NotifyErrorConsumerServiceImpl extends BaseKafkaConsumer<MessageDTO,String> implements NotifyErrorConsumerService {

    private final Duration commitDelay;
    private final Duration delayMinusCommit;
    private final ObjectReader objectReader;
    private final NotifyServiceImpl sendMessageService;
    private final NotifyErrorProducerService notifyErrorProducerService;
    private final TppConnectorImpl tppConnector;
    public NotifyErrorConsumerServiceImpl(ObjectMapper objectMapper,
                                          NotifyServiceImpl sendMessageService,
                                          @Value("${spring.application.name}") String applicationName,
                                          @Value("${spring.cloud.stream.kafka.bindings.consumerNotify-in-0.consumer.ackTime}") long commitMillis,
                                          @Value("${app.message-core.build-delay-duration}") String delayMinusCommit, NotifyErrorProducerService notifyErrorProducerService, TppConnectorImpl tppConnector) {
        super(applicationName);
        this.commitDelay = Duration.ofMillis(commitMillis);
        this.notifyErrorProducerService = notifyErrorProducerService;
        this.tppConnector = tppConnector;
        Duration buildDelayDuration = Duration.parse(delayMinusCommit).minusMillis(commitMillis);
        Duration defaultDurationDelay = Duration.ofMillis(2L);
        this.delayMinusCommit = defaultDurationDelay.compareTo(buildDelayDuration) >= 0 ? defaultDurationDelay : buildDelayDuration;
        this.objectReader = objectMapper.readerFor(MessageDTO.class);
        this.sendMessageService = sendMessageService;
    }
    @Override
    protected Duration getCommitDelay() {
        return commitDelay;
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
    protected Mono<String> execute(MessageDTO payload, Message<String> message, Map<String, Object> ctx) {

        String messageId = payload.getMessageId();

        MessageHeaders headers = message.getHeaders();
        String tppId = (String) headers.get(ERROR_MSG_HEADER_TPP_ID);
        if (tppId == null){
            log.warn("[NOTIFY-ERROR-CONSUMER-SERVICE][EXECUTE] Missing header: ERROR_MSG_HEADER_TPP_ID for message ID: {}", messageId);
            return Mono.just("[NOTIFY-ERROR-CONSUMER-SERVICE][EXECUTE] Message %s not processed due to missing headers".formatted(messageId));
        }

        Long retry = (Long) headers.get(ERROR_MSG_HEADER_RETRY);
        if (retry == null){
            log.warn("[NOTIFY-ERROR-CONSUMER-SERVICE][EXECUTE]Missing header: ERROR_MSG_HEADER_RETRY for message ID: {}", messageId);
            return Mono.just("[NOTIFY-ERROR-CONSUMER-SERVICE][EXECUTE]Message %s not processed due to missing headers".formatted(messageId));
        }

        log.info("[NOTIFY-ERROR-CONSUMER-SERVICE][EXECUTE]Queue message received with ID: {} and payload: {} for tppId", messageId, payload);
        return tppConnector.getTppEnabled(tppId)
                        .flatMap(tppDTO -> {
                            String entityId = tppDTO.getEntityId();
                            log.info("[NOTIFY-ERROR-CONSUMER-SERVICE][EXECUTE]Attempting to send message ID: {} to TPP: {} at retry attempt: {}", messageId, entityId, retry);
                            sendMessageService.sendNotify(payload, tppDTO, retry).subscribe();
                            return Mono.just("[NOTIFY-ERROR-CONSUMER-SERVICE][EXECUTE]Processing attempt for message %s to TPP %s in progress".formatted(messageId, entityId));
                        })
                        .doOnError(e -> {
                            log.error("[NOTIFY-ERROR-CONSUMER-SERVICE][EXECUTE]Error getting tppID: {} for messageId: {}. Error: {}", tppId, messageId, e.getMessage());
                            notifyErrorProducerService.enqueueNotify(payload,tppId,retry + 1);
                        });
    }

}
