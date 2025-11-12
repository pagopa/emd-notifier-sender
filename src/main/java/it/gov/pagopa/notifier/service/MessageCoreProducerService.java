package it.gov.pagopa.notifier.service;


import it.gov.pagopa.notifier.dto.MessageDTO;
import reactor.core.publisher.Mono;

/**
 * <p>Service for enqueuing messages to the message core broker for retry processing.</p>
 *
 * <p>Handles retry count validation and message resubmission after failures.</p>
 */
public interface MessageCoreProducerService {

     /**
      * <p>Enqueues a message for delayed retry after a failed processing attempt.</p>
      *
      * @param messageDTO the message to be enqueued
      * @param retry the current retry attempt count
      * @return {@code Mono<Void>} completes when the message is enqueued, or empty if max retries exceeded
      */
     Mono<Void> enqueueMessage(MessageDTO messageDTO, long retry);

}
