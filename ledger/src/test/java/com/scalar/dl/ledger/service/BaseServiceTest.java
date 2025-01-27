package com.scalar.dl.ledger.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.dl.ledger.contract.ContractEntry;
import com.scalar.dl.ledger.contract.ContractManager;
import com.scalar.dl.ledger.crypto.CertificateManager;
import com.scalar.dl.ledger.crypto.ClientKeyValidator;
import com.scalar.dl.ledger.crypto.DigitalSignatureValidator;
import com.scalar.dl.ledger.crypto.SecretEntry;
import com.scalar.dl.ledger.crypto.SecretManager;
import com.scalar.dl.ledger.exception.SignatureException;
import com.scalar.dl.ledger.model.AbstractRequest;
import com.scalar.dl.ledger.model.CertificateRegistrationRequest;
import com.scalar.dl.ledger.model.ContractRegistrationRequest;
import com.scalar.dl.ledger.model.ContractsListingRequest;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class BaseServiceTest {
  @Mock private CertificateManager certManager;
  @Mock private SecretManager secretManager;
  @Mock private ClientKeyValidator clientKeyValidator;
  @Mock private ContractManager contractManager;
  @Mock private CertificateRegistrationRequest certRegistrationRequest;
  @Mock private SecretEntry secretEntry;
  @Mock private ContractRegistrationRequest contractRegistrationRequest;
  @Mock private ContractsListingRequest contractsListingRequest;
  @Mock private DigitalSignatureValidator validator;
  @InjectMocks private BaseService service;

  private static final String SOME_CONTRACT_ID = "contract_id";
  private static final String SOME_CONTRACT_NAME = "contract_name";
  private static final byte[] SOME_CONTRACT_BYTE_CODE = "contract".getBytes(StandardCharsets.UTF_8);
  private static final String SOME_ENTITY_ID = "entity_id";
  private static final int SOME_KEY_VERSION = 1;
  private static final byte[] SOME_SIGNATURE = "signature".getBytes(StandardCharsets.UTF_8);
  private static final String SOME_PEM = "pem";

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  private void configureCertificateRegistrationRequest(CertificateRegistrationRequest request) {
    when(request.getEntityId()).thenReturn(SOME_ENTITY_ID);
    when(request.getKeyVersion()).thenReturn(SOME_KEY_VERSION);
    when(request.getCertPem()).thenReturn(SOME_PEM);
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

  private void configureRequestValidation(AbstractRequest request, boolean isValid) {
    when(clientKeyValidator.getValidator(request.getEntityId(), request.getKeyVersion()))
        .thenReturn(validator);
    if (isValid) {
      doNothing().when(request).validateWith(validator);
    } else {
      doThrow(mock(SignatureException.class)).when(request).validateWith(validator);
    }
  }

  private ContractEntry prepareContractEntry(String contractId) {
    ContractEntry entry = mock(ContractEntry.class);
    when(entry.getId()).thenReturn(contractId);
    return entry;
  }

  @Test
  public void register_ProperCertificateGiven_ShouldRegisterCertificate() {
    // Arrange
    configureCertificateRegistrationRequest(certRegistrationRequest);
    doNothing().when(certManager).register(any());

    // Act
    service.register(certRegistrationRequest);

    // Assert
    verify(certManager).register(any());
  }

  @Test
  public void register_ProperSecretGiven_ShouldRegisterSecret() {
    // Arrange
    doNothing().when(secretManager).register(any());

    // Act
    service.register(secretEntry);

    // Assert
    verify(secretManager).register(secretEntry);
  }

  @Test
  public void register_ProperContractGiven_ShouldRegisterContract() {
    // Arrange
    configureContractRegistrationRequest(contractRegistrationRequest);
    configureRequestValidation(contractRegistrationRequest, true);

    // Act
    service.register(contractRegistrationRequest);

    // Assert
    verify(contractManager).register(any());
  }

  @Test
  public void register_InvalidRequestGiven_ShouldThrowSignatureException() {
    // Arrange
    configureContractRegistrationRequest(contractRegistrationRequest);
    configureRequestValidation(contractRegistrationRequest, false);

    // Act assert
    assertThatThrownBy(() -> service.register(contractRegistrationRequest))
        .isInstanceOf(SignatureException.class);

    // Assert
    verify(contractManager, never()).register(any());
  }

  @Test
  public void list_ProperListingRequestWithContractIdGiven_ShouldListContractsOfTheContractId() {
    // Arrange
    configureContractsListingRequest(contractsListingRequest, SOME_CONTRACT_ID);
    configureRequestValidation(contractsListingRequest, true);
    ContractEntry entry1 = prepareContractEntry(SOME_CONTRACT_ID);
    ContractEntry entry2 = prepareContractEntry(SOME_CONTRACT_ID + "x");
    List<ContractEntry> entries = Arrays.asList(entry1, entry2);
    when(contractManager.scan(anyString(), anyInt())).thenReturn(entries);

    // Act
    List<ContractEntry> actual = service.list(contractsListingRequest);

    // Assert
    verify(contractManager)
        .scan(contractsListingRequest.getEntityId(), contractsListingRequest.getKeyVersion());
    assertThat(actual).containsOnly(entry1);
  }

  @Test
  public void list_ProperListingRequestWithoutContractIdGiven_ShouldListContracts() {
    // Arrange
    configureContractsListingRequest(contractsListingRequest, null);
    configureRequestValidation(contractsListingRequest, true);
    ContractEntry entry1 = prepareContractEntry(SOME_CONTRACT_ID);
    ContractEntry entry2 = prepareContractEntry(SOME_CONTRACT_ID + "x");
    List<ContractEntry> entries = Arrays.asList(entry1, entry2);
    when(contractManager.scan(anyString(), anyInt())).thenReturn(entries);

    // Act
    List<ContractEntry> actual = service.list(contractsListingRequest);

    // Assert
    verify(contractManager)
        .scan(contractsListingRequest.getEntityId(), contractsListingRequest.getKeyVersion());
    assertThat(actual).containsExactly(entry1, entry2);
  }
}
