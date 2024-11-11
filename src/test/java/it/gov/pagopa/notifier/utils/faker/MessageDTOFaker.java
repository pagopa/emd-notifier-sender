package it.gov.pagopa.notifier.utils.faker;



import it.gov.pagopa.notifier.dto.MessageDTO;

public class MessageDTOFaker {

    private MessageDTOFaker(){}
    public static MessageDTO mockInstance() {
        return MessageDTO.builder()
                .messageId("messageId")
                .messageUrl("messageUrl")
                .content("message")
                .triggerDateTime("date")
                .senderDescription("sender")
                .recipientId("recipientId")
                .originId("originId")
                .build();

    }

}
