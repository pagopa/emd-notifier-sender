package it.gov.pagopa.notifier.service;

import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.event.producer.MessageCoreProducer;
import it.gov.pagopa.notifier.faker.MessageDTOFaker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = {
        MessageCoreProducerServiceImpl.class
})
 class MessageCoreProducerServiceTest {

    @Autowired
    MessageCoreProducerService messageCoreProducerService;
    @MockBean
    MessageCoreProducer messageErrorProducer;

    private final static MessageDTO messegeDTO = MessageDTOFaker.mockInstance();
    private final static long retry = 1;

    @Test
    void enqueueMessage_OK(){
        messageCoreProducerService.enqueueMessage(messegeDTO,retry);
        Mockito.verify(messageErrorProducer,times(1)).sendToMessageQueue(any());
    }

}
