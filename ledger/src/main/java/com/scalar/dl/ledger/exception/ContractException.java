package com.scalar.dl.ledger.exception;

import com.scalar.dl.ledger.service.StatusCode;

public class ContractException extends LedgerException {

  public ContractException(String message, StatusCode code) {
    super(message, code);
  }

  public ContractException(String message, Throwable cause, StatusCode code) {
    super(message, cause, code);
  }
}
