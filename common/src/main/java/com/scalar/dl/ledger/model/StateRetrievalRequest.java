package com.scalar.dl.ledger.model;

import java.util.Objects;
import javax.annotation.concurrent.Immutable;

@Immutable
public class StateRetrievalRequest {
  private final String transactionId;

  public StateRetrievalRequest(String transactionId) {
    this.transactionId = transactionId;
  }

  public String getTransactionId() {
    return transactionId;
  }

  /**
   * Returns a hash code value for the object.
   *
   * @return a hash code value for this object.
   */
  @Override
  public int hashCode() {
    return Objects.hash(transactionId);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof StateRetrievalRequest)) {
      return false;
    }
    StateRetrievalRequest other = (StateRetrievalRequest) o;
    return this.transactionId.equals(other.transactionId);
  }
}
