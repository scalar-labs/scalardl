package com.scalar.dl.ledger.database.scalardb;

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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.scalar.db.api.ConditionBuilder;
import com.scalar.db.api.Delete;
import com.scalar.db.api.ConditionalExpression;
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
import com.scalar.db.io.Key;
import com.scalar.dl.ledger.config.LedgerConfig;
import com.scalar.dl.ledger.error.CommonError;
import com.scalar.dl.ledger.exception.DatabaseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
            .partitionKey(Key.ofInt(COLUMN_PARTITION_ID, DEFAULT_PARTITION_ID))
            .clusteringKey(Key.ofText(COLUMN_NAME, SOME_NAMESPACE))
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
