package com.scalar.dl.ledger.validation;

import com.google.common.annotations.VisibleForTesting;
import com.scalar.dl.ledger.contract.ContractMachine;
import com.scalar.dl.ledger.error.LedgerError;
import com.scalar.dl.ledger.exception.ValidationException;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import com.scalar.dl.ledger.statemachine.Ledger;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A validator to determine whether prevHash of the current asset is equal to hash of the previous
 * asset (same asset id but with age - 1)
 */
@NotThreadSafe
public class PrevHashValidator implements LedgerValidator {
  private byte[] prevHash;

  @Override
  public void initialize() {
    prevHash = null;
  }

  @VisibleForTesting
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void initialize(byte[] prevHash) {
    this.prevHash = prevHash;
  }

  @Override
  public StatusCode validate(
      Ledger<?> ledger, ContractMachine contract, @Nonnull String namespace, InternalAsset record) {
    boolean isValid = validate(record);
    prevHash = record.hash();
    if (isValid) {
      return StatusCode.OK;
    }
    return StatusCode.INVALID_PREV_HASH;
  }

  private boolean validate(InternalAsset record) {
    if (prevHash != null && !Arrays.equals(prevHash, record.prevHash())) {
      throw new ValidationException(
          LedgerError.VALIDATION_FAILED_FOR_PREV_HASH, prevHash, record.prevHash());
    }
    return true;
  }
}
