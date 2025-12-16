package com.scalar.dl.ledger.database;

import com.scalar.dl.ledger.model.ContractExecutionRequest;
import com.scalar.dl.ledger.statemachine.AssetKey;
import java.util.Map;

public interface TransactionManager {

  Transaction startWith(ContractExecutionRequest request);

  Transaction startWith();

  TransactionState getState(String transactionId);

  TransactionState abort(String transactionId);

  void recover(Map<AssetKey, Integer> assetIds);
}
