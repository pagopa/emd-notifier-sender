package it.gov.pagopa.notifier.repository;


import it.gov.pagopa.notifier.model.Message;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>Reactive repository interface for {@link Message} persistence operations.</p>
 *
 * <p>Extends {@link ReactiveMongoRepository} to provide standard CRUD operations
 * plus custom query methods for message retrieval and filtering.</p>
 *
 * <p>Collection name: {@code messages}</p>
 *
 */
public interface MessageRepository extends ReactiveMongoRepository<Message,String> {

    /**
     * <p>Finds all messages for a specific recipient and entity combination.</p>
     *
     * <p>Useful for retrieving all notifications sent to a citizen via a particular TPP.</p>
     *
     * @param recipientId the recipient's identifier (citizen fiscal code)
     * @param entityId the TPP entity identifier
     * @return {@code Flux<Message>} emitting matching messages (empty if none found)
     */
    Flux<Message> findByRecipientIdAndEntityId(String recipientId, String entityId);

    /**
     * <p>Finds a single message by message ID and entity ID.</p>
     *
     * <p>Since message IDs should be unique across entities, this typically returns
     * at most one result.</p>
     *
     * @param messageId the unique message identifier
     * @param entityId the TPP entity identifier
     * @return {@code Mono<Message>} emitting the message if found, empty otherwise
     */
    Mono<Message> findByMessageIdAndEntityId(String messageId, String entityId);

    /**
     * <p>Finds messages with registration dates within the specified range (inclusive).</p>
     *
     * <p>Used for batch deletion and retention policy enforcement.
     * Date strings should be in ISO-8601 format (e.g., "2025-10-30").</p>
     *
     * @param startDate the start date of the range (inclusive)
     * @param endDate the end date of the range (inclusive)
     * @return {@code Flux<Message>} emitting matching messages (empty if none found)
     */
    Flux<Message> findByMessageRegistrationDateBetween(String startDate, String endDate);

}
