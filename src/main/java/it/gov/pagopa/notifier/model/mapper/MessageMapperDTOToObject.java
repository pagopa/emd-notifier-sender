package it.gov.pagopa.notifier.model.mapper;

import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.enums.MessageState;
import it.gov.pagopa.notifier.model.Message;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MessageMapperDTOToObject {

    public Message map(MessageDTO messageDTO, String idPsp, String entityId, MessageState messageState){
        return Message.builder()
                .associatedPayment(messageDTO.getAssociatedPayment())
                .content(messageDTO.getContent())
                .title(messageDTO.getTitle())
                .content(messageDTO.getContent())
                .entityId(entityId)
                .idPsp(idPsp)
                .messageId(messageDTO.getMessageId())
                .messageUrl(messageDTO.getMessageUrl())
                .originId(messageDTO.getOriginId())
                .recipientId(messageDTO.getRecipientId())
                .senderDescription(messageDTO.getSenderDescription())
                .channel(messageDTO.getChannel())
                .triggerDateTime(messageDTO.getTriggerDateTime())
                .messageRegistrationDate(String.valueOf(LocalDateTime.now()))
                .messageState(messageState)
                .analogSchedulingDate(messageDTO.getAnalogSchedulingDate())
                .workflowType(messageDTO.getWorkflowType())
                .build();
    }
}
