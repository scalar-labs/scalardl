package com.scalar.dl.ledger.server;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import com.scalar.dl.ledger.crypto.SecretEntry;
import com.scalar.dl.ledger.service.LedgerService;
import com.scalar.dl.rpc.CertificateRegistrationRequest;
import com.scalar.dl.rpc.FunctionRegistrationRequest;
import com.scalar.dl.rpc.SecretRegistrationRequest;
import io.grpc.stub.StreamObserver;
import java.nio.charset.StandardCharsets;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
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
  private LedgerPrivilegedService grpc;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    CommonService commonService = new CommonService(null, null);
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
            SOME_ENTITY_ID, SOME_CERT_VERSION, SOME_PUBLIC_KEY);
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
    ArgumentCaptor<SecretEntry> captor = ArgumentCaptor.forClass(SecretEntry.class);
    verify(ledger).register(captor.capture());
    SecretEntry actual = captor.getValue();
    assertThat(actual.getEntityId()).isEqualTo(SOME_ENTITY_ID);
    assertThat(actual.getKeyVersion()).isEqualTo(SOME_KEY_VERSION);
    assertThat(actual.getSecretKey()).isEqualTo(SOME_SECRET_KEY);
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
}
