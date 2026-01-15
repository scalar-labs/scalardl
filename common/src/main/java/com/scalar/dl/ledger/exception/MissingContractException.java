package com.scalar.dl.ledger.exception;

import com.scalar.dl.ledger.error.ScalarDlError;
import com.scalar.dl.ledger.service.StatusCode;

public class MissingContractException extends DatabaseException {

  public MissingContractException(String message) {
    super(message, StatusCode.CONTRACT_NOT_FOUND);
  }

  public MissingContractException(String message, Throwable cause) {
    super(message, cause, StatusCode.CONTRACT_NOT_FOUND);
  }

  public MissingContractException(ScalarDlError error, Object... args) {
    super(error.buildMessage(args), error.getStatusCode());
  }
}
