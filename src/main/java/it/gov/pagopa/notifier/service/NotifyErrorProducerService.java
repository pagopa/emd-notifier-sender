package it.gov.pagopa.notifier.service;



import it.gov.pagopa.notifier.dto.TppDTO;
import it.gov.pagopa.notifier.model.Message;
import reactor.core.publisher.Mono;

public interface NotifyErrorProducerService {

     Mono<String> enqueueNotify(Message message, TppDTO tppDTO, long retry);
}
