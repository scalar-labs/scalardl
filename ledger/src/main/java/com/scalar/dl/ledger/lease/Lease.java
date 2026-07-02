package com.scalar.dl.ledger.lease;

import java.util.Optional;
import javax.annotation.Nullable;

/**
 * A generic, storage-agnostic lease for leader election and best-effort coordination among multiple
 * nodes. A lease is a single named record holding the current holder and an expiration time;
 * acquiring, renewing, and failing over are all expressed as a conditional compare-and-swap on that
 * record ({@link #tryAcquireOrRenew}). Independent {@code leaseName}s are independent leases that
 * live in one shared lease table.
 *
 * <p><b>This lease does NOT provide mutual exclusion.</b> Under a holder's GC pause or clock skew,
 * more than one node may briefly believe it is the holder (this is inherent to a fencing-token-free
 * lease). Therefore any work performed while holding the lease MUST be idempotent and crash-safe.
 * Using a lease to guard non-idempotent work can cause silent data corruption; such use cases need
 * a fencing token (a monotonic version enforced by the downstream resource) in addition to this
 * lease.
 *
 * <p>Implementations back this with concrete storage (e.g. ScalarDB); the interface itself is free
 * of any storage-specific type so that callers stay decoupled from the backend.
 */
public interface Lease {

  /**
   * Reads the current state of the named lease.
   *
   * @param leaseName the lease name; different names are independent leases
   * @return the current lease record, or empty if it has never been acquired
   * @throws LeaseTableNotFoundException if the backing lease table does not exist
   * @throws LeaseException if the read fails due to an infrastructure error
   */
  Optional<LeaseEntry> get(String leaseName);

  /**
   * Atomically acquires or renews the named lease via a conditional compare-and-swap.
   *
   * <ul>
   *   <li>When {@code observed} is null, this attempts to acquire an absent lease (succeeds only if
   *       the record still does not exist).
   *   <li>When {@code observed} is non-null, this attempts to renew or take over the lease
   *       (succeeds only if the record still matches {@code observed}'s holder and expiry).
   *       Comparing on the observed expiry, not just the holder, prevents a node from resurrecting
   *       a lease it already lost during a pause.
   * </ul>
   *
   * @param leaseName the lease name
   * @param observed the lease state this attempt is conditioned on, or null to acquire an absent
   *     lease
   * @param holder the identifier of this node
   * @param expiry the new expiration time in epoch milliseconds
   * @return true if this node now holds the lease; false if the compare-and-swap lost (another node
   *     holds or changed it)
   * @throws LeaseTableNotFoundException if the backing lease table does not exist (e.g. when a
   *     caller acquires without a preceding {@link #get})
   * @throws LeaseException if the write fails due to an infrastructure error
   */
  boolean tryAcquireOrRenew(
      String leaseName, @Nullable LeaseEntry observed, String holder, long expiry);

  /**
   * Ensures the backing lease table exists, creating it if absent (idempotent). Intended to be
   * called lazily on a {@link LeaseTableNotFoundException} so that an upgraded deployment
   * provisions the table without a separate schema-loader run.
   *
   * @throws LeaseException if the creation fails due to an infrastructure error
   */
  void createTable();
}
