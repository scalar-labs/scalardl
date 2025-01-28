package com.scalar.dl.ledger.validation;

import com.scalar.dl.ledger.contract.ContractMachine;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import com.scalar.dl.ledger.statemachine.Ledger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstraction for validators. Validators will be used to determine whether various properties of
 * {@link InternalAsset}s are valid.
 */
public interface LedgerValidator {

  // for stateful validators
  default void initialize() {
    return;
  }

  StatusCode validate(Ledger<?> ledger, ContractMachine contract, InternalAsset record);

  default void logError(String message, Object recomputed, Object stored) {
    LogHolder.LOGGER.error(message);
    LogHolder.LOGGER.error("recomputed:" + recomputed);
    LogHolder.LOGGER.error("stored: " + stored);
  }
}

final class LogHolder {
  static final Logger LOGGER = LoggerFactory.getLogger(LedgerValidator.class);
}
