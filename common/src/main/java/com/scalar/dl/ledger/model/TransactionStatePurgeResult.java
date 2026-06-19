package com.scalar.dl.ledger.model;

import java.util.Objects;
import javax.annotation.concurrent.Immutable;

@Immutable
public final class TransactionStatePurgeResult {
  private final int totalTargets;
  private final int purged;
  private final int skipped;

  public TransactionStatePurgeResult(int totalTargets, int purged, int skipped) {
    this.totalTargets = totalTargets;
    this.purged = purged;
    this.skipped = skipped;
  }

  public int getTotalTargets() {
    return totalTargets;
  }

  public int getPurged() {
    return purged;
  }

  public int getSkipped() {
    return skipped;
  }

  /**
   * Returns a hash code value for the object.
   *
   * @return a hash code value for this object.
   */
  @Override
  public int hashCode() {
    return Objects.hash(totalTargets, purged, skipped);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof TransactionStatePurgeResult)) {
      return false;
    }
    TransactionStatePurgeResult other = (TransactionStatePurgeResult) o;
    return this.totalTargets == other.totalTargets
        && this.purged == other.purged
        && this.skipped == other.skipped;
  }
}
