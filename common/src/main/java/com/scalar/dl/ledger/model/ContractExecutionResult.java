package com.scalar.dl.ledger.model;

import com.google.common.collect.ImmutableList;
import com.scalar.dl.ledger.proof.AssetProof;
import com.scalar.dl.ledger.util.JsonpSerDe;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.json.JsonObject;

/**
 * The result of contract execution. It contains the result of the contract execution along with a
 * list of {@link AssetProof}s from Ledger and Auditor.
 */
@Immutable
// non-final for mocking
public class ContractExecutionResult {
  private static final JsonpSerDe serde = new JsonpSerDe();
  private final JsonObject contractResultJson;
  private final String contractResult;
  private final String functionResult;
  private final ImmutableList<AssetProof> ledgerProofs;
  private final ImmutableList<AssetProof> auditorProofs;

  /**
   * Constructs a {@code ContractExecutionResult} with the specified results and list of {@link
   * AssetProof}s.
   *
   * @param contractResult a {@code String}
   * @param functionResult a {@code String}
   * @param ledgerProofs a list of {@link AssetProof}s from Ledger
   * @param auditorProofs a list of {@link AssetProof}s from Auditor
   */
  public ContractExecutionResult(
      @Nullable String contractResult,
      @Nullable String functionResult,
      @Nullable List<AssetProof> ledgerProofs,
      @Nullable List<AssetProof> auditorProofs) {
    this.contractResultJson = null;
    this.contractResult = contractResult;
    this.functionResult = functionResult;
    this.ledgerProofs =
        ledgerProofs == null ? ImmutableList.of() : ImmutableList.copyOf(ledgerProofs);
    this.auditorProofs =
        auditorProofs == null ? ImmutableList.of() : ImmutableList.copyOf(auditorProofs);
  }

  /**
   * Returns the result of contract execution.
   *
   * @return the result of contract execution
   * @deprecated This method will be removed in release 5.0.0
   */
  @Deprecated
  public Optional<JsonObject> getResult() {
    if (contractResultJson != null) {
      return Optional.of(contractResultJson);
    }
    return contractResult == null
        ? Optional.empty()
        : Optional.of(serde.deserialize(contractResult));
  }

  /**
   * Returns the result of contract execution.
   *
   * @return the result of contract execution
   */
  public Optional<String> getContractResult() {
    if (contractResultJson != null) {
      return Optional.of(contractResultJson.toString());
    }
    return Optional.ofNullable(contractResult);
  }

  /**
   * Returns the result of function execution.
   *
   * @return the result of function execution
   */
  public Optional<String> getFunctionResult() {
    return Optional.ofNullable(functionResult);
  }

  /**
   * Returns the list of {@link com.scalar.dl.ledger.asset.AssetProof}s from Ledger.
   *
   * @return the list of {@link com.scalar.dl.ledger.asset.AssetProof}s from Ledger
   * @deprecated This method will be removed in release 5.0.0
   */
  @Deprecated
  public List<com.scalar.dl.ledger.asset.AssetProof> getProofs() {
    return ledgerProofs.stream()
        .map(com.scalar.dl.ledger.asset.AssetProof::new)
        .collect(Collectors.toList());
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
    return Objects.hash(contractResult, functionResult, ledgerProofs, auditorProofs);
  }

  /**
   * Indicates whether some other object is "equal to" this object. The other object is considered
   * equal if it is the same instance or if:
   *
   * <ul>
   *   <li>it is also an {@code ContractExecutionResult} and
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
    if (!(o instanceof ContractExecutionResult)) {
      return false;
    }
    ContractExecutionResult other = (ContractExecutionResult) o;
    return this.contractResult.equals(other.contractResult)
        && this.functionResult.equals(other.functionResult)
        && this.ledgerProofs.equals(other.ledgerProofs)
        && this.auditorProofs.equals(other.auditorProofs);
  }
}
