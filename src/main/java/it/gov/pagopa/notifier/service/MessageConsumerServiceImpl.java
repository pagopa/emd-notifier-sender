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
public class MessageConsumerServiceImpl extends BaseKafkaConsumer<MessageDTO,String> implements MessageConsumerService {

    private final Duration commitDelay;
    private final Duration delayMinusCommit;
    private final ObjectReader objectReader;
    private final SendNotificationServiceImpl sendMessageService;
    private final MessageServiceImpl messageCoreService;
    private final long maxRetry;
    public MessageConsumerServiceImpl(ObjectMapper objectMapper,
                                      SendNotificationServiceImpl sendMessageService,
                                      @Value("${app.retry.max-retry}") long maxRetry,
                                      @Value("${spring.application.name}") String applicationName,
                                      @Value("${spring.cloud.stream.kafka.bindings.consumerCommands-in-0.consumer.ackTime}") long commitMillis,
                                      @Value("${app.message-core.build-delay-duration}") String delayMinusCommit,
                                      MessageServiceImpl messageCoreService) {
        super(applicationName);
        this.commitDelay = Duration.ofMillis(commitMillis);
        this.messageCoreService = messageCoreService;
        Duration buildDelayDuration = Duration.parse(delayMinusCommit).minusMillis(commitMillis);
        Duration defaultDurationDelay = Duration.ofMillis(2L);
        this.delayMinusCommit = defaultDurationDelay.compareTo(buildDelayDuration) >= 0 ? defaultDurationDelay : buildDelayDuration;
        this.objectReader = objectMapper.readerFor(MessageDTO.class);
        this.maxRetry = maxRetry;
        this.sendMessageService = sendMessageService;
    }

    @Override
    protected Duration getCommitDelay() {
        return commitDelay;
    }

    @Override
    protected void subscribeAfterCommits(Flux<List<String>> afterCommits2subscribe) {
        afterCommits2subscribe
                .buffer(delayMinusCommit)
                .subscribe(r -> log.info("[NOTIFIER-SENDER-COMMANDS] Processed offsets committed successfully"));
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
        log.info("[EMD-PROCESS-COMMAND] Queue message received: {}",message.getPayload());
        MessageHeaders headers = message.getHeaders();
        long retry = getNextRetry(headers);
        if(retry == -1)
            messageCoreService.sendMessage(messageDTO)
                .subscribe();
        else if(retry!=0) {
            String messageUrl = (String) headers.get(ERROR_MSG_MESSAGE_URL);
            String authenticationUrl = (String) headers.get(ERROR_MSG_AUTH_URL);
            String entidyId = (String) headers.get(ERROR_MSG_ENTITY_ID);
            log.info("[EMD-PROCESS-COMMAND] Try {} for message {}",retry,messageDTO.getMessageId());
            sendMessageService.sendMessage(messageDTO, messageUrl, authenticationUrl, entidyId,retry)
                    .subscribe();

        }
        else
            log.info("[EMD-PROCESS-COMMAND] Message {} not retryable", messageDTO.getMessageId());
        return Mono.empty();
    }

    private long getNextRetry(MessageHeaders headers) {
        Long retry = (Long) headers.get(ERROR_MSG_HEADER_RETRY);
        if(retry == null)
            return -1;
        else if (retry >=0 && retry<maxRetry)
            return 1 + retry;
        else
            return 0;
    }

}
