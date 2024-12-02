package it.gov.pagopa.notifier.utils.faker;



import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.enums.Channel;

public class MessageDTOFaker {
    public static MessageDTO mockInstance() {
        return MessageDTO.builder()
                .messageId("messageId")
                .recipientId("recipientId")
                .triggerDateTime("triggerDateTime")
                .senderDescription("sender")
                .messageUrl("messageUrl")
                .originId("originId")
                .content("message")
                .associatedPayment(true)
                .idPsp("originId")
                .channel(Channel.valueOf("SEND"))
                .build();

    }


}
