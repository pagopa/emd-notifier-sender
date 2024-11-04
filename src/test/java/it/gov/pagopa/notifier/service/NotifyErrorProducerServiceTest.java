package it.gov.pagopa.notifier.service;

import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.event.producer.NotifyErrorProducer;
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
        NotifyErrorProducerServiceImpl.class
})
 class NotifyErrorProducerServiceTest {

    @Autowired
    NotifyErrorProducerServiceImpl notifyErrorProducerService;
    @MockBean
    NotifyErrorProducer notifyErrorProducer;

    private final static MessageDTO messegeDTO = MessageDTOFaker.mockInstance();
    private final static String messegaUrl = "messegaUrl";
    private final static String authenticationUrl = "authenticationUrl";
    private final static long retry = 1;
    private final static String entityId = "entityId";

    @Test
    void enqueueNotify_OK(){
        notifyErrorProducerService.enqueueNotify(messegeDTO,messegaUrl,authenticationUrl,entityId, retry);
        Mockito.verify(notifyErrorProducer,times(1)).sendToNotifyErrorQueue(any());
    }
}
