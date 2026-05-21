package com.scalar.dl.auditor.model;

import com.scalar.dl.auditor.ordering.LockRecoveryResult;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;

@Immutable
public class AssetLockRecoveryResult {
  private final LockRecoveryResult result;

  public AssetLockRecoveryResult(LockRecoveryResult result) {
    this.result = Objects.requireNonNull(result);
  }

  public LockRecoveryResult getResult() {
    return result;
  }

  /**
   * Returns a hash code value for the object.
   *
   * @return a hash code value for this object.
   */
  @Override
  public int hashCode() {
    return Objects.hash(result);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof AssetLockRecoveryResult)) {
      return false;
    }
    AssetLockRecoveryResult other = (AssetLockRecoveryResult) o;
    return this.result.equals(other.result);
  }
}
