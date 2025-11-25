package com.scalar.dl.ledger.database.scalardb;

import com.google.common.collect.ImmutableMap;
import com.scalar.db.api.TableMetadata;
import java.util.Map;

public interface TableMetadataProvider {

  default Map<String, TableMetadata> getStorageTables() {
    return ImmutableMap.of();
  }

  default Map<String, TableMetadata> getTransactionTables() {
    return ImmutableMap.of();
  }
}
