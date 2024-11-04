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

import static it.gov.pagopa.notifier.constants.NotifierSenderConstants.MessageHeader.ERROR_MSG_HEADER_RETRY;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = {
        MessageCoreConsumerServiceImpl.class,
        ObjectMapper.class
})
@TestPropertySource(properties = {
        "app.retry.max-retry=5",
        "spring.application.name=test",
        "spring.cloud.stream.kafka.bindings.consumerMessage-in-0.consumer.ackTime=500",
        "app.message-core.build-delay-duration=PT1S"
})
class MessageCoreConsumerServiceImplTest {

    @MockBean
    MessageServiceImpl messageService;
    @Autowired
    MessageCoreConsumerServiceImpl messageConsumerServiceImpl;
    private MemoryAppender memoryAppender;

    @BeforeEach
    public void setup() {
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("it.gov.pagopa.notifier.service.MessageCoreConsumerServiceImpl");
        memoryAppender = new MemoryAppender();
        memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.setLevel(ch.qos.logback.classic.Level.INFO);
        logger.addAppender(memoryAppender);
        memoryAppender.start();
    }


    @Test
    void processCommand_Ok(){
        MessageDTO messageDTO = MessageDTOFaker.mockInstance();
        long retry = 1;
        Message<String> message = MessageBuilder
                .withPayload(messageDTO.toString())
                .setHeader(ERROR_MSG_HEADER_RETRY, retry)
                .build();
        retry +=1;
        when(messageService.processMessage(messageDTO,retry)).thenReturn(Mono.empty());
        messageConsumerServiceImpl.execute(messageDTO,message,null);
        Mockito.verify(messageService,times(1)).processMessage(messageDTO,retry);

    }

    @Test
    void processCommand_Ko(){
        MessageDTO messageDTO = MessageDTOFaker.mockInstance();
        long retry = 10;
        Message<String> message = MessageBuilder
                .withPayload(messageDTO.toString())
                .setHeader(ERROR_MSG_HEADER_RETRY, retry)
                .build();
        retry +=1;
        messageConsumerServiceImpl.execute(messageDTO,message,null);
        Mockito.verify(messageService,times(0)).processMessage(messageDTO,retry);

    }
    @Test
    void getObjectReader() {
        ObjectReader objectReader = messageConsumerServiceImpl.getObjectReader();
        Assertions.assertNotNull(objectReader);
    }
    @Test
    void getCommitDelay() {
        Duration expected = Duration.ofMillis(500L);
        Duration commitDelay = messageConsumerServiceImpl.getCommitDelay();
        Assertions.assertEquals(expected,commitDelay);
    }

    @Test
    void givenMessagesWhenAfterCommitsThenSuccessfully() {
        Flux<List<String>> afterCommits2Subscribe = Flux.just(List.of("TEXT1","TEXT2","TEXT3"));
        messageConsumerServiceImpl.subscribeAfterCommits(afterCommits2Subscribe);
        Assertions.assertEquals(
                ("[MESSAGE-CORE-COMMANDS] Processed offsets committed successfully"),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }
}
