package it.gov.pagopa.notifier.stub.service;

import it.gov.pagopa.notifier.dto.MessageDTO;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Stub service for message management.
 */
@Service
public interface StubMessageCoreService {

    /**
     * Retrieves messages for a given fiscal code and entity.
     *
     * @param fiscalCode the fiscal code
     * @param entityId the entity identifier
     * @return a Mono with the list of messages
     */
    Mono<List<MessageDTO>> getMessages(String fiscalCode,String entityId);


}
