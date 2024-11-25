package it.gov.pagopa.common.reactive.kafka;

import it.gov.pagopa.common.kafka.utils.KafkaConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;

class BaseKafkaConsumerTest {

    private TestKafkaConsumer testConsumer;

    @BeforeEach
    void setUp() {
        testConsumer = new TestKafkaConsumer("test-app");
    }

    @Test
    void testExecuteSuccess() {

        Flux<Message<String>> messageFlux = Flux.just(
                createMessage("message1"),
                createMessage("message2"),
                createMessage("message3")
        );


        StepVerifier.create(Flux.defer(() -> {
                    testConsumer.execute(messageFlux);
                    return messageFlux.map(Message::getPayload);
                }))
                .expectNext("{\"message\": \"message1\"}", "{\"message\": \"message2\"}", "{\"message\": \"message3\"}")
                .verifyComplete();
    }

    @Test
    void testExecuteWithErrorMessage() {

        Flux<Message<String>> messageFlux = Flux.just(
                createMessage("message1"),
                createMessage("error"),
                createMessage("message2")
        );


        StepVerifier.create(Flux.defer(() -> {
                    testConsumer.execute(messageFlux);
                    return messageFlux.map(Message::getPayload);
                }))
                .expectNext("{\"message\": \"message1\"}", "{\"message\": \"error\"}", "{\"message\": \"message2\"}")
                .verifyComplete();
    }

    @Test
    void testDiscardForeignMessages() {

        Message<String> foreignMessage = MessageBuilder.withPayload("foreign-message")
                .setHeader(KafkaConstants.ERROR_MSG_HEADER_APPLICATION_NAME, "other-app".getBytes(StandardCharsets.UTF_8))
                .setHeader(KafkaHeaders.RECEIVED_PARTITION, 0)
                .setHeader(KafkaHeaders.OFFSET, 1L)
                .build();

        Flux<Message<String>> messageFlux = Flux.just(foreignMessage);

        StepVerifier.create(Flux.defer(() -> {
                    testConsumer.execute(messageFlux);
                    return messageFlux.map(Message::getPayload);
                }))
                .expectNext("foreign-message")
                .verifyComplete();
    }

    private Message<String> createMessage(String payload) {
        String jsonPayload = "{\"message\": \"" + payload + "\"}";
        return MessageBuilder.withPayload(jsonPayload)
                .setHeader(KafkaHeaders.RECEIVED_PARTITION, 0)
                .setHeader(KafkaHeaders.OFFSET, 1L)
                .setHeader(KafkaConstants.ERROR_MSG_HEADER_APPLICATION_NAME, "test-app".getBytes(StandardCharsets.UTF_8))
                .build();
    }

}