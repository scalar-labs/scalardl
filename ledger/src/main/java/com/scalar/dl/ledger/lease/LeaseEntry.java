package com.scalar.dl.ledger.lease;

import java.util.Objects;
import javax.annotation.concurrent.Immutable;

/**
 * A snapshot of a lease record: which node currently holds it and when it expires. Storage
 * agnostic; it carries no ScalarDB types so that {@link Lease} stays decoupled from the underlying
 * storage.
 */
@Immutable
public class LeaseEntry {
  private final String holder;
  private final long expiry;

  /**
   * Constructs a lease snapshot.
   *
   * @param holder the identifier of the node holding the lease
   * @param expiry the expiration time in epoch milliseconds
   */
  public LeaseEntry(String holder, long expiry) {
    this.holder = holder;
    this.expiry = expiry;
  }

  public String getHolder() {
    return holder;
  }

  /** Returns the expiration time in epoch milliseconds. */
  public long getExpiry() {
    return expiry;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof LeaseEntry)) {
      return false;
    }
    LeaseEntry that = (LeaseEntry) o;
    return expiry == that.expiry && Objects.equals(holder, that.holder);
  }

  @Override
  public int hashCode() {
    return Objects.hash(holder, expiry);
  }

  @Override
  public String toString() {
    return "LeaseEntry{holder=" + holder + ", expiry=" + expiry + '}';
  }
}
