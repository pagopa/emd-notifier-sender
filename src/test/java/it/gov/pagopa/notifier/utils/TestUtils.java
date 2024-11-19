package it.gov.pagopa.notifier.utils;

import it.gov.pagopa.notifier.dto.*;
import it.gov.pagopa.notifier.utils.faker.*;
import org.springframework.messaging.Message;


import java.util.List;

public class TestUtils {

    public TestUtils(){}

    public final static TppDTO TPP_DTO = TppDTOFaker.mockInstance();
    public final static List<TppDTO> TPP_DTO_LIST = List.of(TPP_DTO);
    public final static String TPP_ID = TPP_DTO.getTppId();
    public final static List<String> TPP_ID_STRING_LIST = List.of(TPP_ID);
    public final static TppIdList TPP_ID_LIST = new TppIdList(List.of(TPP_ID));
    public static final MessageDTO MESSAGE_DTO = MessageDTOFaker.mockInstance();
    public final static String FISCAL_CODE = MESSAGE_DTO.getRecipientId();
    public static Message<String> QUEUE_MESSAGE_NO_RETRY_CORE = MessageCoreQueueFaker.mockNoRetryInstance(MESSAGE_DTO);
    public static Message<String> QUEUE_NOTIFIER_NO_RETRY_ERROR = NotifierErrorQueueFaker.mockNoRetryInstance(MESSAGE_DTO);
    public static Message<String> QUEUE_MESSAGE_STRING_CORE = MessageCoreQueueFaker.mockStringInstance(MESSAGE_DTO);
    public static Message<String> QUEUE_NOTIFIER_STRING_ERROR = NotifierErrorQueueFaker.mockStringInstance(MESSAGE_DTO);
    public static Message<MessageDTO> QUEUE_MESSAGE_CORE = MessageCoreQueueFaker.mockInstance(MESSAGE_DTO);
    public static Message<NotifyErrorQueueMessageDTO> QUEUE_NOTIFIER_ERROR = NotifierErrorQueueFaker.mockInstance(MESSAGE_DTO, TPP_DTO);

    public static MessageDTO OUEUE_MESSAGE_BODY = new MessageDTO();

   // public static NotifyErrorQueueMessageDTO QUEUE_MESSAGE_BODY = new NotifyErrorQueueMessageDTO(MESSAGE_DTO, TPP_DTO);
    public static final String MESSAGE_URL = "/message";
    public static final String AUTHENTICATION_URL = "/auth";
    public static final String ENTITY_ID = "entity-id";
    public static final long RETRY = 1L;
    public static final long RETRY_KO = 10L;
    public static final String BEARER_TOKEN = "Bearer ";
    public static final TokenDTO TOKEN_DTO = TokenDTOFaker.mockInstance();
    public static final it.gov.pagopa.notifier.model.Message MESSAGE = MessageFaker.mockInstance();
}
