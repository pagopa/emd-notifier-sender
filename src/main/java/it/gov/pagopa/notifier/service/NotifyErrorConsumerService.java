package it.gov.pagopa.notifier.service;


import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

/**
 * <p>Service contract for consuming notification error messages from Kafka.</p>
 */
public interface NotifyErrorConsumerService {

    /**
     * <p>Consumes and processes a flux of error queue messages.</p>
     *
     * <p>Each message should contain a {@code NotifyErrorQueuePayload} with
     * the failed notification and TPP details, along with retry metadata in headers.</p>
     *
     * @param messageFlux the reactive stream of Kafka messages to process
     */
    void execute(Flux<Message<String>> messageFlux);

}
