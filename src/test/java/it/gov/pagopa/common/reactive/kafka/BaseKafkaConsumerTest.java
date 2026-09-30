package it.gov.pagopa.common.reactive.kafka;

import it.gov.pagopa.common.kafka.utils.KafkaConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(OutputCaptureExtension.class)
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

    @Test
    void ackLogContainsQueueWaitAndProcessingTime(CapturedOutput output) {
        Acknowledgment ack = mock(Acknowledgment.class);
        long timestamp = System.currentTimeMillis() - 10_000;
        Message<String> message = MessageBuilder.fromMessage(createMessage("timed", ack))
                .setHeader(KafkaHeaders.RECEIVED_TOPIC, "source-topic")
                .setHeader(KafkaHeaders.GROUP_ID, "source-group")
                .setHeader(KafkaHeaders.RECEIVED_TIMESTAMP, timestamp)
                .build();

        testConsumer.execute(message);

        verify(ack).acknowledge();
        Matcher timing = Pattern.compile("queueWaitMs=(\\d+) ageAtAckMs=(\\d+) processingMs=(\\d+)")
                .matcher(output.getOut());
        assertThat(timing.find()).isTrue();
        assertThat(Long.parseLong(timing.group(1))).isGreaterThanOrEqualTo(10_000);
        assertThat(Long.parseLong(timing.group(2))).isGreaterThanOrEqualTo(Long.parseLong(timing.group(1)));
        assertThat(output.getOut()).contains("topic=source-topic group=source-group partition=0 offset=1");
    }

    @Test
    void missingAndFutureKafkaTimestampsAreUnknown(CapturedOutput output) {
        testConsumer.execute(createMessage("missing", mock(Acknowledgment.class)));
        testConsumer.execute(MessageBuilder.fromMessage(createMessage("future", mock(Acknowledgment.class)))
                .setHeader(KafkaHeaders.RECEIVED_TIMESTAMP, System.currentTimeMillis() + 60_000)
                .build());

        assertThat(output.getOut().split("queueWaitMs=unknown ageAtAckMs=unknown", -1)).hasSize(3);
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