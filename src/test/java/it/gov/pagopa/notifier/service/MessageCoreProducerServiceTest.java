package it.gov.pagopa.notifier.service;

import it.gov.pagopa.notifier.event.producer.MessageCoreProducer;
import it.gov.pagopa.notifier.event.producer.NotifyDlqProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static it.gov.pagopa.notifier.utils.TestUtils.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = {
        MessageCoreProducerServiceImpl.class
})
@TestPropertySource(properties = {
        "app.retry.max-retry=5",
        "app.retry.initial-delay-seconds=0",
        "app.retry.max-delay-seconds=0"
})
 class MessageCoreProducerServiceTest {

    @Autowired
    MessageCoreProducerServiceImpl messageCoreProducerService;
    @MockBean
    MessageCoreProducer messageErrorProducer;
    @MockBean
    NotifyDlqProducer notifyDlqProducer;


    @Test
    void enqueueMessage_OK(){
        messageCoreProducerService.enqueueMessage(MESSAGE_DTO,RETRY).block();
        Mockito.verify(messageErrorProducer,times(1)).scheduleMessage(any());
    }

    @Test
    void enqueueMessage_K0_RoutesToDlq(){
        // Oltre i max retry: il messaggio NON viene re-inviato in coda, ma instradato alla DLQ.
        Mockito.when(notifyDlqProducer.sendMessageDtoToDlq(any())).thenReturn(true);

        messageCoreProducerService.enqueueMessage(MESSAGE_DTO,RETRY_KO).block();

        Mockito.verify(messageErrorProducer,times(0)).scheduleMessage(any());
        Mockito.verify(notifyDlqProducer,times(1)).sendMessageDtoToDlq(any());
    }


}
