package it.gov.pagopa.notifier.model.mapper;

import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.model.Message;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MessageMapperDTOToObject {

    public Message map(MessageDTO messageDTO, String entityId){
        return Message.builder()
                .associatedPayment(messageDTO.getAssociatedPayment())
                .content(messageDTO.getContent())
                .notes(messageDTO.getNotes())
                .entityId(entityId)
                .idPsp(messageDTO.getIdPsp())
                .messageId(messageDTO.getMessageId())
                .messageUrl(messageDTO.getMessageUrl())
                .originId(messageDTO.getOriginId())
                .recipientId(messageDTO.getRecipientId())
                .senderDescription(messageDTO.getSenderDescription())
                .channel(messageDTO.getChannel())
                .triggerDateTime(messageDTO.getTriggerDateTime())
                .messageRegistrationDate(String.valueOf(LocalDateTime.now()))
                .build();
    }
}
