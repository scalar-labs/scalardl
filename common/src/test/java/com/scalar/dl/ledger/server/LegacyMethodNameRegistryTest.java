package com.scalar.dl.ledger.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.protobuf.Empty;
import com.scalar.dl.rpc.AuditorPrivilegedGrpc;
import com.scalar.dl.rpc.CertificateRegistrationRequest;
import com.scalar.dl.rpc.GatewayPrivilegedGrpc;
import com.scalar.dl.rpc.LedgerGrpc;
import com.scalar.dl.rpc.LedgerPrivilegedGrpc;
import com.scalar.dl.rpc.NamespaceCreationRequest;
import com.scalar.dl.rpc.NamespaceDroppingRequest;
import com.scalar.dl.rpc.NamespacesListingRequest;
import com.scalar.dl.rpc.NamespacesListingResponse;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LegacyMethodNameRegistryTest {
  private static final String SOME_NAMESPACE = "some_namespace";
  private static final String SOME_JSON = "[\"some_namespace\"]";
  private final List<String> calledMethods = new ArrayList<>();
  private Server server;
  private ManagedChannel channel;

  private class FakeLedgerPrivilegedService extends LedgerPrivilegedGrpc.LedgerPrivilegedImplBase {
    @Override
    public void createNamespace(
        NamespaceCreationRequest request, StreamObserver<Empty> responseObserver) {
      calledMethods.add("createNamespace:" + request.getNamespace());
      responseObserver.onNext(Empty.getDefaultInstance());
      responseObserver.onCompleted();
    }

    @Override
    public void dropNamespace(
        NamespaceDroppingRequest request, StreamObserver<Empty> responseObserver) {
      calledMethods.add("dropNamespace:" + request.getNamespace());
      responseObserver.onNext(Empty.getDefaultInstance());
      responseObserver.onCompleted();
    }

    @Override
    public void listNamespaces(
        NamespacesListingRequest request,
        StreamObserver<NamespacesListingResponse> responseObserver) {
      calledMethods.add("listNamespaces:" + request.getPattern());
      responseObserver.onNext(NamespacesListingResponse.newBuilder().setJson(SOME_JSON).build());
      responseObserver.onCompleted();
    }
  }

  @BeforeEach
  public void setUp() throws IOException {
    String serverName = InProcessServerBuilder.generateName();
    ServerServiceDefinition service = new FakeLedgerPrivilegedService().bindService();
    server =
        InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(service)
            .fallbackHandlerRegistry(new LegacyMethodNameRegistry(service))
            .build()
            .start();
    channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
  }

  @AfterEach
  public void tearDown() {
    channel.shutdownNow();
    server.shutdownNow();
  }

  private static <ReqT, RespT> MethodDescriptor<ReqT, RespT> withBareMethodName(
      MethodDescriptor<ReqT, RespT> method, String bareMethodName) {
    return method.toBuilder()
        .setFullMethodName(
            MethodDescriptor.generateFullMethodName(method.getServiceName(), bareMethodName))
        .build();
  }

  private <ReqT, RespT> RespT call(MethodDescriptor<ReqT, RespT> method, ReqT request) {
    return ClientCalls.blockingUnaryCall(channel, method, CallOptions.DEFAULT, request);
  }

  @Test
  public void call_LegacyMethodNamesGiven_ShouldDispatchToCurrentMethods() {
    // Arrange
    NamespaceCreationRequest creationRequest =
        NamespaceCreationRequest.newBuilder().setNamespace(SOME_NAMESPACE).build();
    NamespaceDroppingRequest droppingRequest =
        NamespaceDroppingRequest.newBuilder().setNamespace(SOME_NAMESPACE).build();
    NamespacesListingRequest listingRequest =
        NamespacesListingRequest.newBuilder().setPattern(SOME_NAMESPACE).build();

    // Act
    call(
        withBareMethodName(LedgerPrivilegedGrpc.getCreateNamespaceMethod(), "createNamespace"),
        creationRequest);
    call(
        withBareMethodName(LedgerPrivilegedGrpc.getDropNamespaceMethod(), "dropNamespace"),
        droppingRequest);
    NamespacesListingResponse response =
        call(
            withBareMethodName(LedgerPrivilegedGrpc.getListNamespacesMethod(), "listNamespaces"),
            listingRequest);

    // Assert
    assertThat(calledMethods)
        .containsExactly(
            "createNamespace:" + SOME_NAMESPACE,
            "dropNamespace:" + SOME_NAMESPACE,
            "listNamespaces:" + SOME_NAMESPACE);
    assertThat(response.getJson()).isEqualTo(SOME_JSON);
  }

  @Test
  public void call_CurrentMethodNamesGiven_ShouldDispatchToCurrentMethods() {
    // Arrange
    assertThat(LedgerPrivilegedGrpc.getCreateNamespaceMethod().getFullMethodName())
        .isEqualTo("rpc.LedgerPrivileged/CreateNamespace");
    assertThat(LedgerPrivilegedGrpc.getDropNamespaceMethod().getFullMethodName())
        .isEqualTo("rpc.LedgerPrivileged/DropNamespace");
    assertThat(LedgerPrivilegedGrpc.getListNamespacesMethod().getFullMethodName())
        .isEqualTo("rpc.LedgerPrivileged/ListNamespaces");
    LedgerPrivilegedGrpc.LedgerPrivilegedBlockingStub stub =
        LedgerPrivilegedGrpc.newBlockingStub(channel);

    // Act
    stub.createNamespace(
        NamespaceCreationRequest.newBuilder().setNamespace(SOME_NAMESPACE).build());
    stub.dropNamespace(NamespaceDroppingRequest.newBuilder().setNamespace(SOME_NAMESPACE).build());
    NamespacesListingResponse response =
        stub.listNamespaces(
            NamespacesListingRequest.newBuilder().setPattern(SOME_NAMESPACE).build());

    // Assert
    assertThat(calledMethods)
        .containsExactly(
            "createNamespace:" + SOME_NAMESPACE,
            "dropNamespace:" + SOME_NAMESPACE,
            "listNamespaces:" + SOME_NAMESPACE);
    assertThat(response.getJson()).isEqualTo(SOME_JSON);
  }

  @Test
  public void call_NonRenamedMethodInLegacyCaseGiven_ShouldThrowUnimplemented() {
    // Arrange
    MethodDescriptor<CertificateRegistrationRequest, Empty> method =
        withBareMethodName(LedgerPrivilegedGrpc.getRegisterCertMethod(), "registerCert");

    // Act Assert
    assertThatThrownBy(() -> call(method, CertificateRegistrationRequest.getDefaultInstance()))
        .isInstanceOf(StatusRuntimeException.class)
        .extracting(e -> ((StatusRuntimeException) e).getStatus().getCode())
        .isEqualTo(Status.Code.UNIMPLEMENTED);
    assertThat(calledMethods).isEmpty();
  }

  @Test
  public void lookupMethod_AuditorAndGatewayPrivilegedServicesGiven_ShouldResolveLegacyNames() {
    // Arrange
    LegacyMethodNameRegistry auditorRegistry =
        new LegacyMethodNameRegistry(
            new AuditorPrivilegedGrpc.AuditorPrivilegedImplBase() {}.bindService());
    LegacyMethodNameRegistry gatewayRegistry =
        new LegacyMethodNameRegistry(
            new GatewayPrivilegedGrpc.GatewayPrivilegedImplBase() {}.bindService());

    // Act Assert
    for (String name : new String[] {"createNamespace", "dropNamespace", "listNamespaces"}) {
      assertThat(auditorRegistry.lookupMethod("rpc.AuditorPrivileged/" + name, null))
          .isNotNull()
          .satisfies(
              m ->
                  assertThat(m.getMethodDescriptor().getFullMethodName())
                      .isEqualTo("rpc.AuditorPrivileged/" + name));
      assertThat(gatewayRegistry.lookupMethod("rpc.GatewayPrivileged/" + name, null))
          .isNotNull()
          .satisfies(
              m ->
                  assertThat(m.getMethodDescriptor().getFullMethodName())
                      .isEqualTo("rpc.GatewayPrivileged/" + name));
    }
  }

  @Test
  public void lookupMethod_ServiceWithoutRenamedMethodsGiven_ShouldReturnNull() {
    // Arrange
    LegacyMethodNameRegistry registry =
        new LegacyMethodNameRegistry(new LedgerGrpc.LedgerImplBase() {}.bindService());

    // Act Assert
    assertThat(registry.lookupMethod("rpc.Ledger/createNamespace", null)).isNull();
    assertThat(registry.lookupMethod("rpc.LedgerPrivileged/createNamespace", null)).isNull();
  }
}
