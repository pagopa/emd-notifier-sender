package it.gov.pagopa.notifier.service;

import it.gov.pagopa.notifier.connector.citizen.CitizenConnectorImpl;
import it.gov.pagopa.notifier.connector.tpp.TppConnectorImpl;
import it.gov.pagopa.notifier.dto.CitizenConsentDTO;
import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.dto.TppDTO;
import it.gov.pagopa.notifier.dto.TppIdList;
import it.gov.pagopa.notifier.enums.OutcomeStatus;
import it.gov.pagopa.notifier.model.Outcome;
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
                .flatMap(citizenConsentDTOSList -> {
                    if (citizenConsentDTOSList.isEmpty()) {
                        log.info("[EMD-NOTIFIER-SENDER][SEND]Citizen consent list is empty");
                        return Mono.empty();
                    }

                    log.info("[EMD-NOTIFIER-SENDER][SEND]Citizen consent list: {}", citizenConsentDTOSList);

                    List<String> tppIds = citizenConsentDTOSList.stream()
                            .map(CitizenConsentDTO::getTppId)
                            .toList();

                    return tppConnector.getTppsEnabled(new TppIdList(tppIds))
                            .flatMap(tppList -> {
                                if (tppList.isEmpty()) {
                                    log.info("[EMD-NOTIFIER-SENDER][SEND]Channel list is empty");
                                    return Mono.empty();
                                }
                                log.info("[EMD-NOTIFIER-SENDER][SEND]Channel list: {}", tppList);
                                return sendNotifications(tppList, messageDTO);
                            })
                            .onErrorResume(e -> {
                                log.error("[EMD-NOTIFIER-SENDER][SEND]Error while sending message");
                                messageCoreProducerService.enqueueMessage(messageDTO,retry);
                                return Mono.empty();
                            });
                })
                .onErrorResume(e -> {
                    log.error("[EMD-NOTIFIER-SENDER][SEND]Error while sending message");
                    messageCoreProducerService.enqueueMessage(messageDTO,retry);
                    return Mono.empty();
                });
    }
    private Mono<Void> sendNotifications(List<TppDTO> tppDTOList, MessageDTO messageDTO) {
       return Flux.fromIterable(tppDTOList)
                .flatMap(tppDTO -> {
                    log.info("[EMD-NOTIFIER-SENDER][SEND]Prepare sending message to: {}", tppDTO.getTppId());
                    sendNotificationService.sendNotification(messageDTO, tppDTO.getMessageUrl(), tppDTO.getAuthenticationUrl(), tppDTO.getEntityId(),0);
                    return Mono.empty();
                })
                .then()
                .thenReturn(new Outcome(OutcomeStatus.OK)).then();
    }

}
