package com.scalar.dl.ledger.database;

import com.scalar.dl.ledger.model.ContractExecutionRequest;
import java.util.Map;

public interface TransactionManager {

  Transaction startWith(ContractExecutionRequest request);

  Transaction startWith();

  TransactionState getState(String transactionId);

  TransactionState abort(String transactionId);

  void recover(Map<String, Integer> assetIds);
}
