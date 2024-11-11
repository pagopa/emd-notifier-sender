package it.gov.pagopa.notifier.event.producer;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static it.gov.pagopa.notifier.utils.TestUtils.QUEUE_MESSAGE_CORE;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class})
class MessageCoreProducerTest {

    @Mock
    private StreamBridge streamBridge;
    @Mock
    private ScheduledExecutorService scheduler;
    @InjectMocks
    private MessageCoreProducer messageErrorProducer;
    @Test
     void testStreamBridgeSendCalled() throws Exception {
        ArgumentCaptor<Callable<Object>> runnableCaptor = ArgumentCaptor.forClass(Callable.class);
        when(scheduler.schedule(runnableCaptor.capture(), eq(5L), eq(TimeUnit.SECONDS))).thenReturn(null);

        messageErrorProducer.sendToMessageQueue(QUEUE_MESSAGE_CORE);

        Callable<Object> capturedRunnable = runnableCaptor.getValue();
        capturedRunnable.call();

        verify(scheduler).schedule(any(Callable.class), eq(5L), eq(TimeUnit.SECONDS));
        verify(streamBridge, times(1)).send(eq("messageSender-out-0"), any(), eq(QUEUE_MESSAGE_CORE));

    }
}

