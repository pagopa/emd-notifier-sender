package it.gov.pagopa.notifier.dto;

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
    private String content;
    private String notes;
    private Boolean associatedPayment;
    private String idPsp;

    public static BaseMessage extractBaseFields(Message messageDTO, String note) {
        return BaseMessage.builder()
                .messageId(messageDTO.getMessageId())
                .recipientId(messageDTO.getRecipientId())
                .triggerDateTime(normalizeToLocalDateTimeFormat(messageDTO.getTriggerDateTime()))
                .senderDescription(messageDTO.getSenderDescription())
                .messageUrl(messageDTO.getMessageUrl())
                .originId(messageDTO.getOriginId())
                .content(messageDTO.getContent())
                .associatedPayment(messageDTO.getAssociatedPayment())
                .idPsp(messageDTO.getIdPsp())
                .notes(messageDTO.getNotes() != null ? messageDTO.getNotes() : note)
                .build();
    }
    
    private static String normalizeToLocalDateTimeFormat(String inputDateTime) {
        try {
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(inputDateTime);
            LocalDateTime localDateTime = offsetDateTime.toLocalDateTime();

            return localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e1) {
            try {
                LocalDateTime localDateTime = LocalDateTime.parse(inputDateTime);
                return localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException e2) {
                throw new IllegalArgumentException("Formato data non riconosciuto: " + inputDateTime, e2);
            }
        }
    }


}
