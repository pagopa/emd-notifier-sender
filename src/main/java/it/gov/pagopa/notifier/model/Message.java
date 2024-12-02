package it.gov.pagopa.notifier.model;

import it.gov.pagopa.notifier.enums.Channel;
import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class Message {

    private Boolean associatedPayment;
    private String content;
    private String entityId;
    private String idPsp;
    private String messageId;
    private String messageUrl;
    private String originId;
    private String recipientId;
    private String senderDescription;
    private Channel channel;
    private String triggerDateTime;
    private String messageRegistrationDate;
}
