package it.gov.pagopa.notifier.utils.faker;

import it.gov.pagopa.notifier.dto.MessageDTO;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static it.gov.pagopa.notifier.constants.NotifierSenderConstants.MessageHeader.ERROR_MSG_HEADER_RETRY;
import static it.gov.pagopa.notifier.constants.NotifierSenderConstants.MessageHeader.ERROR_MSG_HEADER_TPP_ID;
import static it.gov.pagopa.notifier.utils.TestUtils.RETRY;


public class NotifierErrorQueueFaker {


    public static Message<MessageDTO> mockInstance(MessageDTO messageDTO, String tppId) {
        return MessageBuilder
                .withPayload(messageDTO)
                .setHeader(ERROR_MSG_HEADER_TPP_ID,tppId)
                .setHeader(ERROR_MSG_HEADER_RETRY, RETRY)
                .build();
    }

    public static Message<String> mockStringInstance(MessageDTO messageDTO, String tppId) {
        return MessageBuilder
                .withPayload(messageDTO.toString())
                .setHeader(ERROR_MSG_HEADER_TPP_ID,tppId)
                .setHeader(ERROR_MSG_HEADER_RETRY, RETRY)
                .build();
    }

    public static Message<String> mockNoRetryInstance(MessageDTO messageDTO, String tppId) {
        return MessageBuilder
                .withPayload(messageDTO.toString())
                .build();
    }
}
