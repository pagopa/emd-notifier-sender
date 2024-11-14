package it.gov.pagopa.notifier.dto.mapper;

import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.model.Message;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MessageMapperDTOToObject {

    public Message map(MessageDTO messageDTO, String entityId){
        return Message.builder()
                .messageId(messageDTO.getMessageId())
                .recipientId(messageDTO.getRecipientId())
                .triggerDateTime(messageDTO.getTriggerDateTime())
                .messageUrl(messageDTO.getMessageUrl())
                .content(messageDTO.getContent())
                .originId(messageDTO.getOriginId())
                .elaborationDateTime(LocalDateTime.now())
                .entityId(entityId)
                .associatedPayment(messageDTO.getAssociatedPayment())
                .build();
    }
}
