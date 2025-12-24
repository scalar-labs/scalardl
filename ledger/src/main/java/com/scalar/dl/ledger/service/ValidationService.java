package com.scalar.dl.ledger.service;

import com.google.common.annotations.VisibleForTesting;
import com.scalar.dl.ledger.contract.ContractEntry;
import com.scalar.dl.ledger.contract.ContractMachine;
import com.scalar.dl.ledger.contract.ContractManager;
import com.scalar.dl.ledger.crypto.ClientKeyValidator;
import com.scalar.dl.ledger.database.AssetProofComposer;
import com.scalar.dl.ledger.database.TransactionManager;
import com.scalar.dl.ledger.model.LedgerValidationRequest;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.ledger.statemachine.Context;
import com.scalar.dl.ledger.statemachine.DeserializationType;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import com.scalar.dl.ledger.util.Argument;
import com.scalar.dl.ledger.validation.ContractValidator;
import com.scalar.dl.ledger.validation.HashValidator;
import com.scalar.dl.ledger.validation.LedgerTracerBase;
import com.scalar.dl.ledger.validation.LedgerTracerManager;
import com.scalar.dl.ledger.validation.LedgerValidator;
import com.scalar.dl.ledger.validation.NonceValidator;
import com.scalar.dl.ledger.validation.OutputValidator;
import com.scalar.dl.ledger.validation.PrevHashValidator;
import com.scalar.dl.ledger.validation.TransactionScannableLedgerTracerManager;
import java.util.ArrayList;
import java.util.List;

public abstract class ValidationService {
  protected final ClientKeyValidator clientKeyValidator;
  protected final ContractManager contractManager;
  private final TransactionManager transactionManager;
  protected final AssetProofComposer proofComposer;

  protected ValidationService(
      ClientKeyValidator clientKeyValidator,
      ContractManager contractManager,
      TransactionManager transactionManager,
      AssetProofComposer proofComposer) {
    this.clientKeyValidator = clientKeyValidator;
    this.contractManager = contractManager;
    this.transactionManager = transactionManager;
    this.proofComposer = proofComposer;
  }

  public abstract LedgerValidationResult validate(LedgerValidationRequest request);

  protected List<LedgerValidator> validateInit() {
    List<LedgerValidator> validators = new ArrayList<>();
    validators.add(new ContractValidator(clientKeyValidator));
    validators.add(new OutputValidator());
    validators.add(new PrevHashValidator());
    validators.add(new HashValidator());
    validators.add(new NonceValidator());
    validators.forEach(LedgerValidator::initialize);

    return validators;
  }

  protected StatusCode validateEach(
      Context context, List<LedgerValidator> validators, String namespace, InternalAsset asset) {
    ContractEntry entry = contractManager.get(ContractEntry.Key.deserialize(asset.contractId()));
    ContractMachine contract = contractManager.getInstance(entry);
    LedgerTracerBase<?> tracer = getLedgerTracerBase(context, contract.getDeserializationType());
    tracer.setInput(asset.input());
    String contractArgument = Argument.getContractArgument(asset.argument());
    contract.invoke(tracer, contractArgument, entry.getProperties().orElse(null));

    for (LedgerValidator validator : validators) {
      StatusCode code = validator.validate(tracer, contract, namespace, asset);
      if (code != StatusCode.OK) {
        return code;
      }
    }
    return StatusCode.OK;
  }

  @VisibleForTesting
  LedgerTracerBase<?> getLedgerTracerBase(Context context, DeserializationType type) {
    LedgerTracerManager tracerManager =
        new TransactionScannableLedgerTracerManager(transactionManager);
    return tracerManager.start(context, type);
  }
}
