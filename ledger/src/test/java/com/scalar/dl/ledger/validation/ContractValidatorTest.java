package com.scalar.dl.ledger.validation;

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
  private static final String ENTITY_ID = "entity_id";
  private static final int CERT_VERSION = 1;
  private static final String CONTRACT_ID = "contract_id";
  private static final String CONTRACT_ARGUMENT = "contract_argument";
  private static final String CONTRACT_ID_IN_ASSET =
      ENTITY_ID + "/" + CERT_VERSION + "/" + CONTRACT_ID;
  private static final String PRIVATE_KEY_A =
      "-----BEGIN EC PRIVATE KEY-----\n"
          + "MHcCAQEEIF4SjQxTArRcZaROSFjlBP2rR8fAKtL8y+kmGiSlM5hEoAoGCCqGSM49\n"
          + "AwEHoUQDQgAEY0i/iAFxIBS3etbjoSC1/aUKQV66+wiawL4bZqklu86ObIc7wrif\n"
          + "HExPmVhKFSklOyZqGoOiVZA0zf0LZeFaPA==\n"
          + "-----END EC PRIVATE KEY-----";
  private static final String PRIVATE_KEY_B =
      "-----BEGIN EC PRIVATE KEY-----\n"
          + "MHcCAQEEIAHSsi6IZaB4aO7qbvkf4uv4HIAHNdMH2l6YDGyyYzY+oAoGCCqGSM49\n"
          + "AwEHoUQDQgAEDhDSlG3KmPN2zK16AFB68vSa4M5MLuEtNSL7c1/ul8b6HKrq9Ivo\n"
          + "xmxDUidA3pmIotkcjPtMSAxoDC98NjV2Aw==\n"
          + "-----END EC PRIVATE KEY-----";
  private static final String CERTIFICATE_A =
      "-----BEGIN CERTIFICATE-----\n"
          + "MIICQTCCAeagAwIBAgIUEKARigcZQ3sLEXdlEtjYissVx0cwCgYIKoZIzj0EAwIw\n"
          + "QTELMAkGA1UEBhMCSlAxDjAMBgNVBAgTBVRva3lvMQ4wDAYDVQQHEwVUb2t5bzES\n"
          + "MBAGA1UEChMJU2FtcGxlIENBMB4XDTE4MDYyMTAyMTUwMFoXDTE5MDYyMTAyMTUw\n"
          + "MFowRTELMAkGA1UEBhMCSlAxDjAMBgNVBAgTBVRva3lvMQ4wDAYDVQQHEwVUb2t5\n"
          + "bzEWMBQGA1UEChMNU2FtcGxlIENsaWVudDBZMBMGByqGSM49AgEGCCqGSM49AwEH\n"
          + "A0IABGNIv4gBcSAUt3rW46Egtf2lCkFeuvsImsC+G2apJbvOjmyHO8K4nxxMT5lY\n"
          + "ShUpJTsmahqDolWQNM39C2XhWjyjgbcwgbQwDgYDVR0PAQH/BAQDAgWgMB0GA1Ud\n"
          + "JQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjAMBgNVHRMBAf8EAjAAMB0GA1UdDgQW\n"
          + "BBTpBQl/JxB7yr77uMVT9mMicPeVJTAfBgNVHSMEGDAWgBQrJo3N3/0j3oPS6F6m\n"
          + "wunHe8xLpzA1BgNVHREELjAsghJjbGllbnQuZXhhbXBsZS5jb22CFnd3dy5jbGll\n"
          + "bnQuZXhhbXBsZS5jb20wCgYIKoZIzj0EAwIDSQAwRgIhAJPtXSzuncDJXnM+7us8\n"
          + "46MEVjGHJy70bRY1My23RkxbAiEA5oFgTKMvls8e4UpnmUgFNP+FH8a5bF4tUPaV\n"
          + "BQiBbgk=\n"
          + "-----END CERTIFICATE-----";
  private static final String CERTIFICATE_B =
      "-----BEGIN CERTIFICATE-----\n"
          + "MIICjDCCAjKgAwIBAgIUTnLDk2Y+84DRD8bbQuZE1xlxidkwCgYIKoZIzj0EAwIw\n"
          + "bzELMAkGA1UEBhMCSlAxDjAMBgNVBAgTBVRva3lvMQ4wDAYDVQQHEwVUb2t5bzEf\n"
          + "MB0GA1UEChMWU2FtcGxlIEludGVybWVkaWF0ZSBDQTEfMB0GA1UEAxMWU2FtcGxl\n"
          + "IEludGVybWVkaWF0ZSBDQTAeFw0xODA4MDkwNzAwMDBaFw0yMTA4MDgwNzAwMDBa\n"
          + "MEUxCzAJBgNVBAYTAkFVMRMwEQYDVQQIEwpTb21lLVN0YXRlMSEwHwYDVQQKExhJ\n"
          + "bnRlcm5ldCBXaWRnaXRzIFB0eSBMdGQwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNC\n"
          + "AAQOENKUbcqY83bMrXoAUHry9Jrgzkwu4S01IvtzX+6Xxvocqur0i+jGbENSJ0De\n"
          + "mYii2RyM+0xIDGgML3w2NXYDo4HVMIHSMA4GA1UdDwEB/wQEAwIFoDATBgNVHSUE\n"
          + "DDAKBggrBgEFBQcDAjAMBgNVHRMBAf8EAjAAMB0GA1UdDgQWBBSsklJebvmvOepv\n"
          + "QhvsCVFO4h+z+jAfBgNVHSMEGDAWgBT0HscZ7eRWv8QlQgfbtaT7BDNQEzAxBggr\n"
          + "BgEFBQcBAQQlMCMwIQYIKwYBBQUHMAGGFWh0dHA6Ly9sb2NhbGhvc3Q6ODg4OTAq\n"
          + "BgNVHR8EIzAhMB+gHaAbhhlodHRwOi8vbG9jYWxob3N0Ojg4ODgvY3JsMAoGCCqG\n"
          + "SM49BAMCA0gAMEUCIAJavUnxqZm/a/szytCNdmESZdL++H71+YHHuTkxud8DAiEA\n"
          + "6GUKwnt7oDqLgoavBNhBVmbmxMJjo+D3YEwTOJ/X4bs=\n"
          + "-----END CERTIFICATE-----";
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
    StatusCode result = validator.validate(ledger, contract, asset);

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
    assertThatThrownBy(() -> validator.validate(ledger, contract, asset))
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
    assertThatThrownBy(() -> validator.validate(ledger, contract, asset))
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
    assertThatThrownBy(() -> validator.validate(ledger, contract, asset))
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
    assertThatThrownBy(() -> validator.validate(ledger, contract, asset))
        .isInstanceOf(ValidationException.class)
        .hasMessage(LedgerError.VALIDATION_FAILED_FOR_CONTRACT.buildMessage())
        .extracting("code")
        .isEqualTo(StatusCode.INVALID_CONTRACT);
  }
}
