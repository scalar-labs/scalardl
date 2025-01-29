package com.scalar.dl.client.service;

import com.scalar.dl.rpc.ContractExecutionRequest;
import com.scalar.dl.rpc.ContractExecutionResponse;
import com.scalar.dl.rpc.ExecutionOrderingResponse;
import com.scalar.dl.rpc.ExecutionValidationRequest;

public abstract class AbstractAuditorClient implements Client {

  abstract ExecutionOrderingResponse order(ContractExecutionRequest request);

  abstract ContractExecutionResponse validate(ExecutionValidationRequest request);
}
