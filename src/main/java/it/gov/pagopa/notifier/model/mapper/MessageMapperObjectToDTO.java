package it.gov.pagopa.notifier.model.mapper;

import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.model.Message;
import org.springframework.stereotype.Service;

@Service
public class MessageMapperObjectToDTO {

    public MessageDTO map(Message message){
        return MessageDTO.builder()
                .recipientId(message.getRecipientId())
                .messageId(message.getMessageId())
                .senderDescription(message.getSenderDescription())
                .triggerDateTime(message.getTriggerDateTime())
                .messageUrl(message.getMessageUrl())
                .content(message.getContent())
                .originId(message.getOriginId())
                .associatedPayment(true)
                .build();

    }
}
