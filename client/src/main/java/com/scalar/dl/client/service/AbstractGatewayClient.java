package com.scalar.dl.client.service;

import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.rpc.ContractExecutionRequest;
import com.scalar.dl.rpc.FunctionRegistrationRequest;
import com.scalar.dl.rpc.LedgerValidationRequest;
import com.scalar.dl.rpc.SignedFunctionRegistrationRequest;

public abstract class AbstractGatewayClient implements Client {

  abstract void register(FunctionRegistrationRequest request);

  abstract void register(SignedFunctionRegistrationRequest request);

  abstract ContractExecutionResult execute(ContractExecutionRequest request);

  abstract LedgerValidationResult validate(LedgerValidationRequest request);
}
