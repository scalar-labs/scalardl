package com.scalar.dl.ledger.statemachine;

import javax.annotation.Nonnull;

public interface DeprecatedLedgerReturnable {

  @Nonnull
  com.scalar.dl.ledger.database.Ledger getDeprecatedLedger();
}
