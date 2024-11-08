package it.gov.pagopa.notifier.service;


import it.gov.pagopa.notifier.dto.MessageDTO;
import reactor.core.publisher.Mono;

public interface NotifyErrorProducerService {

     Mono<String> enqueueNotify(MessageDTO messageDTO, String messageUrl, String authenticationUrl, String entityId, long retry);
}
