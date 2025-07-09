package it.gov.pagopa.notifier.utils.faker;

import it.gov.pagopa.notifier.enums.Channel;
import it.gov.pagopa.notifier.model.Message;

public class MessageFaker {
    public static Message mockInstance() {
        return Message.builder()
                .messageId("messageId")
                .messageUrl("messageUrl")
                .content("message")
                .triggerDateTime("2025-04-02T14:06:02")
                .senderDescription("sender")
                .recipientId("recipientId")
                .originId("originId")
                .entityId("entityId")
                .idPsp("idPsp")
                .associatedPayment(true)
                .messageRegistrationDate("messageRegistrationDate")
                .channel(Channel.valueOf("SEND"))
                .id("id")
                .build();

    }
}
