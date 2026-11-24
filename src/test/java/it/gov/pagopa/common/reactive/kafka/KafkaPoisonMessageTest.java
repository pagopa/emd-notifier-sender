package it.gov.pagopa.common.reactive.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.notifier.service.MessageCoreConsumerServiceImpl;
import it.gov.pagopa.notifier.service.MessageServiceImpl;
import it.gov.pagopa.notifier.service.NotifyErrorConsumerServiceImpl;
import it.gov.pagopa.notifier.service.NotifyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Timeout(5)
@ExtendWith(OutputCaptureExtension.class)
class KafkaPoisonMessageTest {

    private static final String VALID_PAYLOAD = "{\"messageId\":\"message-id\",\"recipientId\":\"recipient-id\"}";
    private MessageServiceImpl service;
    private MessageCoreConsumerServiceImpl consumer;

    @BeforeEach
    void setUp() {
        service = mock(MessageServiceImpl.class);
        consumer = new MessageCoreConsumerServiceImpl(new ObjectMapper(), "test", service);
    }

    static Stream<Object> validRetryHeaders() {
        return Stream.of(0L, 0, (short) 0, (byte) 0, "0", "0".getBytes(StandardCharsets.UTF_8));
    }

    @ParameterizedTest
    @MethodSource("validRetryHeaders")
    void acceptsSupportedRetryRepresentations(Object retry) {
        when(service.processMessage(any(), eq(0L))).thenReturn(Mono.empty());
        Acknowledgment ack = mock(Acknowledgment.class);

        consumer.execute(record(VALID_PAYLOAD, retry, 1, ack));

        verify(service).processMessage(any(), eq(0L));
        verify(ack).acknowledge();
    }

    static Stream<Object> invalidRetryHeaders() {
        return Stream.of(null, -1L, Long.MAX_VALUE, "invalid", "9223372036854775808", 1.5d,
                new byte[]{(byte) 0xff});
    }

    @ParameterizedTest
    @MethodSource("invalidRetryHeaders")
    void discardsInvalidRetryWithoutCallingBusiness(Object retry, CapturedOutput output) {
        Acknowledgment ack = mock(Acknowledgment.class);

        consumer.execute(record(VALID_PAYLOAD, retry, 10, ack));

        verifyNoInteractions(service);
        verify(ack).acknowledge();
        assertThat(output.getOut()).contains("[KAFKA_DISCARD]", "topic=source-topic", "group=source-group",
                "partition=2 offset=10", "no automatic recovery");
        assertThat(output.getOut()).doesNotContain("Processing failed; offset is not committed");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "null", "{}", "{\"messageId\":\"id\"}"})
    void discardsMalformedOrIncompletePayload(String payload) {
        Acknowledgment ack = mock(Acknowledgment.class);

        consumer.execute(record(payload, 0L, 1, ack));

        verifyNoInteractions(service);
        verify(ack).acknowledge();
    }

    static Stream<RuntimeException> deterministicFailures() {
        return Stream.of(new ClassCastException("invalid payload type"), new NullPointerException("invalid payload field"));
    }

    @ParameterizedTest
    @MethodSource("deterministicFailures")
    void discardsOnceAndCanProcessNextRecord(RuntimeException failure, CapturedOutput output) {
        when(service.processMessage(any(), eq(0L)))
                .thenReturn(Mono.error(failure))
                .thenReturn(Mono.empty());
        Acknowledgment poisonAck = mock(Acknowledgment.class);
        Acknowledgment nextAck = mock(Acknowledgment.class);

        consumer.execute(record(VALID_PAYLOAD, 0L, 1, poisonAck));
        verify(service, times(1)).processMessage(any(), eq(0L));
        verify(poisonAck).acknowledge();
        consumer.execute(record(VALID_PAYLOAD, 0L, 2, nextAck));

        verify(service, times(2)).processMessage(any(), eq(0L));
        verify(nextAck).acknowledge();
        assertThat(output.getOut()).contains("[KAFKA_DISCARD]", failure.getClass().getSimpleName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"message\":{}}",
            "{\"message\":{\"messageId\":\"id\"},\"tppDTO\":{\"entityId\":\"entity\"}}"})
    void discardsIncompleteNotificationBeforeDereferencing(String payload) {
        NotifyServiceImpl notifyService = mock(NotifyServiceImpl.class);
        NotifyErrorConsumerServiceImpl notifyConsumer = new NotifyErrorConsumerServiceImpl(new ObjectMapper(), notifyService, "test");
        Acknowledgment ack = mock(Acknowledgment.class);

        notifyConsumer.execute(record(payload, 0L, 1, ack));

        verifyNoInteractions(notifyService);
        verify(ack).acknowledge();
    }

    @Test
    void acknowledgmentFailureIsNotDiscardedOrRetried() {
        when(service.processMessage(any(), eq(0L))).thenReturn(Mono.empty());
        Acknowledgment ack = mock(Acknowledgment.class);
        doThrow(new NullPointerException("ack failure")).when(ack).acknowledge();

        assertThrows(NullPointerException.class, () -> consumer.execute(record(VALID_PAYLOAD, 0L, 1, ack)));

        verify(ack, times(1)).acknowledge();
        verify(service, times(1)).processMessage(any(), eq(0L));
    }

    private static Message<String> record(String payload, Object retry, long offset, Acknowledgment ack) {
        return MessageBuilder.withPayload(payload)
                .setHeader("retry", retry)
                .setHeader(KafkaHeaders.RECEIVED_TOPIC, "source-topic")
                .setHeader(KafkaHeaders.GROUP_ID, "source-group")
                .setHeader(KafkaHeaders.RECEIVED_PARTITION, 2)
                .setHeader(KafkaHeaders.OFFSET, offset)
                .setHeader(KafkaHeaders.ACKNOWLEDGMENT, ack)
                .build();
    }
}
