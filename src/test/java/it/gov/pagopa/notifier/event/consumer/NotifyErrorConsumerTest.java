package it.gov.pagopa.notifier.event.consumer;

import it.gov.pagopa.notifier.service.NotifyErrorConsumerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

import static it.gov.pagopa.notifier.utils.TestUtils.QUEUE_NOTIFIER_STRING_ERROR;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotifyErrorConsumerTest {

    @Mock
    NotifyErrorConsumerService notifyErrorConsumerService;
    @InjectMocks
    NotifyErrorConsumer notifyErrorConsumer;
    private Consumer<Flux<Message<String>>> consumerCommands;
    @BeforeEach
    public void setUp(){
        consumerCommands = notifyErrorConsumer.consumerNotify(notifyErrorConsumerService);
    }


    @Test
    void consumerCommands(){
        Flux<Message<String>> flux = Flux.just(QUEUE_NOTIFIER_STRING_ERROR);
        consumerCommands.accept(flux);
        verify(notifyErrorConsumerService).execute(flux);
    }


}
