package it.gov.pagopa.notifier.service;

import it.gov.pagopa.notifier.event.producer.MessageCoreProducer;
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
        "app.retry.max-retry=5"
})
 class MessageCoreProducerServiceTest {

    @Autowired
    MessageCoreProducerServiceImpl messageCoreProducerService;
    @MockBean
    MessageCoreProducer messageErrorProducer;


    @Test
    void enqueueMessage_OK(){
        messageCoreProducerService.enqueueMessage(MESSAGE_DTO,RETRY).block();
        Mockito.verify(messageErrorProducer,times(1)).sendToMessageQueue(any());
    }

    @Test
    void enqueueMessage_K0(){
        messageCoreProducerService.enqueueMessage(MESSAGE_DTO,RETRY_KO).block();
        Mockito.verify(messageErrorProducer,times(0)).sendToMessageQueue(any());
    }


}
