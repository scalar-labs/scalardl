package com.scalar.dl.ledger.database.scalardb;

import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.AssetScanner;
import com.scalar.dl.ledger.database.Transaction;
import com.scalar.dl.ledger.database.TransactionManager;
import com.scalar.dl.ledger.error.LedgerError;
import com.scalar.dl.ledger.exception.DatabaseException;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

@Immutable
public class TransactionAssetScanner implements AssetScanner {
  private final TransactionManager manager;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public TransactionAssetScanner(TransactionManager manager) {
    this.manager = manager;
  }

  @Nonnull
  @Override
  public List<InternalAsset> doScan(AssetFilter filter) {
    Transaction transaction = manager.startWith();
    try {
      List<InternalAsset> assets = transaction.getLedger().scan(filter);
      transaction.commit();
      return assets;
    } catch (Exception e) {
      transaction.abort();
      throw new DatabaseException(LedgerError.RETRIEVING_ASSET_FAILED, e, e.getMessage());
    }
  }
}
