package it.gov.pagopa.notifier.event.producer;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;

import static it.gov.pagopa.notifier.utils.TestUtils.QUEUE_NOTIFIER_ERROR;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class})
class NotifyErrorProducerTest {

    @Mock
    private StreamBridge streamBridge;
    @InjectMocks
    private NotifyErrorProducer notifyErrorProducer;

    @Test
    void testStreamBridgeSendCalled() {
        notifyErrorProducer.scheduleMessage(QUEUE_NOTIFIER_ERROR);

        verify(streamBridge, times(1)).send(eq("notifySender-out-0"), any(), eq(QUEUE_NOTIFIER_ERROR));
    }
}

