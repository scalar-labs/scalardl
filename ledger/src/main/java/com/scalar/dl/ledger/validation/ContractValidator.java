package com.scalar.dl.ledger.validation;

import com.scalar.dl.ledger.contract.ContractEntry;
import com.scalar.dl.ledger.contract.ContractMachine;
import com.scalar.dl.ledger.crypto.ClientIdentityKey;
import com.scalar.dl.ledger.crypto.ClientKeyValidator;
import com.scalar.dl.ledger.crypto.SignatureValidator;
import com.scalar.dl.ledger.error.LedgerError;
import com.scalar.dl.ledger.exception.ValidationException;
import com.scalar.dl.ledger.model.ContractExecutionRequest;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import com.scalar.dl.ledger.statemachine.Ledger;

/**
 * A validator which will determine whether the signature of a {@link ContractExecutionRequest} is
 * valid or not.
 */
public class ContractValidator implements LedgerValidator {
  private final ClientKeyValidator clientKeyValidator;

  public ContractValidator(ClientKeyValidator clientKeyValidator) {
    this.clientKeyValidator = clientKeyValidator;
  }

  @Override
  public StatusCode validate(Ledger<?> ledger, ContractMachine contract, InternalAsset record) {
    ClientIdentityKey clientIdentityKey = contract.getClientIdentityKey();
    SignatureValidator validator =
        clientKeyValidator.getValidator(
            clientIdentityKey.getEntityId(), clientIdentityKey.getKeyVersion());
    byte[] serialized =
        ContractExecutionRequest.serialize(
            ContractEntry.Key.deserialize(record.contractId()).getId(),
            record.argument(),
            clientIdentityKey.getEntityId(),
            clientIdentityKey.getKeyVersion());
    if (validator.validate(serialized, record.signature())) {
      return StatusCode.OK;
    }
    throw new ValidationException(LedgerError.VALIDATION_FAILED_FOR_CONTRACT);
  }
}
