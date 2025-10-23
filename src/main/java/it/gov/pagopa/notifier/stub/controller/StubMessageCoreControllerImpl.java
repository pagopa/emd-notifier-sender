package it.gov.pagopa.notifier.stub.controller;

import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.stub.service.StubMessageCoreServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;


import java.util.List;


/**
 * Stub controller implementation for message management.
 * <p>
 * This controller provides REST endpoints for message retrieval
 * and allows cross-origin requests from any origin for testing.
 * </p>
 */
@RestController
@CrossOrigin(origins = "*")
public class StubMessageCoreControllerImpl implements StubMessageCoreController {

    private final StubMessageCoreServiceImpl stubMessageCoreService;

    public StubMessageCoreControllerImpl(StubMessageCoreServiceImpl stubMessageCoreService) {
        this.stubMessageCoreService = stubMessageCoreService;
    }


    /**
     * {@inheritDoc}
     * <p>
     * Delegates message retrieval to the service layer and wraps
     * the result in a ResponseEntity with HTTP 200 OK status.
     * </p>
     */
    @Override
    public Mono<ResponseEntity<List<MessageDTO>>> getMessages(String fiscalCode, String entityId) {
        return stubMessageCoreService.getMessages(fiscalCode,entityId)
                .map(ResponseEntity::ok);
    }
}
