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

import static it.gov.pagopa.notifier.utils.TestUtils.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

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
    NotifyServiceImpl notificationService;
    @Autowired
    NotifyErrorConsumerServiceImpl notifyErrorConsumerService;
    private MemoryAppender memoryAppender;

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
    void processCommand_Ok(){
        when(notificationService.sendNotify(any(),any(),anyLong())).thenReturn(Mono.empty());
        notifyErrorConsumerService.execute(QUEUE_MESSAGE_BODY,QUEUE_NOTIFIER_STRING_ERROR,null).block();
        Mockito.verify(notificationService,times(1)).sendNotify(MESSAGE_DTO, TPP_DTO, RETRY);
    }

    @Test
    void processCommand_Ko(){
        when(notificationService.sendNotify(any(), any(),anyLong())).thenReturn(Mono.empty());
        notifyErrorConsumerService.execute(QUEUE_MESSAGE_BODY,QUEUE_NOTIFIER_NO_RETRY_ERROR,null).block();
        Mockito.verify(notificationService,times(0)).sendNotify(MESSAGE_DTO, TPP_DTO, RETRY);
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
