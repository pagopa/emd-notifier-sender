package it.gov.pagopa.notifier.service;

import it.gov.pagopa.notifier.event.producer.NotifyErrorProducer;
import it.gov.pagopa.notifier.repository.MessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import static it.gov.pagopa.notifier.utils.TestUtils.*;
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
    MessageRepository messageRepository;
    @MockBean
    NotifyErrorProducer notifyErrorProducer;

    @Test
    void enqueueNotify_OK(){
        notifyErrorProducerService.enqueueNotify(MESSAGE,TPP_DTO, RETRY).block();
        Mockito.verify(notifyErrorProducer,times(1)).scheduleMessage(any());
    }

    @Test
    void enqueueNotify_KO(){
        Mockito.when(messageRepository.save(any()))
                .thenReturn(Mono.just(MESSAGE));
        notifyErrorProducerService.enqueueNotify(MESSAGE,TPP_DTO, RETRY_KO).block();
        Mockito.verify(notifyErrorProducer,times(0)).scheduleMessage(any());
    }
}
