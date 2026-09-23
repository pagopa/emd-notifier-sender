package it.gov.pagopa.common.reactive.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.common.reactive.kafka.consumer.BaseKafkaConsumer;
import it.gov.pagopa.common.reactive.kafka.exception.UncommittableError;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class KafkaCommitAfterPersistenceTest {

    @Test
    void doesNotCommitLaterOffsetWhileEarlierDatabaseWriteHasFailed() throws InterruptedException {
        Acknowledgment firstAck = mock(Acknowledgment.class);
        Acknowledgment secondAck = mock(Acknowledgment.class);
        Sinks.One<String> databaseWrite = Sinks.one();
        CountDownLatch secondStarted = new CountDownLatch(1);
        CountDownLatch firstRetried = new CountDownLatch(1);
        CountDownLatch committed = new CountDownLatch(1);
        AtomicInteger firstAttempts = new AtomicInteger();

        BaseKafkaConsumer<String, String> consumer = new BaseKafkaConsumer<>(
                "test", Duration.ofMillis(20), Duration.ZERO, 2) {
            private final ObjectReader reader = new ObjectMapper().readerFor(String.class);

            @Override
            protected ObjectReader getObjectReader() {
                return reader;
            }

            @Override
            protected Consumer<Throwable> onDeserializationError(Message<String> message) {
                return error -> {};
            }

            @Override
            protected Mono<String> execute(String payload, Message<String> message, Map<String, Object> ctx) {
                if ("second".equals(payload)) {
                    secondStarted.countDown();
                    return Mono.just("stored");
                }
                return Mono.defer(() -> {
                    if (firstAttempts.incrementAndGet() == 1) {
                        return Mono.error(new UncommittableError("Insert failed"));
                    }
                    firstRetried.countDown();
                    return databaseWrite.asMono();
                });
            }

            @Override
            protected void subscribeAfterCommits(Flux<List<String>> results) {
                results.subscribe(ignored -> committed.countDown());
            }
        };

        consumer.execute(Flux.just(message("first", 1, firstAck), message("second", 2, secondAck)));

        assertTrue(secondStarted.await(2, TimeUnit.SECONDS));
        assertTrue(firstRetried.await(4, TimeUnit.SECONDS));
        verifyNoInteractions(firstAck, secondAck);

        databaseWrite.tryEmitValue("stored");
        assertTrue(committed.await(3, TimeUnit.SECONDS));
        verify(secondAck).acknowledge();
    }

    private static Message<String> message(String payload, long offset, Acknowledgment ack) {
        return MessageBuilder.withPayload("\"" + payload + "\"")
                .setHeader(KafkaHeaders.ACKNOWLEDGMENT, ack)
                .setHeader(KafkaHeaders.RECEIVED_PARTITION, 0)
                .setHeader(KafkaHeaders.OFFSET, offset)
                .build();
    }
}
