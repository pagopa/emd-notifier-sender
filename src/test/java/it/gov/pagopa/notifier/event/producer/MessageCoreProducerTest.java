package it.gov.pagopa.notifier.event.producer;


import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.faker.MessageDTOFaker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static it.gov.pagopa.notifier.constants.NotifierSenderConstants.MessageHeader.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class})
class MessageCoreProducerTest {

    @Mock
    private StreamBridge streamBridge;
    @Mock
    private ScheduledExecutorService scheduler;
    @InjectMocks
    private MessageCoreProducer messageErrorProducer;

    private static final MessageDTO MESSAGE_DTO = MessageDTOFaker.mockInstance();
    private static final long RETRY = 1;

    @Test
     void testStreamBridgeSendCalled() throws Exception {
        Message<MessageDTO> message = MessageBuilder
                .withPayload(MESSAGE_DTO)
                .setHeader(ERROR_MSG_HEADER_RETRY, RETRY)
                .build();

        ArgumentCaptor<Callable<Object>> runnableCaptor = ArgumentCaptor.forClass(Callable.class);
        when(scheduler.schedule(runnableCaptor.capture(), eq(5L), eq(TimeUnit.SECONDS))).thenReturn(null);

        messageErrorProducer.sendToMessageQueue(message);

        Callable<Object> capturedRunnable = runnableCaptor.getValue();
        capturedRunnable.call();

        verify(scheduler).schedule(any(Callable.class), eq(5L), eq(TimeUnit.SECONDS));
        verify(streamBridge, times(1)).send(eq("messageSender-out-0"), any(), eq(message));

    }
}

