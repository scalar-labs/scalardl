package com.scalar.dl.ledger.database.scalardb;

import com.scalar.db.api.Consistency;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.db.api.Scan.Ordering;
import com.scalar.db.api.Scan.Ordering.Order;
import com.scalar.db.exception.transaction.AbortException;
import com.scalar.db.exception.transaction.CommitConflictException;
import com.scalar.db.exception.transaction.CommitException;
import com.scalar.db.exception.transaction.CrudConflictException;
import com.scalar.db.exception.transaction.CrudException;
import com.scalar.db.io.IntValue;
import com.scalar.db.io.Key;
import com.scalar.db.io.TextValue;
import com.scalar.dl.ledger.config.LedgerConfig;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.AssetRecord;
import com.scalar.dl.ledger.database.Snapshot;
import com.scalar.dl.ledger.database.TamperEvidentAssetLedger;
import com.scalar.dl.ledger.exception.ConflictException;
import com.scalar.dl.ledger.exception.DatabaseException;
import com.scalar.dl.ledger.exception.UnexpectedValueException;
import com.scalar.dl.ledger.exception.UnknownTransactionStatusException;
import com.scalar.dl.ledger.exception.ValidationException;
import com.scalar.dl.ledger.model.ContractExecutionRequest;
import com.scalar.dl.ledger.proof.AssetProof;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class ScalarTamperEvidentAssetLedger implements TamperEvidentAssetLedger {
  static final String TABLE = "asset";
  private final DistributedTransaction transaction;
  private final Metadata metadata;
  private final Snapshot snapshot;
  private final ContractExecutionRequest request;
  private final TamperEvidentAssetComposer assetComposer;
  private final AssetProofComposer proofComposer;
  private final TransactionStateManager stateManager;
  private final LedgerConfig config;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public ScalarTamperEvidentAssetLedger(
      DistributedTransaction transaction,
      Metadata metadata,
      Snapshot snapshot,
      ContractExecutionRequest request,
      TamperEvidentAssetComposer assetComposer,
      AssetProofComposer proofComposer,
      TransactionStateManager stateManager,
      LedgerConfig config) {
    this.transaction = transaction;
    this.metadata = metadata;
    this.snapshot = snapshot;
    this.request = request;
    this.assetComposer = assetComposer;
    this.proofComposer = proofComposer;
    this.stateManager = stateManager;
    this.config = config;
  }

  @Override
  public Optional<InternalAsset> get(String assetId) {
    Optional<InternalAsset> recordInSnapshot = snapshot.get(assetId);
    if (recordInSnapshot.isPresent()) {
      return recordInSnapshot;
    }

    Optional<Result> result;
    if (config.isDirectAssetAccessEnabled()) {
      result = getLatestWithScan(assetId);
    } else {
      result = getLatestWithTwoLookups(assetId);
    }
    if (!result.isPresent()) {
      return Optional.empty();
    }

    AssetRecord record = AssetLedgerUtility.getAssetRecordFrom(result.get());
    snapshot.put(assetId, record);

    return Optional.of(record);
  }

  @Override
  public List<InternalAsset> scan(AssetFilter filter) {
    Scan scan =
        AssetLedgerUtility.getScanFrom(filter)
            .withConsistency(Consistency.LINEARIZABLE)
            .forTable(TABLE);

    List<InternalAsset> records = new ArrayList<>();
    scan(scan)
        .forEach(
            r -> {
              AssetRecord record = AssetLedgerUtility.getAssetRecordFrom(r);
              records.add(record);
            });

    // add the asset ID to the snapshot to create a proof
    get(filter.getId());

    return records;
  }

  @Override
  public void put(String assetId, String data) {
    if (!snapshot.getReadSet().containsKey(assetId)) {
      get(assetId);
    }
    snapshot.put(assetId, data);
  }

  @Override
  public List<AssetProof> commit() {
    List<Put> puts = Collections.emptyList();

    try {
      if (snapshot.hasWriteSet()) {
        puts = assetComposer.compose(snapshot, request);

        if (config.isTxStateManagementEnabled()) {
          stateManager.putCommit(transaction, transaction.getId());
        }

        transaction.put(puts);

        if (!config.isDirectAssetAccessEnabled()) {
          metadata.put(snapshot.getWriteSet().values());
        }
      }

      transaction.commit();
    } catch (CrudConflictException e) {
      throw new ConflictException("putting asset records failed", e, Collections.emptyMap());
    } catch (CommitConflictException e) {
      throw new ConflictException("committing asset records failed", e, getAssetIds());
    } catch (com.scalar.db.exception.transaction.UnknownTransactionStatusException e) {
      if (e.getUnknownTransactionId().isPresent()) {
        throw new UnknownTransactionStatusException(
            "asset status is unknown", e, e.getUnknownTransactionId().get());
      } else {
        throw new UnknownTransactionStatusException("asset status is unknown", e);
      }
    } catch (CrudException | CommitException e) {
      throw new DatabaseException(
          "putting or committing asset records failed for some reason",
          e,
          StatusCode.DATABASE_ERROR);
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
      throw new DatabaseException("abort failed", e, StatusCode.DATABASE_ERROR);
    }
    if (config.isTxStateManagementEnabled()) {
      stateManager.putAbort(transaction.getId());
    }
  }

  private Map<String, Integer> getAssetIds() {
    Map<String, Integer> ids = new HashMap<>();
    snapshot.getWriteSet().forEach((id, uncommitted) -> ids.put(id, uncommitted.age()));
    return ids;
  }

  private Optional<Result> getLatestWithTwoLookups(String assetId) {
    // Get the latest entry with the following two lookups:
    // 1. Get the latest age from asset_metadata table
    // 2. Get the latest asset entry from asset table with the retrieved age above
    Optional<AssetMetadata> assetMetadata = metadata.get(assetId);
    if (!assetMetadata.isPresent()) {
      return Optional.empty();
    }

    Optional<Result> result = get(assetId, assetMetadata.get().getAge());
    if (!result.isPresent()) {
      throw new ValidationException(
          "asset_metadata and asset are inconsistent.", StatusCode.INCONSISTENT_STATES);
    }

    return result;
  }

  private Optional<Result> get(String assetId, int age) {
    try {
      Get get =
          new Get(
                  new Key(AssetAttribute.toIdValue(assetId)),
                  new Key(AssetAttribute.toAgeValue(age)))
              .forTable(TABLE);
      return transaction.get(get);
    } catch (CrudConflictException e) {
      throw new ConflictException("asset retrieval failed due to conflict", e);
    } catch (CrudException e) {
      throw new DatabaseException("asset retrieval failed", e, StatusCode.DATABASE_ERROR);
    }
  }

  private Optional<Result> getLatestWithScan(String assetId) {
    try {
      Scan scan =
          new Scan(new Key(AssetAttribute.toIdValue(assetId)))
              .withOrdering(new Ordering(AssetRecord.AGE, Order.DESC))
              .withLimit(1)
              .forTable(TABLE);
      List<Result> results = transaction.scan(scan);
      if (results.isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(results.get(0));
    } catch (CrudConflictException e) {
      throw new ConflictException("asset retrieval failed due to conflict", e);
    } catch (CrudException e) {
      throw new DatabaseException("asset retrieval failed", e, StatusCode.DATABASE_ERROR);
    }
  }

  private List<Result> scan(Scan scan) {
    try {
      return transaction.scan(scan);
    } catch (CrudConflictException e) {
      throw new ConflictException("asset retrieval failed due to conflict", e);
    } catch (CrudException e) {
      throw new DatabaseException("asset retrieval failed", e, StatusCode.DATABASE_ERROR);
    }
  }

  private List<AssetProof> createProofs(
      List<Put> puts, Map<String, InternalAsset> readSet, String nonce) {
    Map<String, AssetProof> proofs = new HashMap<>();

    // For written assets
    puts.stream()
        // proofs are created with only asset tables
        .filter(p -> p.forTable().get().equals(ScalarTamperEvidentAssetLedger.TABLE))
        .forEach(
            p -> {
              AssetProof proof = proofComposer.create(p, nonce);
              proofs.putIfAbsent(proof.getId(), proof);
            });

    // For read assets that are not written
    readSet.forEach((k, v) -> proofs.putIfAbsent(k, proofComposer.create(v)));

    return new ArrayList<>(proofs.values());
  }

  static class Metadata {
    private static final String TABLE = "asset_metadata";
    private final DistributedTransaction transaction;

    protected Metadata(DistributedTransaction transaction) {
      this.transaction = transaction;
    }

    public void put(Collection<InternalAsset> assets) {
      try {
        for (InternalAsset asset : assets) {
          Put put =
              new Put(new Key(AssetMetadata.toIdValue(asset.id())))
                  .withValue(AssetMetadata.toAgeValue(asset.age()))
                  .forTable(TABLE);
          transaction.put(put);
        }
      } catch (CrudConflictException e) {
        throw new ConflictException("putting asset metadata failed due to conflict", e);
      } catch (CrudException e) {
        throw new DatabaseException("putting asset metadata failed", e, StatusCode.DATABASE_ERROR);
      }
    }

    public Optional<AssetMetadata> get(String assetId) {
      Get get = new Get(new Key(AssetMetadata.ID, assetId)).forTable(TABLE);

      Optional<Result> result;
      try {
        result = transaction.get(get);
      } catch (CrudConflictException e) {
        throw new ConflictException("asset metadata retrieval failed due to conflict", e);
      } catch (CrudException e) {
        throw new DatabaseException(
            "asset metadata retrieval failed", e, StatusCode.DATABASE_ERROR);
      }

      return result.map(r -> new AssetMetadata(result.get()));
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
        throw new UnexpectedValueException(e);
      }
    }

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
