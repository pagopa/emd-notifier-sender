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
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class KafkaCommitAfterPersistenceTest {

    @Test
    void listenerWaitsForPersistenceBeforeAcknowledgingOrReadingNextRecord() throws Exception {
        Acknowledgment firstAck = mock(Acknowledgment.class);
        Acknowledgment secondAck = mock(Acknowledgment.class);
        Sinks.One<String> databaseWrite = Sinks.one();
        CountDownLatch firstStarted = new CountDownLatch(1);
        AtomicInteger secondStarted = new AtomicInteger();

        BaseKafkaConsumer<String, String> consumer = new BaseKafkaConsumer<>("test") {
            private final ObjectReader reader = new ObjectMapper().readerFor(String.class);

            @Override protected ObjectReader getObjectReader() { return reader; }
            @Override protected Consumer<Throwable> onDeserializationError(Message<String> message) {
                return error -> {};
            }
            @Override protected Mono<String> execute(String payload, Message<String> message, Map<String, Object> ctx) {
                if ("first".equals(payload)) {
                    firstStarted.countDown();
                    return databaseWrite.asMono();
                }
                secondStarted.incrementAndGet();
                return Mono.just("stored");
            }
        };

        CompletableFuture<Void> listener = CompletableFuture.runAsync(() -> {
            consumer.execute(message("first", 4477, firstAck));
            consumer.execute(message("second", 4478, secondAck));
        });
        assertTrue(firstStarted.await(2, TimeUnit.SECONDS));
        verifyNoInteractions(firstAck, secondAck);
        org.junit.jupiter.api.Assertions.assertEquals(0, secondStarted.get());

        databaseWrite.tryEmitValue("stored");
        listener.get(3, TimeUnit.SECONDS);
        verify(firstAck).acknowledge();
        verify(secondAck).acknowledge();
        org.junit.jupiter.api.Assertions.assertEquals(1, secondStarted.get());
    }

    @Test
    void doesNotCommitLaterOffsetWhileEarlierDatabaseWriteHasFailed() throws Exception {
        Acknowledgment firstAck = mock(Acknowledgment.class);
        Acknowledgment secondAck = mock(Acknowledgment.class);
        Sinks.One<String> databaseWrite = Sinks.one();
        CountDownLatch firstRetried = new CountDownLatch(1);
        AtomicInteger firstAttempts = new AtomicInteger();
        AtomicInteger secondStarted = new AtomicInteger();

        BaseKafkaConsumer<String, String> consumer = new BaseKafkaConsumer<>("test") {
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
                    secondStarted.incrementAndGet();
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

        };

        CompletableFuture<Void> listener = CompletableFuture.runAsync(() -> {
            consumer.execute(message("first", 1, firstAck));
            consumer.execute(message("second", 2, secondAck));
        });

        assertTrue(firstRetried.await(4, TimeUnit.SECONDS));
        verifyNoInteractions(firstAck, secondAck);
        org.junit.jupiter.api.Assertions.assertEquals(0, secondStarted.get());

        databaseWrite.tryEmitValue("stored");
        listener.get(3, TimeUnit.SECONDS);
        verify(firstAck).acknowledge();
        verify(secondAck).acknowledge();
        org.junit.jupiter.api.Assertions.assertEquals(1, secondStarted.get());
    }

    private static Message<String> message(String payload, long offset, Acknowledgment ack) {
        return MessageBuilder.withPayload("\"" + payload + "\"")
                .setHeader(KafkaHeaders.ACKNOWLEDGMENT, ack)
                .setHeader(KafkaHeaders.RECEIVED_PARTITION, 0)
                .setHeader(KafkaHeaders.OFFSET, offset)
                .build();
    }
}
