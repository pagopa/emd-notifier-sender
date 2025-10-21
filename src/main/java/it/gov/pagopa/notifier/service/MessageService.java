package it.gov.pagopa.notifier.service;


import it.gov.pagopa.notifier.dto.MessageDTO;
import reactor.core.publisher.Mono;

/**
 * Service for processing notification messages and delivering them to authorized TPPs.
 */
public interface MessageService {

    /**
     * Processes a notification message by retrieving the consents of the citizen of the message
     * and sending the notification to all authorized TPPs.
     * <p>
     * The retry count is used for logging and determining retry behavior.
     *
     * @param messageDTO the message containing recipient and notification details
     * @param retry the current retry attempt count (provided by the consumer)
     * @return a Mono that completes when all notifications have been sent or processing fails
     */
    Mono<Void> processMessage(MessageDTO messageDTO, long retry);
}