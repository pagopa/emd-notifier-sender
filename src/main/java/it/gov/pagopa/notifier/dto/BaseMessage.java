package it.gov.pagopa.notifier.dto;

import it.gov.pagopa.notifier.enums.WorkflowType;
import it.gov.pagopa.notifier.model.Message;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;


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
    private String title;
    private String content;
    private Boolean associatedPayment;
    private String analogSchedulingDate;
    private WorkflowType workflowType;
    private String idPsp;

    public static BaseMessage extractBaseFields(Message messageDTO) {
        return BaseMessage.builder()
                .messageId(messageDTO.getMessageId())
                .recipientId(messageDTO.getRecipientId())
                .triggerDateTime(normalizeToLocalDateTimeFormat(messageDTO.getTriggerDateTime()))
                .senderDescription(messageDTO.getSenderDescription())
                .messageUrl(messageDTO.getMessageUrl())
                .originId(messageDTO.getOriginId())
                .title(messageDTO.getTitle())
                .content(messageDTO.getContent())
                .associatedPayment(messageDTO.getAssociatedPayment())
                .idPsp(messageDTO.getIdPsp())
                .analogSchedulingDate(messageDTO.getAnalogSchedulingDate() != null
                    ? messageDTO.getAnalogSchedulingDate()
                    : null)
                .workflowType(messageDTO.getWorkflowType())
                .build();
    }

    private static String normalizeToLocalDateTimeFormat(String inputDateTime) {
        try {
            OffsetDateTime odt = OffsetDateTime.parse(inputDateTime);
            LocalDateTime ldt = odt.toLocalDateTime();
            return ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        } catch (DateTimeParseException e) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(inputDateTime);
                return ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Formato data non valido: " + inputDateTime, ex);
            }
        }
    }


}
