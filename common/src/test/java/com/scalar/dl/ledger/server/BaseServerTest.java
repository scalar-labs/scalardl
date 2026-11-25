package com.scalar.dl.ledger.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.inject.Injector;
import com.google.protobuf.Empty;
import com.scalar.dl.ledger.config.GrpcServerConfig;
import com.scalar.dl.ledger.config.ServerConfig;
import com.scalar.dl.rpc.LedgerPrivilegedGrpc;
import com.scalar.dl.rpc.NamespaceCreationRequest;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.MethodDescriptor;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BaseServerTest {
  private static final String SOME_NAMESPACE = "some_namespace";
  private final List<String> calledMethods = new ArrayList<>();
  private BaseServer server;
  private ManagedChannel channel;

  private class FakeLedgerPrivilegedService extends LedgerPrivilegedGrpc.LedgerPrivilegedImplBase {
    @Override
    public void createNamespace(
        NamespaceCreationRequest request, StreamObserver<Empty> responseObserver) {
      calledMethods.add("createNamespace:" + request.getNamespace());
      responseObserver.onNext(Empty.getDefaultInstance());
      responseObserver.onCompleted();
    }
  }

  @BeforeEach
  public void setUp() throws IOException {
    int port = findFreePort();
    ServerConfig config = mock(ServerConfig.class);
    when(config.getName()).thenReturn("test");
    when(config.getPrivilegedPort()).thenReturn(port);
    when(config.isServerTlsEnabled()).thenReturn(false);
    when(config.getGrpcServerConfig()).thenReturn(GrpcServerConfig.newBuilder().build());
    Injector injector = mock(Injector.class);
    when(injector.getInstance(FakeLedgerPrivilegedService.class))
        .thenReturn(new FakeLedgerPrivilegedService());

    server = new BaseServer(injector, config);
    server.startPrivileged(FakeLedgerPrivilegedService.class);
    channel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build();
  }

  @AfterEach
  public void tearDown() throws InterruptedException {
    channel.shutdownNow().awaitTermination(10, TimeUnit.SECONDS);
    server.stop();
  }

  private static int findFreePort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }

  @Test
  public void startPrivileged_LegacyMethodNameGiven_ShouldDispatchToCurrentMethod() {
    // Arrange
    MethodDescriptor<NamespaceCreationRequest, Empty> method =
        LedgerPrivilegedGrpc.getCreateNamespaceMethod().toBuilder()
            .setFullMethodName("rpc.LedgerPrivileged/createNamespace")
            .build();

    // Act
    ClientCalls.blockingUnaryCall(
        channel,
        method,
        CallOptions.DEFAULT,
        NamespaceCreationRequest.newBuilder().setNamespace(SOME_NAMESPACE).build());

    // Assert
    assertThat(calledMethods).containsExactly("createNamespace:" + SOME_NAMESPACE);
  }

  @Test
  public void startPrivileged_CurrentMethodNameGiven_ShouldDispatchToCurrentMethod() {
    // Arrange
    LedgerPrivilegedGrpc.LedgerPrivilegedBlockingStub stub =
        LedgerPrivilegedGrpc.newBlockingStub(channel);

    // Act
    stub.createNamespace(
        NamespaceCreationRequest.newBuilder().setNamespace(SOME_NAMESPACE).build());

    // Assert
    assertThat(calledMethods).containsExactly("createNamespace:" + SOME_NAMESPACE);
  }
}
