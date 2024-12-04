package it.gov.pagopa.notifier.service;

import it.gov.pagopa.notifier.connector.citizen.CitizenConnectorImpl;
import it.gov.pagopa.notifier.dto.MessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
public class MessageServiceImpl implements MessageService {


    private final CitizenConnectorImpl citizenConnector;
    private final MessageCoreProducerServiceImpl messageCoreProducerService;
    private final NotifyServiceImpl sendNotificationService;

    public MessageServiceImpl(CitizenConnectorImpl citizenConnector,
                              MessageCoreProducerServiceImpl messageCoreProducerService,
                              NotifyServiceImpl sendNotificationService) {
        this.citizenConnector = citizenConnector;
        this.messageCoreProducerService = messageCoreProducerService;
        this.sendNotificationService = sendNotificationService;
    }


    @Override
    public Mono<Void> processMessage(MessageDTO messageDTO, long retry) {
        String messageId = messageDTO.getMessageId();
        log.info("[MESSAGE-SERVICE][PROCESS-MESSAGE] Start processing message ID: {} at retry attempt {}", messageId, retry);

        return citizenConnector.getCitizenConsentsEnabled(messageDTO.getRecipientId())
                .flatMap(tppIdList -> processTppList(tppIdList, messageDTO, retry))
                .onErrorResume(e -> handleCitizenError(e, messageDTO, retry).then(Mono.empty()))
                .then();
    }

    private Mono<Void> processTppList(List<String> tppIdList, MessageDTO messageDTO, long retry) {
        String messageId = messageDTO.getMessageId();

        if (tppIdList.isEmpty()) {
            log.info("[MESSAGE-SERVICE][PROCESS-TPP-LIST] No consents found for message ID: {} at retry attempt {}", messageId, retry);
            return Mono.empty();
        }

        log.info("[MESSAGE-SERVICE][PROCESS-TPP-LIST] Consent list found for message ID: {} at retry attempt {}: {}", messageId, retry, tppIdList);

        return Flux.fromIterable(tppIdList)
                .flatMap(tppId -> sendNotificationService.sendNotify(messageDTO, tppId, retry))
                .then();
    }

    private Mono<Void> handleCitizenError(Throwable e, MessageDTO messageDTO, long retry) {
        String messageId = messageDTO.getMessageId();
        log.error("[MESSAGE-SERVICE][HANDLE-CITIZEN-ERROR] Error processing message ID: {} at retry attempt {}. Error: {}", messageId, retry, e.getMessage(), e);
        messageCoreProducerService.enqueueMessage(messageDTO, retry + 1);
        return Mono.empty();
    }

}
