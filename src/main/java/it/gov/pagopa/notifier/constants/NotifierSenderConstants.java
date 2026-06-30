package it.gov.pagopa.notifier.constants;

/**
 * Constants used in the Notifier Sender module.
 */
public class NotifierSenderConstants {

    /**
     * Constants for exception codes.
     */
    public static final class ExceptionCode {
        public static final String GENERI_ERROR = "GENERIC_ERROR";
        private ExceptionCode() {}
    }

    /**
     * Constants for exception messages.
     */
    public static final class ExceptionMessage {
        public static final String GENERI_ERROR = "GENERIC_ERROR";
        private ExceptionMessage() {}
    }

    /**
     * Constants for message headers for the {@link org.springframework.messaging.Message}.
     */
    public static final class MessageHeader {
        public static final String ERROR_MSG_AUTH_URL = "authenticationUrl";
        public static final String ERROR_MSG_MESSAGE_URL = "messageUrl";

        public static final String ERROR_MSG_HEADER_RETRY = "retry";
        public static final String ERROR_MSG_ENTITY_ID = "entityId";

        private MessageHeader() {}
    }

    private NotifierSenderConstants() {}
}
