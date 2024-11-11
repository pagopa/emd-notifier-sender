package it.gov.pagopa.notifier.service;

import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.event.producer.NotifyErrorProducer;
import it.gov.pagopa.notifier.utils.faker.MessageDTOFaker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = {
        NotifyErrorProducerServiceImpl.class
})
@TestPropertySource(properties = {
        "app.retry.max-retry=5"
})
 class NotifyErrorProducerServiceTest {

    @Autowired
    NotifyErrorProducerServiceImpl notifyErrorProducerService;
    @MockBean
    NotifyErrorProducer notifyErrorProducer;

    private final static MessageDTO MESSAGE_DTO = MessageDTOFaker.mockInstance();
    private final static String MESSAGE_URL = "messageUrl";
    private final static String AUTHENTICATION_URL = "authenticationUrl";
    private final static long RETRY = 1;
    private final static long RETRY_KO = 10;
    private final static String ENTITY_ID = "entityId";

    @Test
    void enqueueNotify_OK(){
        notifyErrorProducerService.enqueueNotify(MESSAGE_DTO,MESSAGE_URL,AUTHENTICATION_URL,ENTITY_ID, RETRY).block();
        Mockito.verify(notifyErrorProducer,times(1)).sendToNotifyErrorQueue(any());
    }

    @Test
    void enqueueNotify_KO(){
        notifyErrorProducerService.enqueueNotify(MESSAGE_DTO,MESSAGE_URL,AUTHENTICATION_URL,ENTITY_ID, RETRY_KO).block();
        Mockito.verify(notifyErrorProducer,times(0)).sendToNotifyErrorQueue(any());
    }
}
