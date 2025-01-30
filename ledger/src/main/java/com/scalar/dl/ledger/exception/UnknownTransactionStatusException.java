package com.scalar.dl.ledger.exception;

import com.scalar.dl.ledger.service.StatusCode;
import java.util.Optional;

public class UnknownTransactionStatusException extends DatabaseException {
  private Optional<String> unknownTxId = Optional.empty();

  public UnknownTransactionStatusException(String message) {
    super(message, StatusCode.UNKNOWN_TRANSACTION_STATUS);
  }

  public UnknownTransactionStatusException(String message, Throwable cause) {
    super(message, cause, StatusCode.UNKNOWN_TRANSACTION_STATUS);
  }

  public UnknownTransactionStatusException(String message, Throwable cause, String txId) {
    super(message, cause, StatusCode.UNKNOWN_TRANSACTION_STATUS);
    this.unknownTxId = Optional.ofNullable(txId);
  }

  public Optional<String> getUnknownTransactionId() {
    return unknownTxId;
  }
}
