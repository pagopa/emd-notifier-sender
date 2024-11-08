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

    private static final MessageDTO MESSAGE_DTO = MessageDTOFaker.mockInstance();
    private static final String MESSAGE_URL = "messageUrl";
    private static final String AUTHENTICATION_URL = "authenticationUrl";
    private static final long RETRY = 1;

    @Test
    void consumerCommands(){
        Message<String> message = MessageBuilder
                .withPayload(MESSAGE_DTO.toString())
                .setHeader(ERROR_MSG_HEADER_RETRY, RETRY)
                .setHeader(ERROR_MSG_AUTH_URL, AUTHENTICATION_URL)
                .setHeader(ERROR_MSG_MESSAGE_URL, MESSAGE_URL)
                .build();
        Flux<Message<String>> flux = Flux.just(message);
        consumerCommands.accept(flux);
        verify(notifyErrorConsumerService).execute(flux);
    }


}
