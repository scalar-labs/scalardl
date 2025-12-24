package com.scalar.dl.ledger.database.scalardb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.db.api.Consistency;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Scan;
import com.scalar.db.api.Scan.Ordering;
import com.scalar.db.api.Scan.Ordering.Order;
import com.scalar.db.common.CoreError;
import com.scalar.db.exception.transaction.AbortException;
import com.scalar.db.exception.transaction.CommitConflictException;
import com.scalar.db.exception.transaction.CommitException;
import com.scalar.db.exception.transaction.CrudException;
import com.scalar.db.io.BlobValue;
import com.scalar.db.io.IntValue;
import com.scalar.db.io.Key;
import com.scalar.db.io.TextValue;
import com.scalar.db.transaction.consensuscommit.TransactionResult;
import com.scalar.dl.ledger.config.LedgerConfig;
import com.scalar.dl.ledger.crypto.DigitalSignatureSigner;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.AssetProofComposer;
import com.scalar.dl.ledger.database.AssetRecord;
import com.scalar.dl.ledger.database.Snapshot;
import com.scalar.dl.ledger.database.TransactionState;
import com.scalar.dl.ledger.database.scalardb.ScalarTamperEvidentAssetLedger.AssetMetadata;
import com.scalar.dl.ledger.exception.ConflictException;
import com.scalar.dl.ledger.exception.DatabaseException;
import com.scalar.dl.ledger.exception.LedgerException;
import com.scalar.dl.ledger.exception.UnknownTransactionStatusException;
import com.scalar.dl.ledger.exception.ValidationException;
import com.scalar.dl.ledger.model.ContractExecutionRequest;
import com.scalar.dl.ledger.proof.AssetProof;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.statemachine.AssetKey;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ScalarTamperEvidentAssetLedgerTest {
  private static final String NAMESPACE = "scalar";
  private static final String DEFAULT_NAMESPACE = "default";
  private static final String CUSTOM_NAMESPACE = "custom_namespace";
  private static final String RESOLVED_CUSTOM_NAMESPACE = "scalar_custom_namespace";
  private static final String ANY_ID = "id";
  private static final String ANY_ID2 = "id2";
  private static final AssetKey ANY_ASSET_KEY = AssetKey.of(DEFAULT_NAMESPACE, ANY_ID);
  private static final AssetKey ANY_ASSET_KEY2 = AssetKey.of(DEFAULT_NAMESPACE, ANY_ID2);
  private static final int ANY_AGE = 1;
  private static final int ANY_AGE_START = 2;
  private static final int ANY_AGE_END = 5;
  private static final int ANY_LIMIT = 10;
  private static final String ANY_INPUT = "{\"id1\":{\"balance\":1000},\"id2\":{\"balance\":1000}}";
  private static final String ANY_OUTPUT = "{\"balance\":1100}";
  private static final String ANY_CONTRACT_ID = "com.any.contract.AnyContract";
  private static final String ANY_ARGUMENT =
      "{\"asset_ids\":[\"id1\",\"id2\"],\"amount\":100,\"nonce\":\"nonce\"}";
  private static final byte[] ANY_SIGNATURE = "signature".getBytes(StandardCharsets.UTF_8);
  private static final byte[] ANY_HASH = "hash".getBytes(StandardCharsets.UTF_8);
  private static final byte[] ANY_PREV_HASH = "prev_hash".getBytes(StandardCharsets.UTF_8);
  private static final String ANY_NONCE = "nonce";
  private static final String ANY_DATA = "data";
  @Mock private TransactionResult result;
  @Mock private TransactionResult metaResult;
  @Mock private DistributedTransaction transaction;
  @Mock private ContractExecutionRequest request;
  @Mock private TamperEvidentAssetComposer assetComposer;
  @Mock private AssetProofComposer proofComposer;
  @Mock private TransactionStateManager stateManager;
  @Mock private ScalarNamespaceResolver namespaceResolver;
  @Mock private InternalAsset asset;
  @Mock private LedgerConfig config;
  private Snapshot snapshot;
  private ScalarTamperEvidentAssetLedger ledger;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    snapshot = new Snapshot();
    when(config.isProofEnabled()).thenReturn(false);
    when(config.getNamespace()).thenReturn(NAMESPACE);
    when(namespaceResolver.resolve(DEFAULT_NAMESPACE)).thenReturn(NAMESPACE);
    ledger =
        new ScalarTamperEvidentAssetLedger(
            transaction,
            new ScalarTamperEvidentAssetLedger.Metadata(transaction, namespaceResolver),
            snapshot,
            request,
            assetComposer,
            proofComposer,
            stateManager,
            namespaceResolver,
            config);
  }

  private void configureResult(TransactionResult result) {
    when(result.getValue(AssetAttribute.ID))
        .thenReturn(Optional.of(new TextValue(AssetAttribute.ID, ANY_ID)));
    when(result.getValue(AssetAttribute.AGE))
        .thenReturn(Optional.of(new IntValue(AssetAttribute.AGE, ANY_AGE)));
    when(result.getValue(AssetAttribute.INPUT))
        .thenReturn(Optional.of(new TextValue(AssetAttribute.INPUT, ANY_INPUT)));
    when(result.getValue(AssetAttribute.OUTPUT))
        .thenReturn(Optional.of(new TextValue(AssetAttribute.OUTPUT, ANY_OUTPUT)));
    when(result.getValue(AssetAttribute.CONTRACT_ID))
        .thenReturn(Optional.of(new TextValue(AssetAttribute.CONTRACT_ID, ANY_CONTRACT_ID)));
    when(result.getValue(AssetAttribute.ARGUMENT))
        .thenReturn(Optional.of(new TextValue(AssetAttribute.ARGUMENT, ANY_ARGUMENT)));
    when(result.getValue(AssetAttribute.SIGNATURE))
        .thenReturn(Optional.of(new BlobValue(AssetAttribute.SIGNATURE, ANY_SIGNATURE)));
    when(result.getValue(AssetAttribute.HASH))
        .thenReturn(Optional.of(new BlobValue(AssetAttribute.HASH, ANY_HASH)));
    when(result.getValue(AssetAttribute.PREV_HASH))
        .thenReturn(Optional.of(new BlobValue(AssetAttribute.PREV_HASH, ANY_PREV_HASH)));

    when(result.getPartitionKey()).thenReturn(Optional.of(new Key(AssetAttribute.ID, ANY_ID)));
    when(result.getClusteringKey()).thenReturn(Optional.of(new Key(AssetAttribute.AGE, ANY_AGE)));
  }

  private void configureMetaResult(TransactionResult result) {
    when(result.getValue(AssetMetadata.ID))
        .thenReturn(Optional.of(new TextValue(AssetMetadata.ID, ANY_ID)));
    when(result.getValue(AssetMetadata.AGE))
        .thenReturn(Optional.of(new IntValue(AssetMetadata.AGE, ANY_AGE)));
  }

  private InternalAsset createAsset(String assetId, int age, String data) {
    return AssetRecord.newBuilder().id(assetId).age(age).data(data).build();
  }

  @Test
  public void get_RecordInSnapShot_ShouldReturnCorrectAsset() throws CrudException {
    // Arrange
    Optional<InternalAsset> expected = Optional.of(asset);
    snapshot.put(ANY_ASSET_KEY, asset);

    // Act
    Optional<InternalAsset> actual = ledger.get(ANY_ID);

    // Assert
    assertThat(actual).isEqualTo(expected);
    verify(transaction, never()).get(any(Get.class));
    verify(transaction, never()).scan(any(Scan.class));
  }

  @Test
  public void get_AssetIdGiven_ShouldReturnCorrectAssetAndStoreRetrievedRecord()
      throws CrudException {
    // Arrange
    configureMetaResult(metaResult);
    configureResult(result);
    Optional<AssetRecord> expected = Optional.of(AssetLedgerUtility.getAssetRecordFrom(result));
    when(transaction.get(any(Get.class)))
        .thenReturn(Optional.of(metaResult))
        .thenReturn(Optional.of(result));
    when(config.isDirectAssetAccessEnabled()).thenReturn(false);

    // Act
    Optional<InternalAsset> actual = ledger.get(ANY_ID);

    // Assert
    assertThat(actual).isEqualTo(expected);
    assertThat(snapshot.getReadSet()).containsOnly(entry(ANY_ASSET_KEY, expected.get()));
    ArgumentCaptor<Get> getCaptor = ArgumentCaptor.forClass(Get.class);
    verify(transaction, times(2)).get(getCaptor.capture());
    List<Get> captured = getCaptor.getAllValues();
    assertThat(captured.get(0).getPartitionKey().get())
        .isEqualTo(Collections.singletonList(new TextValue(AssetMetadata.ID, ANY_ID)));
    assertThat(captured.get(1).getPartitionKey().get())
        .isEqualTo(Collections.singletonList(new TextValue(AssetAttribute.ID, ANY_ID)));
    assertThat(captured.get(1).getClusteringKey().get().get())
        .isEqualTo(Collections.singletonList(new IntValue(AssetAttribute.AGE, ANY_AGE)));
    verify(transaction, never()).scan(any(Scan.class));
  }

  @Test
  public void
      get_AssetIdGivenAndDirectAssetAccessEnabled_ShouldReturnCorrectAssetAndStoreRetrievedRecord()
          throws CrudException {
    // Arrange
    configureResult(result);
    Optional<AssetRecord> expected = Optional.of(AssetLedgerUtility.getAssetRecordFrom(result));
    when(transaction.scan(any(Scan.class))).thenReturn(Collections.singletonList(result));
    when(config.isDirectAssetAccessEnabled()).thenReturn(true);

    // Act
    Optional<InternalAsset> actual = ledger.get(ANY_ID);

    // Assert
    assertThat(actual).isEqualTo(expected);
    assertThat(snapshot.getReadSet()).containsOnly(entry(ANY_ASSET_KEY, expected.get()));
    verify(transaction, never()).get(any(Get.class));
    ArgumentCaptor<Scan> scanCaptor = ArgumentCaptor.forClass(Scan.class);
    verify(transaction).scan(scanCaptor.capture());
    List<Scan> captured = scanCaptor.getAllValues();
    assertThat(captured.get(0).getPartitionKey().get())
        .isEqualTo(Collections.singletonList(new TextValue(AssetAttribute.ID, ANY_ID)));
    assertThat(captured.get(0).getOrderings())
        .isEqualTo(Collections.singletonList(new Ordering(AssetAttribute.AGE, Order.DESC)));
    assertThat(captured.get(0).getLimit()).isEqualTo(1);
  }

  @Test
  public void get_AssetIdGivenAndCrudExceptionThrownInGetAge_ShouldThrowAssetRetrievalException()
      throws CrudException {
    // Arrange
    CrudException toThrow = mock(CrudException.class);
    when(transaction.get(any(Get.class))).thenThrow(toThrow);
    when(config.isDirectAssetAccessEnabled()).thenReturn(false);

    // Act Assert
    assertThatThrownBy(() -> ledger.get(ANY_ID))
        .isInstanceOf(DatabaseException.class)
        .hasCause(toThrow);
    verify(transaction).get(any(Get.class));
    verify(transaction, never()).scan(any(Scan.class));
  }

  @Test
  public void
      get_AssetIdGivenAndDirectAssetAccessEnabledAndCrudExceptionThrownInGetAge_ShouldThrowAssetRetrievalException()
          throws CrudException {
    // Arrange
    CrudException toThrow = mock(CrudException.class);
    when(transaction.scan(any(Scan.class))).thenThrow(toThrow);
    when(config.isDirectAssetAccessEnabled()).thenReturn(true);

    // Act Assert
    assertThatThrownBy(() -> ledger.get(ANY_ID))
        .isInstanceOf(DatabaseException.class)
        .hasCause(toThrow);
    verify(transaction, never()).get(any(Get.class));
    verify(transaction).scan(any(Scan.class));
  }

  @Test
  public void
      get_AssetIdGivenAndCrudExceptionThrownInGetFromAssetTable_ShouldThrowAssetRetrievalException()
          throws CrudException {
    // Arrange
    configureMetaResult(metaResult);
    configureResult(result);
    CrudException toThrow = mock(CrudException.class);
    when(transaction.get(any(Get.class))).thenReturn(Optional.of(metaResult)).thenThrow(toThrow);
    when(config.isDirectAssetAccessEnabled()).thenReturn(false);

    // Act Assert
    assertThatThrownBy(() -> ledger.get(ANY_ID))
        .isInstanceOf(DatabaseException.class)
        .hasCause(toThrow);
    verify(transaction, times(2)).get(any(Get.class));
    verify(transaction, never()).scan(any(Scan.class));
  }

  @Test
  public void get_AssetIdGivenAndNothingReturnedFromMetaTable_ShouldReturnEmpty()
      throws CrudException {
    // Arrange
    when(transaction.get(any(Get.class))).thenReturn(Optional.empty());
    when(config.isDirectAssetAccessEnabled()).thenReturn(false);

    // Act
    Optional<InternalAsset> actual = ledger.get(ANY_ID);

    // Assert
    assertThat(actual.isPresent()).isFalse();
    verify(transaction).get(any(Get.class));
    verify(transaction, never()).scan(any(Scan.class));
  }

  @Test
  public void
      get_AssetIdGivenAndDirectAssetAccessEnabledAndNothingReturnedFromMetaTable_ShouldReturnEmpty()
          throws CrudException {
    // Arrange
    when(transaction.scan(any(Scan.class))).thenReturn(Collections.emptyList());
    when(config.isDirectAssetAccessEnabled()).thenReturn(true);

    // Act
    Optional<InternalAsset> actual = ledger.get(ANY_ID);

    // Assert
    assertThat(actual.isPresent()).isFalse();
    verify(transaction, never()).get(any(Get.class));
    verify(transaction).scan(any(Scan.class));
  }

  @Test
  public void get_AssetIdGivenAndNothingReturnedFromAssetTable_ShouldThrowValidationException()
      throws CrudException {
    // Arrange
    configureMetaResult(metaResult);
    when(transaction.get(any(Get.class)))
        .thenReturn(Optional.of(metaResult))
        .thenReturn(Optional.empty());
    when(config.isDirectAssetAccessEnabled()).thenReturn(false);

    // Act
    Throwable thrown = catchThrowable(() -> ledger.get(ANY_ID));

    // Assert
    assertThat(thrown).isInstanceOf(ValidationException.class);
    assertThat(((ValidationException) thrown).getCode()).isEqualTo(StatusCode.INCONSISTENT_STATES);
    verify(transaction, times(2)).get(any(Get.class));
    verify(transaction, never()).scan(any(Scan.class));
  }

  @Test
  public void scan_AssetFilterWithAscAgeOrderGiven_ShouldScanByTransactionWithAsc()
      throws CrudException {
    // Arrange
    AssetFilter filter = new AssetFilter(ANY_ID).withAgeOrder(AssetFilter.AgeOrder.ASC);
    configureMetaResult(metaResult);
    configureResult(result);
    when(transaction.get(any(Get.class)))
        .thenReturn(Optional.of(metaResult))
        .thenReturn(Optional.of(result));
    when(config.isDirectAssetAccessEnabled()).thenReturn(false);

    // Act
    ledger.scan(filter);

    // Assert
    Scan expected =
        new Scan(new Key(AssetAttribute.ID, ANY_ID))
            .withOrdering(new Scan.Ordering(AssetAttribute.AGE, Scan.Ordering.Order.ASC))
            .withConsistency(Consistency.LINEARIZABLE)
            .forNamespace(NAMESPACE)
            .forTable(ScalarTamperEvidentAssetLedger.TABLE);
    verify(transaction).scan(expected);
    assertThat(snapshot.getReadSet()).containsOnlyKeys(ANY_ASSET_KEY);
  }

  @Test
  public void scan_AssetFilterWithDescAgeOrderGiven_ShouldScanByTransactionWithDesc()
      throws CrudException {
    // Arrange
    AssetFilter filter = new AssetFilter(ANY_ID).withAgeOrder(AssetFilter.AgeOrder.DESC);
    configureMetaResult(metaResult);
    configureResult(result);
    when(transaction.get(any(Get.class)))
        .thenReturn(Optional.of(metaResult))
        .thenReturn(Optional.of(result));
    when(config.isDirectAssetAccessEnabled()).thenReturn(false);

    // Act
    ledger.scan(filter);

    // Assert
    Scan expected =
        new Scan(new Key(AssetAttribute.ID, ANY_ID))
            .withOrdering(new Scan.Ordering(AssetAttribute.AGE, Scan.Ordering.Order.DESC))
            .withConsistency(Consistency.LINEARIZABLE)
            .forNamespace(NAMESPACE)
            .forTable(ScalarTamperEvidentAssetLedger.TABLE);
    verify(transaction).scan(expected);
    assertThat(snapshot.getReadSet()).containsOnlyKeys(ANY_ASSET_KEY);
  }

  @Test
  public void scan_AssetFilterWithNonInclusiveStartAgeGiven_ShouldScanByTransactionWithStart()
      throws CrudException {
    // Arrange
    AssetFilter filter = new AssetFilter(ANY_ID).withStartAge(ANY_AGE_START, false);
    configureMetaResult(metaResult);
    configureResult(result);
    when(transaction.get(any(Get.class)))
        .thenReturn(Optional.of(metaResult))
        .thenReturn(Optional.of(result));
    when(config.isDirectAssetAccessEnabled()).thenReturn(false);

    // Act
    ledger.scan(filter);

    // Assert
    Scan expected =
        new Scan(new Key(AssetAttribute.ID, ANY_ID))
            .withStart(new Key(AssetAttribute.AGE, ANY_AGE_START), false)
            .withOrdering(new Scan.Ordering(AssetAttribute.AGE, Scan.Ordering.Order.DESC))
            .withConsistency(Consistency.LINEARIZABLE)
            .forNamespace(NAMESPACE)
            .forTable(ScalarTamperEvidentAssetLedger.TABLE);
    verify(transaction).scan(expected);
    assertThat(snapshot.getReadSet()).containsOnlyKeys(ANY_ASSET_KEY);
  }

  @Test
  public void scan_AssetFilterWithNonInclusiveEndAgeGiven_ShouldScanByTransactionWithEnd()
      throws CrudException {
    // Arrange
    AssetFilter filter = new AssetFilter(ANY_ID).withEndAge(ANY_AGE_END, false);
    configureMetaResult(metaResult);
    configureResult(result);
    when(transaction.get(any(Get.class)))
        .thenReturn(Optional.of(metaResult))
        .thenReturn(Optional.of(result));
    when(config.isDirectAssetAccessEnabled()).thenReturn(false);

    // Act
    ledger.scan(filter);

    // Assert
    Scan expected =
        new Scan(new Key(AssetAttribute.ID, ANY_ID))
            .withEnd(new Key(AssetAttribute.AGE, ANY_AGE_END), false)
            .withOrdering(new Scan.Ordering(AssetAttribute.AGE, Scan.Ordering.Order.DESC))
            .withConsistency(Consistency.LINEARIZABLE)
            .forNamespace(NAMESPACE)
            .forTable(ScalarTamperEvidentAssetLedger.TABLE);
    verify(transaction).scan(expected);
    assertThat(snapshot.getReadSet()).containsOnlyKeys(ANY_ASSET_KEY);
  }

  @Test
  public void scan_AssetFilterWithLimitGiven_ShouldScanByTransactionWithLimit()
      throws CrudException {
    // Arrange
    AssetFilter filter = new AssetFilter(ANY_ID).withLimit(ANY_LIMIT);
    configureMetaResult(metaResult);
    configureResult(result);
    when(transaction.get(any(Get.class)))
        .thenReturn(Optional.of(metaResult))
        .thenReturn(Optional.of(result));
    when(config.isDirectAssetAccessEnabled()).thenReturn(false);

    // Act
    ledger.scan(filter);

    // Assert
    Scan expected =
        new Scan(new Key(AssetAttribute.ID, ANY_ID))
            .withLimit(ANY_LIMIT)
            .withOrdering(new Scan.Ordering(AssetAttribute.AGE, Scan.Ordering.Order.DESC))
            .withConsistency(Consistency.LINEARIZABLE)
            .forNamespace(NAMESPACE)
            .forTable(ScalarTamperEvidentAssetLedger.TABLE);
    verify(transaction).scan(expected);
    assertThat(snapshot.getReadSet()).containsOnlyKeys(ANY_ASSET_KEY);
  }

  @Test
  public void put_AssetIdAndJsonGiven_ShouldStoreToWriteSet() {
    // Arrange

    // Act
    ledger.put(ANY_ID, ANY_DATA);

    // Assert
    InternalAsset expected = createAsset(ANY_ID, 0, ANY_DATA);
    assertThat(snapshot.getWriteSet().size()).isEqualTo(1);
    // equals is only implemented in the asset in this class
    assertThat(expected).isEqualTo(snapshot.getWriteSet().get(ANY_ASSET_KEY));
  }

  @Test
  public void scan_AssetFilterWithNamespaceGiven_ShouldScanWithSpecifiedNamespace()
      throws CrudException {
    // Arrange
    AssetFilter filter = new AssetFilter(CUSTOM_NAMESPACE, ANY_ID);
    when(namespaceResolver.resolve(CUSTOM_NAMESPACE)).thenReturn(RESOLVED_CUSTOM_NAMESPACE);
    configureMetaResult(metaResult);
    configureResult(result);
    when(transaction.get(any(Get.class)))
        .thenReturn(Optional.of(metaResult))
        .thenReturn(Optional.of(result));
    when(config.isDirectAssetAccessEnabled()).thenReturn(false);

    // Act
    ledger.scan(filter);

    // Assert
    Scan expected =
        Scan.newBuilder()
            .namespace(RESOLVED_CUSTOM_NAMESPACE)
            .table(ScalarTamperEvidentAssetLedger.TABLE)
            .partitionKey(Key.ofText(AssetAttribute.ID, ANY_ID))
            .ordering(Scan.Ordering.desc(AssetAttribute.AGE))
            .consistency(Consistency.LINEARIZABLE)
            .build();
    verify(transaction).scan(expected);
    assertThat(snapshot.getReadSet()).containsOnlyKeys(AssetKey.of(CUSTOM_NAMESPACE, ANY_ID));
  }

  @Test
  public void get_NamespaceAndAssetIdGiven_ShouldReturnCorrectAssetFromSpecifiedNamespace()
      throws CrudException {
    // Arrange
    configureMetaResult(metaResult);
    configureResult(result);
    when(namespaceResolver.resolve(CUSTOM_NAMESPACE)).thenReturn(RESOLVED_CUSTOM_NAMESPACE);
    when(transaction.get(any(Get.class)))
        .thenReturn(Optional.of(metaResult))
        .thenReturn(Optional.of(result));
    when(config.isDirectAssetAccessEnabled()).thenReturn(false);
    AssetKey expectedKey = AssetKey.of(CUSTOM_NAMESPACE, ANY_ID);

    // Act
    Optional<InternalAsset> actual = ledger.get(CUSTOM_NAMESPACE, ANY_ID);

    // Assert
    assertThat(actual).isPresent();
    assertThat(snapshot.getReadSet()).containsKey(expectedKey);
    ArgumentCaptor<Get> getCaptor = ArgumentCaptor.forClass(Get.class);
    verify(transaction, times(2)).get(getCaptor.capture());
    List<Get> captured = getCaptor.getAllValues();
    // Verify the namespace is resolved and used for the DB access
    assertThat(captured.get(0).forNamespace()).isEqualTo(Optional.of(RESOLVED_CUSTOM_NAMESPACE));
    assertThat(captured.get(1).forNamespace()).isEqualTo(Optional.of(RESOLVED_CUSTOM_NAMESPACE));
  }

  @Test
  public void put_NamespaceAndAssetIdAndJsonGiven_ShouldStoreToWriteSetWithCorrectKey() {
    // Arrange
    AssetKey expectedKey = AssetKey.of(CUSTOM_NAMESPACE, ANY_ID);

    // Act
    ledger.put(CUSTOM_NAMESPACE, ANY_ID, ANY_DATA);

    // Assert
    InternalAsset expected = createAsset(ANY_ID, 0, ANY_DATA);
    assertThat(snapshot.getWriteSet().size()).isEqualTo(1);
    assertThat(snapshot.getWriteSet()).containsKey(expectedKey);
    assertThat(expected).isEqualTo(snapshot.getWriteSet().get(expectedKey));
  }

  @Test
  public void commit_NonEmptySnapshotGiven_ShouldCommitProperly()
      throws CommitException, com.scalar.db.exception.transaction.UnknownTransactionStatusException,
          CrudException {
    // Arrange
    snapshot.put(ANY_ASSET_KEY, asset);
    snapshot.put(ANY_ASSET_KEY, ANY_DATA);
    doNothing().when(transaction).put(any(List.class));
    doNothing().when(transaction).put(any(Put.class));
    when(config.isDirectAssetAccessEnabled()).thenReturn(false);

    // Act
    ledger.commit();

    // Assert
    verify(transaction).put(any(List.class));
    verify(transaction).put(any(Put.class));
    verify(transaction).commit();
  }

  @Test
  public void commit_NonEmptySnapshotGivenAndDirectAssetAccessEnabled_ShouldCommitProperly()
      throws CommitException, com.scalar.db.exception.transaction.UnknownTransactionStatusException,
          CrudException {
    // Arrange
    snapshot.put(ANY_ASSET_KEY, asset);
    snapshot.put(ANY_ASSET_KEY, ANY_DATA);
    doNothing().when(transaction).put(any(List.class));
    when(config.isDirectAssetAccessEnabled()).thenReturn(true);

    // Act
    ledger.commit();

    // Assert
    verify(transaction).put(any(List.class));
    verify(transaction, never()).put(any(Put.class)); // for asset_metadata
    verify(transaction).commit();
  }

  @Test
  public void commit_EmptySnapshotGiven_ShouldDoNothing()
      throws CommitException, com.scalar.db.exception.transaction.UnknownTransactionStatusException,
          CrudException {
    // Arrange
    when(config.isDirectAssetAccessEnabled()).thenReturn(false);
    when(config.isTxStateManagementEnabled()).thenReturn(false);

    // Act
    ledger.commit();

    // Assert
    verify(transaction, never()).put(any(List.class));
    verify(transaction, never()).put(any(Put.class));
    verify(transaction).commit();
  }

  @Test
  public void commit_EmptySnapshotGivenAndDirectAssetAccessEnabled_ShouldDoNothing()
      throws CommitException, com.scalar.db.exception.transaction.UnknownTransactionStatusException,
          CrudException {
    // Arrange
    when(config.isDirectAssetAccessEnabled()).thenReturn(true);
    when(config.isTxStateManagementEnabled()).thenReturn(false);

    // Act
    ledger.commit();

    // Assert
    verify(transaction, never()).put(any(List.class));
    verify(transaction, never()).put(any(Put.class));
    verify(transaction).commit();
  }

  @Test
  public void
      commit_NonEmptySnapshotGivenAndCommitConflictExceptionThrown_ShouldThrowAssetOverwriteException()
          throws CommitException,
              com.scalar.db.exception.transaction.UnknownTransactionStatusException, CrudException {
    // Arrange
    snapshot.put(ANY_ASSET_KEY, asset);
    snapshot.put(ANY_ASSET_KEY, ANY_DATA);
    doNothing().when(transaction).put(any(List.class));
    doNothing().when(transaction).put(any(Put.class));
    CommitConflictException toThrow = mock(CommitConflictException.class);
    doThrow(toThrow).when(transaction).commit();
    when(config.isDirectAssetAccessEnabled()).thenReturn(false);

    // Act
    assertThatThrownBy(() -> ledger.commit())
        .isInstanceOf(ConflictException.class)
        .hasCause(toThrow);

    // Assert
    verify(transaction).put(any(List.class));
    verify(transaction).put(any(Put.class));
    verify(transaction).commit();
  }

  @Test
  public void commit_NonEmptySnapshotGivenAndCommitExceptionThrown_ShouldThrowDatabaseException()
      throws CommitException, com.scalar.db.exception.transaction.UnknownTransactionStatusException,
          CrudException {
    // Arrange
    snapshot.put(ANY_ASSET_KEY, asset);
    snapshot.put(ANY_ASSET_KEY, ANY_DATA);
    doNothing().when(transaction).put(any(List.class));
    doNothing().when(transaction).put(any(Put.class));
    CommitException toThrow = mock(CommitException.class);
    doThrow(toThrow).when(transaction).commit();
    when(config.isDirectAssetAccessEnabled()).thenReturn(false);

    // Act
    assertThatThrownBy(() -> ledger.commit())
        .isInstanceOf(DatabaseException.class)
        .hasCause(toThrow);

    // Assert
    verify(transaction).put(any(List.class));
    verify(transaction).put(any(Put.class));
    verify(transaction).commit();
  }

  @Test
  public void
      commit_NonEmptySnapshotGivenAndUTSExceptionThrownWithUnknownTransactionIdPresent_ShouldThrowUASException()
          throws CommitException,
              com.scalar.db.exception.transaction.UnknownTransactionStatusException, CrudException {
    // Arrange
    snapshot.put(ANY_ASSET_KEY, asset);
    snapshot.put(ANY_ASSET_KEY, ANY_DATA);
    doNothing().when(transaction).put(any(List.class));
    doNothing().when(transaction).put(any(Put.class));
    com.scalar.db.exception.transaction.UnknownTransactionStatusException toThrow =
        mock(com.scalar.db.exception.transaction.UnknownTransactionStatusException.class);
    when(toThrow.getUnknownTransactionId()).thenReturn(Optional.of(ANY_ID));
    doThrow(toThrow).when(transaction).commit();
    when(config.isDirectAssetAccessEnabled()).thenReturn(false);

    // Act
    Throwable thrown = catchThrowable(() -> ledger.commit());

    // Assert
    assertThat(thrown).isInstanceOf(UnknownTransactionStatusException.class).hasCause(toThrow);
    assertThat(((UnknownTransactionStatusException) thrown).getUnknownTransactionId())
        .isEqualTo(Optional.of(ANY_ID));
    verify(transaction).put(any(List.class));
    verify(transaction).put(any(Put.class));
    verify(transaction).commit();
  }

  @Test
  public void commit_NonEmptySnapshotGivenAndUTSExceptionThrown_ShouldThrowUASException()
      throws CommitException, com.scalar.db.exception.transaction.UnknownTransactionStatusException,
          CrudException {
    // Arrange
    snapshot.put(ANY_ASSET_KEY, asset);
    snapshot.put(ANY_ASSET_KEY, ANY_DATA);
    doNothing().when(transaction).put(any(List.class));
    doNothing().when(transaction).put(any(Put.class));
    com.scalar.db.exception.transaction.UnknownTransactionStatusException toThrow =
        mock(com.scalar.db.exception.transaction.UnknownTransactionStatusException.class);
    doThrow(toThrow).when(transaction).commit();
    when(config.isDirectAssetAccessEnabled()).thenReturn(false);

    // Act
    assertThatThrownBy(() -> ledger.commit())
        .isInstanceOf(UnknownTransactionStatusException.class)
        .hasCause(toThrow);

    // Assert
    verify(transaction).put(any(List.class));
    verify(transaction).put(any(Put.class));
    verify(transaction).commit();
  }

  @Test
  public void commit_CommitExceptionThrownInCommit_ShouldThrowDatabaseException()
      throws CommitException, com.scalar.db.exception.transaction.UnknownTransactionStatusException,
          CrudException {
    // Arrange
    snapshot.put(ANY_ASSET_KEY, asset);
    snapshot.put(ANY_ASSET_KEY, ANY_DATA);
    doNothing().when(transaction).put(any(List.class));
    CommitException toThrow = mock(CommitException.class);
    doThrow(toThrow).when(transaction).commit();
    when(config.isDirectAssetAccessEnabled()).thenReturn(false);

    // Act Assert
    assertThatThrownBy(() -> ledger.commit())
        .isInstanceOf(DatabaseException.class)
        .hasCause(toThrow);

    // Assert
    verify(transaction).put(any(List.class));
    verify(transaction).put(any(Put.class));
    verify(transaction).commit();
  }

  @Test
  public void
      commit_NonEmptySnapshotGivenWithAssetProofEnabled_ShouldCommitProperlyAndReturnProofs()
          throws CommitException,
              com.scalar.db.exception.transaction.UnknownTransactionStatusException, CrudException {
    // Arrange
    snapshot.put(ANY_ASSET_KEY, ANY_DATA);
    snapshot.put(ANY_ASSET_KEY2, asset);
    doNothing().when(transaction).put(any(List.class));
    doNothing().when(transaction).put(any(Put.class));
    Put put =
        new Put(
                new Key(AssetAttribute.toIdValue(ANY_ID)),
                new Key(AssetAttribute.toAgeValue(ANY_AGE)))
            .withValue(AssetAttribute.toHashValue(ANY_HASH))
            .withValue(AssetAttribute.toPrevHashValue(ANY_PREV_HASH))
            .withValue(AssetAttribute.toInputValue(ANY_INPUT))
            .forNamespace(NAMESPACE)
            .forTable(ScalarTamperEvidentAssetLedger.TABLE);
    when(assetComposer.compose(any(), any()))
        .thenReturn(Collections.singletonMap(ANY_ASSET_KEY, put));
    when(request.getNonce()).thenReturn(ANY_NONCE);
    when(asset.id()).thenReturn(ANY_ID2);
    when(asset.age()).thenReturn(ANY_AGE);
    when(asset.hash()).thenReturn(ANY_HASH);
    when(asset.prevHash()).thenReturn(ANY_PREV_HASH);
    when(asset.input()).thenReturn(ANY_INPUT);
    when(asset.argument()).thenReturn(ANY_ARGUMENT);

    when(config.isProofEnabled()).thenReturn(true);
    DigitalSignatureSigner signer = mock(DigitalSignatureSigner.class);
    when(signer.sign(any())).thenReturn(ANY_SIGNATURE);
    proofComposer = new AssetProofComposer(signer);
    ledger =
        new ScalarTamperEvidentAssetLedger(
            transaction,
            new ScalarTamperEvidentAssetLedger.Metadata(transaction, namespaceResolver),
            snapshot,
            request,
            assetComposer,
            proofComposer,
            stateManager,
            namespaceResolver,
            config);
    when(config.isDirectAssetAccessEnabled()).thenReturn(false);

    // Act
    List<AssetProof> proofs = ledger.commit();

    // Assert
    verify(transaction).put(any(List.class));
    verify(transaction).put(any(Put.class));
    verify(transaction).commit();
    AssetProof proof1 =
        AssetProof.newBuilder()
            .namespace(DEFAULT_NAMESPACE)
            .id(ANY_ID)
            .age(ANY_AGE)
            .input(ANY_INPUT)
            .hash(ANY_HASH)
            .prevHash(ANY_PREV_HASH)
            .nonce(ANY_NONCE)
            .signature(ANY_SIGNATURE)
            .build();
    AssetProof proof2 =
        AssetProof.newBuilder()
            .namespace(DEFAULT_NAMESPACE)
            .id(ANY_ID2)
            .age(ANY_AGE)
            .input(ANY_INPUT)
            .hash(ANY_HASH)
            .prevHash(ANY_PREV_HASH)
            .nonce(ANY_NONCE)
            .signature(ANY_SIGNATURE)
            .build();
    assertThat(proofs).containsOnly(proof1, proof2);
  }

  @Test
  public void
      commit_ReadSetOnlySnapshotGivenWithAssetProofEnabled_ShouldCommitProperlyAndReturnProofs()
          throws CommitException,
              com.scalar.db.exception.transaction.UnknownTransactionStatusException, CrudException {
    // Arrange
    snapshot.put(ANY_ASSET_KEY, asset);
    when(asset.id()).thenReturn(ANY_ID2);
    when(asset.age()).thenReturn(ANY_AGE);
    when(asset.hash()).thenReturn(ANY_HASH);
    when(asset.prevHash()).thenReturn(ANY_PREV_HASH);
    when(asset.input()).thenReturn(ANY_INPUT);
    when(asset.argument()).thenReturn(ANY_ARGUMENT);

    when(config.isProofEnabled()).thenReturn(true);
    DigitalSignatureSigner signer = mock(DigitalSignatureSigner.class);
    when(signer.sign(any())).thenReturn(ANY_SIGNATURE);
    proofComposer = new AssetProofComposer(signer);
    ledger =
        new ScalarTamperEvidentAssetLedger(
            transaction,
            new ScalarTamperEvidentAssetLedger.Metadata(transaction, namespaceResolver),
            snapshot,
            request,
            assetComposer,
            proofComposer,
            stateManager,
            namespaceResolver,
            config);
    when(config.isDirectAssetAccessEnabled()).thenReturn(false);

    // Act
    List<AssetProof> proofs = ledger.commit();

    // Assert
    verify(transaction, never()).put(any(List.class));
    verify(transaction, never()).put(any(Put.class));
    verify(transaction).commit();
    AssetProof proof =
        AssetProof.newBuilder()
            .namespace(DEFAULT_NAMESPACE)
            .id(ANY_ID2)
            .age(ANY_AGE)
            .input(ANY_INPUT)
            .hash(ANY_HASH)
            .prevHash(ANY_PREV_HASH)
            .nonce(ANY_NONCE)
            .signature(ANY_SIGNATURE)
            .build();
    assertThat(proofs).containsOnly(proof);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void commit_WriteTransactionGiven_ShouldPutWithStateManagerAccordingToConfig(
      boolean txStateManagementEnabled)
      throws CrudException, CommitException,
          com.scalar.db.exception.transaction.UnknownTransactionStatusException {
    // Arrange
    when(config.isTxStateManagementEnabled()).thenReturn(txStateManagementEnabled);
    snapshot.put(ANY_ASSET_KEY, asset);
    snapshot.put(ANY_ASSET_KEY, ANY_DATA);
    doNothing().when(transaction).put(any(List.class));
    doNothing().when(transaction).put(any(Put.class));
    when(transaction.getId()).thenReturn(ANY_NONCE);
    doNothing().when(stateManager).putCommit(any(DistributedTransaction.class), anyString());

    // Act
    ledger.commit();

    // Assert
    if (txStateManagementEnabled) {
      verify(stateManager).putCommit(transaction, ANY_NONCE);
    } else {
      verify(stateManager, never()).putCommit(any(DistributedTransaction.class), anyString());
    }
    verify(transaction).put(any(List.class));
    verify(transaction).put(any(Put.class));
    verify(transaction).commit();
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void commit_ReadOnlyTransactionGiven_ShouldPutWithStateManagerAccordingToConfig(
      boolean txStateManagementEnabled)
      throws CrudException, CommitException,
          com.scalar.db.exception.transaction.UnknownTransactionStatusException {
    // Arrange
    when(config.isTxStateManagementEnabled()).thenReturn(txStateManagementEnabled);
    when(config.isDirectAssetAccessEnabled()).thenReturn(false);
    when(transaction.getId()).thenReturn(ANY_NONCE);
    doNothing().when(stateManager).putCommit(any(DistributedTransaction.class), anyString());
    snapshot.put(ANY_ASSET_KEY, asset);

    // Act
    ledger.commit();

    // Assert
    if (txStateManagementEnabled) {
      verify(stateManager).putCommit(transaction, ANY_NONCE);
    } else {
      verify(stateManager, never()).putCommit(any(DistributedTransaction.class), anyString());
    }
    verify(transaction, never()).put(any(List.class));
    verify(transaction, never()).put(any(Put.class));
    verify(transaction).commit();
  }

  @Test
  public void abort_TxStateManagementEnabled_ShouldAbortWithStateManager() throws AbortException {
    // Arrange
    when(config.isTxStateManagementEnabled()).thenReturn(true);
    when(transaction.getId()).thenReturn(ANY_NONCE);
    when(stateManager.putAbort(anyString())).thenReturn(TransactionState.ABORTED);

    // Act
    ledger.abort();

    // Assert
    verify(transaction).abort();
    verify(stateManager).putAbort(ANY_NONCE);
  }

  @Test
  public void abort_TxStateManagementDisabled_ShouldNotAbortWithStateManager()
      throws AbortException {
    // Arrange
    when(config.isTxStateManagementEnabled()).thenReturn(false);
    when(transaction.getId()).thenReturn(ANY_NONCE);
    when(stateManager.putAbort(anyString())).thenReturn(TransactionState.ABORTED);

    // Act
    ledger.abort();

    // Assert
    verify(transaction).abort();
    verify(stateManager, never()).putAbort(ANY_NONCE);
  }

  @Test
  public void get_TableNotFoundExceptionThrown_ShouldThrowLedgerExceptionWithNamespaceNotFound()
      throws CrudException {
    // Arrange
    IllegalArgumentException toThrow =
        new IllegalArgumentException(CoreError.TABLE_NOT_FOUND.buildMessage(NAMESPACE + ".asset"));
    when(transaction.get(any(Get.class))).thenThrow(toThrow);
    when(config.isDirectAssetAccessEnabled()).thenReturn(false);

    // Act
    Throwable thrown = catchThrowable(() -> ledger.get(ANY_ID));

    // Assert
    assertThat(thrown).isInstanceOf(LedgerException.class);
    assertThat(((LedgerException) thrown).getCode()).isEqualTo(StatusCode.NAMESPACE_NOT_FOUND);
  }

  @Test
  public void
      get_TableNotFoundExceptionThrownAndDirectAssetAccessEnabled_ShouldThrowLedgerExceptionWithNamespaceNotFound()
          throws CrudException {
    // Arrange
    IllegalArgumentException toThrow =
        new IllegalArgumentException(CoreError.TABLE_NOT_FOUND.buildMessage(NAMESPACE + ".asset"));
    when(transaction.scan(any(Scan.class))).thenThrow(toThrow);
    when(config.isDirectAssetAccessEnabled()).thenReturn(true);

    // Act
    Throwable thrown = catchThrowable(() -> ledger.get(ANY_ID));

    // Assert
    assertThat(thrown).isInstanceOf(LedgerException.class);
    assertThat(((LedgerException) thrown).getCode()).isEqualTo(StatusCode.NAMESPACE_NOT_FOUND);
  }

  @Test
  public void
      get_NamespaceAndAssetIdGivenAndTableNotFoundExceptionThrown_ShouldThrowLedgerExceptionWithNamespaceNotFound()
          throws CrudException {
    // Arrange
    IllegalArgumentException toThrow =
        new IllegalArgumentException(
            CoreError.TABLE_NOT_FOUND.buildMessage(RESOLVED_CUSTOM_NAMESPACE + ".asset"));
    when(namespaceResolver.resolve(CUSTOM_NAMESPACE)).thenReturn(RESOLVED_CUSTOM_NAMESPACE);
    when(transaction.get(any(Get.class))).thenThrow(toThrow);
    when(config.isDirectAssetAccessEnabled()).thenReturn(false);

    // Act
    Throwable thrown = catchThrowable(() -> ledger.get(CUSTOM_NAMESPACE, ANY_ID));

    // Assert
    assertThat(thrown).isInstanceOf(LedgerException.class);
    assertThat(((LedgerException) thrown).getCode()).isEqualTo(StatusCode.NAMESPACE_NOT_FOUND);
  }

  @Test
  public void get_OtherIllegalArgumentExceptionThrown_ShouldRethrowException()
      throws CrudException {
    // Arrange
    IllegalArgumentException toThrow = new IllegalArgumentException("some other error");
    when(transaction.get(any(Get.class))).thenThrow(toThrow);
    when(config.isDirectAssetAccessEnabled()).thenReturn(false);

    // Act Assert
    assertThatThrownBy(() -> ledger.get(ANY_ID))
        .isInstanceOf(IllegalArgumentException.class)
        .isSameAs(toThrow);
  }

  @Test
  public void scan_TableNotFoundExceptionThrown_ShouldThrowLedgerExceptionWithNamespaceNotFound()
      throws CrudException {
    // Arrange
    AssetFilter filter = new AssetFilter(ANY_ID);
    IllegalArgumentException toThrow =
        new IllegalArgumentException(CoreError.TABLE_NOT_FOUND.buildMessage(NAMESPACE + ".asset"));
    when(transaction.scan(any(Scan.class))).thenThrow(toThrow);

    // Act
    Throwable thrown = catchThrowable(() -> ledger.scan(filter));

    // Assert
    assertThat(thrown).isInstanceOf(LedgerException.class);
    assertThat(((LedgerException) thrown).getCode()).isEqualTo(StatusCode.NAMESPACE_NOT_FOUND);
  }

  @Test
  public void
      scan_AwareAssetFilterWithNamespaceGivenAndTableNotFoundExceptionThrown_ShouldThrowLedgerExceptionWithNamespaceNotFound()
          throws CrudException {
    // Arrange
    AssetFilter filter = new AssetFilter(CUSTOM_NAMESPACE, ANY_ID);
    IllegalArgumentException toThrow =
        new IllegalArgumentException(
            CoreError.TABLE_NOT_FOUND.buildMessage(RESOLVED_CUSTOM_NAMESPACE + ".asset"));
    when(namespaceResolver.resolve(CUSTOM_NAMESPACE)).thenReturn(RESOLVED_CUSTOM_NAMESPACE);
    when(transaction.scan(any(Scan.class))).thenThrow(toThrow);

    // Act
    Throwable thrown = catchThrowable(() -> ledger.scan(filter));

    // Assert
    assertThat(thrown).isInstanceOf(LedgerException.class);
    assertThat(((LedgerException) thrown).getCode()).isEqualTo(StatusCode.NAMESPACE_NOT_FOUND);
  }

  @Test
  public void scan_OtherIllegalArgumentExceptionThrown_ShouldRethrowException()
      throws CrudException {
    // Arrange
    AssetFilter filter = new AssetFilter(ANY_ID);
    IllegalArgumentException toThrow = new IllegalArgumentException("some other error");
    when(transaction.scan(any(Scan.class))).thenThrow(toThrow);

    // Act Assert
    assertThatThrownBy(() -> ledger.scan(filter))
        .isInstanceOf(IllegalArgumentException.class)
        .isSameAs(toThrow);
  }
}
