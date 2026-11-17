package it.gov.pagopa.common.reactive.kafka.exception;

/** Invalid input that must be logged and discarded instead of retried. */
public class NonRetryableMessageException extends UncommittableError {

    public NonRetryableMessageException(String message) {
        super(message);
    }
}
