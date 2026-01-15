package com.scalar.dl.ledger.exception;

import com.scalar.dl.ledger.error.ScalarDlError;
import com.scalar.dl.ledger.service.StatusCode;

public class MissingSecretException extends DatabaseException {

  public MissingSecretException(String message) {
    super(message, StatusCode.SECRET_NOT_FOUND);
  }

  public MissingSecretException(String message, Throwable cause) {
    super(message, cause, StatusCode.SECRET_NOT_FOUND);
  }

  public MissingSecretException(ScalarDlError error, Object... args) {
    super(error.buildMessage(args), error.getStatusCode());
  }
}
