package com.scalar.dl.ledger.validation;

import com.scalar.dl.ledger.contract.ContractMachine;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import com.scalar.dl.ledger.statemachine.Ledger;
import javax.annotation.concurrent.Immutable;

/**
 * A validator to determine whether the recomputed output of a contract execution is consistent with
 * its stored result.
 */
@Immutable
public class OutputValidator implements LedgerValidator {

  @Override
  public StatusCode validate(Ledger<?> ledger, ContractMachine contract, InternalAsset record) {
    LedgerTracerBase<?> tracer = (LedgerTracerBase<?>) ledger;
    String recomputed = tracer.getOutput(record.id());
    String stored = record.data();
    if (!recomputed.equals(stored)) {
      logError("validation failed for output", recomputed, stored);
      return StatusCode.INVALID_OUTPUT;
    }
    return StatusCode.OK;
  }
}
