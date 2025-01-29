package com.scalar.dl.ledger.database.scalardb;

import com.scalar.db.api.Put;
import com.scalar.dl.ledger.database.Snapshot;
import com.scalar.dl.ledger.model.ContractExecutionRequest;
import java.util.List;

public interface TamperEvidentAssetComposer {

  List<Put> compose(Snapshot snapshot, ContractExecutionRequest request);
}
