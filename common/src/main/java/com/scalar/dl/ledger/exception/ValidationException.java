package com.scalar.dl.ledger.exception;

import com.scalar.dl.ledger.error.ScalarDlError;
import com.scalar.dl.ledger.service.StatusCode;

public class ValidationException extends LedgerException {

  public ValidationException(String message, StatusCode code) {
    super(message, code);
  }

  public ValidationException(String message, Throwable cause, StatusCode code) {
    super(message, cause, code);
  }

  public ValidationException(ScalarDlError error, Object... args) {
    super(error.buildMessage(args), error.getStatusCode());
  }
}
