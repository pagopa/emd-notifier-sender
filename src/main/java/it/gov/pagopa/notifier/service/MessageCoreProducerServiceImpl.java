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

import static it.gov.pagopa.notifier.constants.NotifierSenderConstants.MessageHeader.ERROR_MSG_HEADER_RETRY;

@Slf4j
@Service
public class MessageCoreProducerServiceImpl implements MessageCoreProducerService {

    private final MessageCoreProducer messageCoreProducer;
    private final Long maxTry;

    public MessageCoreProducerServiceImpl(MessageCoreProducer messageCoreProducer,
                                          @Value("${app.retry.max-retry}") long maxRetry){
        this.messageCoreProducer = messageCoreProducer;
        this.maxTry = maxRetry;
    }

    @Override
    public Mono<Void> enqueueMessage(MessageDTO messageDTO, long retry) {
        String messageId = messageDTO.getMessageId();

        if (retry > maxTry) {
            log.info("[MESSAGE-CORE-PRODUCER-SERVICE] Message ID: {} exceeds max retry attempts ({}). Not retryable.", messageId, maxTry);
            return Mono.empty();
        }

        log.info("[MESSAGE-CORE-PRODUCER-SERVICE] Enqueuing message ID: {} with retry attempt: {}", messageId, retry);

        return Mono.fromRunnable(() -> {
            log.debug("[MESSAGE-CORE-PRODUCER-SERVICE] Sending message ID: {} with retry attempt: {} to message queue.", messageId, retry);
            messageCoreProducer.sendToMessageQueue(createMessage(messageDTO, retry));
        });
    }

    @NotNull
    private static Message<MessageDTO> createMessage(MessageDTO messageDTO, long retry) {
        log.debug("[MESSAGE-CORE-PRODUCER-SERVICE] Creating message for ID: {} with retry attempt: {}", messageDTO.getMessageId(), retry);
        return MessageBuilder
                .withPayload(messageDTO)
                .setHeader(ERROR_MSG_HEADER_RETRY, retry)
                .build();
    }

}
