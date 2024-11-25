package it.gov.pagopa.notifier.service;


import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.dto.TppDTO;
import reactor.core.publisher.Mono;

public interface NotifyErrorProducerService {

     Mono<String> enqueueNotify(MessageDTO messageDTO, TppDTO tppDTO, long retry);
}
