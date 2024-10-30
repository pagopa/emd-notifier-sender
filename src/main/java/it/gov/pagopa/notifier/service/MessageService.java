package it.gov.pagopa.notifier.service;


import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.model.Outcome;
import reactor.core.publisher.Mono;

public interface MessageService {

    Mono<Outcome> sendMessage(MessageDTO messageDTO);
}
