package com.scalar.dl.ledger.exception;

import com.scalar.dl.ledger.service.StatusCode;

public class KeyException extends SecurityException {

  public KeyException(String message, StatusCode code) {
    super(message, code);
  }

  public KeyException(String message, Throwable cause, StatusCode code) {
    super(message, cause, code);
  }
}
