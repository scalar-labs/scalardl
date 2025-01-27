package com.scalar.dl.client.service;

import com.scalar.dl.ledger.database.TransactionState;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.ledger.proof.AssetProof;
import com.scalar.dl.ledger.service.ThrowableFunction;
import com.scalar.dl.rpc.AssetProofRetrievalRequest;
import com.scalar.dl.rpc.ContractExecutionRequest;
import com.scalar.dl.rpc.ContractExecutionResponse;
import com.scalar.dl.rpc.ExecutionAbortRequest;
import com.scalar.dl.rpc.FunctionRegistrationRequest;
import com.scalar.dl.rpc.LedgerValidationRequest;
import java.util.Optional;

public abstract class AbstractLedgerClient implements Client {
  protected static final ThrowableFunction<ContractExecutionResponse, ContractExecutionResponse>
      DEFAULT_AUDITING_HOOK = r -> null;

  abstract void register(FunctionRegistrationRequest request);

  abstract ContractExecutionResult execute(ContractExecutionRequest request);

  abstract ContractExecutionResult execute(
      ContractExecutionRequest request,
      ThrowableFunction<ContractExecutionResponse, ContractExecutionResponse> auditingHook);

  abstract LedgerValidationResult validate(LedgerValidationRequest request);

  abstract Optional<AssetProof> retrieve(AssetProofRetrievalRequest request);

  abstract TransactionState abort(ExecutionAbortRequest request);
}
