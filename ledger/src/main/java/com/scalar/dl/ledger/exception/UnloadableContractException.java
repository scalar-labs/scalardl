package com.scalar.dl.ledger.exception;

import com.scalar.dl.ledger.error.ScalarDlError;
import com.scalar.dl.ledger.service.StatusCode;

public class UnloadableContractException extends ContractException {

  public UnloadableContractException(String message) {
    super(message, StatusCode.UNLOADABLE_CONTRACT);
  }

  public UnloadableContractException(String message, Throwable cause) {
    super(message, cause, StatusCode.UNLOADABLE_CONTRACT);
  }

  public UnloadableContractException(ScalarDlError error, Object... args) {
    super(error.buildMessage(args), error.getStatusCode());
  }

  public UnloadableContractException(ScalarDlError error, Throwable cause, Object... args) {
    super(error.buildMessage(args), cause, error.getStatusCode());
  }
}
