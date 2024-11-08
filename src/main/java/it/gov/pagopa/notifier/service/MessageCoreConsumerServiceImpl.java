package it.gov.pagopa.notifier.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.common.reactive.kafka.consumer.BaseKafkaConsumer;
import it.gov.pagopa.notifier.dto.MessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static it.gov.pagopa.notifier.constants.NotifierSenderConstants.MessageHeader.*;


@Service
@Slf4j
public class MessageCoreConsumerServiceImpl extends BaseKafkaConsumer<MessageDTO,String> implements MessageCoreConsumerService {

    private final Duration commitDelay;
    private final Duration delayMinusCommit;
    private final ObjectReader objectReader;
    private final MessageServiceImpl messageCoreService;
    public MessageCoreConsumerServiceImpl(ObjectMapper objectMapper,
                                          @Value("${spring.application.name}") String applicationName,
                                          @Value("${spring.cloud.stream.kafka.bindings.consumerMessage-in-0.consumer.ackTime}") long commitMillis,
                                          @Value("${app.message-core.build-delay-duration}") String delayMinusCommit,
                                          MessageServiceImpl messageCoreService) {
        super(applicationName);
        this.commitDelay = Duration.ofMillis(commitMillis);
        this.messageCoreService = messageCoreService;
        Duration buildDelayDuration = Duration.parse(delayMinusCommit).minusMillis(commitMillis);
        Duration defaultDurationDelay = Duration.ofMillis(2L);
        this.delayMinusCommit = defaultDurationDelay.compareTo(buildDelayDuration) >= 0 ? defaultDurationDelay : buildDelayDuration;
        this.objectReader = objectMapper.readerFor(MessageDTO.class);
    }

    @Override
    protected Duration getCommitDelay() {
        return commitDelay;
    }

    @Override
    protected void subscribeAfterCommits(Flux<List<String>> afterCommits2subscribe) {
        afterCommits2subscribe
                .buffer(delayMinusCommit)
                .subscribe(r -> log.info("[MESSAGE-CORE-COMMANDS] Processed offsets committed successfully"));
    }

    @Override
    protected ObjectReader getObjectReader() {
        return objectReader;
    }

//    @Override
//    protected Consumer<Throwable> onDeserializationError(Message<String> message) {
//        return null;
//    }
//
//    @Override
//    protected void notifyError(Message<String> message, Throwable e) {
//
//    }

    @Override
    protected Mono<String> execute(MessageDTO messageDTO, Message<String> message, Map<String, Object> ctx) {
        log.info("[MESSAGE-CORE-COMMANDS] Queue message received: {}",message.getPayload());
        MessageHeaders headers = message.getHeaders();
        Long retry =  (Long) headers.get(ERROR_MSG_HEADER_RETRY);
        if(retry != null) {
            log.info("[MESSAGE-CORE-COMMANDS] Try {} for message {}",retry,messageDTO.getMessageId());
            messageCoreService.processMessage(messageDTO, retry)
                    .thenReturn("[MESSAGE-CORE-COMMANDS] Message %s processed successfully".formatted(messageDTO.getMessageId()));
        }
        return Mono.just("[MESSAGE-CORE-COMMANDS] Message %s not processed".formatted(messageDTO.getMessageId()));
    }

}
