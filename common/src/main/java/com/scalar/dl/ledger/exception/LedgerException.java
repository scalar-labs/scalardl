package com.scalar.dl.ledger.exception;

import com.scalar.dl.ledger.error.ScalarDlError;
import com.scalar.dl.ledger.service.StatusCode;

public class LedgerException extends RuntimeException {
  private final StatusCode code;

  public LedgerException(String message, StatusCode code) {
    super(message);
    this.code = code;
  }

  public LedgerException(String message, Throwable cause, StatusCode code) {
    super(message, cause);
    this.code = code;
  }

  public LedgerException(ScalarDlError error, Object... args) {
    this(error.buildMessage(args), error.getStatusCode());
  }

  public LedgerException(ScalarDlError error, Throwable cause, Object... args) {
    this(error.buildMessage(args), cause, error.getStatusCode());
  }

  public StatusCode getCode() {
    return code;
  }
}
