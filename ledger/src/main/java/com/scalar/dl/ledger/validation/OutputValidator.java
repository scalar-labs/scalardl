package com.scalar.dl.ledger.validation;

import com.scalar.dl.ledger.contract.ContractMachine;
import com.scalar.dl.ledger.error.LedgerError;
import com.scalar.dl.ledger.exception.ValidationException;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.statemachine.AssetKey;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * A validator to determine whether the recomputed output of a contract execution is consistent with
 * its stored result.
 */
@Immutable
public class OutputValidator implements LedgerValidator {

  @Override
  public StatusCode validate(
      LedgerTracerBase<?> tracer,
      ContractMachine contract,
      @Nonnull String namespace,
      InternalAsset record) {
    String recomputed = tracer.getOutput(AssetKey.of(namespace, record.id()));
    String stored = record.data();
    if (!recomputed.equals(stored)) {
      throw new ValidationException(LedgerError.VALIDATION_FAILED_FOR_OUTPUT, recomputed, stored);
    }
    return StatusCode.OK;
  }
}
