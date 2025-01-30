package com.scalar.dl.ledger.exception;

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

  public StatusCode getCode() {
    return code;
  }
}
