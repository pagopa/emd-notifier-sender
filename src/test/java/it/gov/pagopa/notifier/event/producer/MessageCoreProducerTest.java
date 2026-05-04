package it.gov.pagopa.notifier.event.producer;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;

import static it.gov.pagopa.notifier.utils.TestUtils.QUEUE_MESSAGE_CORE;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class})
class MessageCoreProducerTest {

    @Mock
    private StreamBridge streamBridge;
    @InjectMocks
    private MessageCoreProducer messageErrorProducer;

    @Test
    void testStreamBridgeSendCalled() {
        messageErrorProducer.scheduleMessage(QUEUE_MESSAGE_CORE);

        verify(streamBridge, times(1)).send(eq("messageSender-out-0"), any(), eq(QUEUE_MESSAGE_CORE));

    }
}

