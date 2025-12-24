package com.scalar.dl.ledger.validation;

import static com.scalar.dl.ledger.test.TestConstants.CERTIFICATE_A;
import static com.scalar.dl.ledger.test.TestConstants.CERTIFICATE_B;
import static com.scalar.dl.ledger.test.TestConstants.PRIVATE_KEY_A;
import static com.scalar.dl.ledger.test.TestConstants.PRIVATE_KEY_B;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.scalar.dl.ledger.contract.ContractMachine;
import com.scalar.dl.ledger.crypto.ClientIdentityKey;
import com.scalar.dl.ledger.crypto.ClientKeyValidator;
import com.scalar.dl.ledger.crypto.DigitalSignatureSigner;
import com.scalar.dl.ledger.crypto.DigitalSignatureValidator;
import com.scalar.dl.ledger.error.LedgerError;
import com.scalar.dl.ledger.exception.ValidationException;
import com.scalar.dl.ledger.model.ContractExecutionRequest;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import com.scalar.dl.ledger.statemachine.Ledger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ContractValidatorTest {
  private static final String NAMESPACE = "namespace";
  private static final String ENTITY_ID = "entity_id";
  private static final int CERT_VERSION = 1;
  private static final String CONTRACT_ID = "contract_id";
  private static final String CONTRACT_ARGUMENT = "contract_argument";
  private static final String CONTRACT_ID_IN_ASSET =
      ENTITY_ID + "/" + CERT_VERSION + "/" + CONTRACT_ID;
  @Mock private ContractMachine contract;
  @Mock private Ledger<?> ledger;
  @Mock private ClientKeyValidator clientKeyValidator;
  @Mock private ClientIdentityKey clientIdentityKey;
  @InjectMocks private ContractValidator validator;
  private DigitalSignatureSigner dsSigner;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    dsSigner = new DigitalSignatureSigner(PRIVATE_KEY_A);
  }

  private InternalAsset createAssetMock(String contractId, String argument, byte[] signature) {
    InternalAsset asset = mock(InternalAsset.class);
    when(asset.contractId()).thenReturn(contractId);
    when(asset.argument()).thenReturn(argument);
    when(asset.signature()).thenReturn(signature);
    return asset;
  }

  @Test
  public void validate_CorrectAssetGiven_ShouldReturnOK() {
    // Arrange
    when(clientIdentityKey.getEntityId()).thenReturn(ENTITY_ID);
    when(clientIdentityKey.getKeyVersion()).thenReturn(CERT_VERSION);
    when(contract.getClientIdentityKey()).thenReturn(clientIdentityKey);
    when(clientKeyValidator.getValidator(anyString(), anyInt()))
        .thenReturn(new DigitalSignatureValidator(CERTIFICATE_A));
    byte[] signature =
        dsSigner.sign(
            ContractExecutionRequest.serialize(
                CONTRACT_ID, CONTRACT_ARGUMENT, ENTITY_ID, CERT_VERSION));
    InternalAsset asset = createAssetMock(CONTRACT_ID_IN_ASSET, CONTRACT_ARGUMENT, signature);

    // Act
    StatusCode result = validator.validate(ledger, contract, NAMESPACE, asset);

    // Assert
    assertThat(result).isEqualTo(StatusCode.OK);
  }

  @Test
  public void validate_AssetWithTamperedContractIdGiven_ShouldReturnInvalid() {
    // Arrange
    when(clientIdentityKey.getEntityId()).thenReturn(ENTITY_ID);
    when(clientIdentityKey.getKeyVersion()).thenReturn(CERT_VERSION);
    when(contract.getClientIdentityKey()).thenReturn(clientIdentityKey);
    when(clientKeyValidator.getValidator(anyString(), anyInt()))
        .thenReturn(new DigitalSignatureValidator(CERTIFICATE_A));
    String tamperedContractId = CONTRACT_ID_IN_ASSET + "x";
    byte[] signature =
        dsSigner.sign(
            ContractExecutionRequest.serialize(
                CONTRACT_ID, CONTRACT_ARGUMENT, ENTITY_ID, CERT_VERSION));
    InternalAsset asset = createAssetMock(tamperedContractId, CONTRACT_ARGUMENT, signature);

    // Act Asset
    assertThatThrownBy(() -> validator.validate(ledger, contract, NAMESPACE, asset))
        .isInstanceOf(ValidationException.class)
        .hasMessage(LedgerError.VALIDATION_FAILED_FOR_CONTRACT.buildMessage())
        .extracting("code")
        .isEqualTo(StatusCode.INVALID_CONTRACT);
  }

  @Test
  public void validate_AssetWithTamperedArgumentGiven_ShouldReturnInvalid() {
    // Arrange
    when(clientIdentityKey.getEntityId()).thenReturn(ENTITY_ID);
    when(clientIdentityKey.getKeyVersion()).thenReturn(CERT_VERSION);
    when(contract.getClientIdentityKey()).thenReturn(clientIdentityKey);
    when(clientKeyValidator.getValidator(anyString(), anyInt()))
        .thenReturn(new DigitalSignatureValidator(CERTIFICATE_A));
    byte[] signature =
        dsSigner.sign(
            ContractExecutionRequest.serialize(
                CONTRACT_ID, CONTRACT_ARGUMENT, ENTITY_ID, CERT_VERSION));
    String tampered = CONTRACT_ARGUMENT + "x";
    InternalAsset asset = createAssetMock(CONTRACT_ID_IN_ASSET, tampered, signature);

    // Act Asset
    assertThatThrownBy(() -> validator.validate(ledger, contract, NAMESPACE, asset))
        .isInstanceOf(ValidationException.class)
        .hasMessage(LedgerError.VALIDATION_FAILED_FOR_CONTRACT.buildMessage())
        .extracting("code")
        .isEqualTo(StatusCode.INVALID_CONTRACT);
  }

  @Test
  public void validate_AssetWithTamperedSignatureGiven_ShouldReturnInvalid() {
    // Arrange
    when(clientIdentityKey.getEntityId()).thenReturn(ENTITY_ID);
    when(clientIdentityKey.getKeyVersion()).thenReturn(CERT_VERSION);
    when(contract.getClientIdentityKey()).thenReturn(clientIdentityKey);
    when(clientKeyValidator.getValidator(anyString(), anyInt()))
        .thenReturn(new DigitalSignatureValidator(CERTIFICATE_A));
    dsSigner = new DigitalSignatureSigner(PRIVATE_KEY_B);
    byte[] tampered =
        dsSigner.sign(
            ContractExecutionRequest.serialize(
                CONTRACT_ID, CONTRACT_ARGUMENT, ENTITY_ID, CERT_VERSION));
    InternalAsset asset = createAssetMock(CONTRACT_ID_IN_ASSET, CONTRACT_ARGUMENT, tampered);

    // Act Asset
    assertThatThrownBy(() -> validator.validate(ledger, contract, NAMESPACE, asset))
        .isInstanceOf(ValidationException.class)
        .hasMessage(LedgerError.VALIDATION_FAILED_FOR_CONTRACT.buildMessage())
        .extracting("code")
        .isEqualTo(StatusCode.INVALID_CONTRACT);
  }

  @Test
  public void validate_AssetGivenAndCertificateTampered_ShouldReturnInvalid() {
    // Arrange
    when(clientIdentityKey.getEntityId()).thenReturn(ENTITY_ID);
    when(clientIdentityKey.getKeyVersion()).thenReturn(CERT_VERSION);
    when(contract.getClientIdentityKey()).thenReturn(clientIdentityKey);
    // tampered certificate B
    when(clientKeyValidator.getValidator(anyString(), anyInt()))
        .thenReturn(new DigitalSignatureValidator(CERTIFICATE_B));
    byte[] signature =
        dsSigner.sign(
            ContractExecutionRequest.serialize(
                CONTRACT_ID, CONTRACT_ARGUMENT, ENTITY_ID, CERT_VERSION));
    InternalAsset asset = createAssetMock(CONTRACT_ID_IN_ASSET, CONTRACT_ARGUMENT, signature);

    // Act Asset
    assertThatThrownBy(() -> validator.validate(ledger, contract, NAMESPACE, asset))
        .isInstanceOf(ValidationException.class)
        .hasMessage(LedgerError.VALIDATION_FAILED_FOR_CONTRACT.buildMessage())
        .extracting("code")
        .isEqualTo(StatusCode.INVALID_CONTRACT);
  }
}
