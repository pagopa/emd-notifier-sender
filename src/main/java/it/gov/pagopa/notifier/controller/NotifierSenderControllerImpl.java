package it.gov.pagopa.notifier.controller;

import it.gov.pagopa.notifier.dto.DeleteRequestDTO;
import it.gov.pagopa.notifier.dto.DeleteResponseDTO;
import it.gov.pagopa.notifier.service.NotifyServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class NotifierSenderControllerImpl implements NotifierSenderController {
    private final NotifyServiceImpl notifyService;

    public NotifierSenderControllerImpl(NotifyServiceImpl notifyService) {
        this.notifyService = notifyService;
    }

    /**
     *{@inheritDoc}
     */
    public Mono<ResponseEntity<DeleteResponseDTO>> deleteMessages(DeleteRequestDTO deleteRequestDTO) {
        return notifyService.deleteMessages(deleteRequestDTO)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
