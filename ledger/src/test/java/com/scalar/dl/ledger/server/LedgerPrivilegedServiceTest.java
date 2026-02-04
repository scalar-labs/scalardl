package com.scalar.dl.ledger.server;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import com.scalar.dl.ledger.service.LedgerService;
import com.scalar.dl.rpc.CertificateRegistrationRequest;
import com.scalar.dl.rpc.FunctionRegistrationRequest;
import com.scalar.dl.rpc.NamespaceCreationRequest;
import com.scalar.dl.rpc.NamespaceDroppingRequest;
import com.scalar.dl.rpc.NamespacesListingRequest;
import com.scalar.dl.rpc.NamespacesListingResponse;
import com.scalar.dl.rpc.SecretRegistrationRequest;
import io.grpc.stub.StreamObserver;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class LedgerPrivilegedServiceTest {
  private static final int SOME_CERT_VERSION = 1;
  private static final String SOME_PUBLIC_KEY = "public_key";
  private static final String SOME_ENTITY_ID = "entity_id";
  private static final int SOME_KEY_VERSION = 1;
  private static final String SOME_SECRET_KEY = "secret_key";
  private static final String SOME_FUNCTION_ID = "function_id";
  private static final String SOME_FUNCTION_BINARY_NAME = "function_name";
  private static final byte[] SOME_FUNCTION_BYTE_CODE = "function".getBytes(StandardCharsets.UTF_8);
  @Mock private LedgerService ledger;
  @Mock private GateKeeper gateKeeper;
  private LedgerPrivilegedService grpc;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    doNothing().when(gateKeeper).letIn();
    doNothing().when(gateKeeper).letOut();
    CommonService commonService = new CommonService(null, gateKeeper);
    grpc = new LedgerPrivilegedService(ledger, commonService);
  }

  @Test
  public void registerCertificate_CertificateRegistrationRequestGiven_ShouldCallRegister() {
    // Arrange
    CertificateRegistrationRequest request =
        CertificateRegistrationRequest.newBuilder()
            .setEntityId(SOME_ENTITY_ID)
            .setKeyVersion(SOME_CERT_VERSION)
            .setCertPem(SOME_PUBLIC_KEY)
            .build();
    StreamObserver<Empty> observer = mock(StreamObserver.class);

    // Act
    grpc.registerCert(request, observer);

    // Assert
    com.scalar.dl.ledger.model.CertificateRegistrationRequest expected =
        new com.scalar.dl.ledger.model.CertificateRegistrationRequest(
            null, SOME_ENTITY_ID, SOME_CERT_VERSION, SOME_PUBLIC_KEY);
    verify(ledger).register(expected);
  }

  @Test
  public void registerSecret_SecretRegistrationRequestGiven_ShouldCallRegister() {
    // Arrange
    SecretRegistrationRequest request =
        SecretRegistrationRequest.newBuilder()
            .setEntityId(SOME_ENTITY_ID)
            .setKeyVersion(SOME_KEY_VERSION)
            .setSecretKey(SOME_SECRET_KEY)
            .build();
    StreamObserver<Empty> observer = mock(StreamObserver.class);

    // Act
    grpc.registerSecret(request, observer);

    // Assert
    com.scalar.dl.ledger.model.SecretRegistrationRequest expected =
        new com.scalar.dl.ledger.model.SecretRegistrationRequest(
            null, SOME_ENTITY_ID, SOME_KEY_VERSION, SOME_SECRET_KEY);
    verify(ledger).register(expected);
  }

  @Test
  public void registerFunction_FunctionRegistrationRequestGiven_ShouldCallRegister() {
    // Arrange
    FunctionRegistrationRequest request =
        FunctionRegistrationRequest.newBuilder()
            .setFunctionId(SOME_FUNCTION_ID)
            .setFunctionBinaryName(SOME_FUNCTION_BINARY_NAME)
            .setFunctionByteCode(ByteString.copyFrom(SOME_FUNCTION_BYTE_CODE))
            .build();
    StreamObserver<Empty> observer = mock(StreamObserver.class);

    // Act
    grpc.registerFunction(request, observer);

    // Assert
    com.scalar.dl.ledger.model.FunctionRegistrationRequest expected =
        new com.scalar.dl.ledger.model.FunctionRegistrationRequest(
            SOME_FUNCTION_ID, SOME_FUNCTION_BINARY_NAME, SOME_FUNCTION_BYTE_CODE);

    verify(ledger).register(expected);
  }

  @Test
  public void createNamespace_NamespaceCreationRequestGiven_ShouldCallCreate() {
    // Arrange
    String namespace = "test_namespace";
    NamespaceCreationRequest request =
        NamespaceCreationRequest.newBuilder().setNamespace(namespace).build();
    StreamObserver<Empty> observer = mock(StreamObserver.class);

    // Act
    grpc.createNamespace(request, observer);

    // Assert
    com.scalar.dl.ledger.model.NamespaceCreationRequest expected =
        new com.scalar.dl.ledger.model.NamespaceCreationRequest(namespace);
    verify(ledger).create(expected);
  }

  @Test
  public void dropNamespace_NamespaceDroppingRequestGiven_ShouldCallDrop() {
    // Arrange
    String namespace = "test_namespace";
    NamespaceDroppingRequest request =
        NamespaceDroppingRequest.newBuilder().setNamespace(namespace).build();
    StreamObserver<Empty> observer = mock(StreamObserver.class);

    // Act
    grpc.dropNamespace(request, observer);

    // Assert
    com.scalar.dl.ledger.model.NamespaceDroppingRequest expected =
        new com.scalar.dl.ledger.model.NamespaceDroppingRequest(namespace);
    verify(ledger).drop(expected);
  }

  @Test
  public void listNamespaces_NamespacesListingRequestGiven_ShouldCallList() {
    // Arrange
    String pattern = "test";
    NamespacesListingRequest request =
        NamespacesListingRequest.newBuilder().setPattern(pattern).build();
    when(ledger.list(new com.scalar.dl.ledger.model.NamespacesListingRequest(pattern)))
        .thenReturn(Arrays.asList("test_namespace1", "test_namespace2"));
    StreamObserver<NamespacesListingResponse> observer = mock(StreamObserver.class);

    // Act
    grpc.listNamespaces(request, observer);

    // Assert
    com.scalar.dl.ledger.model.NamespacesListingRequest expected =
        new com.scalar.dl.ledger.model.NamespacesListingRequest(pattern);
    verify(ledger).list(expected);
  }
}
