package it.gov.pagopa.notifier.utils;

import it.gov.pagopa.notifier.dto.*;
import it.gov.pagopa.notifier.utils.faker.*;
import org.springframework.messaging.Message;


import java.util.List;

public class TestUtils {

  public static final TppDTO TPP_DTO = TppDTOFaker.mockInstance();
    public static final List<TppDTO> TPP_DTO_LIST = List.of(TPP_DTO);
    public static final String TPP_ID = TPP_DTO.getTppId();
    public static final List<String> TPP_ID_STRING_LIST = List.of(TPP_ID);
    public static final TppIdList TPP_ID_LIST = new TppIdList(List.of(TPP_ID));
    public static final MessageDTO MESSAGE_DTO = MessageDTOFaker.mockInstance();
    public static final String FISCAL_CODE = MESSAGE_DTO.getRecipientId();
    public static final Message<String> QUEUE_MESSAGE_NO_RETRY_CORE = MessageCoreQueueFaker.mockNoRetryInstance(MESSAGE_DTO);
    public static final Message<String> QUEUE_NOTIFIER_NO_RETRY_ERROR = NotifierErrorQueueFaker.mockNoRetryInstance(MESSAGE_DTO,TPP_DTO);
    public static final Message<String> QUEUE_MESSAGE_STRING_CORE = MessageCoreQueueFaker.mockStringInstance(MESSAGE_DTO);
    public static final Message<String> QUEUE_NOTIFIER_STRING_ERROR = NotifierErrorQueueFaker.mockStringInstance(MESSAGE_DTO,TPP_DTO);
    public static final Message<MessageDTO> QUEUE_MESSAGE_CORE = MessageCoreQueueFaker.mockInstance(MESSAGE_DTO);
    public static final Message<NotifyErrorQueuePayload> QUEUE_NOTIFIER_ERROR = NotifierErrorQueueFaker.mockInstance(MESSAGE_DTO,TPP_DTO);

    public static final NotifyErrorQueuePayload NOTIFIER_ERROR_PAYLOAD = new NotifyErrorQueuePayload(TPP_DTO,MESSAGE_DTO);
    public static final String MESSAGE_URL = "/message";
    public static final String AUTHENTICATION_URL = "/auth";
    public static final long RETRY = 1L;
    public static final long RETRY_KO = 10L;
    public static final String BEARER_TOKEN = "Bearer ";
    public static final TokenDTO TOKEN_DTO = TokenDTOFaker.mockInstance();
    public static final it.gov.pagopa.notifier.model.Message MESSAGE = MessageFaker.mockInstance();

}
