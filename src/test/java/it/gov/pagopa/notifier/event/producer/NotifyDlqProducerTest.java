package it.gov.pagopa.notifier.event.producer;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;

import static it.gov.pagopa.notifier.utils.TestUtils.QUEUE_MESSAGE_CORE;
import static it.gov.pagopa.notifier.utils.TestUtils.QUEUE_NOTIFIER_ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class})
class NotifyDlqProducerTest {

    @Mock
    private StreamBridge streamBridge;
    @InjectMocks
    private NotifyDlqProducer notifyDlqProducer;

    @Test
    void sendToDlq_callsStreamBridge() {
        when(streamBridge.send(eq("notifyDlq-out-0"), any(), eq(QUEUE_NOTIFIER_ERROR))).thenReturn(true);

        boolean result = notifyDlqProducer.sendToDlq(QUEUE_NOTIFIER_ERROR);

        verify(streamBridge, times(1)).send(eq("notifyDlq-out-0"), any(), eq(QUEUE_NOTIFIER_ERROR));
        org.junit.jupiter.api.Assertions.assertTrue(result);
    }

    @Test
    void sendMessageDtoToDlq_callsStreamBridge() {
        when(streamBridge.send(eq("notifyDlq-out-0"), any(), eq(QUEUE_MESSAGE_CORE))).thenReturn(true);

        boolean result = notifyDlqProducer.sendMessageDtoToDlq(QUEUE_MESSAGE_CORE);

        verify(streamBridge, times(1)).send(eq("notifyDlq-out-0"), any(), eq(QUEUE_MESSAGE_CORE));
        org.junit.jupiter.api.Assertions.assertTrue(result);
    }
}

