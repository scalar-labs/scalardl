package com.scalar.dl.ledger.database.scalardb;

import com.google.inject.Inject;
import com.scalar.db.api.DistributedStorage;
import com.scalar.db.api.DistributedStorageAdmin;
import com.scalar.db.api.DistributedTransactionAdmin;
import com.scalar.dl.ledger.config.LedgerConfig;
import java.util.Set;

public class LedgerNamespaceRegistry extends AbstractScalarNamespaceRegistry {

  @Inject
  public LedgerNamespaceRegistry(
      LedgerConfig config,
      DistributedStorage storage,
      DistributedStorageAdmin storageAdmin,
      DistributedTransactionAdmin transactionAdmin,
      ScalarNamespaceResolver namespaceResolver,
      Set<TableMetadataProvider> tableMetadataProviders) {
    super(
        config, storage, storageAdmin, transactionAdmin, namespaceResolver, tableMetadataProviders);
  }
}
