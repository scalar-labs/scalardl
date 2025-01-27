package com.scalar.dl.ledger.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import com.scalar.dl.ledger.config.LedgerConfig;
import com.scalar.dl.ledger.contract.ContractExecutor;
import com.scalar.dl.ledger.crypto.AuditorKeyValidator;
import com.scalar.dl.ledger.crypto.ClientKeyValidator;
import com.scalar.dl.ledger.crypto.SecretEntry;
import com.scalar.dl.ledger.crypto.SignatureValidator;
import com.scalar.dl.ledger.exception.DatabaseException;
import com.scalar.dl.ledger.exception.SignatureException;
import com.scalar.dl.ledger.function.FunctionManager;
import com.scalar.dl.ledger.model.AbstractRequest;
import com.scalar.dl.ledger.model.CertificateRegistrationRequest;
import com.scalar.dl.ledger.model.ContractExecutionRequest;
import com.scalar.dl.ledger.model.ContractRegistrationRequest;
import com.scalar.dl.ledger.model.ContractsListingRequest;
import com.scalar.dl.ledger.model.FunctionRegistrationRequest;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class LedgerServiceTest {
  @Mock private BaseService base;
  @Mock private LedgerConfig config;
  @Mock private ContractExecutor contractExecutor;
  @Mock private FunctionManager functionManager;
  @Mock private ClientKeyValidator clientKeyValidator;
  @Mock private AuditorKeyValidator auditorKeyValidator;
  @Mock private SignatureValidator signatureValidator;
  @Mock private CertificateRegistrationRequest certRegistrationRequest;
  @Mock private SecretEntry secretEntry;
  @Mock private FunctionRegistrationRequest functionRegistrationRequest;
  @Mock private ContractRegistrationRequest contractRegistrationRequest;
  @Mock private ContractsListingRequest contractsListingRequest;
  @Mock private ContractExecutionRequest contractExecutionRequest;
  @InjectMocks private LedgerService service;

  private static final String SOME_CONTRACT_ID = "contract_id";
  private static final String SOME_CONTRACT_NAME = "contract_name";
  private static final byte[] SOME_CONTRACT_BYTE_CODE = "contract".getBytes(StandardCharsets.UTF_8);
  private static final String SOME_ENTITY_ID = "entity_id";
  private static final int SOME_KEY_VERSION = 1;
  private static final byte[] SOME_SIGNATURE = "signature".getBytes(StandardCharsets.UTF_8);
  private static final String SOME_PEM = "pem";
  private static final String SOME_FUNCTION_ID = "function_id";
  private static final String SOME_FUNCTION_NAME = "function_name";
  private static final byte[] SOME_FUNCTION_BYTE_CODE = "function".getBytes(StandardCharsets.UTF_8);

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    when(config.isFunctionEnabled()).thenReturn(false);
  }

  private void configureCertificateRegistrationRequest(CertificateRegistrationRequest request) {
    when(request.getEntityId()).thenReturn(SOME_ENTITY_ID);
    when(request.getKeyVersion()).thenReturn(SOME_KEY_VERSION);
    when(request.getCertPem()).thenReturn(SOME_PEM);
  }

  private void configureFunctionRegistrationRequest(FunctionRegistrationRequest request) {
    when(request.getFunctionId()).thenReturn(SOME_FUNCTION_ID);
    when(request.getFunctionBinaryName()).thenReturn(SOME_FUNCTION_NAME);
    when(request.getFunctionByteCode()).thenReturn(SOME_FUNCTION_BYTE_CODE);
  }

  private void configureContractRegistrationRequest(ContractRegistrationRequest request) {
    when(request.getContractId()).thenReturn(SOME_CONTRACT_ID);
    when(request.getContractBinaryName()).thenReturn(SOME_CONTRACT_NAME);
    when(request.getContractByteCode()).thenReturn(SOME_CONTRACT_BYTE_CODE);
    when(request.getEntityId()).thenReturn(SOME_ENTITY_ID);
    when(request.getKeyVersion()).thenReturn(SOME_KEY_VERSION);
    when(request.getContractProperties()).thenReturn(Optional.empty());
    when(request.getSignature()).thenReturn(SOME_SIGNATURE);
  }

  private void configureContractsListingRequest(
      ContractsListingRequest request, String contractId) {
    when(request.getContractId()).thenReturn(Optional.ofNullable(contractId));
    when(request.getEntityId()).thenReturn(SOME_ENTITY_ID);
    when(request.getKeyVersion()).thenReturn(SOME_KEY_VERSION);
    when(request.getSignature()).thenReturn(SOME_SIGNATURE);
  }

  private void configureContractExecutionRequest(ContractExecutionRequest request) {
    when(request.getContractId()).thenReturn(SOME_CONTRACT_ID);
    when(request.getEntityId()).thenReturn(SOME_ENTITY_ID);
    when(request.getKeyVersion()).thenReturn(SOME_KEY_VERSION);
  }

  private void configureRequestValidation(AbstractRequest request, boolean isValid) {
    when(clientKeyValidator.getValidator(anyString(), anyInt())).thenReturn(signatureValidator);
    if (isValid) {
      doNothing().when(request).validateWith(signatureValidator);
    } else {
      doThrow(mock(SignatureException.class)).when(request).validateWith(signatureValidator);
    }
  }

  @Test
  public void register_ProperCertificateGiven_ShouldRegisterCertificate() {
    // Arrange
    configureCertificateRegistrationRequest(certRegistrationRequest);

    // Act
    service.register(certRegistrationRequest);

    // Assert
    verify(base).register(certRegistrationRequest);
  }

  @Test
  public void register_ProperSecretGiven_ShouldRegisterSecret() {
    // Arrange

    // Act
    service.register(secretEntry);

    // Assert
    verify(base).register(secretEntry);
  }

  @Test
  public void register_ProperFunctionGiven_ShouldRegisterFunction() {
    // Arrange
    configureFunctionRegistrationRequest(functionRegistrationRequest);

    // Act
    service.register(functionRegistrationRequest);

    // Assert
    verify(functionManager).register(any());
  }

  @Test
  public void register_ProperContractGiven_ShouldRegisterContract() {
    // Arrange
    configureContractRegistrationRequest(contractRegistrationRequest);

    // Act
    service.register(contractRegistrationRequest);

    // Assert
    verify(base).register(contractRegistrationRequest);
  }

  @Test
  public void register_ExecutableContractGiven_ShouldRegister() {
    // Arrange
    configureContractRegistrationRequest(contractRegistrationRequest);
    when(config.getExecutableContractNames()).thenReturn(ImmutableSet.of(SOME_CONTRACT_NAME));

    // Act
    service.register(contractRegistrationRequest);

    // Assert
    verify(base).register(contractRegistrationRequest);
  }

  @Test
  public void register_NonExecutableContractGiven_ShouldThrowDatabaseException() {
    // Arrange
    when(config.getExecutableContractNames()).thenReturn(ImmutableSet.of(SOME_CONTRACT_NAME + "x"));

    // Act Assert
    assertThatThrownBy(() -> service.register(contractRegistrationRequest))
        .isInstanceOf(DatabaseException.class);

    // Assert
    verify(base, never()).register(contractRegistrationRequest);
  }

  @Test
  public void list_ProperListingRequestWithContractIdGiven_ShouldListContractsOfTheContractId() {
    // Arrange
    configureContractsListingRequest(contractsListingRequest, SOME_CONTRACT_ID);

    // Act
    service.list(contractsListingRequest);

    // Assert
    verify(base).list(contractsListingRequest);
  }

  @Test
  public void list_ProperListingRequestWithoutContractIdGiven_ShouldListContracts() {
    // Arrange
    configureContractsListingRequest(contractsListingRequest, null);

    // Act
    service.list(contractsListingRequest);

    // Assert
    verify(base).list(contractsListingRequest);
  }

  @Test
  public void execute_ProperRequestGiven_ShouldExecuteContract() {
    // Arrange
    configureContractExecutionRequest(contractExecutionRequest);
    configureRequestValidation(contractExecutionRequest, true);

    // Act
    service.execute(contractExecutionRequest);

    // Assert
    verify(contractExecutionRequest).validateWith(signatureValidator);
    verify(contractExecutor).execute(contractExecutionRequest);
  }

  @Test
  public void execute_InvalidRequestGiven_ShouldThrowSignatureException() {
    // Arrange
    configureContractExecutionRequest(contractExecutionRequest);
    configureRequestValidation(contractExecutionRequest, false);

    // Act assert
    assertThatThrownBy(() -> service.execute(contractExecutionRequest))
        .isInstanceOf(SignatureException.class);

    // Assert
    verify(contractExecutionRequest).validateWith(signatureValidator);
    verify(contractExecutor, never()).execute(contractExecutionRequest);
  }

  @Test
  public void execute_AuditorEnabledAndValidAuditorSignatureGiven_ShouldExecuteContract() {
    // Arrange
    configureContractExecutionRequest(contractExecutionRequest);
    configureRequestValidation(contractExecutionRequest, true);
    when(contractExecutionRequest.getAuditorSignature()).thenReturn(SOME_SIGNATURE);
    SignatureValidator auditorValidator = mock(SignatureValidator.class);
    when(auditorKeyValidator.getValidator()).thenReturn(auditorValidator);
    doNothing().when(contractExecutionRequest).validateAuditorSignatureWith(auditorValidator);
    when(config.isAuditorEnabled()).thenReturn(true);

    // Act
    service.execute(contractExecutionRequest);

    // Assert
    verify(contractExecutionRequest).validateWith(signatureValidator);
    verify(contractExecutionRequest).validateAuditorSignatureWith(auditorValidator);
    verify(contractExecutor).execute(contractExecutionRequest);
  }

  @Test
  public void
      execute_AuditorSignatureGivenButAuditorDisabled_ShouldThrowIllegalArgumentException() {
    // Arrange
    configureContractExecutionRequest(contractExecutionRequest);
    configureRequestValidation(contractExecutionRequest, true);
    when(contractExecutionRequest.getAuditorSignature()).thenReturn(SOME_SIGNATURE);
    when(config.isAuditorEnabled()).thenReturn(false);

    // Act
    Throwable thrown = catchThrowable(() -> service.execute(contractExecutionRequest));

    // Assert
    assertThat(thrown).isExactlyInstanceOf(IllegalArgumentException.class);
    verify(contractExecutionRequest).validateWith(signatureValidator);
    verify(contractExecutor, never()).execute(contractExecutionRequest);
  }

  @Test
  public void
      execute_AuditorEnabledButAuditorSignatureNotGiven_ShouldThrowIllegalArgumentException() {
    // Arrange
    configureContractExecutionRequest(contractExecutionRequest);
    configureRequestValidation(contractExecutionRequest, true);
    when(contractExecutionRequest.getAuditorSignature()).thenReturn(null);
    when(config.isAuditorEnabled()).thenReturn(true);

    // Act
    Throwable thrown = catchThrowable(() -> service.execute(contractExecutionRequest));

    // Assert
    assertThat(thrown).isExactlyInstanceOf(IllegalArgumentException.class);
    verify(contractExecutionRequest).validateWith(signatureValidator);
    verify(contractExecutor, never()).execute(contractExecutionRequest);
  }

  @Test
  public void execute_InvalidAuditorSignatureGiven_ShouldThrowSignatureException() {
    // Arrange
    configureContractExecutionRequest(contractExecutionRequest);
    configureRequestValidation(contractExecutionRequest, true);
    when(contractExecutionRequest.getAuditorSignature()).thenReturn(SOME_SIGNATURE);
    SignatureValidator auditorValidator = mock(SignatureValidator.class);
    when(auditorKeyValidator.getValidator()).thenReturn(auditorValidator);
    SignatureException toThrow = mock(SignatureException.class);
    doThrow(toThrow).when(contractExecutionRequest).validateAuditorSignatureWith(auditorValidator);
    when(config.isAuditorEnabled()).thenReturn(true);

    // Act
    Throwable thrown = catchThrowable(() -> service.execute(contractExecutionRequest));

    // Assert
    assertThat(thrown).isEqualTo(toThrow);
    verify(contractExecutionRequest).validateWith(signatureValidator);
    verify(contractExecutor, never()).execute(contractExecutionRequest);
  }
}
