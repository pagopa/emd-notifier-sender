package it.gov.pagopa.notifier.utils.faker;

import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.dto.NotifyErrorQueueMessageDTO;
import it.gov.pagopa.notifier.dto.TppDTO;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static it.gov.pagopa.notifier.constants.NotifierSenderConstants.MessageHeader.*;
import static it.gov.pagopa.notifier.utils.TestUtils.*;


public class NotifierErrorQueueFaker {

    public  NotifierErrorQueueFaker(){}

    public static Message<NotifyErrorQueueMessageDTO> mockInstance(MessageDTO messageDTO, TppDTO tppDTO) {
        return MessageBuilder
                .withPayload(new NotifyErrorQueueMessageDTO(messageDTO,tppDTO))
                .setHeader(ERROR_MSG_HEADER_RETRY, RETRY)
                .build();
    }

    public static Message<String> mockStringInstance(MessageDTO messageDTO) {
        return MessageBuilder
                .withPayload(messageDTO.toString())
                .setHeader(ERROR_MSG_HEADER_RETRY, RETRY)
                .setHeader(ERROR_MSG_AUTH_URL, AUTHENTICATION_URL)
                .setHeader(ERROR_MSG_MESSAGE_URL, MESSAGE_URL)
                .setHeader(ERROR_MSG_ENTITY_ID,ENTITY_ID)
                .build();
    }

    public static Message<String> mockNoRetryInstance(MessageDTO messageDTO) {
        return MessageBuilder
                .withPayload(messageDTO.toString())
                .setHeader(ERROR_MSG_AUTH_URL, AUTHENTICATION_URL)
                .setHeader(ERROR_MSG_MESSAGE_URL, MESSAGE_URL)
                .setHeader(ERROR_MSG_ENTITY_ID,ENTITY_ID)
                .build();
    }
}
