package com.scalar.dl.client.service;

import com.scalar.dl.auditor.ordering.LockRecoveryResult;
import com.scalar.dl.ledger.model.TransactionStatePurgeResult;
import com.scalar.dl.rpc.AssetLockRecoveryRequest;
import com.scalar.dl.rpc.ContractExecutionRequest;
import com.scalar.dl.rpc.ContractExecutionResponse;
import com.scalar.dl.rpc.ExecutionOrderingResponse;
import com.scalar.dl.rpc.ExecutionValidationRequest;
import com.scalar.dl.rpc.TransactionStatePurgeRequest;

public abstract class AbstractAuditorClient implements Client {

  abstract ExecutionOrderingResponse order(ContractExecutionRequest request);

  abstract ContractExecutionResponse validate(ExecutionValidationRequest request);

  abstract LockRecoveryResult recover(AssetLockRecoveryRequest request);

  abstract TransactionStatePurgeResult purgeTransactionStates(TransactionStatePurgeRequest request);
}
