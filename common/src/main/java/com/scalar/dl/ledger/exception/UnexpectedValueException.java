package com.scalar.dl.ledger.exception;

import com.scalar.dl.ledger.service.StatusCode;

public class UnexpectedValueException extends ValidationException {
  private static final String DEFAULT_MESSAGE =
      "Unexpected record value is observed. There is a chance of a bug or tampering.";

  public UnexpectedValueException() {
    super(DEFAULT_MESSAGE, StatusCode.INCONSISTENT_STATES);
  }

  public UnexpectedValueException(Throwable cause) {
    super(DEFAULT_MESSAGE, cause, StatusCode.INCONSISTENT_STATES);
  }

  public UnexpectedValueException(String message, StatusCode code) {
    super(message, code);
  }

  public UnexpectedValueException(String message, Throwable cause, StatusCode code) {
    super(message, cause, code);
  }
}
