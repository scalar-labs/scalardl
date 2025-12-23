package com.scalar.dl.ledger.database.scalardb;

import com.google.common.collect.ImmutableMap;
import com.scalar.db.api.Consistency;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.db.api.Scan.Ordering;
import com.scalar.db.api.Scan.Ordering.Order;
import com.scalar.db.api.TableMetadata;
import com.scalar.db.exception.transaction.AbortException;
import com.scalar.db.exception.transaction.CommitConflictException;
import com.scalar.db.exception.transaction.CommitException;
import com.scalar.db.exception.transaction.CrudConflictException;
import com.scalar.db.exception.transaction.CrudException;
import com.scalar.db.io.DataType;
import com.scalar.db.io.IntValue;
import com.scalar.db.io.Key;
import com.scalar.db.io.TextValue;
import com.scalar.dl.ledger.config.LedgerConfig;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.AssetProofComposer;
import com.scalar.dl.ledger.database.AssetRecord;
import com.scalar.dl.ledger.database.Snapshot;
import com.scalar.dl.ledger.database.TamperEvidentAssetLedger;
import com.scalar.dl.ledger.error.CommonError;
import com.scalar.dl.ledger.error.LedgerError;
import com.scalar.dl.ledger.exception.ConflictException;
import com.scalar.dl.ledger.exception.DatabaseException;
import com.scalar.dl.ledger.exception.UnexpectedValueException;
import com.scalar.dl.ledger.exception.UnknownTransactionStatusException;
import com.scalar.dl.ledger.exception.ValidationException;
import com.scalar.dl.ledger.model.ContractExecutionRequest;
import com.scalar.dl.ledger.namespace.NamespaceManager;
import com.scalar.dl.ledger.proof.AssetProof;
import com.scalar.dl.ledger.statemachine.AssetKey;
import com.scalar.dl.ledger.statemachine.Context;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class ScalarTamperEvidentAssetLedger implements TamperEvidentAssetLedger {
  static final String TABLE = "asset";
  private static final TableMetadata TABLE_METADATA =
      TableMetadata.newBuilder()
          .addColumn(AssetRecord.ID, DataType.TEXT)
          .addColumn(AssetRecord.AGE, DataType.INT)
          .addColumn(AssetRecord.ARGUMENT, DataType.TEXT)
          .addColumn(AssetRecord.CONTRACT_ID, DataType.TEXT)
          .addColumn(AssetRecord.HASH, DataType.BLOB)
          .addColumn(AssetRecord.INPUT, DataType.TEXT)
          .addColumn(AssetRecord.OUTPUT, DataType.TEXT)
          .addColumn(AssetRecord.PREV_HASH, DataType.BLOB)
          .addColumn(AssetRecord.SIGNATURE, DataType.BLOB)
          .addPartitionKey(AssetRecord.ID)
          .addClusteringKey(AssetRecord.AGE)
          .build();
  private static final TableMetadata METADATA_TABLE_METADATA =
      TableMetadata.newBuilder()
          .addColumn(AssetMetadata.ID, DataType.TEXT)
          .addColumn(AssetMetadata.AGE, DataType.INT)
          .addPartitionKey(AssetMetadata.ID)
          .build();
  private final DistributedTransaction transaction;
  private final Metadata metadata;
  private final Snapshot snapshot;
  private final ContractExecutionRequest request;
  private final TamperEvidentAssetComposer assetComposer;
  private final AssetProofComposer proofComposer;
  private final TransactionStateManager stateManager;
  private final ScalarNamespaceResolver namespaceResolver;
  private final LedgerConfig config;
  private final Context context;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public ScalarTamperEvidentAssetLedger(
      DistributedTransaction transaction,
      Metadata metadata,
      Snapshot snapshot,
      ContractExecutionRequest request,
      TamperEvidentAssetComposer assetComposer,
      AssetProofComposer proofComposer,
      TransactionStateManager stateManager,
      ScalarNamespaceResolver namespaceResolver,
      LedgerConfig config) {
    this.transaction = transaction;
    this.metadata = metadata;
    this.snapshot = snapshot;
    this.request = request;
    this.assetComposer = assetComposer;
    this.proofComposer = proofComposer;
    this.stateManager = stateManager;
    this.namespaceResolver = namespaceResolver;
    this.config = config;
    // Although currently a context is statically set to the default namespace, it will be set based
    // on the context specified in the request when the isolated namespace feature is supported.
    this.context = Context.withNamespace(NamespaceManager.DEFAULT_NAMESPACE);
  }

  static Map<String, TableMetadata> getTransactionTables() {
    return ImmutableMap.of(TABLE, TABLE_METADATA, Metadata.TABLE, METADATA_TABLE_METADATA);
  }

  @Override
  public Optional<InternalAsset> get(String assetId) {
    return get(context.getNamespace(), assetId);
  }

  @Override
  public Optional<InternalAsset> get(String namespace, String assetId) {
    AssetKey key = AssetKey.of(namespace, assetId);
    Optional<InternalAsset> recordInSnapshot = snapshot.get(key);
    if (recordInSnapshot.isPresent()) {
      return recordInSnapshot;
    }

    Optional<Result> result;
    if (config.isDirectAssetAccessEnabled()) {
      result = getLatestWithScan(namespace, assetId);
    } else {
      result = getLatestWithTwoLookups(namespace, assetId);
    }
    if (!result.isPresent()) {
      return Optional.empty();
    }

    AssetRecord record = AssetLedgerUtility.getAssetRecordFrom(result.get());
    snapshot.put(key, record);

    return Optional.of(record);
  }

  @Override
  public List<InternalAsset> scan(AssetFilter filter) {
    String namespace = filter.getNamespace().orElse(context.getNamespace());
    Scan scan =
        AssetLedgerUtility.getScanFrom(filter)
            .withConsistency(Consistency.LINEARIZABLE)
            .forNamespace(namespaceResolver.resolve(namespace))
            .forTable(TABLE);

    List<InternalAsset> records = new ArrayList<>();
    scan(scan)
        .forEach(
            r -> {
              AssetRecord record = AssetLedgerUtility.getAssetRecordFrom(r);
              records.add(record);
            });

    // add the asset ID to the snapshot to create a proof
    get(namespace, filter.getId());

    return records;
  }

  @Override
  public void put(String assetId, String data) {
    put(context.getNamespace(), assetId, data);
  }

  @Override
  public void put(String namespace, String assetId, String data) {
    AssetKey key = AssetKey.of(namespace, assetId);
    if (!snapshot.getReadSet().containsKey(key)) {
      get(namespace, assetId);
    }
    snapshot.put(key, data);
  }

  @Override
  public List<AssetProof> commit() {
    ImmutableMap<AssetKey, Put> puts = ImmutableMap.of();

    try {
      if (snapshot.hasWriteSet()) {
        puts = ImmutableMap.copyOf(assetComposer.compose(snapshot, request));

        transaction.put(new ArrayList<>(puts.values()));

        if (!config.isDirectAssetAccessEnabled()) {
          metadata.put(snapshot.getWriteSet());
        }
      }

      if (config.isTxStateManagementEnabled()) {
        stateManager.putCommit(transaction, transaction.getId());
      }

      transaction.commit();
    } catch (CrudConflictException e) {
      throw new ConflictException(
          LedgerError.PUTTING_ASSET_FAILED_DUE_TO_CONFLICT,
          e,
          Collections.emptyMap(),
          e.getMessage());
    } catch (CommitConflictException e) {
      throw new ConflictException(
          LedgerError.COMMITTING_ASSET_FAILED_DUE_TO_CONFLICT, e, getAssetKeys(), e.getMessage());
    } catch (com.scalar.db.exception.transaction.UnknownTransactionStatusException e) {
      if (e.getUnknownTransactionId().isPresent()) {
        throw new UnknownTransactionStatusException(
            LedgerError.UNKNOWN_ASSET_STATUS, e, e.getUnknownTransactionId().get(), e.getMessage());
      } else {
        throw new UnknownTransactionStatusException(
            LedgerError.UNKNOWN_ASSET_STATUS, e, null, e.getMessage());
      }
    } catch (CrudException | CommitException e) {
      throw new DatabaseException(LedgerError.PUTTING_OR_COMMITTING_FAILED, e, e.getMessage());
    }

    if (config.isProofEnabled() && request != null) {
      return createProofs(puts, snapshot.getReadSet(), request.getNonce());
    }
    return Collections.emptyList();
  }

  @Override
  public void abort() {
    try {
      transaction.abort();
    } catch (AbortException e) {
      throw new DatabaseException(LedgerError.ABORTING_TRANSACTION_FAILED, e, e.getMessage());
    }
    if (config.isTxStateManagementEnabled()) {
      stateManager.putAbort(transaction.getId());
    }
  }

  private Map<AssetKey, Integer> getAssetKeys() {
    Map<AssetKey, Integer> ids = new HashMap<>();
    snapshot.getWriteSet().forEach((key, uncommitted) -> ids.put(key, uncommitted.age()));
    return ids;
  }

  private Optional<Result> getLatestWithTwoLookups(String namespace, String assetId) {
    // Get the latest entry with the following two lookups:
    // 1. Get the latest age from asset_metadata table
    // 2. Get the latest asset entry from asset table with the retrieved age above
    Optional<AssetMetadata> assetMetadata = metadata.get(namespace, assetId);
    if (!assetMetadata.isPresent()) {
      return Optional.empty();
    }

    Optional<Result> result = get(namespace, assetId, assetMetadata.get().getAge());
    if (!result.isPresent()) {
      throw new ValidationException(LedgerError.INCONSISTENT_ASSET_METADATA);
    }

    return result;
  }

  private Optional<Result> get(String namespace, String assetId, int age) {
    try {
      Get get =
          new Get(
                  new Key(AssetAttribute.toIdValue(assetId)),
                  new Key(AssetAttribute.toAgeValue(age)))
              .forNamespace(namespaceResolver.resolve(namespace))
              .forTable(TABLE);
      return transaction.get(get);
    } catch (CrudConflictException e) {
      throw new ConflictException(
          LedgerError.RETRIEVING_ASSET_FAILED_DUE_TO_CONFLICT, e, e.getMessage());
    } catch (CrudException e) {
      throw new DatabaseException(LedgerError.RETRIEVING_ASSET_FAILED, e, e.getMessage());
    }
  }

  private Optional<Result> getLatestWithScan(String namespace, String assetId) {
    try {
      Scan scan =
          new Scan(new Key(AssetAttribute.toIdValue(assetId)))
              .withOrdering(new Ordering(AssetRecord.AGE, Order.DESC))
              .withLimit(1)
              .forNamespace(namespaceResolver.resolve(namespace))
              .forTable(TABLE);
      List<Result> results = transaction.scan(scan);
      if (results.isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(results.get(0));
    } catch (CrudConflictException e) {
      throw new ConflictException(
          LedgerError.RETRIEVING_ASSET_FAILED_DUE_TO_CONFLICT, e, e.getMessage());
    } catch (CrudException e) {
      throw new DatabaseException(LedgerError.RETRIEVING_ASSET_FAILED, e, e.getMessage());
    }
  }

  private List<Result> scan(Scan scan) {
    try {
      return transaction.scan(scan);
    } catch (CrudConflictException e) {
      throw new ConflictException(
          LedgerError.RETRIEVING_ASSET_FAILED_DUE_TO_CONFLICT, e, e.getMessage());
    } catch (CrudException e) {
      throw new DatabaseException(LedgerError.RETRIEVING_ASSET_FAILED, e, e.getMessage());
    }
  }

  private List<AssetProof> createProofs(
      Map<AssetKey, Put> puts, Map<AssetKey, InternalAsset> readSet, String nonce) {
    Map<AssetKey, AssetProof> proofs = new HashMap<>();

    // For written assets
    puts.forEach(
        (key, put) -> {
          // proofs are created with only asset tables
          if (put.forTable().get().equals(ScalarTamperEvidentAssetLedger.TABLE)) {
            AssetProof proof = createProofFrom(key, put, nonce);
            proofs.put(key, proof);
          }
        });

    // For read assets that are not written
    readSet.forEach((k, v) -> proofs.putIfAbsent(k, proofComposer.create(k.namespace(), v)));

    return new ArrayList<>(proofs.values());
  }

  private AssetProof createProofFrom(AssetKey key, Put p, String nonce) {
    String namespace = key.namespace();
    String id = p.getPartitionKey().getColumns().get(0).getTextValue();
    int age = p.getClusteringKey().get().getColumns().get(0).getIntValue();
    String input = p.getTextValue(AssetAttribute.INPUT);
    byte[] hash = p.getBlobValueAsBytes(AssetAttribute.HASH);
    byte[] prevHash = p.getBlobValueAsBytes(AssetAttribute.PREV_HASH);
    return proofComposer.create(namespace, id, age, nonce, input, hash, prevHash);
  }

  static class Metadata {
    private static final String TABLE = "asset_metadata";
    private final DistributedTransaction transaction;
    private final ScalarNamespaceResolver namespaceResolver;

    protected Metadata(DistributedTransaction transaction, ScalarNamespaceResolver resolver) {
      this.transaction = transaction;
      this.namespaceResolver = resolver;
    }

    public void put(Map<AssetKey, InternalAsset> writeSet) {
      try {
        for (Entry<AssetKey, InternalAsset> entry : writeSet.entrySet()) {
          AssetKey assetKey = entry.getKey();
          InternalAsset asset = entry.getValue();
          Put put =
              new Put(new Key(AssetMetadata.toIdValue(asset.id())))
                  .withValue(AssetMetadata.toAgeValue(asset.age()))
                  .forNamespace(namespaceResolver.resolve(assetKey.namespace()))
                  .forTable(TABLE);
          transaction.put(put);
        }
      } catch (CrudConflictException e) {
        throw new ConflictException(
            LedgerError.PUTTING_ASSET_METADATA_FAILED_DUE_TO_CONFLICT, e, e.getMessage());
      } catch (CrudException e) {
        throw new DatabaseException(LedgerError.PUTTING_ASSET_METADATA_FAILED, e, e.getMessage());
      }
    }

    public Optional<AssetMetadata> get(String namespace, String assetId) {
      Get get =
          new Get(new Key(AssetMetadata.ID, assetId))
              .forNamespace(namespaceResolver.resolve(namespace))
              .forTable(TABLE);

      Optional<Result> result;
      try {
        result = transaction.get(get);
      } catch (CrudConflictException e) {
        throw new ConflictException(
            LedgerError.RETRIEVING_ASSET_METADATA_FAILED_DUE_TO_CONFLICT, e, e.getMessage());
      } catch (CrudException e) {
        throw new DatabaseException(
            LedgerError.RETRIEVING_ASSET_METADATA_FAILED, e, e.getMessage());
      }

      return result.map(AssetMetadata::new);
    }
  }

  static class AssetMetadata {
    public static final String ID = "asset_id";
    public static final String AGE = "latest_age";
    private final String id;
    private final int age;

    /**
     * Constructs an {@code AssetMetadata} from the specified {@link Result}
     *
     * @param result a {@link Result}
     */
    public AssetMetadata(Result result) {
      try {
        this.id = getIdFrom(result);
        this.age = getAgeFrom(result);
      } catch (Exception e) {
        throw new UnexpectedValueException(
            CommonError.UNEXPECTED_RECORD_VALUE_OBSERVED, e, e.getMessage());
      }
    }

    /**
     * SpotBugs detects Bug Type "CT_CONSTRUCTOR_THROW" saying that "The object under construction
     * remains partially initialized and may be vulnerable to Finalizer attacks."
     */
    @Override
    protected final void finalize() {}

    /**
     * Returns the id of the asset.
     *
     * @return the id of the asset
     */
    public String getId() {
      return id;
    }

    /**
     * Returns the age of the asset.
     *
     * @return the age of the asset
     */
    public int getAge() {
      return age;
    }

    /**
     * Extract, and return as a {@code TextValue}, the asset id from a {@link Result}
     *
     * @param result a {@code Result}
     * @return the extracted asset id as a {@code TextValue}
     */
    public static String getIdFrom(Result result) {
      return result.getValue(ID).get().getAsString().get();
    }

    /**
     * Extract, and return as an {@code IntValue}, the age of the asset from a {@link Result}
     *
     * @param result a {@code Result}
     * @return the extracted asset age as an {@code IntValue}
     */
    public static int getAgeFrom(Result result) {
      return result.getValue(AGE).get().getAsInt();
    }

    /**
     * Returns a {@code TextValue} with name "asset_id" and value the given string
     *
     * @param assetId the asset's id
     * @return a {@code TextValue}
     */
    public static TextValue toIdValue(String assetId) {
      return new TextValue(ID, assetId);
    }

    /**
     * Returns a {@code IntValue} with name "latest_age" and value the given int
     *
     * @param age the asset's age
     * @return a {@code IntValue}
     */
    public static IntValue toAgeValue(int age) {
      return new IntValue(AGE, age);
    }
  }
}
