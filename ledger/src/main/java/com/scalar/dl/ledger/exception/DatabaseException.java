package com.scalar.dl.ledger.exception;

import com.scalar.dl.ledger.service.StatusCode;

public class DatabaseException extends LedgerException {

  public DatabaseException(String message, StatusCode code) {
    super(message, code);
  }

  public DatabaseException(String message, Throwable cause, StatusCode code) {
    super(message, cause, code);
  }
}
