package it.gov.pagopa.notifier.service;


import org.springframework.messaging.Message;

/**
 * <p>Service for consuming and processing messages from the message core queue.</p>
 *
 * <p>Processes each Kafka record before returning to the listener.</p>
 */
public interface MessageCoreConsumerService {

    /**
     * <p>Processes one message from the broker.</p>
     *
     * @param message the message to process
     */
    void execute(Message<String> message);

}
