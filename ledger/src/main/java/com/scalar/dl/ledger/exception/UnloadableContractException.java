package com.scalar.dl.ledger.exception;

import com.scalar.dl.ledger.service.StatusCode;

public class UnloadableContractException extends ContractException {

  public UnloadableContractException(String message) {
    super(message, StatusCode.UNLOADABLE_CONTRACT);
  }

  public UnloadableContractException(String message, Throwable cause) {
    super(message, cause, StatusCode.UNLOADABLE_CONTRACT);
  }
}
