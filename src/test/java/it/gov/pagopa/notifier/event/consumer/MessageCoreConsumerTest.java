package it.gov.pagopa.notifier.event.consumer;

import it.gov.pagopa.notifier.service.MessageCoreConsumerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

import static it.gov.pagopa.notifier.utils.TestUtils.QUEUE_MESSAGE_STRING_CORE;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MessageCoreConsumerTest {

    @Mock
    MessageCoreConsumerService messageCoreConsumerService;
    @InjectMocks
    MessageCoreConsumer messageCoreConsumer;
    private Consumer<Flux<Message<String>>> consumerCommands;
    @BeforeEach
    public void setUp(){
        consumerCommands = messageCoreConsumer.consumerMessage(messageCoreConsumerService);
    }



    @Test
    void consumerCommands(){
        Flux<Message<String>> flux = Flux.just(QUEUE_MESSAGE_STRING_CORE);
        consumerCommands.accept(flux);
        verify(messageCoreConsumerService).execute(flux);
    }


}
