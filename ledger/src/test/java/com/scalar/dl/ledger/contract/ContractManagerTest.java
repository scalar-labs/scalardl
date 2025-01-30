package com.scalar.dl.ledger.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.scalar.dl.ledger.crypto.CertificateEntry;
import com.scalar.dl.ledger.crypto.ClientKeyValidator;
import com.scalar.dl.ledger.crypto.DigitalSignatureSigner;
import com.scalar.dl.ledger.crypto.DigitalSignatureValidator;
import com.scalar.dl.ledger.database.ContractRegistry;
import com.scalar.dl.ledger.exception.ContractValidationException;
import com.scalar.dl.ledger.exception.DatabaseException;
import com.scalar.dl.ledger.exception.MissingContractException;
import com.scalar.dl.ledger.exception.UnloadableContractException;
import com.scalar.dl.ledger.model.ContractRegistrationRequest;
import java.nio.charset.StandardCharsets;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ContractManagerTest {
  private static final String PRIVATE_KEY_A =
      "-----BEGIN EC PRIVATE KEY-----\n"
          + "MHcCAQEEIF4SjQxTArRcZaROSFjlBP2rR8fAKtL8y+kmGiSlM5hEoAoGCCqGSM49\n"
          + "AwEHoUQDQgAEY0i/iAFxIBS3etbjoSC1/aUKQV66+wiawL4bZqklu86ObIc7wrif\n"
          + "HExPmVhKFSklOyZqGoOiVZA0zf0LZeFaPA==\n"
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
  private static final String ANY_CONTRACT_ID = "MyCreate";
  private static final String ANY_CONTRACT_NAME = "Create";
  private static final byte[] ANY_BYTE_CODE = "byte_code".getBytes(StandardCharsets.UTF_8);
  private static final String ANY_ENTITY_ID = "entity_id";
  private static final int ANY_CERT_VERSION = 1;
  @Mock private ContractRegistry registry;
  @Mock private ContractLoader loader;
  @Mock private ClientKeyValidator clientKeyValidator;
  @Mock private ContractEntry entry;
  @Mock private DigitalSignatureValidator validator;
  private ContractManager manager;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    manager = spy(new ContractManager(registry, loader, clientKeyValidator));

    doReturn(validator).when(clientKeyValidator).getValidator(anyString(), anyInt());
    when(validator.validate(any(), any())).thenReturn(true);
    prepareContractEntry();
  }

  private void prepareContractEntry() {
    when(entry.getId()).thenReturn(ANY_CONTRACT_ID);
    when(entry.getBinaryName()).thenReturn(ANY_CONTRACT_NAME);
    when(entry.getByteCode()).thenReturn(ANY_BYTE_CODE);
    when(entry.getEntityId()).thenReturn(ANY_ENTITY_ID);
    when(entry.getKeyVersion()).thenReturn(ANY_CERT_VERSION);
    CertificateEntry.Key certKey = new CertificateEntry.Key(ANY_ENTITY_ID, ANY_CERT_VERSION);
    when(entry.getClientIdentityKey()).thenReturn(certKey);
    when(entry.getKey()).thenReturn(new ContractEntry.Key(ANY_CONTRACT_ID, certKey));
  }

  private ContractEntry createContractEntryWith() {
    DigitalSignatureSigner signer = new DigitalSignatureSigner(PRIVATE_KEY_A);
    byte[] serialized =
        ContractRegistrationRequest.serialize(
            ANY_CONTRACT_ID,
            ANY_CONTRACT_NAME,
            ANY_BYTE_CODE,
            null,
            ANY_ENTITY_ID,
            ANY_CERT_VERSION);
    return new ContractEntry(
        ANY_CONTRACT_ID,
        ANY_CONTRACT_NAME,
        ANY_ENTITY_ID,
        ANY_CERT_VERSION,
        ANY_BYTE_CODE,
        null,
        1L,
        signer.sign(serialized));
  }

  @Test
  public void register_ContractEntryGiven_ShouldBind() {
    // Arrange
    when(registry.lookup(entry.getKey())).thenThrow(MissingContractException.class);
    doReturn(TestJsonpBasedContract.class).when(loader).defineClass(entry);

    // Act
    manager.register(entry);

    // Assert
    verify(registry).lookup(entry.getKey());
    verify(validator).validate(any(), any());
    verify(manager).defineClass(entry);
    verify(registry).bind(entry);
  }

  @Test
  public void
      register_ContractEntryForAlreadyRegisteredContractGiven_ShouldThrowDatabaseException() {
    // Arrange
    when(registry.lookup(entry.getKey())).thenReturn(entry);

    // Act Assert
    assertThatThrownBy(() -> manager.register(entry)).isInstanceOf(DatabaseException.class);

    verify(validator, never()).validate(any(), any());
    verify(manager, never()).defineClass(entry);
    verify(registry, never()).bind(entry);
  }

  @Test
  public void register_ValidationFailed_ShouldThrowException() {
    // Arrange
    when(registry.lookup(entry.getKey())).thenThrow(MissingContractException.class);
    when(validator.validate(any(), any())).thenReturn(false);

    // Act Assert
    Throwable thrown = catchThrowable(() -> manager.register(entry));

    assertThat(thrown).isExactlyInstanceOf(ContractValidationException.class);
    verify(registry, never()).bind(entry);
  }

  @Test
  public void register_LoadFailedWithRuntimeException_ShouldThrowException() {
    // Arrange
    when(registry.lookup(entry.getKey())).thenThrow(MissingContractException.class);
    doThrow(RuntimeException.class).when(loader).defineClass(entry);

    // Act Assert
    Throwable thrown = catchThrowable(() -> manager.register(entry));

    assertThat(thrown).isExactlyInstanceOf(UnloadableContractException.class);
    verify(manager).defineClass(entry);
    verify(registry, never()).bind(entry);
  }

  @Test
  public void getInstance_ContractEntryForContractGiven_ShouldReturnContract() {
    // Arrange
    // NOTICE: it doesn't work if TestContract is defined as an inner class of this
    Class<? extends Contract> clazz = TestContract.class;
    doReturn(clazz).when(loader).defineClass(entry);

    // Act
    ContractMachine contract = manager.getInstance(entry);

    // Assert
    verify(loader).defineClass(entry);
    verify(manager).validateContract(entry);
    assertThat(contract.isRoot()).isFalse();
    assertThat(contract.getClientIdentityKey().getEntityId()).isEqualTo(ANY_ENTITY_ID);
    assertThat(contract.getClientIdentityKey().getKeyVersion()).isEqualTo(ANY_CERT_VERSION);
    assertThat(contract.getContractBase()).isInstanceOf(DeprecatedContract.class);
  }

  @Test
  public void getInstance_ContractEntryForContractBaseGiven_ShouldReturnContractBase() {
    // Arrange
    // NOTICE: it doesn't work if TestContract is defined as an inner class of this
    Class<? extends ContractBase<?>> clazz = TestJsonpBasedContract.class;
    doReturn(clazz).when(loader).defineClass(entry);

    // Act
    ContractMachine contract = manager.getInstance(entry);

    // Assert
    verify(loader).defineClass(entry);
    verify(manager).validateContract(entry);
    assertThat(contract.isRoot()).isFalse();
    assertThat(contract.getClientIdentityKey().getEntityId()).isEqualTo(ANY_ENTITY_ID);
    assertThat(contract.getClientIdentityKey().getKeyVersion()).isEqualTo(ANY_CERT_VERSION);
    assertThat(contract.getContractBase()).isInstanceOf(JsonpBasedContract.class);
  }

  @Test
  public void getInstance_SameEntryGiven_ShouldReturnContractWithoutValidation() {
    // Arrange
    // NOTICE: it doesn't work if TestContract is defined as an inner class of this
    Class<? extends ContractBase<?>> clazz = TestJsonpBasedContract.class;
    doReturn(clazz).when(loader).defineClass(entry);

    // Act
    manager.getInstance(entry);
    manager.getInstance(entry);

    // Assert
    verify(loader, times(2)).defineClass(entry);
    verify(manager).validateContract(entry);
  }

  @Test
  public void getInstance_SameEntryGivenAfterExpired_ShouldReturnContractWithValidation() {
    // Arrange
    // it expires right after put
    Cache<ContractEntry.Key, Object> cache = CacheBuilder.newBuilder().maximumSize(0).build();
    manager = spy(new ContractManager(registry, loader, clientKeyValidator, cache));
    // NOTICE: it doesn't work if TestContract is defined as an inner class of this
    Class<? extends ContractBase<?>> clazz = TestJsonpBasedContract.class;
    doReturn(clazz).when(loader).defineClass(entry);

    // Act
    manager.getInstance(entry);
    manager.getInstance(entry);

    // Assert
    verify(loader, times(2)).defineClass(entry);
    verify(manager, times(2)).validateContract(entry);
  }

  @Test
  public void
      getInstance_ContractEntryForContractBaseGivenValidationSucceeded_ShouldReturnContractBase() {
    // Arrange
    ContractEntry entry = createContractEntryWith();
    DigitalSignatureValidator validator = new DigitalSignatureValidator(CERTIFICATE_A);
    doReturn(validator).when(clientKeyValidator).getValidator(anyString(), anyInt());
    manager = spy(new ContractManager(registry, loader, clientKeyValidator));
    // NOTICE: it doesn't work if TestContract is defined as an inner class of this
    Class<? extends ContractBase<?>> clazz = TestJsonpBasedContract.class;
    doReturn(clazz).when(loader).defineClass(entry);

    // Act
    ContractMachine contract = manager.getInstance(entry);

    // Assert
    verify(manager).validateContract(entry);
    verify(loader).defineClass(entry);
    assertThat(contract.isRoot()).isFalse();
    assertThat(contract.getClientIdentityKey().getEntityId()).isEqualTo(ANY_ENTITY_ID);
    assertThat(contract.getClientIdentityKey().getKeyVersion()).isEqualTo(ANY_CERT_VERSION);
  }

  @Test
  public void
      getInstance_ContractEntryForContractBaseGivenAndValidationFailed_ShouldThrowContractValidationException() {
    // Arrange
    ContractEntry entry = createContractEntryWith();
    // It will happen in case the certificate entry is tampered
    DigitalSignatureValidator validator = new DigitalSignatureValidator(CERTIFICATE_B);
    doReturn(validator).when(clientKeyValidator).getValidator(anyString(), anyInt());
    manager = spy(new ContractManager(registry, loader, clientKeyValidator));
    // NOTICE: it doesn't work if TestContract is defined as an inner class of this
    Class<? extends ContractBase<?>> clazz = TestJsonpBasedContract.class;
    doReturn(clazz).when(loader).defineClass(entry);

    // Act
    AssertionsForClassTypes.assertThatThrownBy(() -> manager.getInstance(entry))
        .isInstanceOf(ContractValidationException.class);

    // Assert
    verify(manager).validateContract(entry);
    verify(loader, never()).defineClass(entry);
  }

  @Test
  public void
      getInstance_ContractEntryForContractBaseGivenAndDefineClassFailed_ShouldThrowContractValidationException() {
    // Arrange
    ContractEntry entry = createContractEntryWith();
    DigitalSignatureValidator validator = new DigitalSignatureValidator(CERTIFICATE_A);
    doReturn(validator).when(clientKeyValidator).getValidator(anyString(), anyInt());
    manager = spy(new ContractManager(registry, loader, clientKeyValidator));
    SecurityException toThrow = mock(SecurityException.class);
    doThrow(toThrow).when(loader).defineClass(entry);

    // Act
    AssertionsForClassTypes.assertThatThrownBy(() -> manager.getInstance(entry))
        .isInstanceOf(UnloadableContractException.class)
        .hasCause(toThrow);

    // Assert
    verify(manager).validateContract(entry);
    verify(loader).defineClass(entry);
  }
}
