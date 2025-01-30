package com.scalar.dl.ledger.exception;

import com.scalar.dl.ledger.service.StatusCode;

public class UnloadableKeyException extends KeyException {

  public UnloadableKeyException(String message) {
    super(message, StatusCode.UNLOADABLE_KEY);
  }

  public UnloadableKeyException(String message, Throwable cause) {
    super(message, cause, StatusCode.UNLOADABLE_KEY);
  }
}
