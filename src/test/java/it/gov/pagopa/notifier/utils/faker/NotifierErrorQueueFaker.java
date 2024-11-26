package it.gov.pagopa.notifier.utils.faker;

import it.gov.pagopa.notifier.dto.MessageDTO;
import it.gov.pagopa.notifier.dto.NotifyErrorQueuePayload;
import it.gov.pagopa.notifier.dto.TppDTO;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static it.gov.pagopa.notifier.constants.NotifierSenderConstants.MessageHeader.*;
import static it.gov.pagopa.notifier.utils.TestUtils.*;


public class NotifierErrorQueueFaker {


    public static Message<NotifyErrorQueuePayload> mockInstance(MessageDTO messageDTO, TppDTO tppDTO) {
        return MessageBuilder
                .withPayload(new NotifyErrorQueuePayload(tppDTO,messageDTO))
                .setHeader(ERROR_MSG_HEADER_RETRY, RETRY)
                .build();
    }

    public static Message<String> mockStringInstance(MessageDTO messageDTO, TppDTO tppDTO) {
        return MessageBuilder
                .withPayload(new NotifyErrorQueuePayload(tppDTO,messageDTO).toString())
                .setHeader(ERROR_MSG_HEADER_RETRY, RETRY)
                .build();
    }

    public static Message<String> mockNoRetryInstance(MessageDTO messageDTO, TppDTO tppDTO) {
        return MessageBuilder
                .withPayload(new NotifyErrorQueuePayload(tppDTO,messageDTO).toString())
                .build();
    }
}
