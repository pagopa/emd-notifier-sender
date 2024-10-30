package it.gov.pagopa.notifier.service;


import it.gov.pagopa.notifier.dto.MessageDTO;
import reactor.core.publisher.Mono;

public interface QueueMessageProducerService {

     Mono<Void> enqueueMessage(MessageDTO messageDTO, String messageUrl, String authenticationUrl, String entityId);
     void enqueueMessage(MessageDTO messageDTO, String messageUrl, String authenticationUrl, String entityId, long retry);
}
