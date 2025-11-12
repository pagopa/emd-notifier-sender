package it.gov.pagopa.notifier.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.notifier.constants.NotifierSenderConstants;

/**
 * Exception thrown when an error occurs during TPP invocation.
 */
public class TppInvocationException extends ServiceException {
  public TppInvocationException() {
    this(NotifierSenderConstants.ExceptionMessage.GENERI_ERROR,true,null);
  }

  public TppInvocationException(String message, boolean printStackTrace, Throwable ex) {
    this(NotifierSenderConstants.ExceptionCode.GENERI_ERROR, message, printStackTrace, ex);
  }
  public TppInvocationException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message,null, printStackTrace, ex);
  }
}
