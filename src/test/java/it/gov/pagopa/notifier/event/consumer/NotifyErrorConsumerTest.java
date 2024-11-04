package it.gov.pagopa.notifier.event.consumer;

import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.faker.MessageDTOFaker;
import it.gov.pagopa.notifier.service.NotifyErrorConsumerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

import static it.gov.pagopa.notifier.constants.NotifierSenderConstants.MessageHeader.*;
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
        MessageDTO messageDTO = MessageDTOFaker.mockInstance();
        String messageUrl = "messegaUrl";
        String authenticationUrl = "authenticationUrl";
        long retry = 1;
        Message<String> message = MessageBuilder
                .withPayload(messageDTO.toString())
                .setHeader(ERROR_MSG_HEADER_RETRY, retry)
                .setHeader(ERROR_MSG_AUTH_URL, authenticationUrl)
                .setHeader(ERROR_MSG_MESSAGE_URL, messageUrl)
                .build();
        Flux<Message<String>> flux = Flux.just(message);
        consumerCommands.accept(flux);
        verify(notifyErrorConsumerService).execute(flux);
    }


}
