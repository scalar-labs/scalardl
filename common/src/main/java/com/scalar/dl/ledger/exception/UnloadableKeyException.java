package com.scalar.dl.ledger.exception;

import com.scalar.dl.ledger.error.ScalarDlError;
import com.scalar.dl.ledger.service.StatusCode;

public class UnloadableKeyException extends KeyException {

  public UnloadableKeyException(String message) {
    super(message, StatusCode.UNLOADABLE_KEY);
  }

  public UnloadableKeyException(String message, Throwable cause) {
    super(message, cause, StatusCode.UNLOADABLE_KEY);
  }

  public UnloadableKeyException(ScalarDlError error, Object... args) {
    super(error.buildMessage(args), error.getStatusCode());
  }

  public UnloadableKeyException(ScalarDlError error, Throwable cause, Object... args) {
    super(error.buildMessage(args), cause, error.getStatusCode());
  }
}
