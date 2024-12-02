package it.gov.pagopa.notifier.dto.mapper;

import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.model.Message;
import org.springframework.stereotype.Service;

@Service
public class MessageMapperObjectToDTO {

    public MessageDTO map(Message message){
        return MessageDTO.builder()
                .messageId(message.getMessageId())
                .recipientId(message.getRecipientId())
                .triggerDateTime(message.getTriggerDateTime())
                .senderDescription(message.getSenderDescription())
                .messageUrl(message.getMessageUrl())
                .originId(message.getOriginId())
                .content(message.getContent())
                .associatedPayment(message.getAssociatedPayment())
                .idPsp(message.getIdPsp())
                .channel(message.getChannel())
                .build();

    }
}
