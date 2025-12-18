package com.scalar.dl.ledger.exception;

import com.scalar.dl.ledger.error.ScalarDlError;
import com.scalar.dl.ledger.service.StatusCode;

public class ContractException extends LedgerException {

  public ContractException(String message, StatusCode code) {
    super(message, code);
  }

  public ContractException(String message, Throwable cause, StatusCode code) {
    super(message, cause, code);
  }

  public ContractException(ScalarDlError error, Object... args) {
    this(error.buildMessage(args), error.getStatusCode());
  }

  public ContractException(ScalarDlError error, Throwable cause, Object... args) {
    this(error.buildMessage(args), cause, error.getStatusCode());
  }
}
