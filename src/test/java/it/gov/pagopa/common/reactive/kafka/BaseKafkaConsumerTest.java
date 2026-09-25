package it.gov.pagopa.common.reactive.kafka;

import it.gov.pagopa.common.kafka.utils.KafkaConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class BaseKafkaConsumerTest {

    private TestKafkaConsumer testConsumer;

    @BeforeEach
    void setUp() {
        testConsumer = new TestKafkaConsumer("test-app");
    }

    @Test
    void testExecuteSuccess() {
        for (int i = 0; i < 3; i++) {
            Acknowledgment ack = mock(Acknowledgment.class);
            testConsumer.execute(createMessage("message" + i, ack));
            verify(ack).acknowledge();
        }
    }

    @Test
    void testDiscardForeignMessages() {
        Acknowledgment ack = mock(Acknowledgment.class);
        Message<String> foreignMessage = MessageBuilder.withPayload("\"foreign-message\"")
                .setHeader(KafkaConstants.ERROR_MSG_HEADER_APPLICATION_NAME, "other-app".getBytes(StandardCharsets.UTF_8))
                .setHeader(KafkaHeaders.RECEIVED_PARTITION, 0)
                .setHeader(KafkaHeaders.OFFSET, 1L)
                .setHeader(KafkaHeaders.ACKNOWLEDGMENT, ack)
                .build();

        testConsumer.execute(foreignMessage);
        verify(ack).acknowledge();
    }

    private Message<String> createMessage(String payload, Acknowledgment ack) {
        String jsonPayload = "\"" + payload + "\"";
        return MessageBuilder.withPayload(jsonPayload)
                .setHeader(KafkaHeaders.RECEIVED_PARTITION, 0)
                .setHeader(KafkaHeaders.OFFSET, 1L)
                .setHeader(KafkaConstants.ERROR_MSG_HEADER_APPLICATION_NAME, "test-app".getBytes(StandardCharsets.UTF_8))
                .setHeader(KafkaHeaders.ACKNOWLEDGMENT, ack)
                .build();
    }

}