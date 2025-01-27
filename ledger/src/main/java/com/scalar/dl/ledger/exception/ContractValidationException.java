package com.scalar.dl.ledger.exception;

import com.scalar.dl.ledger.service.StatusCode;

public class ContractValidationException extends ValidationException {

  public ContractValidationException(String message) {
    super(message, StatusCode.INVALID_CONTRACT);
  }

  public ContractValidationException(String message, Throwable cause) {
    super(message, cause, StatusCode.INVALID_CONTRACT);
  }
}
