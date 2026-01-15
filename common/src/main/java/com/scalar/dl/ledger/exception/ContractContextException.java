package com.scalar.dl.ledger.exception;

import com.scalar.dl.ledger.service.StatusCode;

public class ContractContextException extends ContractException {

  public ContractContextException(String message) {
    super(message, StatusCode.CONTRACT_CONTEXTUAL_ERROR);
  }

  public ContractContextException(String message, Throwable cause) {
    super(message, cause, StatusCode.CONTRACT_CONTEXTUAL_ERROR);
  }
}
