package com.scalar.dl.ledger.database.scalardb;

import static com.scalar.dl.ledger.database.scalardb.AbstractScalarNamespaceRegistry.NAMESPACE_COLUMN_NAME;
import static com.scalar.dl.ledger.database.scalardb.AbstractScalarNamespaceRegistry.NAMESPACE_NAME_SEPARATOR;
import static com.scalar.dl.ledger.database.scalardb.AbstractScalarNamespaceRegistry.NAMESPACE_TABLE_METADATA;
import static com.scalar.dl.ledger.database.scalardb.AbstractScalarNamespaceRegistry.NAMESPACE_TABLE_NAME;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.scalar.db.api.ConditionBuilder;
import com.scalar.db.api.DistributedStorage;
import com.scalar.db.api.DistributedStorageAdmin;
import com.scalar.db.api.DistributedTransactionAdmin;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.TableMetadata;
import com.scalar.db.common.CoreError;
import com.scalar.db.exception.storage.ExecutionException;
import com.scalar.db.exception.storage.NoMutationException;
import com.scalar.db.io.Key;
import com.scalar.dl.ledger.config.LedgerConfig;
import com.scalar.dl.ledger.error.CommonError;
import com.scalar.dl.ledger.exception.DatabaseException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class LedgerNamespaceRegistryTest {
  private static final String BASE_NAMESPACE = "scalar";
  private static final String SOME_NAMESPACE = "some_namespace";
  private static final String SOME_TABLE_NAME_1 = "some_table_name_1";
  private static final String SOME_TABLE_NAME_2 = "some_table_name_2";
  private static final String SOME_TABLE_NAME_3 = "some_table_name_3";
  private static final String SOME_TABLE_NAME_4 = "some_table_name_4";
  @Mock private LedgerConfig config;
  @Mock private DistributedStorage storage;
  @Mock private DistributedStorageAdmin storageAdmin;
  @Mock private DistributedTransactionAdmin transactionAdmin;
  @Mock private ScalarTransactionManager transactionManager;
  @Mock private ScalarCertificateRegistry certificateRegistry;
  @Mock private ScalarSecretRegistry secretRegistry;
  @Mock private TableMetadata tableMetadata1;
  @Mock private TableMetadata tableMetadata2;
  @Mock private TableMetadata tableMetadata3;
  @Mock private TableMetadata tableMetadata4;
  @Mock private ScalarNamespaceResolver namespaceResolver;
  private LedgerNamespaceRegistry namespaceRegistry;
  private AutoCloseable closeable;

  @BeforeEach
  public void setUp() {
    closeable = openMocks(this);
    when(transactionManager.getTransactionTables())
        .thenReturn(
            ImmutableMap.of(SOME_TABLE_NAME_1, tableMetadata1, SOME_TABLE_NAME_2, tableMetadata2));
    when(certificateRegistry.getStorageTables())
        .thenReturn(ImmutableMap.of(SOME_TABLE_NAME_3, tableMetadata3));
    when(secretRegistry.getStorageTables())
        .thenReturn(ImmutableMap.of(SOME_TABLE_NAME_4, tableMetadata4));
    when(namespaceResolver.resolve(SOME_NAMESPACE))
        .thenReturn(BASE_NAMESPACE + NAMESPACE_NAME_SEPARATOR + SOME_NAMESPACE);
    namespaceRegistry =
        new LedgerNamespaceRegistry(
            config,
            storage,
            storageAdmin,
            transactionAdmin,
            namespaceResolver,
            ImmutableSet.of(transactionManager, certificateRegistry, secretRegistry));
  }

  @AfterEach
  public void tearDown() throws Exception {
    closeable.close();
  }

  @Test
  public void create_NewNamespaceGiven_ShouldCreateProperly() throws ExecutionException {
    // Arrange
    String fullNamespace = BASE_NAMESPACE + NAMESPACE_NAME_SEPARATOR + SOME_NAMESPACE;
    Put expectedPut =
        Put.newBuilder()
            .namespace(BASE_NAMESPACE)
            .table(NAMESPACE_TABLE_NAME)
            .partitionKey(Key.ofText(NAMESPACE_COLUMN_NAME, SOME_NAMESPACE))
            .condition(ConditionBuilder.putIfNotExists())
            .build();
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);

    // Act
    namespaceRegistry.create(SOME_NAMESPACE);

    // Assert
    verify(storageAdmin).createNamespace(fullNamespace, true);
    verify(storageAdmin)
        .createTable(BASE_NAMESPACE, NAMESPACE_TABLE_NAME, NAMESPACE_TABLE_METADATA, true);
    verify(transactionAdmin).createTable(fullNamespace, SOME_TABLE_NAME_1, tableMetadata1, true);
    verify(transactionAdmin).createTable(fullNamespace, SOME_TABLE_NAME_2, tableMetadata2, true);
    verify(storageAdmin).createTable(fullNamespace, SOME_TABLE_NAME_3, tableMetadata3, true);
    verify(storageAdmin).createTable(fullNamespace, SOME_TABLE_NAME_4, tableMetadata4, true);
    verify(storage).put(expectedPut);
  }

  @Test
  public void create_ExistingNamespaceGiven_ShouldThrowException() throws ExecutionException {
    // Arrange
    String fullNamespace = BASE_NAMESPACE + NAMESPACE_NAME_SEPARATOR + SOME_NAMESPACE;
    Put expectedPut =
        Put.newBuilder()
            .namespace(BASE_NAMESPACE)
            .table(NAMESPACE_TABLE_NAME)
            .partitionKey(Key.ofText(NAMESPACE_COLUMN_NAME, SOME_NAMESPACE))
            .condition(ConditionBuilder.putIfNotExists())
            .build();
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    NoMutationException toThrow = Mockito.mock(NoMutationException.class);
    doThrow(toThrow).when(storage).put(any(Put.class));

    // Act Assert
    assertThatThrownBy(() -> namespaceRegistry.create(SOME_NAMESPACE))
        .isInstanceOf(DatabaseException.class)
        .hasMessageContaining(CommonError.NAMESPACE_ALREADY_EXISTS.getMessage());
    verify(storageAdmin).createNamespace(fullNamespace, true);
    verify(storageAdmin)
        .createTable(BASE_NAMESPACE, NAMESPACE_TABLE_NAME, NAMESPACE_TABLE_METADATA, true);
    verify(transactionAdmin).createTable(fullNamespace, SOME_TABLE_NAME_1, tableMetadata1, true);
    verify(transactionAdmin).createTable(fullNamespace, SOME_TABLE_NAME_2, tableMetadata2, true);
    verify(storageAdmin).createTable(fullNamespace, SOME_TABLE_NAME_3, tableMetadata3, true);
    verify(storageAdmin).createTable(fullNamespace, SOME_TABLE_NAME_4, tableMetadata4, true);
    verify(storage).put(expectedPut);
  }

  @Test
  public void create_CreateNamespaceManagementTableFailed_ShouldThrowDatabaseException()
      throws ExecutionException {
    // Arrange
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    ExecutionException toThrow = new ExecutionException("details");
    doThrow(toThrow)
        .when(storageAdmin)
        .createTable(BASE_NAMESPACE, NAMESPACE_TABLE_NAME, NAMESPACE_TABLE_METADATA, true);

    // Act Assert
    assertThatThrownBy(() -> namespaceRegistry.create(SOME_NAMESPACE))
        .isInstanceOf(DatabaseException.class)
        .hasCause(toThrow)
        .hasMessageContaining(CommonError.CREATING_NAMESPACE_TABLE_FAILED.buildMessage("details"));
    verify(storageAdmin)
        .createTable(BASE_NAMESPACE, NAMESPACE_TABLE_NAME, NAMESPACE_TABLE_METADATA, true);
    verify(storageAdmin, never()).createNamespace(any(), anyBoolean());
  }

  @Test
  public void create_CreateNamespaceFailed_ShouldThrowDatabaseException()
      throws ExecutionException {
    // Arrange
    String fullNamespace = BASE_NAMESPACE + NAMESPACE_NAME_SEPARATOR + SOME_NAMESPACE;
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    ExecutionException toThrow = new ExecutionException("details");
    doThrow(toThrow).when(storageAdmin).createNamespace(fullNamespace, true);

    // Act Assert
    assertThatThrownBy(() -> namespaceRegistry.create(SOME_NAMESPACE))
        .isInstanceOf(DatabaseException.class)
        .hasCause(toThrow)
        .hasMessageContaining(CommonError.CREATING_NAMESPACE_FAILED.buildMessage("details"));
    verify(storageAdmin)
        .createTable(BASE_NAMESPACE, NAMESPACE_TABLE_NAME, NAMESPACE_TABLE_METADATA, true);
    verify(storageAdmin).createNamespace(fullNamespace, true);
    verify(storage, never()).get(any(Get.class));
    verify(storage, never()).put(any(Put.class));
  }

  @Test
  public void create_AddNamespaceEntryFailed_ShouldThrowDatabaseException()
      throws ExecutionException {
    // Arrange
    String fullNamespace = BASE_NAMESPACE + NAMESPACE_NAME_SEPARATOR + SOME_NAMESPACE;
    Put expectedPut =
        Put.newBuilder()
            .namespace(BASE_NAMESPACE)
            .table(NAMESPACE_TABLE_NAME)
            .partitionKey(Key.ofText(NAMESPACE_COLUMN_NAME, SOME_NAMESPACE))
            .condition(ConditionBuilder.putIfNotExists())
            .build();
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    ExecutionException toThrow = new ExecutionException("details");
    doThrow(toThrow).when(storage).put(expectedPut);

    // Act Assert
    assertThatThrownBy(() -> namespaceRegistry.create(SOME_NAMESPACE))
        .isInstanceOf(DatabaseException.class)
        .hasCause(toThrow)
        .hasMessageContaining(CommonError.CREATING_NAMESPACE_FAILED.buildMessage("details"));
    verify(storageAdmin)
        .createTable(BASE_NAMESPACE, NAMESPACE_TABLE_NAME, NAMESPACE_TABLE_METADATA, true);
    verify(storageAdmin).createNamespace(fullNamespace, true);
    verify(transactionAdmin).createTable(fullNamespace, SOME_TABLE_NAME_1, tableMetadata1, true);
    verify(transactionAdmin).createTable(fullNamespace, SOME_TABLE_NAME_2, tableMetadata2, true);
    verify(storageAdmin).createTable(fullNamespace, SOME_TABLE_NAME_3, tableMetadata3, true);
    verify(storageAdmin).createTable(fullNamespace, SOME_TABLE_NAME_4, tableMetadata4, true);
    verify(storage).put(expectedPut);
  }

  @Test
  public void create_AddNamespaceEntryFailedDueToTableNotFound_ShouldRetry()
      throws ExecutionException {
    // Arrange
    String fullNamespace = BASE_NAMESPACE + NAMESPACE_NAME_SEPARATOR + SOME_NAMESPACE;
    Put expectedPut =
        Put.newBuilder()
            .namespace(BASE_NAMESPACE)
            .table(NAMESPACE_TABLE_NAME)
            .partitionKey(Key.ofText(NAMESPACE_COLUMN_NAME, SOME_NAMESPACE))
            .condition(ConditionBuilder.putIfNotExists())
            .build();
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    IllegalArgumentException toThrow =
        new IllegalArgumentException(CoreError.TABLE_NOT_FOUND.buildMessage(NAMESPACE_TABLE_NAME));
    doThrow(toThrow).doNothing().when(storage).put(expectedPut);

    // Act
    namespaceRegistry.create(SOME_NAMESPACE);

    // Assert
    verify(storageAdmin, times(2))
        .createTable(BASE_NAMESPACE, NAMESPACE_TABLE_NAME, NAMESPACE_TABLE_METADATA, true);
    verify(storageAdmin, times(2)).createNamespace(fullNamespace, true);
    verify(transactionAdmin, times(2))
        .createTable(fullNamespace, SOME_TABLE_NAME_1, tableMetadata1, true);
    verify(transactionAdmin, times(2))
        .createTable(fullNamespace, SOME_TABLE_NAME_2, tableMetadata2, true);
    verify(storageAdmin, times(2))
        .createTable(fullNamespace, SOME_TABLE_NAME_3, tableMetadata3, true);
    verify(storageAdmin, times(2))
        .createTable(fullNamespace, SOME_TABLE_NAME_4, tableMetadata4, true);
    verify(storage, times(2)).put(expectedPut);
  }

  @Test
  public void create_AddNamespaceEntryFailedDueToNamespaceNotFound_ShouldRetry()
      throws ExecutionException {
    // Arrange
    String fullNamespace = BASE_NAMESPACE + NAMESPACE_NAME_SEPARATOR + SOME_NAMESPACE;
    Put expectedPut =
        Put.newBuilder()
            .namespace(BASE_NAMESPACE)
            .table(NAMESPACE_TABLE_NAME)
            .partitionKey(Key.ofText(NAMESPACE_COLUMN_NAME, SOME_NAMESPACE))
            .condition(ConditionBuilder.putIfNotExists())
            .build();
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    IllegalArgumentException toThrow =
        new IllegalArgumentException(CoreError.NAMESPACE_NOT_FOUND.buildMessage(BASE_NAMESPACE));
    doThrow(toThrow).doNothing().when(storage).put(expectedPut);

    // Act
    namespaceRegistry.create(SOME_NAMESPACE);

    // Assert
    verify(storageAdmin, times(2))
        .createTable(BASE_NAMESPACE, NAMESPACE_TABLE_NAME, NAMESPACE_TABLE_METADATA, true);
    verify(storageAdmin, times(2)).createNamespace(fullNamespace, true);
    verify(transactionAdmin, times(2))
        .createTable(fullNamespace, SOME_TABLE_NAME_1, tableMetadata1, true);
    verify(transactionAdmin, times(2))
        .createTable(fullNamespace, SOME_TABLE_NAME_2, tableMetadata2, true);
    verify(storageAdmin, times(2))
        .createTable(fullNamespace, SOME_TABLE_NAME_3, tableMetadata3, true);
    verify(storageAdmin, times(2))
        .createTable(fullNamespace, SOME_TABLE_NAME_4, tableMetadata4, true);
    verify(storage, times(2)).put(expectedPut);
  }

  @Test
  public void create_MaxAttemptsExceeded_ShouldThrowDatabaseException() throws ExecutionException {
    // Arrange
    String fullNamespace = BASE_NAMESPACE + NAMESPACE_NAME_SEPARATOR + SOME_NAMESPACE;
    Put expectedPut =
        Put.newBuilder()
            .namespace(BASE_NAMESPACE)
            .table(NAMESPACE_TABLE_NAME)
            .partitionKey(Key.ofText(NAMESPACE_COLUMN_NAME, SOME_NAMESPACE))
            .condition(ConditionBuilder.putIfNotExists())
            .build();
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    IllegalArgumentException toThrow =
        new IllegalArgumentException(CoreError.TABLE_NOT_FOUND.buildMessage(NAMESPACE_TABLE_NAME));
    doThrow(toThrow).when(storage).put(expectedPut);

    // Act Assert
    assertThatThrownBy(() -> namespaceRegistry.create(SOME_NAMESPACE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(CoreError.TABLE_NOT_FOUND.buildMessage(NAMESPACE_TABLE_NAME));
    verify(storageAdmin, times(5))
        .createTable(BASE_NAMESPACE, NAMESPACE_TABLE_NAME, NAMESPACE_TABLE_METADATA, true);
    verify(storageAdmin, times(5)).createNamespace(fullNamespace, true);
    verify(transactionAdmin, times(5))
        .createTable(fullNamespace, SOME_TABLE_NAME_1, tableMetadata1, true);
    verify(transactionAdmin, times(5))
        .createTable(fullNamespace, SOME_TABLE_NAME_2, tableMetadata2, true);
    verify(storageAdmin, times(5))
        .createTable(fullNamespace, SOME_TABLE_NAME_3, tableMetadata3, true);
    verify(storageAdmin, times(5))
        .createTable(fullNamespace, SOME_TABLE_NAME_4, tableMetadata4, true);
    verify(storage, times(5)).put(expectedPut);
  }

  @Test
  public void create_NonRetryableException_ShouldNotRetry() throws ExecutionException {
    // Arrange
    String fullNamespace = BASE_NAMESPACE + NAMESPACE_NAME_SEPARATOR + SOME_NAMESPACE;
    Put expectedPut =
        Put.newBuilder()
            .namespace(BASE_NAMESPACE)
            .table(NAMESPACE_TABLE_NAME)
            .partitionKey(Key.ofText(NAMESPACE_COLUMN_NAME, SOME_NAMESPACE))
            .condition(ConditionBuilder.putIfNotExists())
            .build();
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    // This is an IllegalArgumentException but with a different error code (not retryable)
    IllegalArgumentException toThrow =
        new IllegalArgumentException("Some other error that should not trigger retry");
    doThrow(toThrow).when(storage).put(expectedPut);

    // Act Assert
    assertThatThrownBy(() -> namespaceRegistry.create(SOME_NAMESPACE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Some other error that should not trigger retry");
    // Should only attempt once (no retry)
    verify(storageAdmin, times(1))
        .createTable(BASE_NAMESPACE, NAMESPACE_TABLE_NAME, NAMESPACE_TABLE_METADATA, true);
    verify(storageAdmin, times(1)).createNamespace(fullNamespace, true);
    verify(transactionAdmin, times(1))
        .createTable(fullNamespace, SOME_TABLE_NAME_1, tableMetadata1, true);
    verify(transactionAdmin, times(1))
        .createTable(fullNamespace, SOME_TABLE_NAME_2, tableMetadata2, true);
    verify(storageAdmin, times(1))
        .createTable(fullNamespace, SOME_TABLE_NAME_3, tableMetadata3, true);
    verify(storageAdmin, times(1))
        .createTable(fullNamespace, SOME_TABLE_NAME_4, tableMetadata4, true);
    verify(storage, times(1)).put(expectedPut);
  }
}
