package it.gov.pagopa.notifier.repository;



import it.gov.pagopa.notifier.model.Message;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface MessageRepository extends ReactiveMongoRepository<Message,String> {
    Flux<Message> findByRecipientIdAndEntityId(String recipientId, String entityId);

    Mono<Message> findByMessageIdAndEntityIdrAndRecipientId(String messageId, String entityId, String recipientId);

}
