package it.gov.pagopa.notifier.service;

import it.gov.pagopa.notifier.connector.citizen.CitizenConnectorImpl;
import it.gov.pagopa.notifier.connector.tpp.TppConnectorImpl;
import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.dto.TppDTO;
import it.gov.pagopa.notifier.dto.TppIdList;

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
    private final SendNotificationServiceImpl sendNotificationService;

    public MessageServiceImpl(CitizenConnectorImpl citizenConnector,
                              TppConnectorImpl tppConnector,
                              MessageCoreProducerServiceImpl messageCoreProducerService, SendNotificationServiceImpl sendNotificationService) {
        this.tppConnector = tppConnector;
        this.citizenConnector = citizenConnector;
        this.messageCoreProducerService = messageCoreProducerService;
        this.sendNotificationService = sendNotificationService;
    }


    @Override
    public Mono<Void> processMessage(MessageDTO messageDTO, long retry) {
        log.info("[EMD-NOTIFIER-SENDER][SEND] Received message: {}", messageDTO);

        return citizenConnector.getCitizenConsentsEnabled(messageDTO.getRecipientId())
                .flatMap(tppIdList ->processTppList(tppIdList,messageDTO,retry))
                .onErrorResume(e -> handleError(e, messageDTO, retry));
    }

    private Mono<Void> processTppList(List<String> tppIdList,MessageDTO messageDTO,long retry) {
        if (tppIdList.isEmpty()) {
            log.info("[EMD-NOTIFIER-SENDER][SEND] Citizen consent list is empty");
            return Mono.empty();
        }

        log.info("[EMD-NOTIFIER-SENDER][SEND] Citizen consent list: {}", tppIdList);

        return tppConnector.getTppsEnabled(new TppIdList(tppIdList))
                .flatMap(tppList -> sendNotifications(tppList, messageDTO))
                .onErrorResume(e -> handleError(e, messageDTO, retry));
    }

    private Mono<Void> sendNotifications(List<TppDTO> tppDTOList, MessageDTO messageDTO) {
        if (tppDTOList.isEmpty()) {
            log.info("[EMD-NOTIFIER-SENDER][SEND] Channel list is empty");
            return Mono.empty();
        }

        log.info("[EMD-NOTIFIER-SENDER][SEND] Channel list: {}", tppDTOList);

        return Flux.fromIterable(tppDTOList)
                .flatMap(tppDTO -> {
                    log.info("[EMD-NOTIFIER-SENDER][SEND] Preparing to send message to: {}", tppDTO.getTppId());
                    return sendNotificationService.sendNotification(messageDTO, tppDTO.getMessageUrl(), tppDTO.getAuthenticationUrl(), tppDTO.getEntityId(), 0);
                })
                .then();
    }

    private Mono<Void> handleError(Throwable e, MessageDTO messageDTO, long retry) {
        log.error("[EMD-NOTIFIER-SENDER][SEND] Error processing message {} . Retry attempt: {}", messageDTO.getMessageId(), retry , e);
        enqueueWithRetry(messageDTO, retry);
        return Mono.empty();
    }

    private void enqueueWithRetry(MessageDTO messageDTO, long retry) {
        log.info("[EMD-NOTIFIER-SENDER][RETRY] Enqueueing message {} . Retry attempt: {}", messageDTO, retry);
        messageCoreProducerService.enqueueMessage(messageDTO, retry + 1);
    }

}
