package it.gov.pagopa.notifier.utils.faker;

import it.gov.pagopa.notifier.dto.NotifyErrorQueuePayload;
import it.gov.pagopa.notifier.dto.TppDTO;

import it.gov.pagopa.notifier.model.Message;
import org.springframework.messaging.support.MessageBuilder;

import static it.gov.pagopa.notifier.constants.NotifierSenderConstants.MessageHeader.*;
import static it.gov.pagopa.notifier.utils.TestUtils.*;


public class NotifierErrorQueueFaker {


    public static org.springframework.messaging.Message<NotifyErrorQueuePayload> mockInstance(Message message, TppDTO tppDTO) {
        return MessageBuilder
                .withPayload(new NotifyErrorQueuePayload(tppDTO,message))
                .setHeader(ERROR_MSG_HEADER_RETRY, RETRY)
                .build();
    }

    public static org.springframework.messaging.Message<String> mockStringInstance(Message message, TppDTO tppDTO) {
        return MessageBuilder
                .withPayload(new NotifyErrorQueuePayload(tppDTO,message).toString())
                .setHeader(ERROR_MSG_HEADER_RETRY, RETRY)
                .build();
    }

    public static org.springframework.messaging.Message<String> mockNoRetryInstance(Message message, TppDTO tppDTO) {
        return MessageBuilder
                .withPayload(new NotifyErrorQueuePayload(tppDTO,message).toString())
                .build();
    }
}
