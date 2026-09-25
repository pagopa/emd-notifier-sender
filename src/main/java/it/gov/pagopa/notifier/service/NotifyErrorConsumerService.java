package it.gov.pagopa.notifier.service;


import org.springframework.messaging.Message;

/**
 * <p>Service contract for consuming notification error messages from Kafka.</p>
 */
public interface NotifyErrorConsumerService {

    /**
     * <p>Consumes and processes one error queue message before the listener proceeds.</p>
     *
     * <p>Each message should contain a {@code NotifyErrorQueuePayload} with
     * the failed notification and TPP details, along with retry metadata in headers.</p>
     *
     * @param message the Kafka message to process
     */
    void execute(Message<String> message);

}
