package it.gov.pagopa.notifier.service;

import it.gov.pagopa.notifier.connector.citizen.CitizenConnectorImpl;
import it.gov.pagopa.notifier.connector.tpp.TppConnectorImpl;
import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.dto.TppDTO;
import it.gov.pagopa.notifier.dto.TppIdList;
import it.gov.pagopa.notifier.enums.MessageState;
import it.gov.pagopa.notifier.model.Message;
import it.gov.pagopa.notifier.model.mapper.MessageMapperDTOToObject;
import it.gov.pagopa.notifier.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.util.List;



@Slf4j
@Service
public class MessageServiceImpl implements MessageService {


    private final CitizenConnectorImpl citizenConnector;
    private final TppConnectorImpl tppConnector;
    private final MessageCoreProducerServiceImpl messageCoreProducerService;
    private final NotifyServiceImpl sendNotificationService;
    private final MessageRepository messageRepository;
    private final MessageMapperDTOToObject mapperDTOToObject;
    private final String note;
    public MessageServiceImpl(CitizenConnectorImpl citizenConnector,
                              TppConnectorImpl tppConnector,
                              MessageCoreProducerServiceImpl messageCoreProducerService, NotifyServiceImpl sendNotificationService,
                              MessageRepository messageRepository,
                              MessageMapperDTOToObject mapperDTOToObject,
                              @Value("${message-notes}") String note) {
        this.tppConnector = tppConnector;
        this.citizenConnector = citizenConnector;
        this.messageCoreProducerService = messageCoreProducerService;
        this.sendNotificationService = sendNotificationService;
        this.messageRepository = messageRepository;
        this.mapperDTOToObject = mapperDTOToObject;
        this.note = note;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<Void> processMessage(MessageDTO messageDTO, long retry) {
        String messageId = messageDTO.getMessageId();
        log.info("[MESSAGE-SERVICE][PROCESS-MESSAGE] Start processing message ID: {} at retry attempt {}", messageId, retry);

        return citizenConnector.getCitizenConsentsEnabled(messageDTO.getRecipientId())
                .flatMap(tppIdList -> processTppList(tppIdList, messageDTO, retry))
                .onErrorResume(e -> handleError(e, messageDTO, retry));
    }

    private Mono<Void> processTppList(List<String> tppIdList, MessageDTO messageDTO, long retry) {
        String messageId = messageDTO.getMessageId();

        if (tppIdList.isEmpty()) {
            log.info("[MESSAGE-SERVICE][PROCESS-TPP-LIST] No consents found for message ID: {} at retry attempt {}", messageId, retry);
            return Mono.empty();
        }

        log.info("[MESSAGE-SERVICE][PROCESS-TPP-LIST] Consent list found for message ID: {} at retry attempt {}: {}", messageId, retry, tppIdList);

        return tppConnector.getTppsEnabled(new TppIdList(tppIdList))
                .flatMap(tppDTOList -> sendNotifications(tppDTOList, messageDTO, retry));

    }


    /**
     * Sends notifications to each one of the TPP on the list.
     *
     * @param tppDTOList List of TPPs to send notifications to.
     * @param messageDTO The message data transfer object.
     * @param retry      The current retry attempt count.
     * @return A Mono that completes when all notifications have been sent.
     */
    private Mono<Void> sendNotifications(List<TppDTO> tppDTOList, MessageDTO messageDTO, long retry) {
        String messageId = messageDTO.getMessageId();

        if (tppDTOList.isEmpty()) {
            log.info("[MESSAGE-SERVICE][SEND-NOTIFICATIONS] No channels available for message ID: {} at retry attempt {}", messageId, retry);
            return Mono.empty();
        }

        log.info("[MESSAGE-SERVICE][SEND-NOTIFICATIONS] Sending notifications for message ID: {} at retry attempt {}", messageId, retry);

        return Flux.fromIterable(tppDTOList)
                .flatMap(tppDTO -> {
                    Message message = mapperDTOToObject.map(messageDTO, tppDTO.getIdPsp(), tppDTO.getEntityId(), note, MessageState.IN_PROCESS);
                    return messageRepository.save(message)
                            .doOnNext(savedMessage -> log.info("[MESSAGE-SERVICE][SEND-NOTIFICATIONS] Saved IN-PROCESS message ID: {} for entity ID: {}", savedMessage.getMessageId(), savedMessage.getEntityId()))
                            .map(savedMessage -> Tuples.of(savedMessage, tppDTO))
                            .doOnError(e -> log.error("[MESSAGE-SERVICE][SEND-NOTIFICATIONS] Error saving message ID: {} for entity ID: {}. Error: {}", message.getMessageId(), tppDTO.getEntityId(), e.getMessage()))
                            .onErrorReturn(Tuples.of(Message.builder().id("REFUSE").messageId(messageDTO.getMessageId()).build(), tppDTO));
                })
                .flatMap(tuple -> {
                    Message savedMessage = tuple.getT1();
                    TppDTO tppDTO = tuple.getT2();
                    if(!savedMessage.getId().equals("REFUSE")){
                        log.info("[MESSAGE-SERVICE][SEND-NOTIFICATIONS] Sending message ID: {} at retry attempt {} to TPP: {}", savedMessage.getMessageId(), retry, tppDTO.getTppId());
                        return sendNotificationService.sendNotify(savedMessage, tppDTO, 0);
                    }
                    log.info("[MESSAGE-SERVICE][SEND-NOTIFICATIONS] Message ID: {} for entity ID: {}. Will not processed", savedMessage.getMessageId(), tppDTO.getEntityId());
                    return Mono.empty();
                })
                .then();
    }

    private Mono<Void> handleError(Throwable e, MessageDTO messageDTO, long retry) {
        String messageId = messageDTO.getMessageId();
        log.error("[MESSAGE-SERVICE][HANDLE-ERROR] Error processing message ID: {} at retry attempt {}. Error: {}", messageId, retry, e.getMessage(), e);
        log.info("[MESSAGE-SERVICE][ENQUEUE-WITH-RETRY] Re-enqueuing message ID: {} with increased retry count: {}", messageId, retry + 1);
        return messageCoreProducerService.enqueueMessage(messageDTO, retry + 1);
    }

}
