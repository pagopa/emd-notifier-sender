package it.gov.pagopa.notifier.service;


import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.dto.NotifyErrorQueuePayload;
import it.gov.pagopa.notifier.dto.TppDTO;
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
    public Mono<String> enqueueNotify(MessageDTO messageDTO, TppDTO tppDTO, long retry) {
        String messageId = messageDTO.getMessageId();
        String entityId = tppDTO.getEntityId();

        if (retry > maxTry) {
            log.info("[NOTIFY-ERROR-PRODUCER-SERVICE][ENQUEUE-NOTIFY] Message ID: {} for TPP: {} exceeds max retry attempts ({}). Not retryable.", messageId, entityId, maxTry);
            return Mono.empty();
        }

        log.info("[NOTIFY-ERROR-PRODUCER-SERVICE][ENQUEUE-NOTIFY] Enqueuing message ID: {} for TPP: {} with retry attempt: {}", messageId, entityId, retry);

        return Mono.fromRunnable(() -> {
            log.debug("[NOTIFY-ERROR-PRODUCER-SERVICE][ENQUEUE-NOTIFY] Sending message ID: {} for TPP: {} with retry: {} to notify error queue.", messageId, entityId, retry);
            notifyErrorProducer.scheduleMessage(createMessage(messageDTO, tppDTO, retry));
        });
    }

    @NotNull
    private static Message<NotifyErrorQueuePayload> createMessage(MessageDTO messageDTO, TppDTO tppDTO, long retry) {
        log.debug("[NOTIFY-ERROR-PRODUCER-SERVICE][CREATE-MESSAGE] Creating message for ID: {} with retry: {}, entityId: {}",
                messageDTO.getMessageId(), retry, tppDTO.getEntityId());

        return MessageBuilder
                .withPayload(new NotifyErrorQueuePayload(tppDTO,messageDTO))
                .setHeader(ERROR_MSG_HEADER_RETRY, retry)
                .build();
    }



}
