package com.scalar.dl.ledger.model;

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
}
