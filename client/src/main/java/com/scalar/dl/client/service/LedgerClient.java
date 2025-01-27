package com.scalar.dl.client.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.rpc.RpcUtil;
import com.scalar.dl.ledger.config.TargetConfig;
import com.scalar.dl.ledger.database.TransactionState;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.ledger.proof.AssetProof;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.service.ThrowableConsumer;
import com.scalar.dl.ledger.service.ThrowableFunction;
import com.scalar.dl.ledger.util.JsonpSerDe;
import com.scalar.dl.rpc.AssetProofRetrievalRequest;
import com.scalar.dl.rpc.AssetProofRetrievalResponse;
import com.scalar.dl.rpc.CertificateRegistrationRequest;
import com.scalar.dl.rpc.ContractExecutionRequest;
import com.scalar.dl.rpc.ContractExecutionResponse;
import com.scalar.dl.rpc.ContractRegistrationRequest;
import com.scalar.dl.rpc.ContractsListingRequest;
import com.scalar.dl.rpc.ExecutionAbortRequest;
import com.scalar.dl.rpc.ExecutionAbortResponse;
import com.scalar.dl.rpc.FunctionRegistrationRequest;
import com.scalar.dl.rpc.LedgerGrpc;
import com.scalar.dl.rpc.LedgerPrivilegedGrpc;
import com.scalar.dl.rpc.LedgerValidationRequest;
import com.scalar.dl.rpc.LedgerValidationResponse;
import com.scalar.dl.rpc.SecretRegistrationRequest;
import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.concurrent.Immutable;
import javax.json.Json;
import javax.json.JsonObject;

@Immutable
public class LedgerClient extends AbstractLedgerClient {
  private final ManagedChannel channel;
  private final ManagedChannel privilegedChannel;
  private final LedgerGrpc.LedgerBlockingStub ledgerStub;
  private final LedgerPrivilegedGrpc.LedgerPrivilegedBlockingStub ledgerPrivilegedStub;

  @Inject
  public LedgerClient(TargetConfig config) {
    NettyChannelBuilder builder =
        NettyChannelBuilder.forAddress(config.getTargetHost(), config.getTargetPort());
    RpcUtil.configureTls(builder, config);
    RpcUtil.configureHeader(builder, config);

    channel = builder.build();
    ledgerStub = LedgerGrpc.newBlockingStub(channel);

    NettyChannelBuilder privilegedBuilder =
        NettyChannelBuilder.forAddress(config.getTargetHost(), config.getTargetPrivilegedPort());
    RpcUtil.configureTls(privilegedBuilder, config);
    privilegedChannel = privilegedBuilder.build();
    ledgerPrivilegedStub = LedgerPrivilegedGrpc.newBlockingStub(privilegedChannel);
  }

  @VisibleForTesting
  LedgerClient(
      LedgerGrpc.LedgerBlockingStub ledgerStub,
      LedgerPrivilegedGrpc.LedgerPrivilegedBlockingStub ledgerPrivilegedStub) {
    this.channel = null;
    this.privilegedChannel = null;
    this.ledgerStub = ledgerStub;
    this.ledgerPrivilegedStub = ledgerPrivilegedStub;
  }

  @Override
  public void shutdown() {
    try {
      channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
      privilegedChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw new ClientException(e.getMessage(), e, StatusCode.RUNTIME_ERROR);
    }
  }

  @Override
  public void register(CertificateRegistrationRequest request) {
    ThrowableConsumer<CertificateRegistrationRequest> f = r -> ledgerPrivilegedStub.registerCert(r);
    try {
      accept(f, request);
    } catch (Exception e) {
      throwExceptionWithStatusCode(e);
    }
  }

  @Override
  public void register(SecretRegistrationRequest request) {
    ThrowableConsumer<SecretRegistrationRequest> f = r -> ledgerPrivilegedStub.registerSecret(r);
    try {
      accept(f, request);
    } catch (Exception e) {
      throwExceptionWithStatusCode(e);
    }
  }

  @Override
  public void register(FunctionRegistrationRequest request) {
    ThrowableConsumer<FunctionRegistrationRequest> f =
        r -> ledgerPrivilegedStub.registerFunction(r);
    try {
      accept(f, request);
    } catch (Exception e) {
      throwExceptionWithStatusCode(e);
    }
  }

  @Override
  public void register(ContractRegistrationRequest request) {
    ThrowableConsumer<ContractRegistrationRequest> f = r -> ledgerStub.registerContract(r);
    try {
      accept(f, request);
    } catch (Exception e) {
      throwExceptionWithStatusCode(e);
    }
  }

  @Override
  public JsonObject list(ContractsListingRequest request) {
    try {
      String jsonString = ledgerStub.listContracts(request).getJson();
      return jsonString.isEmpty()
          ? Json.createObjectBuilder().build()
          : new JsonpSerDe().deserialize(jsonString);
    } catch (Exception e) {
      throwExceptionWithStatusCode(e);
    }
    // Java compiler requires this line even though it won't come here
    return Json.createObjectBuilder().build();
  }

  @Override
  public ContractExecutionResult execute(ContractExecutionRequest request) {
    return execute(request, DEFAULT_AUDITING_HOOK);
  }

  @Override
  public ContractExecutionResult execute(
      ContractExecutionRequest request,
      ThrowableFunction<ContractExecutionResponse, ContractExecutionResponse> auditingHook) {
    try {
      ContractExecutionResponse response = ledgerStub.executeContract(request);
      ContractExecutionResponse auditorResponse = auditingHook.apply(response);

      String contractResult =
          response.getContractResult().isEmpty() ? null : response.getContractResult();
      String functionResult =
          response.getFunctionResult().isEmpty() ? null : response.getFunctionResult();
      List<AssetProof> proofs = new ArrayList<>();
      response.getProofsList().forEach(p -> proofs.add(new AssetProof(p)));

      List<AssetProof> auditorProofs = null;
      if (auditorResponse != null) {
        auditorProofs =
            auditorResponse.getProofsList().stream()
                .map(AssetProof::new)
                .collect(Collectors.toList());
      }

      return new ContractExecutionResult(contractResult, functionResult, proofs, auditorProofs);
    } catch (Exception e) {
      throwExceptionWithStatusCode(e);
    }
    // Java compiler requires this line even though it won't come here
    return new ContractExecutionResult(null, null, null, null);
  }

  @Override
  public LedgerValidationResult validate(LedgerValidationRequest request) {
    try {
      LedgerValidationResponse response = ledgerStub.validateLedger(request);
      AssetProof proof = response.hasProof() ? new AssetProof(response.getProof()) : null;
      return new LedgerValidationResult(StatusCode.get(response.getStatusCode()), proof, null);
    } catch (Exception e) {
      throwExceptionWithStatusCode(e);
    }
    // Java compiler requires this line even though it won't come here
    return new LedgerValidationResult(StatusCode.RUNTIME_ERROR, null, null);
  }

  @Override
  public Optional<AssetProof> retrieve(AssetProofRetrievalRequest request) {
    try {
      AssetProofRetrievalResponse response = ledgerStub.retrieveAssetProof(request);
      AssetProof proof = response.hasProof() ? new AssetProof(response.getProof()) : null;
      return Optional.ofNullable(proof);
    } catch (Exception e) {
      throwExceptionWithStatusCode(e);
    }
    // Java compiler requires this line even though it won't come here
    return Optional.empty();
  }

  @Override
  public TransactionState abort(ExecutionAbortRequest request) {
    try {
      ExecutionAbortResponse response = ledgerStub.abortExecution(request);
      return TransactionState.getInstance(response.getState().getNumber());
    } catch (Exception e) {
      throwExceptionWithStatusCode(e);
    }
    // Java compiler requires this line even though it won't come here
    return TransactionState.UNKNOWN;
  }
}
