package com.scalar.dl.ledger.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.scalar.db.config.DatabaseConfig;
import com.scalar.dl.ledger.config.LedgerConfig;
import com.scalar.dl.ledger.contract.ContractEntry;
import com.scalar.dl.ledger.contract.ContractExecutor;
import com.scalar.dl.ledger.contract.ContractLoader;
import com.scalar.dl.ledger.contract.ContractManager;
import com.scalar.dl.ledger.crypto.AuditorKeyValidator;
import com.scalar.dl.ledger.crypto.CertificateEntry;
import com.scalar.dl.ledger.crypto.CertificateManager;
import com.scalar.dl.ledger.crypto.ClientKeyValidator;
import com.scalar.dl.ledger.crypto.DigitalSignatureSigner;
import com.scalar.dl.ledger.crypto.DigitalSignatureValidator;
import com.scalar.dl.ledger.crypto.SecretManager;
import com.scalar.dl.ledger.database.ContractRegistry;
import com.scalar.dl.ledger.database.Ledger;
import com.scalar.dl.ledger.database.Transaction;
import com.scalar.dl.ledger.database.TransactionManager;
import com.scalar.dl.ledger.function.FunctionManager;
import com.scalar.dl.ledger.model.ContractExecutionRequest;
import com.scalar.dl.ledger.namespace.NamespaceManager;
import com.scalar.dl.ledger.statemachine.DeprecatedLedger;
import com.scalar.dl.ledger.statemachine.DeserializationType;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.json.Json;
import javax.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class LedgerServicePermissionTest {
  private static final String BASE_PATH_NAME =
      "build/classes/java/permissionTest/com/scalar/dl/ledger/service/";
  private static final String PACKAGE = "com.scalar.dl.ledger.service.";
  private static final String CALLER_CONTRACT_ID = "GoodCallerInOrg1";
  private static final String CALLER_CONTRACT_NAME = PACKAGE + "GoodCallerContract";
  private static final String SIMPLE_CONTRACT_ID = "GoodInOrg1";
  private static final String SIMPLE_CONTRACT_NAME = PACKAGE + "GoodContract";
  private static final String BADREAD_CONTRACT_ID = "BadReadInOrg1";
  private static final String BADREAD_CONTRACT_NAME = PACKAGE + "BadReadContract";
  private static final String BADWRITE_CONTRACT_ID = "BadWriteInOrg1";
  private static final String BADWRITE_CONTRACT_NAME = PACKAGE + "BadWriteContract";
  private static final String ASSET_ATTRIBUTE_ID = "asset";
  static final String BALANCE_ATTRIBUTE_NAME = "balance";
  static final String CONTRACT_ID_ATTRIBUTE_NAME = "contract_id";
  static final String ASSET_ATTRIBUTE_NAME = "asset_id";
  static final String AMOUNT_ATTRIBUTE_NAME = "amount";
  private static final String NONCE_ATTRIBUTE_NAME = "nonce";
  private static final String ENTITY_ID = "entity_id";
  private static final int KEY_VERSION = 1;
  @Mock private TransactionManager transactionManager;
  @Mock private Transaction transaction;
  @Mock private Ledger ledger;
  @Mock private ContractRegistry registry;
  @Mock private CertificateManager certManager;
  @Mock private SecretManager secretManager;
  @Mock private DigitalSignatureSigner signer;
  @Mock private DigitalSignatureValidator validator;
  @Mock private FunctionManager functionManager;
  @Mock private ClientKeyValidator clientKeyValidator;
  @Mock private AuditorKeyValidator auditorKeyValidator;
  @Mock private NamespaceManager namespaceManager;
  private LedgerService service;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    Properties props = new Properties();
    props.setProperty(DatabaseConfig.CONTACT_POINTS, "localhost");
    props.setProperty(LedgerConfig.PROOF_ENABLED, "false");
    LedgerConfig config = new LedgerConfig(props);

    AtomicReference<ContractLoader> contractLoader = new AtomicReference<>();
    AccessController.doPrivileged(
        (PrivilegedAction<Void>)
            () -> {
              contractLoader.set(new ContractLoader(new ProtectionDomain(null, null)));
              return null;
            });
    ContractManager contractManager =
        new ContractManager(registry, contractLoader.get(), clientKeyValidator);
    ContractExecutor contractExecutor =
        new ContractExecutor(config, contractManager, functionManager, transactionManager);
    service =
        new LedgerService(
            new BaseService(
                certManager, secretManager, clientKeyValidator, contractManager, namespaceManager),
            config,
            clientKeyValidator,
            auditorKeyValidator,
            contractExecutor,
            functionManager);
    when(clientKeyValidator.getValidator(anyString(), anyInt())).thenReturn(validator);
    when(signer.sign(any())).thenReturn("any_bytes".getBytes(StandardCharsets.UTF_8));
    when(validator.validate(any(), any())).thenReturn(true);

    // Set up the security manager
    System.setProperty("java.security.manager", "default");
    System.setProperty("java.security.policy", "src/dist/security.policy");
    System.setSecurityManager(new SecurityManager());
  }

  private JsonObject prepareArgument(String contractId) {
    return Json.createObjectBuilder()
        .add(CONTRACT_ID_ATTRIBUTE_NAME, contractId)
        .add(ASSET_ATTRIBUTE_NAME, ASSET_ATTRIBUTE_ID)
        .add(AMOUNT_ATTRIBUTE_NAME, 100)
        .add(NONCE_ATTRIBUTE_NAME, Long.toString(System.currentTimeMillis()))
        .build();
  }

  private ContractEntry prepareContractEntry(
      String id, String binaryName, CertificateEntry.Key certKey) {
    ContractEntry entry = mock(ContractEntry.class);
    byte[] byteCode = null;
    try {
      String className = binaryName.substring(binaryName.lastIndexOf(".") + 1) + ".class";
      byteCode = Files.readAllBytes(new File(BASE_PATH_NAME + className).toPath());
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
    when(entry.getId()).thenReturn(id);
    when(entry.getEntityId()).thenReturn(certKey.getEntityId());
    when(entry.getKeyVersion()).thenReturn(certKey.getKeyVersion());
    when(entry.getClientIdentityKey()).thenReturn(certKey);
    when(entry.getBinaryName()).thenReturn(binaryName);
    when(entry.getByteCode()).thenReturn(byteCode);
    when(entry.getKey()).thenReturn(new ContractEntry.Key(id, certKey));
    return entry;
  }

  private ContractExecutionRequest prepareExecutionRequest(
      DigitalSignatureSigner signer, String id, JsonObject argument) {
    byte[] serialized =
        ContractExecutionRequest.serialize(id, argument.toString(), ENTITY_ID, KEY_VERSION);
    return new ContractExecutionRequest(
        UUID.randomUUID().toString(),
        ENTITY_ID,
        KEY_VERSION,
        id,
        argument.toString(),
        Collections.emptyList(),
        null,
        signer.sign(serialized),
        null);
  }

  private Ledger prepareLedger(ContractExecutionRequest request) {
    when(transactionManager.startWith(request)).thenReturn(transaction);
    doReturn(new DeprecatedLedger(ledger))
        .when(transaction)
        .getLedger(DeserializationType.DEPRECATED);
    return ledger;
  }

  @Test
  public void execute_GoodContract_ShouldExecuteProperly() {
    // Arrange
    JsonObject argument = prepareArgument(SIMPLE_CONTRACT_ID);
    CertificateEntry.Key certKey = new CertificateEntry.Key(ENTITY_ID, KEY_VERSION);
    ContractEntry simpleEntry =
        prepareContractEntry(SIMPLE_CONTRACT_ID, SIMPLE_CONTRACT_NAME, certKey);
    ContractExecutionRequest request =
        prepareExecutionRequest(signer, SIMPLE_CONTRACT_ID, argument);
    ContractEntry.Key key = new ContractEntry.Key(SIMPLE_CONTRACT_ID, certKey);
    when(registry.lookup(key)).thenReturn(simpleEntry);
    ledger = prepareLedger(request);
    doNothing().when(ledger).put(anyString(), any(JsonObject.class));

    // Act assert
    assertThatCode(() -> service.execute(request)).doesNotThrowAnyException();
  }

  @Test
  public void execute_BadContract_ShouldThrowAccessControlException() {
    // Arrange
    JsonObject argument = prepareArgument(BADWRITE_CONTRACT_ID);
    CertificateEntry.Key certKey = new CertificateEntry.Key(ENTITY_ID, KEY_VERSION);
    ContractEntry simpleEntry =
        prepareContractEntry(BADWRITE_CONTRACT_ID, BADWRITE_CONTRACT_NAME, certKey);
    ContractExecutionRequest request =
        prepareExecutionRequest(signer, BADWRITE_CONTRACT_ID, argument);
    ContractEntry.Key key = new ContractEntry.Key(BADWRITE_CONTRACT_ID, certKey);
    when(registry.lookup(key)).thenReturn(simpleEntry);
    ledger = prepareLedger(request);
    doNothing().when(ledger).put(anyString(), any(JsonObject.class));

    // Act
    Throwable thrown = catchThrowable(() -> service.execute(request));

    // Assert
    assertThat(thrown)
        .isInstanceOf(java.security.AccessControlException.class)
        .hasMessage("access denied (\"java.io.FilePermission\" \"myFile.txt\" \"write\")");
  }

  @Test
  public void execute_GoodContractCallingGoodContract_ShouldExecuteBothContracts() {
    // Arrange
    JsonObject argument = prepareArgument(SIMPLE_CONTRACT_ID);
    CertificateEntry.Key certKey = new CertificateEntry.Key(ENTITY_ID, KEY_VERSION);
    ContractEntry callerEntry =
        prepareContractEntry(CALLER_CONTRACT_ID, CALLER_CONTRACT_NAME, certKey);
    ContractEntry simpleEntry =
        prepareContractEntry(SIMPLE_CONTRACT_ID, SIMPLE_CONTRACT_NAME, certKey);
    ContractExecutionRequest request =
        prepareExecutionRequest(signer, CALLER_CONTRACT_ID, argument);
    ContractEntry.Key key1 = new ContractEntry.Key(CALLER_CONTRACT_ID, certKey);
    ContractEntry.Key key2 = new ContractEntry.Key(SIMPLE_CONTRACT_ID, certKey);
    when(registry.lookup(key1)).thenReturn(callerEntry);
    when(registry.lookup(key2)).thenReturn(simpleEntry);
    ledger = prepareLedger(request);
    doNothing().when(ledger).put(anyString(), any(JsonObject.class));

    // Act assert
    assertThatCode(() -> service.execute(request)).doesNotThrowAnyException();
  }

  @Test
  public void execute_GoodContractCallingBadReadContract_ShouldThrowAccessControlException() {
    // Arrange
    JsonObject argument = prepareArgument(BADREAD_CONTRACT_ID);
    CertificateEntry.Key certKey = new CertificateEntry.Key(ENTITY_ID, KEY_VERSION);
    ContractEntry callerEntry =
        prepareContractEntry(CALLER_CONTRACT_ID, CALLER_CONTRACT_NAME, certKey);
    ContractEntry badreadEntry =
        prepareContractEntry(BADREAD_CONTRACT_ID, BADREAD_CONTRACT_NAME, certKey);
    ContractExecutionRequest request =
        prepareExecutionRequest(signer, CALLER_CONTRACT_ID, argument);
    ContractEntry.Key key1 = new ContractEntry.Key(CALLER_CONTRACT_ID, certKey);
    ContractEntry.Key key2 = new ContractEntry.Key(BADREAD_CONTRACT_ID, certKey);
    when(registry.lookup(key1)).thenReturn(callerEntry);
    when(registry.lookup(key2)).thenReturn(badreadEntry);
    ledger = prepareLedger(request);
    doNothing().when(ledger).put(anyString(), any(JsonObject.class));

    // Act
    Throwable thrown = catchThrowable(() -> service.execute(request));

    // Assert
    assertThat(thrown)
        .isInstanceOf(java.security.AccessControlException.class)
        .hasMessageContaining("java.io.FilePermission");
  }

  @Test
  public void execute_GoodContractCallingBadWriteContract_ShouldThrowAccessControlException() {
    // Arrange
    JsonObject argument = prepareArgument(BADWRITE_CONTRACT_ID);
    CertificateEntry.Key certKey = new CertificateEntry.Key(ENTITY_ID, KEY_VERSION);
    ContractEntry callerEntry =
        prepareContractEntry(CALLER_CONTRACT_ID, CALLER_CONTRACT_NAME, certKey);
    ContractEntry badwriteEntry =
        prepareContractEntry(BADWRITE_CONTRACT_ID, BADWRITE_CONTRACT_NAME, certKey);
    ContractExecutionRequest request =
        prepareExecutionRequest(signer, CALLER_CONTRACT_ID, argument);
    ContractEntry.Key key1 = new ContractEntry.Key(CALLER_CONTRACT_ID, certKey);
    ContractEntry.Key key2 = new ContractEntry.Key(BADWRITE_CONTRACT_ID, certKey);
    when(registry.lookup(key1)).thenReturn(callerEntry);
    when(registry.lookup(key2)).thenReturn(badwriteEntry);
    ledger = prepareLedger(request);
    doNothing().when(ledger).put(anyString(), any(JsonObject.class));

    // Act
    Throwable thrown = catchThrowable(() -> service.execute(request));

    // Assert
    assertThat(thrown)
        .isInstanceOf(java.security.AccessControlException.class)
        .hasMessageContaining("java.io.FilePermission");
  }
}
