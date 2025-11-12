package it.gov.pagopa.notifier.service;



import it.gov.pagopa.notifier.dto.DeleteRequestDTO;
import it.gov.pagopa.notifier.dto.DeleteResponseDTO;
import it.gov.pagopa.notifier.dto.TppDTO;
import it.gov.pagopa.notifier.model.Message;
import reactor.core.publisher.Mono;

/**
 * <p>Service contract for sending notifications to TPPs and managing message lifecycle.</p>
 */
public interface NotifyService {

    /**
     * <p>Sends a notification message to a TPP.</p>
     *
     * @param message the message to be sent (must not be {@code null})
     * @param tppDTO the TPP configuration with authentication and endpoint details (must not be {@code null})
     * @param retry the current retry attempt count
     * @return {@code Mono<Void>} that completes when notification is sent or re-enqueued
     */
    Mono<Void> sendNotify(Message message, TppDTO tppDTO, long retry);

    /**
     * <p>Deletes messages in batches based on filter criteria.</p>
     *
     * <p>Processes deletions with configurable batch size and interval to manage database load.
     * Filters can specify date ranges; if omitted, all messages are considered.</p>
     *
     * <p>Batch size and interval can be overridden in the request or default to application properties.</p>
     *
     * @param deleteRequestDTO the deletion request with date filters, batch size, and interval settings (must not be {@code null})
     * @return {@code Mono<DeleteResponseDTO>} with deletion statistics including counts and elapsed time
     * @throws it.gov.pagopa.common.web.exception.ClientExceptionWithBody if no messages are found matching the criteria
     */
    Mono<DeleteResponseDTO> deleteMessages(DeleteRequestDTO deleteRequestDTO);
}