package com.scalar.dl.ledger.validation;

import com.scalar.dl.ledger.database.AssetScanner;
import com.scalar.dl.ledger.database.TransactionManager;
import com.scalar.dl.ledger.database.scalardb.TransactionAssetScanner;
import com.scalar.dl.ledger.statemachine.DeserializationType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class TransactionScannableLedgerTracerManager implements LedgerTracerManager {
  private final TransactionManager manager;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public TransactionScannableLedgerTracerManager(TransactionManager manager) {
    this.manager = manager;
  }

  @Override
  public LedgerTracerBase<?> start(DeserializationType type) {
    AssetScanner scanner = new TransactionAssetScanner(manager);
    switch (type) {
      case DEPRECATED:
        return new DeprecatedLedgerTracer(new LedgerTracer(scanner));
      case JSONP_JSON:
        return new JsonpBasedLedgerTracer(scanner);
      case JACKSON_JSON:
        return new JacksonBasedLedgerTracer(scanner);
      case STRING:
        return new StringBasedLedgerTracer(scanner);
      default:
        throw new IllegalArgumentException("unsupported deserialization type");
    }
  }
}
