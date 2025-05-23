package it.gov.pagopa.notifier.service;


import it.gov.pagopa.notifier.dto.MessageDTO;
import reactor.core.publisher.Mono;

public interface MessageCoreProducerService {

     Mono<Void> enqueueMessage(MessageDTO messageDTO, long retry);

}
