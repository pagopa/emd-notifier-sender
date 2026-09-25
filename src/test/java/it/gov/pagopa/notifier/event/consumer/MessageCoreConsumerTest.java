package it.gov.pagopa.notifier.event.consumer;

import it.gov.pagopa.notifier.service.MessageCoreConsumerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

import static it.gov.pagopa.notifier.utils.TestUtils.QUEUE_MESSAGE_STRING_CORE;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MessageCoreConsumerTest {

    @Mock
    MessageCoreConsumerService messageCoreConsumerService;
    @InjectMocks
    MessageCoreConsumer messageCoreConsumer;
    private Consumer<Message<String>> consumerCommands;
    @BeforeEach
    public void setUp(){
        consumerCommands = messageCoreConsumer.consumerMessage(messageCoreConsumerService);
    }



    @Test
    void consumerCommands(){
        consumerCommands.accept(QUEUE_MESSAGE_STRING_CORE);
        verify(messageCoreConsumerService).execute(QUEUE_MESSAGE_STRING_CORE);
    }


}
