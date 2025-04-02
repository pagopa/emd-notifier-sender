package it.gov.pagopa.notifier.repository;


import it.gov.pagopa.notifier.model.Message;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MessageRepository extends ReactiveMongoRepository<Message,String> {
    Flux<Message> findByRecipientIdAndEntityId(String recipientId, String entityId);

    Mono<Message> findByMessageIdAndEntityId(String messageId, String entityId);

    Flux<Message> findByMessageRegistrationDateBetween(String startDate, String endDate);

}
