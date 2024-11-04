package it.gov.pagopa.notifier.service;


import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.event.producer.MessageCoreProducer;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import static it.gov.pagopa.notifier.constants.NotifierSenderConstants.MessageHeader.ERROR_MSG_HEADER_RETRY;

@Slf4j
@Service
public class MessageCoreProducerServiceImpl implements MessageCoreProducerService {

    private final MessageCoreProducer messageCoreProducer;

    public MessageCoreProducerServiceImpl(MessageCoreProducer messageCoreProducer){
        this.messageCoreProducer = messageCoreProducer;
    }

    @Override
    public void enqueueMessage(MessageDTO messageDTO, long retry) {
        Message<MessageDTO> message = createMessage(messageDTO,retry);
        messageCoreProducer.sendToMessageQueue(message);
    }


    @NotNull
    private static Message<MessageDTO> createMessage(MessageDTO messageDTO, long retry) {
        return MessageBuilder
                .withPayload(messageDTO)
                .setHeader(ERROR_MSG_HEADER_RETRY, retry)
                .build();

    }


}
