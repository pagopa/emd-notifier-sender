package it.gov.pagopa.notifier.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@Builder
public class Message {
    private String messageId;
    private String hashedFiscalCode;
    private String triggerDateTime;
    private String senderDescription;
    private String messageUrl;
    private String originId;
    private String content;
    private LocalDateTime elaborationDateTime;
    private String entityId;
}
