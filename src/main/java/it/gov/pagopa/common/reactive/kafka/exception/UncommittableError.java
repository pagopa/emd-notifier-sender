package it.gov.pagopa.common.reactive.kafka.exception;

/**
 * Exception representing uncommittable errors in Kafka processing.
 */
public class UncommittableError extends RuntimeException {
    public UncommittableError(String message){
        super(message);
    }

    public UncommittableError(String message, Exception e){
        super(message, e);
    }
}
