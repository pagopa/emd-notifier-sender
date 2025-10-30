package it.gov.pagopa.notifier.service;



import it.gov.pagopa.notifier.dto.TppDTO;
import it.gov.pagopa.notifier.model.Message;
import reactor.core.publisher.Mono;

/**
 * <p>Service contract for enqueueing failed notification messages to the error queue.</p>
 */
public interface NotifyErrorProducerService {

     /**
      * <p>Enqueues a failed notification for retry if within retry limits.</p>
      *
      * @param message the notification message that failed
      * @param tppDTO the TPP configuration
      * @param retry the current retry attempt count (incremented after failure)
      * @return {@code Mono<String>} signaling completion, or empty if max retries exceeded
      */
     Mono<String> enqueueNotify(Message message, TppDTO tppDTO, long retry);
}
