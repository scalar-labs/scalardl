package com.scalar.dl.ledger.validation;

import com.scalar.dl.ledger.asset.AssetHasher;
import com.scalar.dl.ledger.contract.ContractMachine;
import com.scalar.dl.ledger.error.LedgerError;
import com.scalar.dl.ledger.exception.ValidationException;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.Arrays;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * A validator to determine whether the recomputed hash of an asset matches its stored hash. In the
 * case the hashes are not equal this may indicate tampering.
 */
@Immutable
public class HashValidator implements LedgerValidator {

  public HashValidator() {}

  @Override
  public StatusCode validate(
      Ledger<?> ledger, ContractMachine contract, @Nonnull String namespace, InternalAsset record) {
    byte[] hash =
        new AssetHasher.Builder()
            .id(record.id())
            .age(record.age())
            .input(record.input())
            .output(record.data())
            .contractId(record.contractId())
            .argument(record.argument())
            .signature(record.signature())
            .prevHash(record.prevHash())
            .build()
            .get();

    if (!Arrays.equals(hash, record.hash())) {
      throw new ValidationException(LedgerError.VALIDATION_FAILED_FOR_HASH);
    }
    return StatusCode.OK;
  }
}
