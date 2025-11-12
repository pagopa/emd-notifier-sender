package it.gov.pagopa.notifier.controller;

import it.gov.pagopa.notifier.dto.DeleteRequestDTO;
import it.gov.pagopa.notifier.dto.DeleteResponseDTO;
import it.gov.pagopa.notifier.service.NotifyServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * <p>Implementation of {@link NotifierSenderController}.</p>
 *
 * <p>Maps service results to HTTP status codes and response bodies.</p>
 */
@RestController
public class NotifierSenderControllerImpl implements NotifierSenderController {
    private final NotifyServiceImpl notifyService;

    public NotifierSenderControllerImpl(NotifyServiceImpl notifyService) {
        this.notifyService = notifyService;
    }

    /**
     * {@inheritDoc}
     *
     * @param deleteRequestDTO the criteria for selecting messages to delete
     * @return {@code Mono<ResponseEntity<DeleteResponseDTO>>}
     *         <ul>
     *             <li>200 OK with deletion details if successful,</li>
     *             <li>404 Not Found if no messages match the criteria</li>
     *         </ul>
     */
    public Mono<ResponseEntity<DeleteResponseDTO>> deleteMessages(DeleteRequestDTO deleteRequestDTO) {
        return notifyService.deleteMessages(deleteRequestDTO)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
