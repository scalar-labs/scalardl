package com.scalar.dl.client.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.rpc.RpcUtil;
import com.scalar.dl.ledger.config.TargetConfig;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.ledger.proof.AssetProof;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.service.ThrowableConsumer;
import com.scalar.dl.ledger.util.JsonpSerDe;
import com.scalar.dl.rpc.CertificateRegistrationRequest;
import com.scalar.dl.rpc.ContractExecutionRequest;
import com.scalar.dl.rpc.ContractExecutionResponse;
import com.scalar.dl.rpc.ContractRegistrationRequest;
import com.scalar.dl.rpc.ContractsListingRequest;
import com.scalar.dl.rpc.FunctionRegistrationRequest;
import com.scalar.dl.rpc.GatewayGrpc;
import com.scalar.dl.rpc.GatewayPrivilegedGrpc;
import com.scalar.dl.rpc.LedgerValidationRequest;
import com.scalar.dl.rpc.LedgerValidationResponse;
import com.scalar.dl.rpc.SecretRegistrationRequest;
import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.Immutable;
import javax.json.Json;
import javax.json.JsonObject;

@Immutable
public class GatewayClient extends AbstractGatewayClient {
  private final ManagedChannel channel;
  private final ManagedChannel privilegedChannel;
  private final GatewayGrpc.GatewayBlockingStub gatewayStub;
  private final GatewayPrivilegedGrpc.GatewayPrivilegedBlockingStub gatewayPrivilegedStub;
  private final long deadlineDurationMillis;

  @Inject
  public GatewayClient(TargetConfig config) {
    NettyChannelBuilder builder =
        NettyChannelBuilder.forAddress(config.getTargetHost(), config.getTargetPort());
    RpcUtil.configureTls(builder, config);
    RpcUtil.configureHeader(builder, config);
    RpcUtil.configureDataSize(builder, config);
    channel = builder.build();
    gatewayStub = GatewayGrpc.newBlockingStub(channel);

    NettyChannelBuilder privilegedBuilder =
        NettyChannelBuilder.forAddress(config.getTargetHost(), config.getTargetPrivilegedPort());
    RpcUtil.configureTls(privilegedBuilder, config);
    RpcUtil.configureHeader(privilegedBuilder, config);
    RpcUtil.configureDataSize(privilegedBuilder, config);
    privilegedChannel = privilegedBuilder.build();
    gatewayPrivilegedStub = GatewayPrivilegedGrpc.newBlockingStub(privilegedChannel);

    deadlineDurationMillis = config.getGrpcClientConfig().getDeadlineDurationMillis();
  }

  @VisibleForTesting
  GatewayClient(
      TargetConfig config,
      GatewayGrpc.GatewayBlockingStub gatewayStub,
      GatewayPrivilegedGrpc.GatewayPrivilegedBlockingStub gatewayPrivilegedStub) {
    this.channel = null;
    this.privilegedChannel = null;
    this.gatewayStub = gatewayStub;
    this.gatewayPrivilegedStub = gatewayPrivilegedStub;
    assert config.getGrpcClientConfig() != null;
    deadlineDurationMillis = config.getGrpcClientConfig().getDeadlineDurationMillis();
  }

  /**
   * SpotBugs detects Bug Type "CT_CONSTRUCTOR_THROW" saying that "The object under construction
   * remains partially initialized and may be vulnerable to Finalizer attacks."
   */
  @Override
  protected final void finalize() {}

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
    ThrowableConsumer<CertificateRegistrationRequest> f =
        r -> getGatewayPrivilegedStub().registerCert(r);
    try {
      accept(f, request);
    } catch (Exception e) {
      throwExceptionWithStatusCode(e);
    }
  }

  @Override
  public void register(SecretRegistrationRequest request) {
    ThrowableConsumer<SecretRegistrationRequest> f =
        r -> getGatewayPrivilegedStub().registerSecret(r);
    try {
      accept(f, request);
    } catch (Exception e) {
      throwExceptionWithStatusCode(e);
    }
  }

  @Override
  public void register(FunctionRegistrationRequest request) {
    ThrowableConsumer<FunctionRegistrationRequest> f =
        r -> getGatewayPrivilegedStub().registerFunction(r);
    try {
      accept(f, request);
    } catch (Exception e) {
      throwExceptionWithStatusCode(e);
    }
  }

  @Override
  public void register(ContractRegistrationRequest request) {
    ThrowableConsumer<ContractRegistrationRequest> f = r -> getGatewayStub().registerContract(r);
    try {
      accept(f, request);
    } catch (Exception e) {
      throwExceptionWithStatusCode(e);
    }
  }

  @Override
  public JsonObject list(ContractsListingRequest request) {
    try {
      String jsonString = getGatewayStub().listContracts(request).getJson();
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
    try {
      ContractExecutionResponse response = getGatewayStub().executeContract(request);

      String contractResult =
          response.getContractResult().isEmpty() ? null : response.getContractResult();
      String functionResult =
          response.getFunctionResult().isEmpty() ? null : response.getFunctionResult();
      List<AssetProof> proofs = new ArrayList<>();
      response.getProofsList().forEach(p -> proofs.add(new AssetProof(p)));

      // In gateway, we can guarantee Ledger's proofs and Auditor's proofs are same, at this point.
      // So, we just return the same proofs here.
      return new ContractExecutionResult(contractResult, functionResult, proofs, proofs);
    } catch (Exception e) {
      throwExceptionWithStatusCode(e);
    }
    // Java compiler requires this line even though it won't come here
    return new ContractExecutionResult(null, null, null, null);
  }

  @Override
  public LedgerValidationResult validate(LedgerValidationRequest request) {
    try {
      LedgerValidationResponse response = getGatewayStub().validateLedger(request);
      AssetProof proof = response.hasProof() ? new AssetProof(response.getProof()) : null;
      return new LedgerValidationResult(StatusCode.get(response.getStatusCode()), proof, null);
    } catch (Exception e) {
      throwExceptionWithStatusCode(e);
    }
    // Java compiler requires this line even though it won't come here
    return new LedgerValidationResult(StatusCode.RUNTIME_ERROR, null, null);
  }

  private GatewayGrpc.GatewayBlockingStub getGatewayStub() {
    return gatewayStub.withDeadlineAfter(deadlineDurationMillis, TimeUnit.MILLISECONDS);
  }

  private GatewayPrivilegedGrpc.GatewayPrivilegedBlockingStub getGatewayPrivilegedStub() {
    return gatewayPrivilegedStub.withDeadlineAfter(deadlineDurationMillis, TimeUnit.MILLISECONDS);
  }
}
