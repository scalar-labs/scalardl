package com.scalar.dl.ledger.contract;

import com.google.inject.Inject;
import com.scalar.dl.ledger.config.LedgerConfig;
import com.scalar.dl.ledger.database.Transaction;
import com.scalar.dl.ledger.database.TransactionManager;
import com.scalar.dl.ledger.database.TransactionState;
import com.scalar.dl.ledger.exception.ConflictException;
import com.scalar.dl.ledger.function.FunctionMachine;
import com.scalar.dl.ledger.function.FunctionManager;
import com.scalar.dl.ledger.model.ContractExecutionRequest;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.scalar.dl.ledger.proof.AssetProof;
import com.scalar.dl.ledger.util.Argument;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.annotation.concurrent.Immutable;

@Immutable
public class ContractExecutor {
  private final LedgerConfig config;
  private final ContractManager contractManager;
  private final FunctionManager functionManager;
  private final TransactionManager transactionManager;

  @Inject
  public ContractExecutor(
      LedgerConfig config,
      ContractManager contractManager,
      FunctionManager functionManager,
      TransactionManager transactionManager) {
    this.config = config;
    this.contractManager = contractManager;
    this.functionManager = functionManager;
    this.transactionManager = transactionManager;
  }

  public ContractExecutionResult execute(ContractExecutionRequest request) {
    ContractEntry.Key key = ContractEntry.Key.from(request);
    ContractEntry entry = contractManager.get(key);
    ContractMachine contract = contractManager.getInstance(entry);
    List<FunctionMachine> functions = getFunctions(request.getFunctionIds());
    Optional<String> properties = entry.getProperties();
    String contractArgument = Argument.getContractArgument(request.getContractArgument());

    Transaction transaction = transactionManager.startWith(request);
    try {
      // Execute the contract
      String contractResult =
          contract.invoke(
              transaction.getLedger(contract.getDeserializationType()),
              contractArgument,
              properties.orElse(null));

      // Execute the functions
      String functionResult = null;
      if (transaction.getDatabase() != null) {
        AtomicReference<String> result = new AtomicReference<>(null);
        functions.forEach(
            f -> {
              f.setContractContext(contract.getContext());
              result.set(
                  f.invoke(
                      transaction.getDatabase(),
                      request.getFunctionArgument().orElse(null),
                      contractArgument,
                      properties.orElse(null)));
            });
        functionResult = result.get();
      }

      List<AssetProof> proofs = transaction.commit();
      return new ContractExecutionResult(contractResult, functionResult, proofs, null);
    } catch (ConflictException e) {
      transaction.abort();
      transactionManager.recover(e.getKeys());
      throw e;
    } catch (Exception e) {
      transaction.abort();
      throw e;
    }
  }

  public TransactionState getState(String transactionId) {
    return transactionManager.getState(transactionId);
  }

  public TransactionState abort(String nonce) {
    return transactionManager.abort(nonce);
  }

  private List<FunctionMachine> getFunctions(List<String> functionIds) {
    if (!config.isFunctionEnabled()) {
      return Collections.emptyList();
    }

    return functionIds.stream().map(functionManager::getInstance).collect(Collectors.toList());
  }
}
