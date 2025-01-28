package com.scalar.dl.ledger.exception;

import com.scalar.dl.ledger.error.ScalarDlError;
import com.scalar.dl.ledger.service.StatusCode;

@SuppressWarnings("JavaLangClash")
public class SecurityException extends LedgerException {

  public SecurityException(String message, StatusCode code) {
    super(message, code);
  }

  public SecurityException(String message, Throwable cause, StatusCode code) {
    super(message, cause, code);
  }

  public SecurityException(ScalarDlError error, Throwable cause, Object... args) {
    super(error.buildMessage(args), cause, error.getStatusCode());
  }
}
