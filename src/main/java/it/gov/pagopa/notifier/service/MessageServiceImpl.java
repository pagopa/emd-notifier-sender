package it.gov.pagopa.notifier.service;

import it.gov.pagopa.notifier.connector.citizen.CitizenConnectorImpl;
import it.gov.pagopa.notifier.connector.tpp.TppConnectorImpl;
import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.dto.TppDTO;
import it.gov.pagopa.notifier.dto.TppIdList;
import it.gov.pagopa.notifier.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;



@Slf4j
@Service
public class MessageServiceImpl implements MessageService {


    private final CitizenConnectorImpl citizenConnector;
    private final TppConnectorImpl tppConnector;
    private final MessageCoreProducerServiceImpl messageCoreProducerService;
    private final NotifyServiceImpl sendNotificationService;
    private final MessageRepository messageRepository;

    public MessageServiceImpl(CitizenConnectorImpl citizenConnector,
                              TppConnectorImpl tppConnector,
                              MessageCoreProducerServiceImpl messageCoreProducerService, NotifyServiceImpl sendNotificationService,
                              MessageRepository messageRepository) {
        this.tppConnector = tppConnector;
        this.citizenConnector = citizenConnector;
        this.messageCoreProducerService = messageCoreProducerService;
        this.sendNotificationService = sendNotificationService;
        this.messageRepository = messageRepository;
    }


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
                .flatMap(tppDTOList -> sendNotifications(tppDTOList, messageDTO, retry))
                .onErrorResume(e -> handleError(e, messageDTO, retry));
    }

    private Mono<Void> sendNotifications(List<TppDTO> tppDTOList, MessageDTO messageDTO, long retry) {
        String messageId = messageDTO.getMessageId();

        if (tppDTOList.isEmpty()) {
            log.info("[MESSAGE-SERVICE][SEND-NOTIFICATIONS] No channels available for message ID: {} at retry attempt {}", messageId, retry);
            return Mono.empty();
        }

        log.info("[MESSAGE-SERVICE][SEND-NOTIFICATIONS] Sending notifications for message ID: {} at retry attempt {} to channels: {}", messageId, retry, tppDTOList);

        return Flux.fromIterable(tppDTOList)
                .filterWhen(tppDTO -> messageRepository.findByMessageIdAndEntityId(messageId, tppDTO.getEntityId())
                        .hasElement()
                        .doOnNext(exists -> {
                            if (Boolean.TRUE.equals(exists)) {
                                log.info("[MESSAGE-SERVICE][SEND-NOTIFICATIONS] Found existing message ID: {} for entity ID: {}", messageId, tppDTO.getEntityId());
                            }
                        })
                        .map(exists -> !exists)
                )
                .flatMap(tppDTO -> {
                    log.info("[MESSAGE-SERVICE][SEND-NOTIFICATIONS] Sending message ID: {} at retry attempt {} to TPP: {}", messageId, retry, tppDTO.getTppId());
                    return sendNotificationService.sendNotify(messageDTO, tppDTO, 0);
                })
                .then();
    }

    private Mono<Void> handleError(Throwable e, MessageDTO messageDTO, long retry) {
        String messageId = messageDTO.getMessageId();
        log.error("[MESSAGE-SERVICE][HANDLE-ERROR] Error processing message ID: {} at retry attempt {}. Error: {}", messageId, retry, e.getMessage(), e);
        enqueueWithRetry(messageDTO, retry);
        return Mono.empty();
    }

    private void enqueueWithRetry(MessageDTO messageDTO, long retry) {
        String messageId = messageDTO.getMessageId();
        log.info("[MESSAGE-SERVICE][ENQUEUE-WITH-RETRY] Re-enqueuing message ID: {} with increased retry count: {}", messageId, retry + 1);
        messageCoreProducerService.enqueueMessage(messageDTO, retry + 1);
    }

}
