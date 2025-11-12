package it.gov.pagopa.notifier.service;


import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

/**
 * <p>Service for consuming and processing messages from the message core queue.</p>
 *
 * <p>Handles reactive message stream processing and delegates to domain logic.</p>
 */
public interface MessageCoreConsumerService {

    /**
     * <p>Processes a reactive stream of messages from the message broker.</p>
     *
     * @param messageFlux the reactive stream of messages to process
     */
    void execute(Flux<Message<String>> messageFlux);

}
