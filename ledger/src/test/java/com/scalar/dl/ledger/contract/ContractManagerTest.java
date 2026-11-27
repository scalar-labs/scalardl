package com.scalar.dl.ledger.contract;

import static com.scalar.dl.ledger.test.TestConstants.CERTIFICATE_A;
import static com.scalar.dl.ledger.test.TestConstants.CERTIFICATE_B;
import static com.scalar.dl.ledger.test.TestConstants.PRIVATE_KEY_A;
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
import com.scalar.dl.ledger.crypto.HmacSigner;
import com.scalar.dl.ledger.crypto.HmacValidator;
import com.scalar.dl.ledger.crypto.SignatureSigner;
import com.scalar.dl.ledger.database.ContractRegistry;
import com.scalar.dl.ledger.error.CommonError;
import com.scalar.dl.ledger.exception.ContractValidationException;
import com.scalar.dl.ledger.exception.DatabaseException;
import com.scalar.dl.ledger.exception.MissingContractException;
import com.scalar.dl.ledger.exception.UnloadableContractException;
import com.scalar.dl.ledger.model.ContractRegistrationRequest;
import com.scalar.dl.ledger.namespace.Namespaces;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nullable;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ContractManagerTest {
  private static final String ANY_NAMESPACE = "test_namespace";
  private static final String ANY_CONTRACT_ID = "MyCreate";
  private static final String ANY_CONTRACT_NAME = "Create";
  private static final byte[] ANY_BYTE_CODE = "byte_code".getBytes(StandardCharsets.UTF_8);
  private static final String ANY_ENTITY_ID = "entity_id";
  private static final int ANY_CERT_VERSION = 1;
  private static final String ANY_SECRET_KEY = "secret_key";
  @Mock private ContractRegistry registry;
  @Mock private ContractLoader loader;
  @Mock private ClientKeyValidator clientKeyValidator;
  @Mock private ContractValidationFallbackPolicy fallbackPolicy;
  @Mock private ContractEntry entry;
  @Mock private DigitalSignatureValidator validator;
  private ContractManager manager;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    manager = spy(new ContractManager(registry, loader, clientKeyValidator, fallbackPolicy));

    doReturn(validator).when(clientKeyValidator).getValidator(anyString(), anyString(), anyInt());
    when(validator.validate(any(), any())).thenReturn(true);
    when(fallbackPolicy.isFallbackAllowed(anyString())).thenReturn(true);
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
    return createContractEntrySignedFor(ANY_NAMESPACE);
  }

  private ContractEntry createContractEntrySignedFor(@Nullable String signedNamespace) {
    return createContractEntrySignedFor(new DigitalSignatureSigner(PRIVATE_KEY_A), signedNamespace);
  }

  private ContractEntry createContractEntrySignedFor(
      SignatureSigner signer, @Nullable String signedNamespace) {
    byte[] serialized =
        ContractRegistrationRequest.serialize(
            ANY_CONTRACT_ID,
            ANY_CONTRACT_NAME,
            ANY_BYTE_CODE,
            null,
            signedNamespace,
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
    when(registry.lookup(ANY_NAMESPACE, entry.getKey())).thenThrow(MissingContractException.class);
    doReturn(TestJsonpBasedContract.class).when(loader).defineClass(entry);

    // Act
    manager.register(ANY_NAMESPACE, entry);

    // Assert
    verify(registry).lookup(ANY_NAMESPACE, entry.getKey());
    verify(validator).validate(any(), any());
    verify(manager).defineClass(entry);
    verify(registry).bind(ANY_NAMESPACE, entry);
  }

  @Test
  public void
      register_ContractEntryForAlreadyRegisteredContractGiven_ShouldThrowDatabaseException() {
    // Arrange
    when(registry.lookup(ANY_NAMESPACE, entry.getKey())).thenReturn(entry);

    // Act Assert
    assertThatThrownBy(() -> manager.register(ANY_NAMESPACE, entry))
        .isInstanceOf(DatabaseException.class);

    verify(validator, never()).validate(any(), any());
    verify(manager, never()).defineClass(entry);
    verify(registry, never()).bind(ANY_NAMESPACE, entry);
  }

  @Test
  public void register_ValidationFailed_ShouldThrowException() {
    // Arrange
    when(registry.lookup(ANY_NAMESPACE, entry.getKey())).thenThrow(MissingContractException.class);
    when(validator.validate(any(), any())).thenReturn(false);

    // Act Assert
    Throwable thrown = catchThrowable(() -> manager.register(ANY_NAMESPACE, entry));

    assertThat(thrown).isExactlyInstanceOf(ContractValidationException.class);
    verify(registry, never()).bind(ANY_NAMESPACE, entry);
  }

  @Test
  public void register_LoadFailedWithRuntimeException_ShouldThrowException() {
    // Arrange
    when(registry.lookup(ANY_NAMESPACE, entry.getKey())).thenThrow(MissingContractException.class);
    doThrow(RuntimeException.class).when(loader).defineClass(entry);

    // Act Assert
    Throwable thrown = catchThrowable(() -> manager.register(ANY_NAMESPACE, entry));

    assertThat(thrown).isExactlyInstanceOf(UnloadableContractException.class);
    verify(manager).defineClass(entry);
    verify(registry, never()).bind(ANY_NAMESPACE, entry);
  }

  @Test
  public void getInstance_ContractEntryForContractGiven_ShouldReturnContract() {
    // Arrange
    // NOTICE: it doesn't work if TestContract is defined as an inner class of this
    Class<? extends Contract> clazz = TestContract.class;
    doReturn(clazz).when(loader).defineClass(entry);

    // Act
    ContractMachine contract = manager.getInstance(ANY_NAMESPACE, entry);

    // Assert
    verify(loader).defineClass(entry);
    verify(manager).validateRegisteredContract(ANY_NAMESPACE, entry);
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
    ContractMachine contract = manager.getInstance(ANY_NAMESPACE, entry);

    // Assert
    verify(loader).defineClass(entry);
    verify(manager).validateRegisteredContract(ANY_NAMESPACE, entry);
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
    manager.getInstance(ANY_NAMESPACE, entry);
    manager.getInstance(ANY_NAMESPACE, entry);

    // Assert
    verify(loader, times(2)).defineClass(entry);
    verify(manager).validateRegisteredContract(ANY_NAMESPACE, entry);
  }

  @Test
  public void getInstance_SameEntryGivenAfterExpired_ShouldReturnContractWithValidation() {
    // Arrange
    // it expires right after put
    ConcurrentMap<String, Cache<ContractEntry.Key, Object>> caches = new ConcurrentHashMap<>();
    Cache<ContractEntry.Key, Object> cache = CacheBuilder.newBuilder().maximumSize(0).build();
    caches.put(ANY_NAMESPACE, cache);
    manager =
        spy(new ContractManager(registry, loader, clientKeyValidator, fallbackPolicy, caches));
    // NOTICE: it doesn't work if TestContract is defined as an inner class of this
    Class<? extends ContractBase<?>> clazz = TestJsonpBasedContract.class;
    doReturn(clazz).when(loader).defineClass(entry);

    // Act
    manager.getInstance(ANY_NAMESPACE, entry);
    manager.getInstance(ANY_NAMESPACE, entry);

    // Assert
    verify(loader, times(2)).defineClass(entry);
    verify(manager, times(2)).validateRegisteredContract(ANY_NAMESPACE, entry);
  }

  @Test
  public void
      getInstance_ContractEntryForContractBaseGivenValidationSucceeded_ShouldReturnContractBase() {
    // Arrange
    ContractEntry entry = createContractEntryWith();
    DigitalSignatureValidator validator = new DigitalSignatureValidator(CERTIFICATE_A);
    doReturn(validator).when(clientKeyValidator).getValidator(anyString(), anyString(), anyInt());
    manager = spy(new ContractManager(registry, loader, clientKeyValidator, fallbackPolicy));
    // NOTICE: it doesn't work if TestContract is defined as an inner class of this
    Class<? extends ContractBase<?>> clazz = TestJsonpBasedContract.class;
    doReturn(clazz).when(loader).defineClass(entry);

    // Act
    ContractMachine contract = manager.getInstance(ANY_NAMESPACE, entry);

    // Assert
    verify(manager).validateRegisteredContract(ANY_NAMESPACE, entry);
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
    doReturn(validator).when(clientKeyValidator).getValidator(anyString(), anyString(), anyInt());
    manager = spy(new ContractManager(registry, loader, clientKeyValidator, fallbackPolicy));
    // NOTICE: it doesn't work if TestContract is defined as an inner class of this
    Class<? extends ContractBase<?>> clazz = TestJsonpBasedContract.class;
    doReturn(clazz).when(loader).defineClass(entry);

    // Act
    AssertionsForClassTypes.assertThatThrownBy(() -> manager.getInstance(ANY_NAMESPACE, entry))
        .isInstanceOf(ContractValidationException.class);

    // Assert
    verify(manager).validateRegisteredContract(ANY_NAMESPACE, entry);
    verify(loader, never()).defineClass(entry);
  }

  @Test
  public void
      getInstance_ContractEntryForContractBaseGivenAndDefineClassFailed_ShouldThrowContractValidationException() {
    // Arrange
    ContractEntry entry = createContractEntryWith();
    DigitalSignatureValidator validator = new DigitalSignatureValidator(CERTIFICATE_A);
    doReturn(validator).when(clientKeyValidator).getValidator(anyString(), anyString(), anyInt());
    manager = spy(new ContractManager(registry, loader, clientKeyValidator, fallbackPolicy));
    SecurityException toThrow = mock(SecurityException.class);
    when(toThrow.getMessage()).thenReturn("details");
    doThrow(toThrow).when(loader).defineClass(entry);

    // Act
    AssertionsForClassTypes.assertThatThrownBy(() -> manager.getInstance(ANY_NAMESPACE, entry))
        .isInstanceOf(UnloadableContractException.class)
        .hasMessage(CommonError.LOADING_CONTRACT_FAILED.buildMessage("details"));

    // Assert
    verify(manager).validateRegisteredContract(ANY_NAMESPACE, entry);
    verify(loader).defineClass(entry);
  }

  private DigitalSignatureValidator prepareRealValidator() {
    DigitalSignatureValidator validator = spy(new DigitalSignatureValidator(CERTIFICATE_A));
    doReturn(validator).when(clientKeyValidator).getValidator(anyString(), anyString(), anyInt());
    manager = spy(new ContractManager(registry, loader, clientKeyValidator, fallbackPolicy));
    return validator;
  }

  @Test
  public void
      getInstance_ContractSignedForDefaultNamespaceGivenInNonDefaultNamespace_ShouldReturnContractWithFallbackValidation() {
    // Arrange
    ContractEntry entry = createContractEntrySignedFor(null);
    DigitalSignatureValidator validator = prepareRealValidator();
    Class<? extends ContractBase<?>> clazz = TestJsonpBasedContract.class;
    doReturn(clazz).when(loader).defineClass(entry);

    // Act
    ContractMachine contract = manager.getInstance(ANY_NAMESPACE, entry);
    manager.getInstance(ANY_NAMESPACE, entry);

    // Assert
    assertThat(contract.getContractBase()).isInstanceOf(JsonpBasedContract.class);
    // The successful fallback validation is cached as usual.
    verify(manager).validateRegisteredContract(ANY_NAMESPACE, entry);
    verify(validator, times(2)).validate(any(), any());
    verify(loader, times(2)).defineClass(entry);
  }

  @Test
  public void
      getInstance_ContractSignedForDefaultNamespaceGivenInNamespaceWhereFallbackIsNotAllowed_ShouldThrowContractValidationException() {
    // Arrange
    ContractEntry entry = createContractEntrySignedFor(null);
    DigitalSignatureValidator validator = prepareRealValidator();
    when(fallbackPolicy.isFallbackAllowed(ANY_NAMESPACE)).thenReturn(false);

    // Act Assert
    assertThatThrownBy(() -> manager.getInstance(ANY_NAMESPACE, entry))
        .isInstanceOf(ContractValidationException.class);

    verify(fallbackPolicy).isFallbackAllowed(ANY_NAMESPACE);
    verify(validator).validate(any(), any());
    verify(loader, never()).defineClass(entry);
  }

  @Test
  public void
      getInstance_ContractSignedForSameNamespaceGiven_ShouldReturnContractWithoutFallbackValidation() {
    // Arrange
    ContractEntry entry = createContractEntrySignedFor(ANY_NAMESPACE);
    DigitalSignatureValidator validator = prepareRealValidator();
    Class<? extends ContractBase<?>> clazz = TestJsonpBasedContract.class;
    doReturn(clazz).when(loader).defineClass(entry);

    // Act
    manager.getInstance(ANY_NAMESPACE, entry);

    // Assert
    verify(fallbackPolicy, never()).isFallbackAllowed(anyString());
    verify(validator).validate(any(), any());
    verify(loader).defineClass(entry);
  }

  @Test
  public void
      getInstance_ContractSignedForAnotherNamespaceGiven_ShouldThrowContractValidationExceptionEveryTime() {
    // Arrange
    ContractEntry entry = createContractEntrySignedFor("another_namespace");
    DigitalSignatureValidator validator = prepareRealValidator();

    // Act Assert
    assertThatThrownBy(() -> manager.getInstance(ANY_NAMESPACE, entry))
        .isInstanceOf(ContractValidationException.class);
    assertThatThrownBy(() -> manager.getInstance(ANY_NAMESPACE, entry))
        .isInstanceOf(ContractValidationException.class);

    // The failed validation is not cached.
    verify(manager, times(2)).validateRegisteredContract(ANY_NAMESPACE, entry);
    verify(validator, times(4)).validate(any(), any());
    verify(loader, never()).defineClass(entry);
  }

  @Test
  public void
      getInstance_ContractSignedForAnotherNamespaceGivenWithNamespaceAppendedToProperties_ShouldReturnContractWithFallbackValidation() {
    // Arrange
    String anotherNamespace = "another_namespace";
    String properties = "{\"key\":\"value\"}";
    byte[] serialized =
        ContractRegistrationRequest.serialize(
            ANY_CONTRACT_ID,
            ANY_CONTRACT_NAME,
            ANY_BYTE_CODE,
            properties,
            anotherNamespace,
            ANY_ENTITY_ID,
            ANY_CERT_VERSION);
    // The signed data has no delimiters and does not include the default namespace, so the entry
    // signed for another namespace is the same as the one signed for the default namespace with
    // the properties followed by the name of the namespace.
    ContractEntry entry =
        new ContractEntry(
            ANY_CONTRACT_ID,
            ANY_CONTRACT_NAME,
            ANY_ENTITY_ID,
            ANY_CERT_VERSION,
            ANY_BYTE_CODE,
            properties + anotherNamespace,
            1L,
            new DigitalSignatureSigner(PRIVATE_KEY_A).sign(serialized));
    DigitalSignatureValidator validator = prepareRealValidator();
    Class<? extends ContractBase<?>> clazz = TestJsonpBasedContract.class;
    doReturn(clazz).when(loader).defineClass(entry);

    // Act
    ContractMachine contract = manager.getInstance(ANY_NAMESPACE, entry);

    // Assert
    assertThat(contract.getContractBase()).isInstanceOf(JsonpBasedContract.class);
    verify(validator, times(2)).validate(any(), any());
    verify(loader).defineClass(entry);
  }

  @Test
  public void
      getInstance_ContractSignedForDefaultNamespaceGivenWithDifferentKey_ShouldThrowContractValidationException() {
    // Arrange
    ContractEntry entry = createContractEntrySignedFor(null);
    // The namespace has a different key for the same entity ID and key version
    DigitalSignatureValidator validator = spy(new DigitalSignatureValidator(CERTIFICATE_B));
    doReturn(validator).when(clientKeyValidator).getValidator(anyString(), anyString(), anyInt());
    manager = spy(new ContractManager(registry, loader, clientKeyValidator, fallbackPolicy));

    // Act Assert
    assertThatThrownBy(() -> manager.getInstance(ANY_NAMESPACE, entry))
        .isInstanceOf(ContractValidationException.class);

    verify(validator, times(2)).validate(any(), any());
    verify(loader, never()).defineClass(entry);
  }

  @Test
  public void
      getInstance_ContractSignedForNonDefaultNamespaceGivenInDefaultNamespace_ShouldThrowContractValidationExceptionWithoutFallbackValidation() {
    // Arrange
    ContractEntry entry = createContractEntrySignedFor(ANY_NAMESPACE);
    DigitalSignatureValidator validator = prepareRealValidator();

    // Act Assert
    assertThatThrownBy(() -> manager.getInstance(Namespaces.DEFAULT, entry))
        .isInstanceOf(ContractValidationException.class);

    verify(fallbackPolicy, never()).isFallbackAllowed(anyString());
    verify(validator).validate(any(), any());
    verify(loader, never()).defineClass(entry);
  }

  @Test
  public void
      register_ContractSignedForDefaultNamespaceGivenInNonDefaultNamespace_ShouldThrowContractValidationException() {
    // Arrange
    ContractEntry entry = createContractEntrySignedFor(null);
    DigitalSignatureValidator validator = prepareRealValidator();
    when(registry.lookup(ANY_NAMESPACE, entry.getKey())).thenThrow(MissingContractException.class);

    // Act Assert
    assertThatThrownBy(() -> manager.register(ANY_NAMESPACE, entry))
        .isInstanceOf(ContractValidationException.class);

    verify(validator).validate(any(), any());
    verify(loader, never()).defineClass(entry);
    verify(registry, never()).bind(ANY_NAMESPACE, entry);
  }

  private HmacValidator prepareRealHmacValidator() {
    HmacValidator validator = spy(new HmacValidator(ANY_SECRET_KEY));
    doReturn(validator).when(clientKeyValidator).getValidator(anyString(), anyString(), anyInt());
    manager = spy(new ContractManager(registry, loader, clientKeyValidator, fallbackPolicy));
    return validator;
  }

  @Test
  public void
      getInstance_ContractSignedWithHmacForDefaultNamespaceGivenInNonDefaultNamespace_ShouldReturnContractWithFallbackValidation() {
    // Arrange
    ContractEntry entry = createContractEntrySignedFor(new HmacSigner(ANY_SECRET_KEY), null);
    HmacValidator validator = prepareRealHmacValidator();
    Class<? extends ContractBase<?>> clazz = TestJsonpBasedContract.class;
    doReturn(clazz).when(loader).defineClass(entry);

    // Act
    ContractMachine contract = manager.getInstance(ANY_NAMESPACE, entry);

    // Assert
    assertThat(contract.getContractBase()).isInstanceOf(JsonpBasedContract.class);
    verify(validator, times(2)).validate(any(), any());
    verify(loader).defineClass(entry);
  }

  @Test
  public void
      getInstance_ContractSignedWithHmacForAnotherNamespaceGiven_ShouldThrowContractValidationException() {
    // Arrange
    ContractEntry entry =
        createContractEntrySignedFor(new HmacSigner(ANY_SECRET_KEY), "another_namespace");
    HmacValidator validator = prepareRealHmacValidator();

    // Act Assert
    assertThatThrownBy(() -> manager.getInstance(ANY_NAMESPACE, entry))
        .isInstanceOf(ContractValidationException.class);

    verify(validator, times(2)).validate(any(), any());
    verify(loader, never()).defineClass(entry);
  }
}
