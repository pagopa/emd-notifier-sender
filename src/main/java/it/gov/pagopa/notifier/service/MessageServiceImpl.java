package it.gov.pagopa.notifier.service;

import it.gov.pagopa.notifier.connector.citizen.CitizenConnectorImpl;
import it.gov.pagopa.notifier.connector.tpp.TppConnectorImpl;
import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.dto.TppDTO;

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

    private final NotifyErrorProducerServiceImpl notifyErrorProducerService;
    private final NotifyServiceImpl sendNotificationService;

    public MessageServiceImpl(CitizenConnectorImpl citizenConnector,
                              TppConnectorImpl tppConnector,
                              MessageCoreProducerServiceImpl messageCoreProducerService, NotifyErrorProducerServiceImpl notifyErrorProducerService, NotifyServiceImpl sendNotificationService) {
        this.tppConnector = tppConnector;
        this.citizenConnector = citizenConnector;
        this.messageCoreProducerService = messageCoreProducerService;
        this.notifyErrorProducerService = notifyErrorProducerService;
        this.sendNotificationService = sendNotificationService;
    }


    @Override
    public Mono<Void> processMessage(MessageDTO messageDTO, long retry) {
        String messageId = messageDTO.getMessageId();
        log.info("[MESSAGE-SERVICE][PROCESS-MESSAGE] Start processing message ID: {} at retry attempt {}", messageId, retry);

        return citizenConnector.getCitizenConsentsEnabled(messageDTO.getRecipientId())
                .flatMap(tppIdList -> processTppList(tppIdList, messageDTO, retry))
                .onErrorResume(e -> handleCitizenError(e, messageDTO, retry));
    }

    private Mono<Void> processTppList(List<String> tppIdList, MessageDTO messageDTO, long retry) {
        String messageId = messageDTO.getMessageId();

        if (tppIdList.isEmpty()) {
            log.info("[MESSAGE-SERVICE][PROCESS-TPP-LIST] No consents found for message ID: {} at retry attempt {}", messageId, retry);
            return Mono.empty();
        }

        log.info("[MESSAGE-SERVICE][PROCESS-TPP-LIST] Consent list found for message ID: {} at retry attempt {}: {}", messageId, retry, tppIdList);

        return Flux.fromIterable(tppIdList)
                .flatMap(tppId ->
                        tppConnector.getTppEnabled(tppId)
                                .flatMap(tppDTO -> sendNotificationService.sendNotify(messageDTO, tppDTO, retry))
                                .onErrorResume(e -> handleTppError(e,messageDTO,tppId,retry).then())
                )
                .then();
    }

    private Mono<Void> handleCitizenError(Throwable e, MessageDTO messageDTO, long retry) {
        String messageId = messageDTO.getMessageId();
        log.error("[MESSAGE-SERVICE][HANDLE-CITIZEN-ERROR] Error processing message ID: {} at retry attempt {}. Error: {}", messageId, retry, e.getMessage(), e);
        log.info("[MESSAGE-SERVICE][HANDLE-CITIZEN-ERROR] Re-enqueuing message ID: {} with increased retry count: {}", messageId, retry + 1);
        messageCoreProducerService.enqueueMessage(messageDTO, retry + 1);
        return Mono.empty();
    }

    private Mono<TppDTO> handleTppError(Throwable e, MessageDTO messageDTO, String tppId, long retry) {
        String messageId = messageDTO.getMessageId();
        log.error("[MESSAGE-SERVICE][HANDLE-TPP-ERROR] Error getting TPP: {} for message ID: {} at retry attempt {}. Error: {}", tppId,messageId, retry, e.getMessage(), e);
        notifyErrorProducerService.enqueueNotify(messageDTO, tppId,retry + 1);
        return Mono.empty();
    }

}
