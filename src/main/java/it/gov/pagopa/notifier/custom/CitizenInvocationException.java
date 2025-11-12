package it.gov.pagopa.notifier.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.notifier.constants.NotifierSenderConstants.ExceptionMessage;

/**
 * Exception thrown when an error occurs during citizen invocation.
 */
public class CitizenInvocationException extends ServiceException {

  public CitizenInvocationException() {
    this(ExceptionMessage.GENERI_ERROR,true,null);
  }

  public CitizenInvocationException(String message, boolean printStackTrace, Throwable ex) {
    this(ExceptionMessage.GENERI_ERROR, message, printStackTrace, ex);
  }
  public CitizenInvocationException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message,null, printStackTrace, ex);
  }
}
