package com.scalar.dl.client.service;

import com.google.inject.Inject;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.rpc.RpcUtil;
import com.scalar.dl.ledger.config.TargetConfig;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.service.ThrowableConsumer;
import com.scalar.dl.ledger.service.ThrowableFunction;
import com.scalar.dl.rpc.AuditorGrpc;
import com.scalar.dl.rpc.AuditorPrivilegedGrpc;
import com.scalar.dl.rpc.CertificateRegistrationRequest;
import com.scalar.dl.rpc.ContractExecutionRequest;
import com.scalar.dl.rpc.ContractExecutionResponse;
import com.scalar.dl.rpc.ContractRegistrationRequest;
import com.scalar.dl.rpc.ContractsListingRequest;
import com.scalar.dl.rpc.ExecutionOrderingResponse;
import com.scalar.dl.rpc.ExecutionValidationRequest;
import com.scalar.dl.rpc.SecretRegistrationRequest;
import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.Immutable;
import javax.json.Json;
import javax.json.JsonObject;

@Immutable
public class AuditorClient extends AbstractAuditorClient {
  private final ManagedChannel channel;
  private final ManagedChannel privilegedChannel;
  private final AuditorGrpc.AuditorFutureStub auditorStub;
  private final AuditorPrivilegedGrpc.AuditorPrivilegedBlockingStub auditorPrivilegedStub;

  @Inject
  public AuditorClient(TargetConfig config) {
    NettyChannelBuilder builder =
        NettyChannelBuilder.forAddress(config.getTargetHost(), config.getTargetPort());
    RpcUtil.configureTls(builder, config);
    RpcUtil.configureHeader(builder, config);

    channel = builder.build();
    auditorStub = AuditorGrpc.newFutureStub(channel);

    NettyChannelBuilder privilegedBuilder =
        NettyChannelBuilder.forAddress(config.getTargetHost(), config.getTargetPrivilegedPort());
    RpcUtil.configureTls(privilegedBuilder, config);
    privilegedChannel = privilegedBuilder.build();
    auditorPrivilegedStub = AuditorPrivilegedGrpc.newBlockingStub(privilegedChannel);
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
    ThrowableConsumer<CertificateRegistrationRequest> f =
        r -> auditorPrivilegedStub.registerCert(r);
    try {
      accept(f, request);
    } catch (Exception e) {
      throwExceptionWithStatusCode(e);
    }
  }

  @Override
  public void register(SecretRegistrationRequest request) {
    ThrowableConsumer<SecretRegistrationRequest> f = r -> auditorPrivilegedStub.registerSecret(r);
    try {
      accept(f, request);
    } catch (Exception e) {
      throwExceptionWithStatusCode(e);
    }
  }

  @Override
  public void register(ContractRegistrationRequest request) {
    ThrowableConsumer<ContractRegistrationRequest> f = r -> auditorStub.registerContract(r).get();
    try {
      accept(f, request);
    } catch (Exception e) {
      throwExceptionWithStatusCode(e);
    }
  }

  @Override
  public JsonObject list(ContractsListingRequest request) {
    try {
      return toJsonObject(auditorStub.listContracts(request).get().getJson());
    } catch (Exception e) {
      throwExceptionWithStatusCode(e);
    }
    // Java compiler requires this line even though it won't come here
    return Json.createObjectBuilder().build();
  }

  @Override
  public ExecutionOrderingResponse order(ContractExecutionRequest request) {
    ThrowableFunction<ContractExecutionRequest, ExecutionOrderingResponse> f =
        r -> auditorStub.orderExecution(r).get();
    try {
      return apply(f, request);
    } catch (Exception e) {
      throwExceptionWithStatusCode(e);
    }
    // Java compiler requires this line even though it won't come here
    return ExecutionOrderingResponse.getDefaultInstance();
  }

  @Override
  public ContractExecutionResponse validate(ExecutionValidationRequest request) {
    try {
      return auditorStub.validateExecution(request).get();
    } catch (Exception e) {
      throwExceptionWithStatusCode(e);
    }
    // Java compiler requires this line even though it won't come here
    return ContractExecutionResponse.getDefaultInstance();
  }
}
