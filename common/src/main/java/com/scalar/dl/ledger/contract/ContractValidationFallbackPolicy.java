package com.scalar.dl.ledger.contract;

import com.google.inject.ImplementedBy;

/**
 * A policy to decide whether a registered contract can be validated as the one signed for the
 * default namespace when the validation for its namespace fails. See {@link ContractManager} for
 * the details of the fallback validation.
 */
@ImplementedBy(DisabledContractValidationFallbackPolicy.class)
public interface ContractValidationFallbackPolicy {

  /**
   * Returns whether the fallback validation is allowed in the specified namespace.
   *
   * @param namespace a non-default namespace
   * @return true if the fallback validation is allowed in the namespace
   */
  boolean isFallbackAllowed(String namespace);
}
