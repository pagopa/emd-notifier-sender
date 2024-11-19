package it.gov.pagopa.notifier.event.producer;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static it.gov.pagopa.notifier.utils.TestUtils.QUEUE_MESSAGE_CORE;
import static it.gov.pagopa.notifier.utils.TestUtils.QUEUE_NOTIFIER_ERROR;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class})
class NotifyErrorProducerTest {

    @Mock
    private StreamBridge streamBridge;
    @Mock
    private ScheduledExecutorService scheduler;
    @InjectMocks
    private NotifyErrorProducer notifyErrorProducer;
    @Captor
    private ArgumentCaptor<Callable<Object>> runnableCaptor;
    @Test
     void testStreamBridgeSendCalled() throws Exception {

        when(scheduler.schedule(runnableCaptor.capture(), eq(5L), eq(TimeUnit.SECONDS))).thenReturn(null);

        notifyErrorProducer.scheduleMessage(QUEUE_MESSAGE_CORE);

        Callable<Object> capturedRunnable = runnableCaptor.getValue();
        capturedRunnable.call();

        verify(scheduler).schedule(runnableCaptor.capture(), eq(5L), eq(TimeUnit.SECONDS));
        verify(streamBridge, times(1)).send(eq("notifySender-out-0"), any(), eq(QUEUE_NOTIFIER_ERROR));
    }
}

