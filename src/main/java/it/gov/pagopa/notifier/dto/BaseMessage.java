package it.gov.pagopa.notifier.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@AllArgsConstructor
@Data
@NoArgsConstructor
@SuperBuilder
public class BaseMessage {
    private String messageId;
    private String recipientId;
    private String triggerDateTime;
    private String senderDescription;
    private String messageUrl;
    private String originId;
    private String content;
    private Boolean associatedPayment;
    private String idPsp;

    public static BaseMessage extractBaseFields(MessageDTO messageDTO) {
        return BaseMessage.builder()
                .messageId(messageDTO.getMessageId())
                .recipientId(messageDTO.getRecipientId())
                .triggerDateTime(messageDTO.getTriggerDateTime())
                .senderDescription(messageDTO.getSenderDescription())
                .messageUrl(messageDTO.getMessageUrl())
                .originId(messageDTO.getOriginId())
                .content(messageDTO.getContent())
                .associatedPayment(messageDTO.getAssociatedPayment())
                .idPsp(messageDTO.getIdPsp())
                .build();
    }

}
