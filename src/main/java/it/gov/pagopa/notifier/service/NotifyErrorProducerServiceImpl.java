package it.gov.pagopa.notifier.service;


import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.event.producer.NotifyErrorProducer;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.gov.pagopa.notifier.constants.NotifierSenderConstants.MessageHeader.*;

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


    @Override
    public Mono<String> enqueueNotify(MessageDTO messageDTO, String messageUrl, String authenticationUrl, String entityId, long retry) {
        if (retry > maxTry) {
            log.info("[NOTIFIER-ERROR-COMMANDS] Message {} not retryable", messageDTO.getMessageId());
            return Mono.empty();
        }
        log.info("[NOTIFIER-ERROR-COMMANDS] Enqueuing message {} for retry attempt {}", messageDTO.getMessageId(), retry);
        return Mono.fromRunnable(() -> notifyErrorProducer.sendToNotifyErrorQueue(createMessage(messageDTO,messageUrl, authenticationUrl, entityId, retry)));
    }

    @NotNull
    private static Message<MessageDTO> createMessage(MessageDTO messageDTO, String messageUrl, String authenticationUrl, String entityId, long retry) {
        return MessageBuilder
                .withPayload(messageDTO)
                .setHeader(ERROR_MSG_HEADER_RETRY, retry)
                .setHeader(ERROR_MSG_AUTH_URL, authenticationUrl)
                .setHeader(ERROR_MSG_MESSAGE_URL, messageUrl)
                .setHeader(ERROR_MSG_ENTITY_ID, entityId)
                .build();

    }


}
