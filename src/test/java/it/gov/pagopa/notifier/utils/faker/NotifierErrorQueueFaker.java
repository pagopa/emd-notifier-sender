package it.gov.pagopa.notifier.utils.faker;

import it.gov.pagopa.notifier.dto.MessageDTO;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static it.gov.pagopa.notifier.constants.NotifierSenderConstants.MessageHeader.*;
import static it.gov.pagopa.notifier.utils.TestUtils.*;


public class NotifierErrorQueueFaker {

    public  NotifierErrorQueueFaker(){}

    public static Message<MessageDTO> mockInstance(MessageDTO messageDTO) {
        return MessageBuilder
                .withPayload(messageDTO)
                .setHeader(ERROR_MSG_HEADER_RETRY, RETRY)
                .setHeader(ERROR_MSG_AUTH_URL, AUTHENTICATION_URL)
                .setHeader(ERROR_MSG_MESSAGE_URL, MESSAGE_URL)
                .setHeader(ERROR_MSG_ENTITY_ID,ENTITY_ID)
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
