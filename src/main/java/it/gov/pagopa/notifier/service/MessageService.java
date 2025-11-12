package it.gov.pagopa.notifier.service;


import it.gov.pagopa.notifier.dto.MessageDTO;
import reactor.core.publisher.Mono;

/**
 * <p>Service contract for processing notification messages and delivering them to authorized TPPs.</p>
 */
public interface MessageService {

    /**
     * <p>Processes a notification message by retrieving citizen consents and sending notifications to all authorized TPPs.</p>
     *
     * <p>The retry count is used for logging and determining retry behavior.</p>
     *
     * @param messageDTO the message containing recipient and notification details
     * @param retry the current retry attempt count (provided by the consumer)
     * @return {@code Mono<Void>} that completes when all notifications have been sent or processing fails
     */
    Mono<Void> processMessage(MessageDTO messageDTO, long retry);
}