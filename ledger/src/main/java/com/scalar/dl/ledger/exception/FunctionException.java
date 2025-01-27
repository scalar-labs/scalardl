package com.scalar.dl.ledger.exception;

import com.scalar.dl.ledger.service.StatusCode;

public class FunctionException extends LedgerException {

  public FunctionException(String message, StatusCode code) {
    super(message, code);
  }

  public FunctionException(String message, Throwable cause, StatusCode code) {
    super(message, cause, code);
  }
}
