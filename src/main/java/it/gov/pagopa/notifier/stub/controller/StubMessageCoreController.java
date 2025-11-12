package it.gov.pagopa.notifier.stub.controller;


import it.gov.pagopa.notifier.dto.MessageDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Stub REST API for Message Core management.
 * <p>
 * This interface defines the endpoints for message retrieval
 * and is primarily used for testing and development purposes.
 * </p>
 */
@RequestMapping("stub/emd/message/")
public interface StubMessageCoreController {

    /**
     * Retrieves the list of messages for a given fiscal code and entity.
     *
     * @param fiscalCode the user's fiscal code
     * @param entityId the entity identifier
     * @return a Mono containing the ResponseEntity with the list of MessageDTO
     */
    @GetMapping("/get/{entityId}/{fiscalCode}")
    Mono<ResponseEntity<List<MessageDTO>>> getMessages(@Valid @PathVariable String fiscalCode,@PathVariable String entityId);
}
