package com.scalar.dl.ledger.model;

import com.scalar.dl.ledger.database.TransactionState;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;

@Immutable
public class StateRetrievalResult {
  private final TransactionState state;

  public StateRetrievalResult(TransactionState state) {
    this.state = state;
  }

  public TransactionState getState() {
    return state;
  }

  /**
   * Returns a hash code value for the object.
   *
   * @return a hash code value for this object.
   */
  @Override
  public int hashCode() {
    return Objects.hash(state);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof StateRetrievalResult)) {
      return false;
    }
    StateRetrievalResult other = (StateRetrievalResult) o;
    return this.state.equals(other.state);
  }
}
