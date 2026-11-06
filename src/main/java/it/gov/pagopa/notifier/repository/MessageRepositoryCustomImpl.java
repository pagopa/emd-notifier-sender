package it.gov.pagopa.notifier.repository;

import it.gov.pagopa.notifier.enums.MessageState;
import it.gov.pagopa.notifier.model.Message;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import reactor.core.publisher.Mono;

public class MessageRepositoryCustomImpl implements MessageRepositoryCustom {

    private final ReactiveMongoTemplate mongoTemplate;

    public MessageRepositoryCustomImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<Message> claimErroredMessage(String messageId, String entityId) {
        Query query = Query.query(Criteria.where("messageId").is(messageId)
                .and("entityId").is(entityId)
                .and("messageState").is(MessageState.ERROR));
        Update update = new Update().set("messageState", MessageState.IN_PROCESS);

        return mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                Message.class);
    }

    @Override
    public Mono<Boolean> releaseManualRetryClaim(String messageId, String entityId) {
        Query query = Query.query(Criteria.where("messageId").is(messageId)
                .and("entityId").is(entityId)
                .and("messageState").is(MessageState.IN_PROCESS));
        Update update = new Update().set("messageState", MessageState.ERROR);

        return mongoTemplate.updateFirst(query, update, Message.class)
                .map(result -> result.getModifiedCount() == 1);
    }
}

