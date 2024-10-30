package it.gov.pagopa.notifier.service;

import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.event.producer.MessageProducer;
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
        QueueMessageProducerServiceImpl.class
})
 class QueueMessageProducerServiceTest {

    @Autowired
    QueueMessageProducerServiceImpl messageErrorProducerService;
    @MockBean
    MessageProducer messageErrorProducer;

    private final static MessageDTO messegeDTO = MessageDTOFaker.mockInstance();
    private final static String messegaUrl = "messegaUrl";
    private final static String authenticationUrl = "authenticationUrl";
    private final static long retry = 1;
    private final static String entityId = "entityId";
    @Test
    void sendError1_OK(){
        messageErrorProducerService.enqueueMessage(messegeDTO,messegaUrl,authenticationUrl,entityId);
        Mockito.verify(messageErrorProducer,times(1)).sendToMessageErrorQueue(any());
    }

    @Test
    void sendError2_OK(){
        messageErrorProducerService.enqueueMessage(messegeDTO,messegaUrl,authenticationUrl,entityId, retry);
        Mockito.verify(messageErrorProducer,times(1)).sendToMessageErrorQueue(any());
    }
}
