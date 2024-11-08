package it.gov.pagopa.notifier.service;

import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.common.utils.MemoryAppender;
import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.faker.MessageDTOFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

import static it.gov.pagopa.notifier.constants.NotifierSenderConstants.MessageHeader.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = {
        NotifyErrorConsumerServiceImpl.class,
        ObjectMapper.class,
})
@TestPropertySource(properties = {
        "app.retry.max-retry=5",
        "spring.application.name=test",
        "spring.cloud.stream.kafka.bindings.consumerNotify-in-0.consumer.ackTime=500",
        "app.message-core.build-delay-duration=PT1S"
})
class NotifyErrorConsumerServiceImplTest {

    @MockBean
    SendNotificationServiceImpl notificationService;
    @Autowired
    NotifyErrorConsumerServiceImpl notifyErrorConsumerService;
    private MemoryAppender memoryAppender;

    private static final String MESSAGE_URL = "messageUrl";
    private static final String AUTHENTICATION_URL = "authenticationUrl";
    private static final String ENTITY_ID = "entityId";
    private static final long RETRY = 1L;
    private static final MessageDTO MESSAGE_DTO = MessageDTOFaker.mockInstance();
    @BeforeEach
    public void setup() {
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("it.gov.pagopa.notifier.service.NotifyErrorConsumerServiceImpl");
        memoryAppender = new MemoryAppender();
        memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.setLevel(ch.qos.logback.classic.Level.INFO);
        logger.addAppender(memoryAppender);
        memoryAppender.start();
    }
    @Test
    void processCommand_CallSendMessage(){
        Message<String> message = MessageBuilder
                .withPayload(MESSAGE_DTO.toString())
                .setHeader(ERROR_MSG_HEADER_RETRY, RETRY)
                .setHeader(ERROR_MSG_AUTH_URL, AUTHENTICATION_URL)
                .setHeader(ERROR_MSG_MESSAGE_URL, MESSAGE_URL)
                .setHeader(ERROR_MSG_ENTITY_ID, ENTITY_ID)
                .build();
        Mockito.when(notificationService.sendNotification(any(MessageDTO.class),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyLong()))
                .thenReturn(Mono.empty());
        notifyErrorConsumerService.execute(MESSAGE_DTO,message,null).block();
        Mockito.verify(notificationService,times(1)).sendNotification(MESSAGE_DTO, MESSAGE_URL, AUTHENTICATION_URL, ENTITY_ID, RETRY);
    }

    @Test
    void processCommand_NotRetryable(){
        Message<String> message = MessageBuilder
                .withPayload(MESSAGE_DTO.toString())
                .setHeader(ERROR_MSG_AUTH_URL, AUTHENTICATION_URL)
                .setHeader(ERROR_MSG_MESSAGE_URL, MESSAGE_URL)
                .setHeader(ERROR_MSG_ENTITY_ID, ENTITY_ID)
                .build();
        notifyErrorConsumerService.execute(MESSAGE_DTO,message,null).block();
        Mockito.verify(notificationService,times(0)).sendNotification(MESSAGE_DTO, MESSAGE_URL, AUTHENTICATION_URL, ENTITY_ID, RETRY);
    }

    @Test
    void getObjectReader() {
        ObjectReader objectReader = notifyErrorConsumerService.getObjectReader();
        Assertions.assertNotNull(objectReader);
    }

    @Test
    void getCommitDelay() {
        Duration expected = Duration.ofMillis(500L);
        Duration commitDelay = notifyErrorConsumerService.getCommitDelay();
        Assertions.assertEquals(expected,commitDelay);
    }

    @Test
    void givenMessagesWhenAfterCommitsThenSuccessfully() {
        Flux<List<String>> afterCommits2Subscribe = Flux.just(List.of("TEXT1","TEXT2","TEXT3"));
        notifyErrorConsumerService.subscribeAfterCommits(afterCommits2Subscribe);
        Assertions.assertEquals(
                ("[NOTIFIER-ERROR-COMMANDS] Processed offsets committed successfully"),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }
}
