package com.scalar.dl.ledger.service;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.inject.Inject;
import com.scalar.dl.ledger.config.LedgerConfig;
import com.scalar.dl.ledger.contract.ContractEntry;
import com.scalar.dl.ledger.contract.ContractExecutor;
import com.scalar.dl.ledger.crypto.AuditorKeyValidator;
import com.scalar.dl.ledger.crypto.ClientKeyValidator;
import com.scalar.dl.ledger.crypto.SecretEntry;
import com.scalar.dl.ledger.crypto.SignatureValidator;
import com.scalar.dl.ledger.error.LedgerError;
import com.scalar.dl.ledger.exception.DatabaseException;
import com.scalar.dl.ledger.function.FunctionEntry;
import com.scalar.dl.ledger.function.FunctionManager;
import com.scalar.dl.ledger.model.CertificateRegistrationRequest;
import com.scalar.dl.ledger.model.ContractExecutionRequest;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.scalar.dl.ledger.model.ContractRegistrationRequest;
import com.scalar.dl.ledger.model.ContractsListingRequest;
import com.scalar.dl.ledger.model.ExecutionAbortRequest;
import com.scalar.dl.ledger.model.ExecutionAbortResult;
import com.scalar.dl.ledger.model.FunctionRegistrationRequest;
import com.scalar.dl.ledger.model.NamespaceCreationRequest;
import com.scalar.dl.ledger.model.StateRetrievalRequest;
import com.scalar.dl.ledger.model.StateRetrievalResult;
import java.util.List;
import javax.annotation.concurrent.Immutable;

@Immutable
public class LedgerService {
  private final BaseService base;
  private final LedgerConfig config;
  private final ClientKeyValidator clientKeyValidator;
  private final AuditorKeyValidator auditorKeyValidator;
  private final ContractExecutor executor;
  private final FunctionManager functionManager;

  @Inject
  public LedgerService(
      BaseService base,
      LedgerConfig config,
      ClientKeyValidator clientKeyValidator,
      AuditorKeyValidator auditorKeyValidator,
      ContractExecutor executor,
      FunctionManager functionManager) {
    this.base = base;
    this.config = config;
    this.clientKeyValidator = clientKeyValidator;
    this.auditorKeyValidator = auditorKeyValidator;
    this.executor = executor;
    this.functionManager = functionManager;
  }

  public void register(CertificateRegistrationRequest request) {
    base.register(request);
  }

  public void register(SecretEntry entry) {
    base.register(entry);
  }

  public void register(FunctionRegistrationRequest request) {
    functionManager.register(FunctionEntry.from(request));
  }

  public void register(ContractRegistrationRequest request) {
    if (!config.getExecutableContractNames().isEmpty()
        && !config.getExecutableContractNames().contains(request.getContractBinaryName())) {
      throw new DatabaseException(LedgerError.CONTRACT_IS_NOT_ALLOWED_TO_BE_EXECUTED);
    }
    base.register(request);
  }

  public List<ContractEntry> list(ContractsListingRequest request) {
    return base.list(request);
  }

  public ContractExecutionResult execute(ContractExecutionRequest request) {
    SignatureValidator validator =
        clientKeyValidator.getValidator(request.getEntityId(), request.getKeyVersion());
    request.validateWith(validator);

    if (config.isAuditorEnabled()) {
      checkArgument(
          request.getAuditorSignature() != null,
          LedgerError.AUDITOR_SIGNATURE_REQUIRED.buildMessage());
      validateSignatureFromAuditor(request);
    } else { // Auditor is disabled
      checkArgument(
          request.getAuditorSignature() == null,
          LedgerError.AUDITOR_NOT_CONFIGURED.buildMessage(LedgerConfig.AUDITOR_ENABLED));
    }

    return executor.execute(request);
  }

  public StateRetrievalResult retrieve(StateRetrievalRequest request) {
    return new StateRetrievalResult(executor.getState(request.getTransactionId()));
  }

  public ExecutionAbortResult abort(ExecutionAbortRequest request) {
    SignatureValidator validator =
        clientKeyValidator.getValidator(request.getEntityId(), request.getKeyVersion());
    request.validateWith(validator);

    return new ExecutionAbortResult(executor.abort(request.getNonce()));
  }

  public void create(NamespaceCreationRequest request) {
    base.create(request);
  }

  private void validateSignatureFromAuditor(ContractExecutionRequest request) {
    SignatureValidator validator = auditorKeyValidator.getValidator();
    request.validateAuditorSignatureWith(validator);
  }
}
