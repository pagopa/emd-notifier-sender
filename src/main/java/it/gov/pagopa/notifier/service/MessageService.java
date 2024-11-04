package it.gov.pagopa.notifier.service;


import it.gov.pagopa.notifier.dto.MessageDTO;
import reactor.core.publisher.Mono;

public interface MessageService {

    Mono<Void> processMessage(MessageDTO messageDTO, long retry);
}
