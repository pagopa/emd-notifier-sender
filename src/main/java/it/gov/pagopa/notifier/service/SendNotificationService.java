package it.gov.pagopa.notifier.service;


import it.gov.pagopa.notifier.dto.MessageDTO;
import reactor.core.publisher.Mono;

public interface SendNotificationService {
    Mono<Void> sendMessage(MessageDTO messageDTO, String messageUrl, String authenticationUrl, String entityId);
    Mono<Void> sendMessage(MessageDTO messageDTO, String messageUrl, String authenticationUrl, String entityId, long retry);

}
