package it.gov.pagopa.notifier.controller;


import it.gov.pagopa.notifier.dto.DeleteRequestDTO;
import it.gov.pagopa.notifier.dto.DeleteResponseDTO;
import it.gov.pagopa.notifier.dto.TppDTO;
import it.gov.pagopa.notifier.service.NotifyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Mono;

/**
 * <p>Reactive REST contract exposing message deletion operations.</p>
 *
 * <p>Validates inputs and delegates business logic to {@link NotifyService}.</p>
 *
 * <p>Error semantics and domain flows are documented in the service layer; controller focuses on HTTP contract.</p>
 */
@Tag(
    name = "Notifier Management", 
    description = "API per la gestione delle notifiche."
)
@RestController
@RequestMapping("/emd/notifier-sender")
public interface NotifierSenderController {

    /**
     * <p>Deletes messages in bulk based on provided criteria.</p>
     * <p>Delegates to {@link NotifyService#deleteMessages(DeleteRequestDTO)}.</p>
     * <p>Endpoint: {@code DELETE /emd/notifier-sender/messages/bulk-delete}</p>
     *
     * @param deleteRequestDTO the criteria for selecting messages to delete
     * @return {@code Mono<ResponseEntity<DeleteResponseDTO>>}
     *          <ul>
     *              <li>200 OK with deletion details if successful,</li>
     *              <li>404 Not Found if no messages match the criteria</li>
     *          </ul>
     */
    @Operation(
        summary = "Delete messages in bulk",
        description = "Delete messages based on provided criteria."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Messages deleted successfully",
            content = @Content(schema = @Schema(implementation = TppDTO.class))),
        @ApiResponse(responseCode = "400", description = "No messages match the criteria")
    })
    @DeleteMapping("/messages/bulk-delete")
    Mono<ResponseEntity<DeleteResponseDTO>> deleteMessages(@Valid @RequestBody DeleteRequestDTO deleteRequestDTO);

}
