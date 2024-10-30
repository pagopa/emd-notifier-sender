package it.gov.pagopa.notifier.stub.controller;

import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.stub.service.StubMessageCoreServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;


import java.util.List;


@RestController
@CrossOrigin(origins = "*")
public class StubMessageCoreControllerImpl implements StubMessageCoreController {

    private final StubMessageCoreServiceImpl stubMessageCoreService;

    public StubMessageCoreControllerImpl(StubMessageCoreServiceImpl stubMessageCoreService) {
        this.stubMessageCoreService = stubMessageCoreService;
    }

    @Override
    public Mono<ResponseEntity<List<MessageDTO>>> getMessages(String fiscalCode) {
        return stubMessageCoreService.getMessages(fiscalCode)
                .map(ResponseEntity::ok);
    }
}
