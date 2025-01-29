package com.scalar.dl.ledger.exception;

import com.scalar.dl.ledger.error.ScalarDlError;
import com.scalar.dl.ledger.service.StatusCode;

public class UnloadableFunctionException extends FunctionException {

  public UnloadableFunctionException(String message) {
    super(message, StatusCode.UNLOADABLE_FUNCTION);
  }

  public UnloadableFunctionException(String message, Throwable cause) {
    super(message, cause, StatusCode.UNLOADABLE_FUNCTION);
  }

  public UnloadableFunctionException(ScalarDlError error, Object... args) {
    super(error.buildMessage(args), error.getStatusCode());
  }

  public UnloadableFunctionException(ScalarDlError error, Throwable cause, Object... args) {
    super(error.buildMessage(args), cause, error.getStatusCode());
  }
}
