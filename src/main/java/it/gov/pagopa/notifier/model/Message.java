package it.gov.pagopa.notifier.model;

import lombok.Builder;
import lombok.Data;


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
    private String idPsp;
    private Boolean associatedPayment;
}
