package it.gov.pagopa.notifier.service;



import it.gov.pagopa.notifier.dto.DeleteRequestDTO;
import it.gov.pagopa.notifier.dto.DeleteResponseDTO;
import it.gov.pagopa.notifier.dto.TppDTO;
import it.gov.pagopa.notifier.model.Message;
import reactor.core.publisher.Mono;

/**
 * Service for sending notifications to TPPs.
 */
public interface NotifyService {

    /**
     * Sends a notification message to a TPP. <br>
     * On failure, the message is automatically re-enqueued with incremented retry count.
     *
     * @param message the message to be sent
     * @param tppDTO  the TPP configuration with authentication and endpoint details
     * @param retry   the current retry attempt count
     * @return a Mono that completes when notification is sent or re-enqueued
     */
    Mono<Void> sendNotify(Message message, TppDTO tppDTO, long retry);

    /**
     * Deletes messages in batches based on filter criteria. <br>
     * Processes deletions with configurable batch size and interval to manage database load.
     *
     * @param deleteRequestDTO the deletion request with date filters, batch size,
     *                         and interval settings
     * @return  a Mono deletion statistics including counts and elapsed time
     * @throws it.gov.pagopa.common.web.exception.ClientExceptionWithBody if no
     *         messages are found matching the criteria
     */
    Mono<DeleteResponseDTO> deleteMessages(DeleteRequestDTO deleteRequestDTO);
}