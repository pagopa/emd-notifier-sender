package it.gov.pagopa.notifier.controller;


import it.gov.pagopa.notifier.dto.DeleteRequestDTO;
import it.gov.pagopa.notifier.dto.DeleteResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/emd/notifier-sender")
public interface NotifierSenderController {

    @DeleteMapping("/messages/bulk-delete")
    Mono<ResponseEntity<DeleteResponseDTO>> deleteMessages(@Valid @RequestBody DeleteRequestDTO deleteRequestDTO);

}
