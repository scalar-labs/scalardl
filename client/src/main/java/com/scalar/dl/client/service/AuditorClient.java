package com.scalar.dl.client.service;

import com.google.inject.Inject;
import com.scalar.dl.client.error.ClientError;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.rpc.RpcUtil;
import com.scalar.dl.ledger.config.TargetConfig;
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
import com.scalar.dl.rpc.NamespaceCreationRequest;
import com.scalar.dl.rpc.NamespacesListingRequest;
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
  private final AuditorGrpc.AuditorBlockingStub auditorStub;
  private final AuditorPrivilegedGrpc.AuditorPrivilegedBlockingStub auditorPrivilegedStub;
  private final long deadlineDurationMillis;

  @Inject
  public AuditorClient(TargetConfig config) {
    NettyChannelBuilder builder =
        NettyChannelBuilder.forAddress(config.getTargetHost(), config.getTargetPort());
    RpcUtil.configureTls(builder, config);
    RpcUtil.configureHeader(builder, config);
    RpcUtil.configureDataSize(builder, config);
    channel = builder.build();
    auditorStub = AuditorGrpc.newBlockingStub(channel);

    NettyChannelBuilder privilegedBuilder =
        NettyChannelBuilder.forAddress(config.getTargetHost(), config.getTargetPrivilegedPort());
    RpcUtil.configureTls(privilegedBuilder, config);
    RpcUtil.configureHeader(privilegedBuilder, config);
    RpcUtil.configureDataSize(privilegedBuilder, config);
    privilegedChannel = privilegedBuilder.build();
    auditorPrivilegedStub = AuditorPrivilegedGrpc.newBlockingStub(privilegedChannel);

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
      throw new ClientException(ClientError.SHUTTING_DOWN_CHANNEL_FAILED, e, e.getMessage());
    }
  }

  @Override
  public void register(CertificateRegistrationRequest request) {
    ThrowableConsumer<CertificateRegistrationRequest> f =
        r -> getAuditorPrivilegedStub().registerCert(r);
    try {
      accept(f, request);
    } catch (Exception e) {
      throwExceptionWithStatusCode(e);
    }
  }

  @Override
  public void register(SecretRegistrationRequest request) {
    ThrowableConsumer<SecretRegistrationRequest> f =
        r -> getAuditorPrivilegedStub().registerSecret(r);
    try {
      accept(f, request);
    } catch (Exception e) {
      throwExceptionWithStatusCode(e);
    }
  }

  @Override
  public void register(ContractRegistrationRequest request) {
    ThrowableConsumer<ContractRegistrationRequest> f = r -> getAuditorStub().registerContract(r);
    try {
      accept(f, request);
    } catch (Exception e) {
      throwExceptionWithStatusCode(e);
    }
  }

  @Override
  public JsonObject list(ContractsListingRequest request) {
    try {
      return toJsonObject(getAuditorStub().listContracts(request).getJson());
    } catch (Exception e) {
      throwExceptionWithStatusCode(e);
    }
    // Java compiler requires this line even though it won't come here
    return Json.createObjectBuilder().build();
  }

  @Override
  public ExecutionOrderingResponse order(ContractExecutionRequest request) {
    ThrowableFunction<ContractExecutionRequest, ExecutionOrderingResponse> f =
        r -> getAuditorStub().orderExecution(r);
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
      return getAuditorStub().validateExecution(request);
    } catch (Exception e) {
      throwExceptionWithStatusCode(e);
    }
    // Java compiler requires this line even though it won't come here
    return ContractExecutionResponse.getDefaultInstance();
  }

  @Override
  public void create(NamespaceCreationRequest request) {
    ThrowableConsumer<NamespaceCreationRequest> f =
        r -> getAuditorPrivilegedStub().createNamespace(r);
    try {
      accept(f, request);
    } catch (Exception e) {
      throwExceptionWithStatusCode(e);
    }
  }

  @Override
  public String list(NamespacesListingRequest request) {
    try {
      return getAuditorPrivilegedStub().listNamespaces(request).getJson();
    } catch (Exception e) {
      throwExceptionWithStatusCode(e);
    }
    // Java compiler requires this line even though it won't come here
    return "";
  }

  private AuditorGrpc.AuditorBlockingStub getAuditorStub() {
    return auditorStub.withDeadlineAfter(deadlineDurationMillis, TimeUnit.MILLISECONDS);
  }

  private AuditorPrivilegedGrpc.AuditorPrivilegedBlockingStub getAuditorPrivilegedStub() {
    return auditorPrivilegedStub.withDeadlineAfter(deadlineDurationMillis, TimeUnit.MILLISECONDS);
  }
}
