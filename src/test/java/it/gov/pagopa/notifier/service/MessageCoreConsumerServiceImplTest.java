package it.gov.pagopa.notifier.service;

import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.common.utils.MemoryAppender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import static it.gov.pagopa.notifier.utils.TestUtils.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = {
        MessageCoreConsumerServiceImpl.class,
        ObjectMapper.class
})
@TestPropertySource(properties = {
        "spring.application.name=test",
        "spring.cloud.stream.kafka.bindings.consumerMessage-in-0.consumer.ackTime=500",
        "app.message-core.build-delay-duration=PT1S",
        "app.message-core.max-messages=10"
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
        when(messageService.processMessage(any(),anyLong())).thenReturn(Mono.empty());
        messageConsumerServiceImpl.execute(MESSAGE_DTO,QUEUE_MESSAGE_STRING_CORE,null).block();
        Mockito.verify(messageService,times(1)).processMessage(MESSAGE_DTO,RETRY);

    }

    @Test
    void processCommand_Ko(){
        when(messageService.processMessage(any(), anyLong())).thenReturn(Mono.empty());
        messageConsumerServiceImpl.execute(MESSAGE_DTO, QUEUE_MESSAGE_NO_RETRY_CORE, null).block();
        Mockito.verify(messageService, times(0)).processMessage(MESSAGE_DTO, RETRY);
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
    void onDeserializationError(){
        Consumer<Throwable> result =  messageConsumerServiceImpl.onDeserializationError(QUEUE_NOTIFIER_STRING_ERROR);
        Assertions.assertNotNull(result);
    }

    @Test
    void givenMessagesWhenAfterCommitsThenSuccessfully() throws InterruptedException {


        Flux<List<String>> afterCommits2Subscribe = Flux.just(List.of("TEXT1", "TEXT2", "TEXT3"));
        messageConsumerServiceImpl.subscribeAfterCommits(afterCommits2Subscribe);

        // Wait for the subscription to complete
        Thread.sleep(2000); // Adjust the sleep duration as needed

        Assertions.assertEquals(
                "[MESSAGE-CORE-COMMANDS] Processed offsets committed successfully",
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );

    }

}
