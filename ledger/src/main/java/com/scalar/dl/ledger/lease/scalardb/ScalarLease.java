package com.scalar.dl.ledger.lease.scalardb;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.Inject;
import com.scalar.db.api.ConditionBuilder;
import com.scalar.db.api.Consistency;
import com.scalar.db.api.DistributedStorage;
import com.scalar.db.api.DistributedStorageAdmin;
import com.scalar.db.api.Get;
import com.scalar.db.api.MutationCondition;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.TableMetadata;
import com.scalar.db.common.CoreError;
import com.scalar.db.exception.storage.ExecutionException;
import com.scalar.db.exception.storage.NoMutationException;
import com.scalar.db.io.DataType;
import com.scalar.db.io.Key;
import com.scalar.dl.ledger.database.scalardb.ScalarNamespaceResolver;
import com.scalar.dl.ledger.error.CommonError;
import com.scalar.dl.ledger.lease.Lease;
import com.scalar.dl.ledger.lease.LeaseEntry;
import com.scalar.dl.ledger.lease.LeaseException;
import com.scalar.dl.ledger.lease.LeaseTableNotFoundException;
import com.scalar.dl.ledger.namespace.Namespaces;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A {@link Lease} backed by ScalarDB's Storage API. Acquire/renew/failover are a single conditional
 * mutation ({@code PutIfNotExists} for an absent lease, {@code PutIf} on the observed holder and
 * expiry otherwise), which the storage layer evaluates as a linearizable compare-and-swap. The
 * Storage API is used deliberately instead of Consensus Commit: it is lighter for a single-record
 * CAS, and the Auditor does not use Consensus Commit at all.
 *
 * <p>The lease is a single global table living in the default (base) namespace, so no namespace is
 * taken per call; different {@code leaseName}s share that one table.
 */
@ThreadSafe
public class ScalarLease implements Lease {
  static final String TABLE = "lease";
  static final String LEASE_NAME = "lease_name";
  static final String HOLDER = "holder";
  static final String EXPIRY = "expiry";

  // This schema is mirrored in schema-loader/auditor-schema.json (the "auditor.lease" entry), which
  // provisions the table for new installs. Keep the two in sync: the reactive createTable() below
  // uses this definition, so a drift would create a different table depending on the provisioning
  // path.
  private static final TableMetadata TABLE_METADATA =
      TableMetadata.newBuilder()
          .addColumn(LEASE_NAME, DataType.TEXT)
          .addColumn(HOLDER, DataType.TEXT)
          .addColumn(EXPIRY, DataType.BIGINT)
          .addPartitionKey(LEASE_NAME)
          .build();

  private final DistributedStorage storage;
  private final DistributedStorageAdmin storageAdmin;
  private final String namespace;

  @Inject
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public ScalarLease(
      DistributedStorage storage,
      DistributedStorageAdmin storageAdmin,
      ScalarNamespaceResolver namespaceResolver) {
    this.storage = checkNotNull(storage);
    this.storageAdmin = checkNotNull(storageAdmin);
    // The lease is a single global table in the default (base) namespace.
    this.namespace = checkNotNull(namespaceResolver).resolve(Namespaces.DEFAULT);
  }

  @Override
  public Optional<LeaseEntry> get(String leaseName) {
    Get get =
        Get.newBuilder()
            .namespace(namespace)
            .table(TABLE)
            .partitionKey(Key.ofText(LEASE_NAME, leaseName))
            .consistency(Consistency.SEQUENTIAL)
            .build();

    try {
      return storage.get(get).map(this::toLeaseEntry);
    } catch (IllegalArgumentException e) {
      if (e.getMessage() != null
          && e.getMessage().startsWith(CoreError.TABLE_NOT_FOUND.buildCode())) {
        throw new LeaseTableNotFoundException(CommonError.LEASE_TABLE_NOT_FOUND, e);
      }
      throw e;
    } catch (ExecutionException e) {
      throw new LeaseException(CommonError.READING_LEASE_FAILED, e, e.getMessage());
    }
  }

  @Override
  public boolean tryAcquireOrRenew(
      String leaseName, @Nullable LeaseEntry observed, String holder, long expiry) {
    MutationCondition condition;
    if (observed == null) {
      // Acquire an absent lease: succeeds only if no node has created it yet.
      condition = ConditionBuilder.putIfNotExists();
    } else {
      // Renew or take over: the CAS succeeds only if the record still matches the holder and the
      // expiry we observed. Conditioning on the expiry (not just the holder) prevents resurrecting
      // a lease we already lost during a pause, where a holder-only check would silently overwrite
      // the new holder.
      condition =
          ConditionBuilder.putIf(
                  ConditionBuilder.column(HOLDER).isEqualToText(observed.getHolder()))
              .and(ConditionBuilder.column(EXPIRY).isEqualToBigInt(observed.getExpiry()))
              .build();
    }

    Put put =
        Put.newBuilder()
            .namespace(namespace)
            .table(TABLE)
            .partitionKey(Key.ofText(LEASE_NAME, leaseName))
            .textValue(HOLDER, holder)
            .bigIntValue(EXPIRY, expiry)
            .consistency(Consistency.LINEARIZABLE)
            .condition(condition)
            .build();

    try {
      storage.put(put);
      return true;
    } catch (NoMutationException e) {
      // The compare-and-swap lost: another node holds or changed the lease. Expected during
      // failover contention; not an error.
      return false;
    } catch (IllegalArgumentException e) {
      // Mirror get(): surface a missing table as LeaseTableNotFoundException so an acquire-first
      // caller can provision the table and retry, rather than leaking a raw storage exception.
      if (e.getMessage() != null
          && e.getMessage().startsWith(CoreError.TABLE_NOT_FOUND.buildCode())) {
        throw new LeaseTableNotFoundException(CommonError.LEASE_TABLE_NOT_FOUND, e);
      }
      throw e;
    } catch (ExecutionException e) {
      throw new LeaseException(CommonError.ACQUIRING_OR_RENEWING_LEASE_FAILED, e, e.getMessage());
    }
  }

  @Override
  public void createTable() {
    try {
      storageAdmin.createTable(namespace, TABLE, TABLE_METADATA, true);
    } catch (ExecutionException e) {
      throw new LeaseException(CommonError.CREATING_LEASE_TABLE_FAILED, e, e.getMessage());
    }
  }

  private LeaseEntry toLeaseEntry(Result result) {
    return new LeaseEntry(result.getText(HOLDER), result.getBigInt(EXPIRY));
  }
}
