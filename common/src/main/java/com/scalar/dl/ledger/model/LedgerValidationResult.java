package com.scalar.dl.ledger.model;

import com.scalar.dl.ledger.proof.AssetProof;
import com.scalar.dl.ledger.service.StatusCode;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
public final class LedgerValidationResult {
  private final StatusCode code;
  private final AssetProof ledgerProof;
  private final AssetProof auditorProof;

  /**
   * Constructs a {@code LedgerValidationResult} with the specified status code, asset proof from
   * Ledger, and asset proof from Auditor.
   *
   * @param code a {@link StatusCode}
   * @param ledgerProof an {@link AssetProof}
   * @param auditorProof an {@link AssetProof}
   */
  public LedgerValidationResult(
      StatusCode code, @Nullable AssetProof ledgerProof, @Nullable AssetProof auditorProof) {
    this.code = code;
    this.ledgerProof = ledgerProof;
    this.auditorProof = auditorProof;
  }

  /**
   * Returns the status code.
   *
   * @return the status code
   */
  public StatusCode getCode() {
    return code;
  }

  /**
   * Returns the proof of the asset from Ledger.
   *
   * @return the proof of the asset from Ledger
   * @deprecated This method will be removed in release 5.0.0
   */
  @Deprecated
  public Optional<com.scalar.dl.ledger.asset.AssetProof> getProof() {
    if (ledgerProof == null) {
      return Optional.empty();
    }
    return Optional.of(new com.scalar.dl.ledger.asset.AssetProof(ledgerProof));
  }

  /**
   * Returns the proof of the asset from Ledger.
   *
   * @return the proof of the asset from Ledger.
   */
  public Optional<AssetProof> getLedgerProof() {
    return Optional.ofNullable(ledgerProof);
  }

  /**
   * Returns the proof of the asset from Auditor.
   *
   * @return the proof of the asset from Auditor
   */
  public Optional<AssetProof> getAuditorProof() {
    return Optional.ofNullable(auditorProof);
  }

  /**
   * Returns a hash code value for the object.
   *
   * @return a hash code value for this object.
   */
  @Override
  public int hashCode() {
    return Objects.hash(code, ledgerProof, auditorProof);
  }

  /**
   * Indicates whether some other object is "equal to" this object. The other object is considered
   * equal if it is the same instance or if:
   *
   * <ul>
   *   <li>it is also a {@code LedgerValidationResult} and
   *   <li>both instances have the same status code and asset proof.
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
    if (!(o instanceof LedgerValidationResult)) {
      return false;
    }
    LedgerValidationResult other = (LedgerValidationResult) o;
    return this.code.equals(other.code) && Objects.equals(this.ledgerProof, other.ledgerProof);
  }
}
