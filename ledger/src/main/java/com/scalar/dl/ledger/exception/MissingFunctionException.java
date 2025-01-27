package com.scalar.dl.ledger.exception;

import com.scalar.dl.ledger.service.StatusCode;

public class MissingFunctionException extends DatabaseException {

  public MissingFunctionException(String message) {
    super(message, StatusCode.FUNCTION_NOT_FOUND);
  }

  public MissingFunctionException(String message, Throwable cause) {
    super(message, cause, StatusCode.FUNCTION_NOT_FOUND);
  }
}
