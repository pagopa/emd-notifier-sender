package it.gov.pagopa.common.configuration;

import com.mongodb.MongoCommandException;
import com.mongodb.ServerAddress;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonString;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicInteger;

class MongoRetrySpecsTest {

    private static MongoCommandException throttlingException() {
        BsonDocument response = new BsonDocument()
                .append("ok", new BsonInt32(0))
                .append("code", new BsonInt32(MongoRetrySpecs.COSMOS_DB_TOO_MANY_REQUESTS))
                .append("errmsg", new BsonString("TooManyRequests"));
        return new MongoCommandException(response, new ServerAddress());
    }

    @Test
    void isThrottled_detectsCosmos429() {
        Assertions.assertTrue(MongoRetrySpecs.isThrottled(throttlingException()));
    }

    @Test
    void isThrottled_detectsWrappedCause() {
        Assertions.assertTrue(MongoRetrySpecs.isThrottled(
                new RuntimeException("wrapper", throttlingException())));
    }

    @Test
    void isThrottled_falseForOtherErrors() {
        Assertions.assertFalse(MongoRetrySpecs.isThrottled(new RuntimeException("boom")));
        Assertions.assertFalse(MongoRetrySpecs.isThrottled(null));
    }

    @Test
    void cosmosDbThrottling_retriesThenSucceeds() {
        AtomicInteger attempts = new AtomicInteger();
        Mono<String> mono = Mono.defer(() -> {
            if (attempts.incrementAndGet() < 3) {
                return Mono.error(throttlingException());
            }
            return Mono.just("ok");
        }).retryWhen(MongoRetrySpecs.cosmosDbThrottling());

        StepVerifier.create(mono)
                .expectNext("ok")
                .verifyComplete();

        Assertions.assertEquals(3, attempts.get());
    }

    @Test
    void cosmosDbThrottling_doesNotRetryOtherErrors() {
        AtomicInteger attempts = new AtomicInteger();
        Mono<String> mono = Mono.<String>defer(() -> {
            attempts.incrementAndGet();
            return Mono.error(new IllegalStateException("non-throttling"));
        }).retryWhen(MongoRetrySpecs.cosmosDbThrottling());

        StepVerifier.create(mono)
                .verifyError();

        Assertions.assertEquals(1, attempts.get());
    }
}

