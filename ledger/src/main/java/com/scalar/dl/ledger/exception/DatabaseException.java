package com.scalar.dl.ledger.exception;

import com.scalar.dl.ledger.error.ScalarDlError;
import com.scalar.dl.ledger.service.StatusCode;

public class DatabaseException extends LedgerException {

  public DatabaseException(String message, StatusCode code) {
    super(message, code);
  }

  public DatabaseException(String message, Throwable cause, StatusCode code) {
    super(message, cause, code);
  }

  public DatabaseException(ScalarDlError error, Object... args) {
    super(error.buildMessage(args), error.getStatusCode());
  }

  public DatabaseException(ScalarDlError error, Throwable cause, Object... args) {
    super(error.buildMessage(args), cause, error.getStatusCode());
  }
}
