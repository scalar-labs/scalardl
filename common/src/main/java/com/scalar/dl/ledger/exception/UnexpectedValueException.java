package com.scalar.dl.ledger.exception;

import com.scalar.dl.ledger.error.ScalarDlError;
import com.scalar.dl.ledger.service.StatusCode;

public class UnexpectedValueException extends ValidationException {

  public UnexpectedValueException(String message, StatusCode code) {
    super(message, code);
  }

  public UnexpectedValueException(String message, Throwable cause, StatusCode code) {
    super(message, cause, code);
  }

  public UnexpectedValueException(ScalarDlError error, Object... args) {
    super(error.buildMessage(args), error.getStatusCode());
  }

  public UnexpectedValueException(ScalarDlError error, Throwable cause, Object... args) {
    super(error.buildMessage(args), error.getStatusCode());
  }
}
