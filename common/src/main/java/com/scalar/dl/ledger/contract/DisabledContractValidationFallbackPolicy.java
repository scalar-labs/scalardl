package com.scalar.dl.ledger.contract;

import javax.annotation.concurrent.Immutable;

/** A {@link ContractValidationFallbackPolicy} that never allows the fallback validation. */
@Immutable
public class DisabledContractValidationFallbackPolicy implements ContractValidationFallbackPolicy {

  @Override
  public boolean isFallbackAllowed(String namespace) {
    return false;
  }
}
