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
        log.info("[EMD-NOTIFIER-SENDER][SEND]Received message: {}", messageDTO);

        return citizenConnector.getCitizenConsentsEnabled(messageDTO.getRecipientId())
                .flatMap(tppIdList -> {
                    if (tppIdList.isEmpty()) {
                        log.info("[EMD-NOTIFIER-SENDER][SEND]Citizen consent list is empty");
                        return Mono.empty();
                    }

                    log.info("[EMD-NOTIFIER-SENDER][SEND]Citizen consent list: {}", tppIdList);

                    return tppConnector.getTppsEnabled(new TppIdList(tppIdList))
                            .flatMap(tppList -> {
                                if (tppList.isEmpty()) {
                                    log.info("[EMD-NOTIFIER-SENDER][SEND]Channel list is empty");
                                    return Mono.empty();
                                }
                                log.info("[EMD-NOTIFIER-SENDER][SEND]Channel list: {}", tppList);
                                return sendNotifications(tppList, messageDTO);
                            })
                            .onErrorResume(e -> {
                                log.error("[EMD-NOTIFIER-SENDER][SEND]Error while getting Tpps info");
                                enqueueWithRetry(messageDTO, retry);
                                return Mono.empty();
                            });
                })
                .onErrorResume(e -> {
                    log.error("[EMD-NOTIFIER-SENDER][SEND]Error while getting Tpps id");
                    enqueueWithRetry(messageDTO, retry);
                    return Mono.empty();
                });
    }
    private Mono<Void> sendNotifications(List<TppDTO> tppDTOList, MessageDTO messageDTO) {
       return Flux.fromIterable(tppDTOList)
                .flatMap(tppDTO -> {
                    log.info("[EMD-NOTIFIER-SENDER][SEND]Prepare sending message to: {}", tppDTO.getTppId());
                    return sendNotificationService.sendNotification(messageDTO, tppDTO.getMessageUrl(), tppDTO.getAuthenticationUrl(), tppDTO.getEntityId(),0);
                })
                .then();
    }

    private void enqueueWithRetry(MessageDTO messageDTO, long retry) {
        log.info("[EMD-NOTIFIER-SENDER][RETRY] Enqueueing message for retry: {}, attempt: {}", messageDTO, retry + 1);
        messageCoreProducerService.enqueueMessage(messageDTO, retry + 1);
    }

}
