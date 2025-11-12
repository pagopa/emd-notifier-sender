package it.gov.pagopa.common.web.exception;

import java.io.Serializable;

/**
 * Marker interface for DTOs that can be used as exception payloads.
 * <p>
 * Implementing this interface indicates that a DTO is intended to be used
 * as the response body for service exceptions, ensuring serializability
 * and type safety across exception handlers.
 * </p>
 */
public interface ServiceExceptionPayload extends Serializable{

}
