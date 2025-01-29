package com.scalar.dl.ledger.model;

import com.google.common.collect.ImmutableMap;
import com.scalar.dl.ledger.proof.AssetProof;
import com.scalar.dl.ledger.service.StatusCode;
import java.util.Map;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;

@Immutable
public class LedgersValidationResult {
  private final StatusCode code;
  private final ImmutableMap<String, AssetProof> proofs;

  public LedgersValidationResult(StatusCode code, Map<String, AssetProof> proofs) {
    this.code = code;
    this.proofs = ImmutableMap.copyOf(proofs);
  }

  /**
   * Returns a status code from a validation.
   *
   * @return a status code
   */
  public StatusCode getCode() {
    return code;
  }

  /**
   * Returns the proofs of an asset from multiple ledgers.
   *
   * @return the proofs of an asset
   */
  public Map<String, AssetProof> getProofs() {
    return proofs;
  }

  /**
   * Returns a hash code value for the object.
   *
   * @return a hash code value for this object.
   */
  @Override
  public int hashCode() {
    return Objects.hash(code, proofs);
  }

  /**
   * Indicates whether some other object is "equal to" this object. The other object is considered
   * equal if it is the same instance or if:
   *
   * <ul>
   *   <li>it is also a {@code LedgersValidationResult} and
   *   <li>both instances have the same status code and asset proofs.
   * </ul>
   *
   * @param o an object to be tested for equality
   * @return {@code true} if the other object is "equal to" this object otherwise {@code false}
   */
  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof LedgersValidationResult)) {
      return false;
    }
    LedgersValidationResult other = (LedgersValidationResult) o;
    return this.code.equals(other.code) && this.proofs.equals(other.proofs);
  }
}
