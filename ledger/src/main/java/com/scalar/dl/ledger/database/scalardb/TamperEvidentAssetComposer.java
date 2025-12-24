package com.scalar.dl.ledger.database.scalardb;

import com.scalar.db.api.Put;
import com.scalar.dl.ledger.database.Snapshot;
import com.scalar.dl.ledger.model.ContractExecutionRequest;
import com.scalar.dl.ledger.statemachine.AssetKey;
import java.util.Map;

public interface TamperEvidentAssetComposer {

  Map<AssetKey, Put> compose(Snapshot snapshot, ContractExecutionRequest request);
}
