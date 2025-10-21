package it.gov.pagopa.notifier.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.common.reactive.kafka.consumer.BaseKafkaConsumer;
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

import static it.gov.pagopa.notifier.constants.NotifierSenderConstants.MessageHeader.*;


@Service
@Slf4j
public class MessageCoreConsumerServiceImpl extends BaseKafkaConsumer<MessageDTO,String> implements MessageCoreConsumerService {


    private final ObjectReader objectReader;
    private final MessageServiceImpl messageCoreService;
    public MessageCoreConsumerServiceImpl(ObjectMapper objectMapper,
                                          @Value("${spring.application.name}") String applicationName,
                                          @Value("${spring.cloud.stream.kafka.bindings.consumerMessage-in-0.consumer.ackTime}") long commitDelay,
                                          @Value("${app.message-core.build-delay-duration}") long delayMinusCommit,
                                          MessageServiceImpl messageCoreService) {
        super(applicationName, Duration.ofMillis(commitDelay),Duration.ofMillis(delayMinusCommit));
        this.messageCoreService = messageCoreService;
        this.objectReader = objectMapper.readerFor(MessageDTO.class);
    }

    @Override
    protected void subscribeAfterCommits(Flux<List<String>> afterCommits2subscribe) {
        afterCommits2subscribe
                .subscribe(r -> log.info("[MESSAGE-CORE-COMMANDS] Processed offsets committed successfully"));
    }
    @Override
    protected ObjectReader getObjectReader() {
        return objectReader;
    }
    @Override
    protected Consumer<Throwable> onDeserializationError(Message<String> message) {
        return e -> log.info("[MESSAGE-CORE-CONSUMER-SERVICE][DESERIALIZATION-ERROR] Unexpected JSON : {}", e.getMessage());
    }


    /**
     * Processes a message by extracting the retry count from the headers and
     * invoking the message core service to send the message.
     *
     * @param messageDTO the deserialized message payload
     * @param message the original message with headers
     * @param ctx the processing context
     * @return a Mono containing a status message about the processing attempt
     */
    @Override
    protected Mono<String> execute(MessageDTO messageDTO, Message<String> message, Map<String, Object> ctx) {
        String messageId = messageDTO.getMessageId();
        MessageHeaders headers = message.getHeaders();
        Long retry = (Long) headers.get(ERROR_MSG_HEADER_RETRY);

        log.info("[MESSAGE-CORE-CONSUMER-SERVICE][EXECUTE] Received message with ID: {} and payload: {}", messageId, messageDTO);

        if (retry == null) {
            log.warn("[MESSAGE-CORE-CONSUMER-SERVICE][EXECUTE] No retry header found. Message {} will not be processed.", messageId);
            return Mono.just("[MESSAGE-CORE-CONSUMER-SERVICE][EXECUTE] Message %s not processed due to missing headers".formatted(messageId));
        }

        log.info("[MESSAGE-CORE-CONSUMER-SERVICE][EXECUTE] Processing attempt {} for message ID: {}", retry, messageId);
        messageCoreService.processMessage(messageDTO, retry)
                .subscribe();

        return Mono.just("[NOTIFIER-ERROR-CONSUMER][EXECUTE] Processing attempt %s for message %s in progress".formatted(retry, messageId));
    }

}
