package com.scalar.dl.ledger.statemachine;

import com.scalar.dl.ledger.database.AssetFilter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.json.JsonObject;

public class DeprecatedLedger implements Ledger<JsonObject>, DeprecatedLedgerReturnable {
  private final com.scalar.dl.ledger.database.Ledger ledger;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public DeprecatedLedger(com.scalar.dl.ledger.database.Ledger ledger) {
    this.ledger = ledger;
  }

  @Override
  public Optional<Asset<JsonObject>> get(String assetId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<Asset<JsonObject>> get(String namespace, String assetId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Asset<JsonObject>> scan(AssetFilter filter) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void put(String assetId, JsonObject data) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void put(String namespace, String assetId, JsonObject data) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  @SuppressFBWarnings("EI_EXPOSE_REP")
  public com.scalar.dl.ledger.database.Ledger getDeprecatedLedger() {
    return ledger;
  }
}
