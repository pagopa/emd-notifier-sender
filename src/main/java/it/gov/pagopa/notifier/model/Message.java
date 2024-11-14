package it.gov.pagopa.notifier.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@Builder
public class Message {
    private String messageId;
    private String recipientId;
    private String triggerDateTime;
    private String senderDescription;
    private String messageUrl;
    private String originId;
    private String content;
    private String entityId;
    private Boolean associatedPayment;
    private LocalDateTime elaborationDateTime;

}
