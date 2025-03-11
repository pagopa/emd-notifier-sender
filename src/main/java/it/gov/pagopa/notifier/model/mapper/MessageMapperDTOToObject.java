package it.gov.pagopa.notifier.model.mapper;

import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.model.Message;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MessageMapperDTOToObject {

    public Message map(MessageDTO messageDTO, String entityId, String notes){
        return Message.builder()
                .associatedPayment(messageDTO.getAssociatedPayment())
                .content(messageDTO.getContent())
                .notes(StringUtils.isNotEmpty(messageDTO.getNotes()) ? messageDTO.getNotes() : notes)
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
