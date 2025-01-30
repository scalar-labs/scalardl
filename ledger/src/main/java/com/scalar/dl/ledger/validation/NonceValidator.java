package com.scalar.dl.ledger.validation;

import com.scalar.dl.ledger.contract.ContractMachine;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import com.scalar.dl.ledger.statemachine.Ledger;
import com.scalar.dl.ledger.util.Argument;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.concurrent.Immutable;

/** A validator to determine whether a nonce is used more than once. */
@Immutable
public class NonceValidator implements LedgerValidator {
  private final Set<String> seenNonces;

  public NonceValidator() {
    seenNonces = new HashSet<>();
  }

  @Override
  public void initialize() {
    seenNonces.clear();
  }

  @Override
  public StatusCode validate(Ledger<?> ledger, ContractMachine contract, InternalAsset record) {
    String nonce = Argument.getNonce(record.argument());

    if (seenNonces.contains(nonce)) {
      LogHolder.LOGGER.error(
          "Validation failed: "
              + record.id()
              + " contains the nonce '"
              + nonce
              + "' more than once");
      return StatusCode.INVALID_NONCE;
    }

    seenNonces.add(nonce);
    return StatusCode.OK;
  }
}
