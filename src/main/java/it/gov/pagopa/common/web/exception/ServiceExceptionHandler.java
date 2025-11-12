package it.gov.pagopa.common.web.exception;


import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Handles ServiceException exceptions thrown during request processing.
 */
@RestControllerAdvice
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ServiceExceptionHandler {
  private final ErrorManager errorManager;
  private final Map<Class<? extends ServiceException>, HttpStatus> transcodeMap;

  public ServiceExceptionHandler(ErrorManager errorManager, Map<Class<? extends ServiceException>, HttpStatus> transcodeMap) {
    this.errorManager = errorManager;
    this.transcodeMap = transcodeMap;
  }


  /**
   * Handles ServiceException exceptions.
   *
   * @param error the ServiceException to handle
   * @param request the current HTTP request
   * @return a ResponseEntity representing the error response
   */
  @SuppressWarnings("squid:S1452")
  @ExceptionHandler(ServiceException.class)
  protected ResponseEntity<? extends ServiceExceptionPayload> handleException(ServiceException error, ServerHttpRequest request) {
    if (null != error.getPayload()) {
      return handleBodyProvidedException(error, request);
    }
    return errorManager.handleException(transcodeException(error), request);
  }

  /**
   *  Transcode a ServiceException into a ClientException based on the predefined mapping.
   *
   * @param error the ServiceException to transcode
   * @return the corresponding ClientException
   */
  private ClientException transcodeException(ServiceException error) {
    HttpStatus httpStatus = transcodeMap.get(error.getClass());

    if (httpStatus == null) {
      log.warn("Unhandled exception: {}", error.getClass().getName());
      httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    return new ClientExceptionWithBody(httpStatus, error.getCode(), error.getMessage(), error.isPrintStackTrace(), error);
  }

  /**
   * Handles ServiceException exceptions that provide a payload.
   *
   * @param error the ServiceException to handle
   * @param request the current HTTP request
   * @return a ResponseEntity representing the error response with payload
   */
  private ResponseEntity<? extends ServiceExceptionPayload> handleBodyProvidedException(ServiceException error, ServerHttpRequest request) {
    ClientException clientException = transcodeException(error);
    ErrorManager.logClientException(clientException, request);

    return ResponseEntity.status(clientException.getHttpStatus())
            .contentType(MediaType.APPLICATION_JSON)
            .body(error.getPayload());
  }
}