package it.gov.pagopa.notifier.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.common.reactive.kafka.consumer.BaseKafkaConsumer;
import it.gov.pagopa.common.reactive.kafka.exception.NonRetryableMessageException;
import it.gov.pagopa.notifier.dto.MessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Consumer;

/**
 * <p>Implementation of {@link MessageCoreConsumerService}.</p>
 *
 * <p>Extends {@link BaseKafkaConsumer} to provide Kafka-specific message consumption.</p>
 */
@Service
@Slf4j
public class MessageCoreConsumerServiceImpl extends BaseKafkaConsumer<MessageDTO,String> implements MessageCoreConsumerService {


    private final ObjectReader objectReader;
    private final MessageServiceImpl messageCoreService;
    public MessageCoreConsumerServiceImpl(ObjectMapper objectMapper,
                                          @Value("${spring.application.name}") String applicationName,
                                          MessageServiceImpl messageCoreService) {
        super(applicationName);
        this.messageCoreService = messageCoreService;
        this.objectReader = objectMapper.readerFor(MessageDTO.class);
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
     * <p>Processes a message by extracting retry metadata and delegating to message service.</p>
     *
     * <p>Flow:</p>
     * <ol>
     *   <li>Extract message ID and retry count from headers.</li>
     *   <li>Validate payload and retry header; invalid records are discarded by the base consumer.</li>
     *   <li>Delegate to {@link MessageServiceImpl#processMessage(MessageDTO, long)} for domain logic.</li>
     *   <li>Return processing status message.</li>
     * </ol>
     *
     * @param messageDTO the deserialized message payload
     * @param message the original message with headers
     * @param ctx the processing context from base consumer
     * @return {@code Mono<String>} status message about the processing attempt
     */
    @Override
    protected Mono<String> execute(MessageDTO messageDTO, Message<String> message, Map<String, Object> ctx) {
        if (messageDTO == null || !StringUtils.hasText(messageDTO.getMessageId())
                || !StringUtils.hasText(messageDTO.getRecipientId())) {
            return Mono.error(new NonRetryableMessageException("Missing messageId or recipientId in message payload"));
        }
        String messageId = messageDTO.getMessageId();
        long retry = readRetryHeader(message);

        log.info("[MESSAGE-CORE-CONSUMER-SERVICE][EXECUTE] Received message with ID: {} and payload: {}", messageId, messageDTO);


        log.info("[MESSAGE-CORE-CONSUMER-SERVICE][EXECUTE] Processing attempt {} for message ID: {}", retry, messageId);
        // FIX Graceful Shutdown: rimosso .subscribe() fire-and-forget.
        // BaseKafkaConsumer attende il completamento di questo Mono<String> prima di fare il commit
        // dell'offset Kafka. Con .subscribe() + Mono.just("in progress"), l'offset veniva committed
        // immediatamente mentre la business logic girava in background, causando perdita di messaggi
        // e duplicati in caso di crash/shutdown. Ora la chain è integra: il commit avviene solo
        // dopo che processMessage() ha completato tutta la sua elaborazione.
        return messageCoreService.processMessage(messageDTO, retry)
                .thenReturn("[MESSAGE-CORE-CONSUMER-SERVICE][EXECUTE] Processing attempt %s for message %s completed".formatted(retry, messageId));
    }

}
