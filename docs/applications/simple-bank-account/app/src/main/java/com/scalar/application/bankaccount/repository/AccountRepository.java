package com.scalar.application.bankaccount.repository;

import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.client.service.ClientServiceFactory;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import java.io.File;
import java.io.IOException;
import javax.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRepository {
  private static final Logger logger = LogManager.getLogger(AccountRepository.class);
  private static final String ACCOUNT_HISTORY_ID = "account-history";
  private static final String CREATE_ACCOUNT_ID = "create-account";
  private static final String DEPOSIT_ID = "deposit";
  private static final String TRANSFER_ID = "transfer";
  private static final String WITHDRAW_ID = "withdraw";
  private final ClientService clientService;

  public AccountRepository(@Value("${client.properties.path}") String properties)
      throws IOException {
    ClientServiceFactory clientServiceFactory = new ClientServiceFactory();
    this.clientService = clientServiceFactory.create(new ClientConfig(new File(properties)));
  }

  public ContractExecutionResult create(JsonObject argument) {
    ContractExecutionResult result = clientService.executeContract(CREATE_ACCOUNT_ID, argument);
    logResponse("create", result);
    return result;
  }

  public ContractExecutionResult history(JsonObject argument) {
    ContractExecutionResult result = clientService.executeContract(ACCOUNT_HISTORY_ID, argument);
    logResponse("history", result);
    return result;
  }

  public ContractExecutionResult deposit(JsonObject argument) {
    ContractExecutionResult result = clientService.executeContract(DEPOSIT_ID, argument);
    logResponse("deposit", result);
    return result;
  }

  public ContractExecutionResult withdraw(JsonObject argument) {
    ContractExecutionResult result = clientService.executeContract(WITHDRAW_ID, argument);
    logResponse("withdraw", result);
    return result;
  }

  public ContractExecutionResult transfer(JsonObject argument) {
    ContractExecutionResult result = clientService.executeContract(TRANSFER_ID, argument);
    logResponse("transfer", result);
    return result;
  }

  private void logResponse(String header, ContractExecutionResult result) {
    logger.info(
        header
            + ": ("
            + (result.getContractResult().isPresent() ? result.getContractResult().get() : "{}")
            + ")");
  }
}
