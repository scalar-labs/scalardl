package com.scalar.dl.ledger.database.scalardb;

import static com.scalar.dl.ledger.database.scalardb.AbstractScalarNamespaceRegistry.COLUMN_IMPORTED;
import static com.scalar.dl.ledger.database.scalardb.AbstractScalarNamespaceRegistry.COLUMN_NAME;
import static com.scalar.dl.ledger.database.scalardb.AbstractScalarNamespaceRegistry.COLUMN_PARTITION_ID;
import static com.scalar.dl.ledger.database.scalardb.AbstractScalarNamespaceRegistry.DEFAULT_PARTITION_ID;
import static com.scalar.dl.ledger.database.scalardb.AbstractScalarNamespaceRegistry.NAMESPACE_NAME_SEPARATOR;
import static com.scalar.dl.ledger.database.scalardb.AbstractScalarNamespaceRegistry.NAMESPACE_TABLE_METADATA;
import static com.scalar.dl.ledger.database.scalardb.AbstractScalarNamespaceRegistry.NAMESPACE_TABLE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.scalar.db.api.ConditionBuilder;
import com.scalar.db.api.ConditionalExpression;
import com.scalar.db.api.Delete;
import com.scalar.db.api.DistributedStorage;
import com.scalar.db.api.DistributedStorageAdmin;
import com.scalar.db.api.DistributedTransactionAdmin;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.db.api.Scanner;
import com.scalar.db.api.TableMetadata;
import com.scalar.db.common.CoreError;
import com.scalar.db.exception.storage.ExecutionException;
import com.scalar.db.exception.storage.NoMutationException;
import com.scalar.db.io.DataType;
import com.scalar.db.io.Key;
import com.scalar.dl.ledger.config.LedgerConfig;
import com.scalar.dl.ledger.error.CommonError;
import com.scalar.dl.ledger.exception.DatabaseException;
import com.scalar.dl.ledger.exception.LedgerException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

public class LedgerNamespaceRegistryTest {
  private static final String BASE_NAMESPACE = "scalar";
  private static final String SOME_NAMESPACE = "some_namespace";
  private static final String SOME_TABLE_NAME_1 = "some_table_name_1";
  private static final String SOME_TABLE_NAME_2 = "some_table_name_2";
  private static final String SOME_TABLE_NAME_3 = "some_table_name_3";
  private static final String SOME_TABLE_NAME_4 = "some_table_name_4";
  private static final ImmutableMap<String, String> SOME_OPTIONS =
      ImmutableMap.of("replication-factor", "3", "no-scaling", "true");
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
    when(config.getNamespaceCreationOptions()).thenReturn(SOME_OPTIONS);
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
            .partitionKey(Key.ofInt(COLUMN_PARTITION_ID, DEFAULT_PARTITION_ID))
            .clusteringKey(Key.ofText(COLUMN_NAME, SOME_NAMESPACE))
            .condition(ConditionBuilder.putIfNotExists())
            .build();
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);

    // Act
    namespaceRegistry.create(SOME_NAMESPACE);

    // Assert
    verify(storageAdmin).createNamespace(fullNamespace, true, SOME_OPTIONS);
    verify(storageAdmin)
        .createTable(
            BASE_NAMESPACE, NAMESPACE_TABLE_NAME, NAMESPACE_TABLE_METADATA, true, SOME_OPTIONS);
    verify(transactionAdmin)
        .createTable(fullNamespace, SOME_TABLE_NAME_1, tableMetadata1, true, SOME_OPTIONS);
    verify(transactionAdmin)
        .createTable(fullNamespace, SOME_TABLE_NAME_2, tableMetadata2, true, SOME_OPTIONS);
    verify(storageAdmin)
        .createTable(fullNamespace, SOME_TABLE_NAME_3, tableMetadata3, true, SOME_OPTIONS);
    verify(storageAdmin)
        .createTable(fullNamespace, SOME_TABLE_NAME_4, tableMetadata4, true, SOME_OPTIONS);
    verify(storage).put(expectedPut);
  }

  @Test
  public void create_NoNamespaceCreationOptionsGiven_ShouldPassEmptyOptions()
      throws ExecutionException {
    // Arrange
    String fullNamespace = BASE_NAMESPACE + NAMESPACE_NAME_SEPARATOR + SOME_NAMESPACE;
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    when(config.getNamespaceCreationOptions()).thenReturn(Collections.emptyMap());

    // Act
    namespaceRegistry.create(SOME_NAMESPACE);

    // Assert
    verify(storageAdmin).createNamespace(fullNamespace, true, Collections.emptyMap());
    verify(storageAdmin)
        .createTable(
            BASE_NAMESPACE,
            NAMESPACE_TABLE_NAME,
            NAMESPACE_TABLE_METADATA,
            true,
            Collections.emptyMap());
    verify(transactionAdmin)
        .createTable(
            fullNamespace, SOME_TABLE_NAME_1, tableMetadata1, true, Collections.emptyMap());
    verify(transactionAdmin)
        .createTable(
            fullNamespace, SOME_TABLE_NAME_2, tableMetadata2, true, Collections.emptyMap());
    verify(storageAdmin)
        .createTable(
            fullNamespace, SOME_TABLE_NAME_3, tableMetadata3, true, Collections.emptyMap());
    verify(storageAdmin)
        .createTable(
            fullNamespace, SOME_TABLE_NAME_4, tableMetadata4, true, Collections.emptyMap());
  }

  @Test
  public void create_ExistingNamespaceGiven_ShouldThrowException() throws ExecutionException {
    // Arrange
    String fullNamespace = BASE_NAMESPACE + NAMESPACE_NAME_SEPARATOR + SOME_NAMESPACE;
    Put expectedPut =
        Put.newBuilder()
            .namespace(BASE_NAMESPACE)
            .table(NAMESPACE_TABLE_NAME)
            .partitionKey(Key.ofInt(COLUMN_PARTITION_ID, DEFAULT_PARTITION_ID))
            .clusteringKey(Key.ofText(COLUMN_NAME, SOME_NAMESPACE))
            .condition(ConditionBuilder.putIfNotExists())
            .build();
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    NoMutationException toThrow = Mockito.mock(NoMutationException.class);
    doThrow(toThrow).when(storage).put(any(Put.class));

    // Act Assert
    assertThatThrownBy(() -> namespaceRegistry.create(SOME_NAMESPACE))
        .isInstanceOf(DatabaseException.class)
        .hasMessageContaining(CommonError.NAMESPACE_ALREADY_EXISTS.getMessage());
    verify(storageAdmin).createNamespace(fullNamespace, true, SOME_OPTIONS);
    verify(storageAdmin)
        .createTable(
            BASE_NAMESPACE, NAMESPACE_TABLE_NAME, NAMESPACE_TABLE_METADATA, true, SOME_OPTIONS);
    verify(transactionAdmin)
        .createTable(fullNamespace, SOME_TABLE_NAME_1, tableMetadata1, true, SOME_OPTIONS);
    verify(transactionAdmin)
        .createTable(fullNamespace, SOME_TABLE_NAME_2, tableMetadata2, true, SOME_OPTIONS);
    verify(storageAdmin)
        .createTable(fullNamespace, SOME_TABLE_NAME_3, tableMetadata3, true, SOME_OPTIONS);
    verify(storageAdmin)
        .createTable(fullNamespace, SOME_TABLE_NAME_4, tableMetadata4, true, SOME_OPTIONS);
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
        .createTable(
            BASE_NAMESPACE, NAMESPACE_TABLE_NAME, NAMESPACE_TABLE_METADATA, true, SOME_OPTIONS);

    // Act Assert
    assertThatThrownBy(() -> namespaceRegistry.create(SOME_NAMESPACE))
        .isInstanceOf(DatabaseException.class)
        .hasCause(toThrow)
        .hasMessageContaining(CommonError.CREATING_NAMESPACE_TABLE_FAILED.buildMessage("details"));
    verify(storageAdmin)
        .createTable(
            BASE_NAMESPACE, NAMESPACE_TABLE_NAME, NAMESPACE_TABLE_METADATA, true, SOME_OPTIONS);
    verify(storageAdmin, never()).createNamespace(any(), anyBoolean(), anyMap());
  }

  @Test
  public void create_CreateNamespaceFailed_ShouldThrowDatabaseException()
      throws ExecutionException {
    // Arrange
    String fullNamespace = BASE_NAMESPACE + NAMESPACE_NAME_SEPARATOR + SOME_NAMESPACE;
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    ExecutionException toThrow = new ExecutionException("details");
    doThrow(toThrow).when(storageAdmin).createNamespace(fullNamespace, true, SOME_OPTIONS);

    // Act Assert
    assertThatThrownBy(() -> namespaceRegistry.create(SOME_NAMESPACE))
        .isInstanceOf(DatabaseException.class)
        .hasCause(toThrow)
        .hasMessageContaining(CommonError.CREATING_NAMESPACE_FAILED.buildMessage("details"));
    verify(storageAdmin)
        .createTable(
            BASE_NAMESPACE, NAMESPACE_TABLE_NAME, NAMESPACE_TABLE_METADATA, true, SOME_OPTIONS);
    verify(storageAdmin).createNamespace(fullNamespace, true, SOME_OPTIONS);
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
            .partitionKey(Key.ofInt(COLUMN_PARTITION_ID, DEFAULT_PARTITION_ID))
            .clusteringKey(Key.ofText(COLUMN_NAME, SOME_NAMESPACE))
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
        .createTable(
            BASE_NAMESPACE, NAMESPACE_TABLE_NAME, NAMESPACE_TABLE_METADATA, true, SOME_OPTIONS);
    verify(storageAdmin).createNamespace(fullNamespace, true, SOME_OPTIONS);
    verify(transactionAdmin)
        .createTable(fullNamespace, SOME_TABLE_NAME_1, tableMetadata1, true, SOME_OPTIONS);
    verify(transactionAdmin)
        .createTable(fullNamespace, SOME_TABLE_NAME_2, tableMetadata2, true, SOME_OPTIONS);
    verify(storageAdmin)
        .createTable(fullNamespace, SOME_TABLE_NAME_3, tableMetadata3, true, SOME_OPTIONS);
    verify(storageAdmin)
        .createTable(fullNamespace, SOME_TABLE_NAME_4, tableMetadata4, true, SOME_OPTIONS);
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
            .partitionKey(Key.ofInt(COLUMN_PARTITION_ID, DEFAULT_PARTITION_ID))
            .clusteringKey(Key.ofText(COLUMN_NAME, SOME_NAMESPACE))
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
        .createTable(
            BASE_NAMESPACE, NAMESPACE_TABLE_NAME, NAMESPACE_TABLE_METADATA, true, SOME_OPTIONS);
    verify(storageAdmin, times(2)).createNamespace(fullNamespace, true, SOME_OPTIONS);
    verify(transactionAdmin, times(2))
        .createTable(fullNamespace, SOME_TABLE_NAME_1, tableMetadata1, true, SOME_OPTIONS);
    verify(transactionAdmin, times(2))
        .createTable(fullNamespace, SOME_TABLE_NAME_2, tableMetadata2, true, SOME_OPTIONS);
    verify(storageAdmin, times(2))
        .createTable(fullNamespace, SOME_TABLE_NAME_3, tableMetadata3, true, SOME_OPTIONS);
    verify(storageAdmin, times(2))
        .createTable(fullNamespace, SOME_TABLE_NAME_4, tableMetadata4, true, SOME_OPTIONS);
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
            .partitionKey(Key.ofInt(COLUMN_PARTITION_ID, DEFAULT_PARTITION_ID))
            .clusteringKey(Key.ofText(COLUMN_NAME, SOME_NAMESPACE))
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
        .createTable(
            BASE_NAMESPACE, NAMESPACE_TABLE_NAME, NAMESPACE_TABLE_METADATA, true, SOME_OPTIONS);
    verify(storageAdmin, times(2)).createNamespace(fullNamespace, true, SOME_OPTIONS);
    verify(transactionAdmin, times(2))
        .createTable(fullNamespace, SOME_TABLE_NAME_1, tableMetadata1, true, SOME_OPTIONS);
    verify(transactionAdmin, times(2))
        .createTable(fullNamespace, SOME_TABLE_NAME_2, tableMetadata2, true, SOME_OPTIONS);
    verify(storageAdmin, times(2))
        .createTable(fullNamespace, SOME_TABLE_NAME_3, tableMetadata3, true, SOME_OPTIONS);
    verify(storageAdmin, times(2))
        .createTable(fullNamespace, SOME_TABLE_NAME_4, tableMetadata4, true, SOME_OPTIONS);
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
            .partitionKey(Key.ofInt(COLUMN_PARTITION_ID, DEFAULT_PARTITION_ID))
            .clusteringKey(Key.ofText(COLUMN_NAME, SOME_NAMESPACE))
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
        .createTable(
            BASE_NAMESPACE, NAMESPACE_TABLE_NAME, NAMESPACE_TABLE_METADATA, true, SOME_OPTIONS);
    verify(storageAdmin, times(5)).createNamespace(fullNamespace, true, SOME_OPTIONS);
    verify(transactionAdmin, times(5))
        .createTable(fullNamespace, SOME_TABLE_NAME_1, tableMetadata1, true, SOME_OPTIONS);
    verify(transactionAdmin, times(5))
        .createTable(fullNamespace, SOME_TABLE_NAME_2, tableMetadata2, true, SOME_OPTIONS);
    verify(storageAdmin, times(5))
        .createTable(fullNamespace, SOME_TABLE_NAME_3, tableMetadata3, true, SOME_OPTIONS);
    verify(storageAdmin, times(5))
        .createTable(fullNamespace, SOME_TABLE_NAME_4, tableMetadata4, true, SOME_OPTIONS);
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
            .partitionKey(Key.ofInt(COLUMN_PARTITION_ID, DEFAULT_PARTITION_ID))
            .clusteringKey(Key.ofText(COLUMN_NAME, SOME_NAMESPACE))
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
        .createTable(
            BASE_NAMESPACE, NAMESPACE_TABLE_NAME, NAMESPACE_TABLE_METADATA, true, SOME_OPTIONS);
    verify(storageAdmin, times(1)).createNamespace(fullNamespace, true, SOME_OPTIONS);
    verify(transactionAdmin, times(1))
        .createTable(fullNamespace, SOME_TABLE_NAME_1, tableMetadata1, true, SOME_OPTIONS);
    verify(transactionAdmin, times(1))
        .createTable(fullNamespace, SOME_TABLE_NAME_2, tableMetadata2, true, SOME_OPTIONS);
    verify(storageAdmin, times(1))
        .createTable(fullNamespace, SOME_TABLE_NAME_3, tableMetadata3, true, SOME_OPTIONS);
    verify(storageAdmin, times(1))
        .createTable(fullNamespace, SOME_TABLE_NAME_4, tableMetadata4, true, SOME_OPTIONS);
    verify(storage, times(1)).put(expectedPut);
  }

  @Test
  public void create_ImportedGiven_ShouldPutEntryWithImported() throws ExecutionException {
    // Arrange
    String fullNamespace = BASE_NAMESPACE + NAMESPACE_NAME_SEPARATOR + SOME_NAMESPACE;
    Put expectedPut =
        Put.newBuilder()
            .namespace(BASE_NAMESPACE)
            .table(NAMESPACE_TABLE_NAME)
            .partitionKey(Key.ofInt(COLUMN_PARTITION_ID, DEFAULT_PARTITION_ID))
            .clusteringKey(Key.ofText(COLUMN_NAME, SOME_NAMESPACE))
            .booleanValue(COLUMN_IMPORTED, true)
            .condition(ConditionBuilder.putIfNotExists())
            .build();
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    when(storageAdmin.getTableMetadata(BASE_NAMESPACE, NAMESPACE_TABLE_NAME))
        .thenReturn(NAMESPACE_TABLE_METADATA);

    // Act
    namespaceRegistry.create(SOME_NAMESPACE, true);

    // Assert
    verify(storageAdmin)
        .createTable(
            BASE_NAMESPACE, NAMESPACE_TABLE_NAME, NAMESPACE_TABLE_METADATA, true, SOME_OPTIONS);
    verify(storageAdmin).createNamespace(fullNamespace, true, SOME_OPTIONS);
    verify(storage).put(expectedPut);
  }

  @Test
  public void create_ImportedNotGiven_ShouldNotCheckTableMetadata() throws ExecutionException {
    // Arrange
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);

    // Act
    namespaceRegistry.create(SOME_NAMESPACE, false);

    // Assert
    verify(storageAdmin, never()).getTableMetadata(BASE_NAMESPACE, NAMESPACE_TABLE_NAME);
    ArgumentCaptor<Put> putCaptor = ArgumentCaptor.forClass(Put.class);
    verify(storage).put(putCaptor.capture());
    assertThat(putCaptor.getValue().getColumns()).doesNotContainKey(COLUMN_IMPORTED);
  }

  @Test
  public void create_ImportedGivenButImportedColumnNotExists_ShouldThrowLedgerException()
      throws ExecutionException {
    // Arrange
    String fullNamespace = BASE_NAMESPACE + NAMESPACE_NAME_SEPARATOR + SOME_NAMESPACE;
    TableMetadata oldMetadata =
        TableMetadata.newBuilder()
            .addColumn(COLUMN_PARTITION_ID, DataType.INT)
            .addColumn(COLUMN_NAME, DataType.TEXT)
            .addPartitionKey(COLUMN_PARTITION_ID)
            .addClusteringKey(COLUMN_NAME)
            .build();
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    when(storageAdmin.getTableMetadata(BASE_NAMESPACE, NAMESPACE_TABLE_NAME))
        .thenReturn(oldMetadata);

    // Act Assert
    assertThatThrownBy(() -> namespaceRegistry.create(SOME_NAMESPACE, true))
        .isInstanceOf(LedgerException.class)
        .hasMessage(CommonError.CREATING_IMPORTED_NAMESPACE_NOT_SUPPORTED_BY_SCHEMA.buildMessage());

    verify(storageAdmin, times(1)).getTableMetadata(BASE_NAMESPACE, NAMESPACE_TABLE_NAME);
    verify(storageAdmin, never()).createNamespace(fullNamespace, true, SOME_OPTIONS);
    verify(storage, never()).put(any(Put.class));
  }

  @Test
  public void create_ImportedGivenAndGetTableMetadataFailed_ShouldThrowDatabaseException()
      throws ExecutionException {
    // Arrange
    ExecutionException toThrow = new ExecutionException("details");
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    when(storageAdmin.getTableMetadata(BASE_NAMESPACE, NAMESPACE_TABLE_NAME)).thenThrow(toThrow);

    // Act Assert
    assertThatThrownBy(() -> namespaceRegistry.create(SOME_NAMESPACE, true))
        .isInstanceOf(DatabaseException.class)
        .hasCause(toThrow)
        .hasMessage(CommonError.CREATING_NAMESPACE_FAILED.buildMessage("details"));

    verify(storage, never()).put(any(Put.class));
  }

  @Test
  public void create_ImportedGivenAndPutRejectedDueToStaleMetadata_ShouldThrowDatabaseException()
      throws ExecutionException {
    // Arrange
    IllegalArgumentException toThrow =
        new IllegalArgumentException(
            CoreError.OPERATION_CHECK_ERROR_INVALID_COLUMN.buildMessage("c", "put", "metadata"));
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    when(storageAdmin.getTableMetadata(BASE_NAMESPACE, NAMESPACE_TABLE_NAME))
        .thenReturn(NAMESPACE_TABLE_METADATA);
    doThrow(toThrow).when(storage).put(any(Put.class));

    // Act Assert
    assertThatThrownBy(() -> namespaceRegistry.create(SOME_NAMESPACE, true))
        .isInstanceOf(DatabaseException.class)
        .hasCause(toThrow)
        .hasMessage(CommonError.NAMESPACE_TABLE_SCHEMA_CHANGE_NOT_REFLECTED.buildMessage());

    verify(storage, times(1)).put(any(Put.class));
  }

  @Test
  public void isImported_ImportedNamespaceGiven_ShouldReturnTrue() throws ExecutionException {
    // Arrange
    Get expectedGet =
        Get.newBuilder()
            .namespace(BASE_NAMESPACE)
            .table(NAMESPACE_TABLE_NAME)
            .partitionKey(Key.ofInt(COLUMN_PARTITION_ID, DEFAULT_PARTITION_ID))
            .clusteringKey(Key.ofText(COLUMN_NAME, SOME_NAMESPACE))
            .build();
    Result result = Mockito.mock(Result.class);
    when(result.contains(COLUMN_IMPORTED)).thenReturn(true);
    when(result.isNull(COLUMN_IMPORTED)).thenReturn(false);
    when(result.getBoolean(COLUMN_IMPORTED)).thenReturn(true);
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    when(storageAdmin.tableExists(BASE_NAMESPACE, NAMESPACE_TABLE_NAME)).thenReturn(true);
    when(storage.get(expectedGet)).thenReturn(Optional.of(result));

    // Act
    boolean actual = namespaceRegistry.isImported(SOME_NAMESPACE);

    // Assert
    assertThat(actual).isTrue();
    verify(storage).get(expectedGet);
  }

  @Test
  public void isImported_NamespaceWithNullImportedGiven_ShouldReturnFalse()
      throws ExecutionException {
    // Arrange
    Result result = Mockito.mock(Result.class);
    when(result.contains(COLUMN_IMPORTED)).thenReturn(true);
    when(result.isNull(COLUMN_IMPORTED)).thenReturn(true);
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    when(storageAdmin.tableExists(BASE_NAMESPACE, NAMESPACE_TABLE_NAME)).thenReturn(true);
    when(storage.get(any(Get.class))).thenReturn(Optional.of(result));

    // Act
    boolean actual = namespaceRegistry.isImported(SOME_NAMESPACE);

    // Assert
    assertThat(actual).isFalse();
    verify(result, never()).getBoolean(COLUMN_IMPORTED);
  }

  @Test
  public void isImported_ImportedColumnNotExists_ShouldReturnFalse() throws ExecutionException {
    // Arrange
    Result result = Mockito.mock(Result.class);
    when(result.contains(COLUMN_IMPORTED)).thenReturn(false);
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    when(storageAdmin.tableExists(BASE_NAMESPACE, NAMESPACE_TABLE_NAME)).thenReturn(true);
    when(storage.get(any(Get.class))).thenReturn(Optional.of(result));

    // Act
    boolean actual = namespaceRegistry.isImported(SOME_NAMESPACE);

    // Assert
    assertThat(actual).isFalse();
    verify(result, never()).isNull(COLUMN_IMPORTED);
    verify(result, never()).getBoolean(COLUMN_IMPORTED);
  }

  @Test
  public void isImported_NonExistingNamespaceGiven_ShouldReturnFalse() throws ExecutionException {
    // Arrange
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    when(storageAdmin.tableExists(BASE_NAMESPACE, NAMESPACE_TABLE_NAME)).thenReturn(true);
    when(storage.get(any(Get.class))).thenReturn(Optional.empty());

    // Act
    boolean actual = namespaceRegistry.isImported(SOME_NAMESPACE);

    // Assert
    assertThat(actual).isFalse();
  }

  @Test
  public void isImported_NamespaceTableNotExists_ShouldReturnFalse() throws ExecutionException {
    // Arrange
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    when(storageAdmin.tableExists(BASE_NAMESPACE, NAMESPACE_TABLE_NAME)).thenReturn(false);

    // Act
    boolean actual = namespaceRegistry.isImported(SOME_NAMESPACE);

    // Assert
    assertThat(actual).isFalse();
    verify(storage, never()).get(any(Get.class));
  }

  @Test
  public void isImported_GetFailed_ShouldThrowDatabaseException() throws ExecutionException {
    // Arrange
    ExecutionException toThrow = new ExecutionException("details");
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    when(storageAdmin.tableExists(BASE_NAMESPACE, NAMESPACE_TABLE_NAME)).thenReturn(true);
    when(storage.get(any(Get.class))).thenThrow(toThrow);

    // Act Assert
    assertThatThrownBy(() -> namespaceRegistry.isImported(SOME_NAMESPACE))
        .isInstanceOf(DatabaseException.class)
        .hasCause(toThrow);
  }

  @Test
  public void existsImported_ImportedNamespaceExists_ShouldReturnTrue() throws ExecutionException {
    // Arrange
    Result result1 = Mockito.mock(Result.class);
    Result result2 = Mockito.mock(Result.class);
    when(result1.contains(COLUMN_IMPORTED)).thenReturn(true);
    when(result1.isNull(COLUMN_IMPORTED)).thenReturn(true);
    when(result2.contains(COLUMN_IMPORTED)).thenReturn(true);
    when(result2.isNull(COLUMN_IMPORTED)).thenReturn(false);
    when(result2.getBoolean(COLUMN_IMPORTED)).thenReturn(true);
    Scanner scanner = Mockito.mock(Scanner.class);
    when(scanner.spliterator()).thenReturn(Arrays.asList(result1, result2).spliterator());
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    when(storageAdmin.tableExists(BASE_NAMESPACE, NAMESPACE_TABLE_NAME)).thenReturn(true);
    when(storage.scan(any(Scan.class))).thenReturn(scanner);

    // Act
    boolean actual = namespaceRegistry.existsImported();

    // Assert
    assertThat(actual).isTrue();
  }

  @Test
  public void existsImported_NoImportedNamespaceExists_ShouldReturnFalse()
      throws ExecutionException {
    // Arrange
    Result result1 = Mockito.mock(Result.class);
    Result result2 = Mockito.mock(Result.class);
    when(result1.contains(COLUMN_IMPORTED)).thenReturn(true);
    when(result1.isNull(COLUMN_IMPORTED)).thenReturn(true);
    when(result2.contains(COLUMN_IMPORTED)).thenReturn(false);
    Scanner scanner = Mockito.mock(Scanner.class);
    when(scanner.spliterator()).thenReturn(Arrays.asList(result1, result2).spliterator());
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    when(storageAdmin.tableExists(BASE_NAMESPACE, NAMESPACE_TABLE_NAME)).thenReturn(true);
    when(storage.scan(any(Scan.class))).thenReturn(scanner);

    // Act
    boolean actual = namespaceRegistry.existsImported();

    // Assert
    assertThat(actual).isFalse();
  }

  @Test
  public void existsImported_NamespaceTableNotExists_ShouldReturnFalse() throws ExecutionException {
    // Arrange
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    when(storageAdmin.tableExists(BASE_NAMESPACE, NAMESPACE_TABLE_NAME)).thenReturn(false);

    // Act
    boolean actual = namespaceRegistry.existsImported();

    // Assert
    assertThat(actual).isFalse();
    verify(storage, never()).scan(any(Scan.class));
  }

  @Test
  public void scan_EmptyPatternAndNamespacesExist_ShouldReturnNamespacesFromRegistry()
      throws ExecutionException {
    // Arrange
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    when(storageAdmin.tableExists(BASE_NAMESPACE, NAMESPACE_TABLE_NAME)).thenReturn(true);
    Scanner scanner = Mockito.mock(Scanner.class);
    Result result1 = Mockito.mock(Result.class);
    Result result2 = Mockito.mock(Result.class);
    when(result1.getText(COLUMN_NAME)).thenReturn("ns1");
    when(result2.getText(COLUMN_NAME)).thenReturn("ns2");
    List<Result> results = Arrays.asList(result1, result2);
    when(scanner.spliterator()).thenReturn(results.spliterator());
    ArgumentCaptor<Scan> scanCaptor = ArgumentCaptor.forClass(Scan.class);
    when(storage.scan(scanCaptor.capture())).thenReturn(scanner);

    // Act
    List<String> result = namespaceRegistry.scan("");

    // Assert
    // Registry only returns namespaces from the database.
    // "default" is added by NamespaceManager, not by the registry.
    assertThat(result).containsExactly("ns1", "ns2");
    verify(storageAdmin).tableExists(BASE_NAMESPACE, NAMESPACE_TABLE_NAME);
    // Verify that the Scan object has no where clause when pattern is empty
    Scan capturedScan = scanCaptor.getValue();
    assertThat(capturedScan.getConjunctions()).isEmpty();
  }

  @Test
  public void scan_EmptyPatternAndNoNamespacesExist_ShouldReturnEmptyList()
      throws ExecutionException {
    // Arrange
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    when(storageAdmin.tableExists(BASE_NAMESPACE, NAMESPACE_TABLE_NAME)).thenReturn(true);
    Scanner scanner = Mockito.mock(Scanner.class);
    List<Result> emptyResults = Collections.emptyList();
    when(scanner.spliterator()).thenReturn(emptyResults.spliterator());
    when(storage.scan(any(Scan.class))).thenReturn(scanner);

    // Act
    List<String> result = namespaceRegistry.scan("");

    // Assert
    // Registry returns empty list. "default" is added by NamespaceManager, not by the registry.
    assertThat(result).isEmpty();
    verify(storageAdmin).tableExists(BASE_NAMESPACE, NAMESPACE_TABLE_NAME);
    verify(storage).scan(any(Scan.class));
  }

  @Test
  public void scan_NamespaceTableNotExists_ShouldReturnEmptyList() throws ExecutionException {
    // Arrange
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    when(storageAdmin.tableExists(BASE_NAMESPACE, NAMESPACE_TABLE_NAME)).thenReturn(false);

    // Act
    List<String> result = namespaceRegistry.scan("");

    // Assert
    // When namespace table doesn't exist (e.g., old ScalarDL versions), return empty list
    assertThat(result).isEmpty();
    verify(storageAdmin).tableExists(BASE_NAMESPACE, NAMESPACE_TABLE_NAME);
    verify(storage, never()).scan(any(Scan.class));
  }

  @Test
  public void scan_ScanFailed_ShouldThrowDatabaseException() throws ExecutionException {
    // Arrange
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    when(storageAdmin.tableExists(BASE_NAMESPACE, NAMESPACE_TABLE_NAME)).thenReturn(true);
    ExecutionException toThrow = new ExecutionException("details");
    when(storage.scan(any(Scan.class))).thenThrow(toThrow);

    // Act Assert
    assertThatThrownBy(() -> namespaceRegistry.scan(""))
        .isInstanceOf(DatabaseException.class)
        .hasCause(toThrow)
        .hasMessageContaining(CommonError.SCANNING_NAMESPACES_FAILED.buildMessage("details"));
  }

  @Test
  public void scan_PatternGiven_ShouldReturnMatchingNamespacesFromRegistry()
      throws ExecutionException {
    // Arrange
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    when(storageAdmin.tableExists(BASE_NAMESPACE, NAMESPACE_TABLE_NAME)).thenReturn(true);
    Scanner scanner = Mockito.mock(Scanner.class);
    Result result1 = Mockito.mock(Result.class);
    Result result2 = Mockito.mock(Result.class);
    when(result1.getText(COLUMN_NAME)).thenReturn("abc");
    when(result2.getText(COLUMN_NAME)).thenReturn("def");
    List<Result> results = Arrays.asList(result1, result2);
    when(scanner.spliterator()).thenReturn(results.spliterator());
    ArgumentCaptor<Scan> scanCaptor = ArgumentCaptor.forClass(Scan.class);
    when(storage.scan(scanCaptor.capture())).thenReturn(scanner);

    // Act
    List<String> result = namespaceRegistry.scan("e");

    // Assert
    // Registry returns matching namespaces from database. "default" is not added by registry.
    assertThat(result).containsExactly("abc", "def");
    verify(storageAdmin).tableExists(BASE_NAMESPACE, NAMESPACE_TABLE_NAME);
    // Verify that the Scan object has the correct LIKE condition
    Scan capturedScan = scanCaptor.getValue();
    assertThat(capturedScan.getConjunctions()).hasSize(1);
    ConditionalExpression condition =
        capturedScan.getConjunctions().iterator().next().getConditions().iterator().next();
    assertThat(condition.getOperator()).isEqualTo(ConditionalExpression.Operator.LIKE);
    assertThat(condition.getTextValue()).isEqualTo("%e%");
  }

  @Test
  public void scan_PatternGiven_ShouldReturnOnlyMatchingNamespaces() throws ExecutionException {
    // Arrange
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    when(storageAdmin.tableExists(BASE_NAMESPACE, NAMESPACE_TABLE_NAME)).thenReturn(true);
    Scanner scanner = Mockito.mock(Scanner.class);
    Result result1 = Mockito.mock(Result.class);
    when(result1.getText(COLUMN_NAME)).thenReturn("xyz");
    List<Result> results = Arrays.asList(result1);
    when(scanner.spliterator()).thenReturn(results.spliterator());
    ArgumentCaptor<Scan> scanCaptor = ArgumentCaptor.forClass(Scan.class);
    when(storage.scan(scanCaptor.capture())).thenReturn(scanner);

    // Act
    List<String> result = namespaceRegistry.scan("xyz");

    // Assert
    assertThat(result).containsExactly("xyz");
    verify(storageAdmin).tableExists(BASE_NAMESPACE, NAMESPACE_TABLE_NAME);
    // Verify that the Scan object has the correct LIKE condition
    Scan capturedScan = scanCaptor.getValue();
    assertThat(capturedScan.getConjunctions()).hasSize(1);
    ConditionalExpression condition =
        capturedScan.getConjunctions().iterator().next().getConditions().iterator().next();
    assertThat(condition.getOperator()).isEqualTo(ConditionalExpression.Operator.LIKE);
    assertThat(condition.getTextValue()).isEqualTo("%xyz%");
  }

  @Test
  public void scan_MultipleNamespacesExist_ShouldReturnInOrder() throws ExecutionException {
    // Arrange
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    when(storageAdmin.tableExists(BASE_NAMESPACE, NAMESPACE_TABLE_NAME)).thenReturn(true);
    Scanner scanner = Mockito.mock(Scanner.class);
    Result result1 = Mockito.mock(Result.class);
    Result result2 = Mockito.mock(Result.class);
    when(result1.getText(COLUMN_NAME)).thenReturn("aaa");
    when(result2.getText(COLUMN_NAME)).thenReturn("zzz");
    List<Result> results = Arrays.asList(result1, result2);
    when(scanner.spliterator()).thenReturn(results.spliterator());
    when(storage.scan(any(Scan.class))).thenReturn(scanner);

    // Act
    List<String> result = namespaceRegistry.scan("");

    // Assert
    assertThat(result).containsExactly("aaa", "zzz");
    verify(storageAdmin).tableExists(BASE_NAMESPACE, NAMESPACE_TABLE_NAME);
    verify(storage).scan(any(Scan.class));
  }

  @Test
  public void drop_ExistingNamespaceGiven_ShouldDropProperly() throws ExecutionException {
    // Arrange
    String fullNamespace = BASE_NAMESPACE + NAMESPACE_NAME_SEPARATOR + SOME_NAMESPACE;
    Delete expectedDelete =
        Delete.newBuilder()
            .namespace(BASE_NAMESPACE)
            .table(NAMESPACE_TABLE_NAME)
            .partitionKey(Key.ofInt(COLUMN_PARTITION_ID, DEFAULT_PARTITION_ID))
            .clusteringKey(Key.ofText(COLUMN_NAME, SOME_NAMESPACE))
            .condition(ConditionBuilder.deleteIfExists())
            .build();
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);

    // Act
    namespaceRegistry.drop(SOME_NAMESPACE);

    // Assert
    verify(storageAdmin).dropTable(fullNamespace, SOME_TABLE_NAME_3, true);
    verify(storageAdmin).dropTable(fullNamespace, SOME_TABLE_NAME_4, true);
    verify(transactionAdmin).dropTable(fullNamespace, SOME_TABLE_NAME_1, true);
    verify(transactionAdmin).dropTable(fullNamespace, SOME_TABLE_NAME_2, true);
    verify(storageAdmin).dropNamespace(fullNamespace, true);
    verify(storage).delete(expectedDelete);
  }

  @Test
  public void drop_NonExistingNamespaceGiven_ShouldThrowException() throws ExecutionException {
    // Arrange
    String fullNamespace = BASE_NAMESPACE + NAMESPACE_NAME_SEPARATOR + SOME_NAMESPACE;
    Delete expectedDelete =
        Delete.newBuilder()
            .namespace(BASE_NAMESPACE)
            .table(NAMESPACE_TABLE_NAME)
            .partitionKey(Key.ofInt(COLUMN_PARTITION_ID, DEFAULT_PARTITION_ID))
            .clusteringKey(Key.ofText(COLUMN_NAME, SOME_NAMESPACE))
            .condition(ConditionBuilder.deleteIfExists())
            .build();
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    NoMutationException toThrow = Mockito.mock(NoMutationException.class);
    doThrow(toThrow).when(storage).delete(any(Delete.class));

    // Act Assert
    assertThatThrownBy(() -> namespaceRegistry.drop(SOME_NAMESPACE))
        .isInstanceOf(DatabaseException.class)
        .hasMessageContaining(CommonError.NAMESPACE_NOT_FOUND.buildMessage(SOME_NAMESPACE));
    verify(storageAdmin).dropTable(fullNamespace, SOME_TABLE_NAME_3, true);
    verify(storageAdmin).dropTable(fullNamespace, SOME_TABLE_NAME_4, true);
    verify(transactionAdmin).dropTable(fullNamespace, SOME_TABLE_NAME_1, true);
    verify(transactionAdmin).dropTable(fullNamespace, SOME_TABLE_NAME_2, true);
    verify(storageAdmin).dropNamespace(fullNamespace, true);
    verify(storage).delete(expectedDelete);
  }

  @Test
  public void drop_DropNamespaceFailed_ShouldThrowDatabaseException() throws ExecutionException {
    // Arrange
    String fullNamespace = BASE_NAMESPACE + NAMESPACE_NAME_SEPARATOR + SOME_NAMESPACE;
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    ExecutionException toThrow = new ExecutionException("details");
    doThrow(toThrow).when(storageAdmin).dropNamespace(fullNamespace, true);

    // Act Assert
    assertThatThrownBy(() -> namespaceRegistry.drop(SOME_NAMESPACE))
        .isInstanceOf(DatabaseException.class)
        .hasCause(toThrow)
        .hasMessageContaining(CommonError.DROPPING_NAMESPACE_FAILED.buildMessage("details"));
    verify(storageAdmin).dropTable(fullNamespace, SOME_TABLE_NAME_3, true);
    verify(storageAdmin).dropTable(fullNamespace, SOME_TABLE_NAME_4, true);
    verify(transactionAdmin).dropTable(fullNamespace, SOME_TABLE_NAME_1, true);
    verify(transactionAdmin).dropTable(fullNamespace, SOME_TABLE_NAME_2, true);
    verify(storageAdmin).dropNamespace(fullNamespace, true);
    verify(storage, never()).delete(any(Delete.class));
  }

  @Test
  public void drop_DropTableFailed_ShouldThrowDatabaseException() throws ExecutionException {
    // Arrange
    String fullNamespace = BASE_NAMESPACE + NAMESPACE_NAME_SEPARATOR + SOME_NAMESPACE;
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    ExecutionException toThrow = new ExecutionException("details");
    doThrow(toThrow).when(storageAdmin).dropTable(fullNamespace, SOME_TABLE_NAME_3, true);

    // Act Assert
    assertThatThrownBy(() -> namespaceRegistry.drop(SOME_NAMESPACE))
        .isInstanceOf(DatabaseException.class)
        .hasCause(toThrow)
        .hasMessageContaining(CommonError.DROPPING_NAMESPACE_FAILED.buildMessage("details"));
    verify(storageAdmin).dropTable(fullNamespace, SOME_TABLE_NAME_3, true);
    verify(storageAdmin, never()).dropNamespace(any(), anyBoolean());
    verify(storage, never()).delete(any(Delete.class));
  }

  @Test
  public void drop_DeleteNamespaceEntryFailed_ShouldThrowDatabaseException()
      throws ExecutionException {
    // Arrange
    String fullNamespace = BASE_NAMESPACE + NAMESPACE_NAME_SEPARATOR + SOME_NAMESPACE;
    Delete expectedDelete =
        Delete.newBuilder()
            .namespace(BASE_NAMESPACE)
            .table(NAMESPACE_TABLE_NAME)
            .partitionKey(Key.ofInt(COLUMN_PARTITION_ID, DEFAULT_PARTITION_ID))
            .clusteringKey(Key.ofText(COLUMN_NAME, SOME_NAMESPACE))
            .condition(ConditionBuilder.deleteIfExists())
            .build();
    when(config.getNamespace()).thenReturn(BASE_NAMESPACE);
    ExecutionException toThrow = new ExecutionException("details");
    doThrow(toThrow).when(storage).delete(expectedDelete);

    // Act Assert
    assertThatThrownBy(() -> namespaceRegistry.drop(SOME_NAMESPACE))
        .isInstanceOf(DatabaseException.class)
        .hasCause(toThrow)
        .hasMessageContaining(CommonError.DROPPING_NAMESPACE_FAILED.buildMessage("details"));
    verify(storageAdmin).dropTable(fullNamespace, SOME_TABLE_NAME_3, true);
    verify(storageAdmin).dropTable(fullNamespace, SOME_TABLE_NAME_4, true);
    verify(transactionAdmin).dropTable(fullNamespace, SOME_TABLE_NAME_1, true);
    verify(transactionAdmin).dropTable(fullNamespace, SOME_TABLE_NAME_2, true);
    verify(storageAdmin).dropNamespace(fullNamespace, true);
    verify(storage).delete(expectedDelete);
  }
}
