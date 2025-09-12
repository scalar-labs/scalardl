package com.scalar.dl.ledger.model;

import com.google.common.collect.ImmutableList;
import com.scalar.dl.ledger.proof.AssetProof;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.concurrent.Immutable;

/**
 * The result of an operation execution in Ledger. It contains the result of the internal contract
 * execution along with a list of {@link AssetProof}s from Ledger and Auditor.
 */
@Immutable
// non-final for mocking
public class ExecutionResult {
  private final String result;
  private final ImmutableList<AssetProof> ledgerProofs;
  private final ImmutableList<AssetProof> auditorProofs;

  /**
   * Constructs a {@code ExecutionResult} using the specified {@code ContractExecutionResult}
   *
   * @param contractExecutionResult a {@code ContractExecutionResult}
   */
  public ExecutionResult(ContractExecutionResult contractExecutionResult) {
    this.result = contractExecutionResult.getContractResult().orElse(null);
    this.ledgerProofs = ImmutableList.copyOf(contractExecutionResult.getLedgerProofs());
    this.auditorProofs = ImmutableList.copyOf(contractExecutionResult.getAuditorProofs());
  }

  /**
   * Returns the result of an operation execution in Ledger.
   *
   * @return the result of an operation execution in Ledger
   */
  public Optional<String> getResult() {
    return Optional.ofNullable(result);
  }

  /**
   * Returns the list of {@link AssetProof}s from Ledger.
   *
   * @return the list of {@link AssetProof}s from Ledger
   */
  public List<AssetProof> getLedgerProofs() {
    return ledgerProofs;
  }

  /**
   * Returns the list of {@link AssetProof}s from Auditor.
   *
   * @return the list of {@link AssetProof}s from Auditor
   */
  public List<AssetProof> getAuditorProofs() {
    return auditorProofs;
  }

  /**
   * Returns a hash code value for the object.
   *
   * @return a hash code value for this object.
   */
  @Override
  public int hashCode() {
    return Objects.hash(result, ledgerProofs, auditorProofs);
  }

  /**
   * Indicates whether some other object is "equal to" this object. The other object is considered
   * equal if it is the same instance or if:
   *
   * <ul>
   *   <li>it is also an {@code ExecutionResult} and
   *   <li>both instances have the same result and proofs.
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
    if (!(o instanceof ExecutionResult)) {
      return false;
    }
    ExecutionResult other = (ExecutionResult) o;
    return Objects.equals(this.result, other.result)
        && this.ledgerProofs.equals(other.ledgerProofs)
        && this.auditorProofs.equals(other.auditorProofs);
  }
}
