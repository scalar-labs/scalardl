package com.scalar.dl.ledger.database;

import com.scalar.dl.ledger.proof.AssetProof;
import com.scalar.dl.ledger.statemachine.DeprecatedLedger;
import com.scalar.dl.ledger.statemachine.DeserializationType;
import com.scalar.dl.ledger.statemachine.JacksonBasedAssetLedger;
import com.scalar.dl.ledger.statemachine.JsonpBasedAssetLedger;
import com.scalar.dl.ledger.statemachine.StringBasedAssetLedger;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class Transaction {
  private final TamperEvidentAssetLedger ledger;
  private final Database<?, ?, ?, ?, ?> database;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public Transaction(TamperEvidentAssetLedger ledger, MutableDatabase<?, ?, ?, ?, ?> database) {
    this.ledger = ledger;
    this.database = database;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public TamperEvidentAssetLedger getLedger() {
    return ledger;
  }

  public com.scalar.dl.ledger.statemachine.Ledger<?> getLedger(DeserializationType type) {
    switch (type) {
      case DEPRECATED:
        return new DeprecatedLedger(new AssetLedger(ledger));
      case JSONP_JSON:
        return new JsonpBasedAssetLedger(ledger);
      case JACKSON_JSON:
        return new JacksonBasedAssetLedger(ledger);
      case STRING:
        return new StringBasedAssetLedger(ledger);
      default:
        throw new IllegalArgumentException("unsupported deserialization type");
    }
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public Database<?, ?, ?, ?, ?> getDatabase() {
    return database;
  }

  public List<AssetProof> commit() {
    return ledger.commit();
  }

  public void abort() {
    ledger.abort();
  }
}
