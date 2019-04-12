package com.scalar.application.bankaccount.repository;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.scalar.client.config.ClientConfig;
import com.scalar.client.service.ClientModule;
import com.scalar.client.service.ClientService;
import com.scalar.rpc.ledger.ContractExecutionResponse;
import java.io.File;
import java.io.IOException;
import javax.inject.Singleton;
import javax.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
@Singleton
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
    Injector injector =
        Guice.createInjector(new ClientModule(new ClientConfig(new File(properties))));
    this.clientService = injector.getInstance(ClientService.class);
  }

  public ContractExecutionResponse create(JsonObject argument) {
    ContractExecutionResponse response = clientService.executeContract(CREATE_ACCOUNT_ID, argument);
    logResponse("create", response);
    return response;
  }

  public ContractExecutionResponse history(JsonObject argument) {
    ContractExecutionResponse response =
        clientService.executeContract(ACCOUNT_HISTORY_ID, argument);
    logResponse("history", response);
    return response;
  }

  public ContractExecutionResponse deposit(JsonObject argument) {
    ContractExecutionResponse response = clientService.executeContract(DEPOSIT_ID, argument);
    logResponse("deposit", response);
    return response;
  }

  public ContractExecutionResponse withdraw(JsonObject argument) {
    ContractExecutionResponse response = clientService.executeContract(WITHDRAW_ID, argument);
    logResponse("withdraw", response);
    return response;
  }

  public ContractExecutionResponse transfer(JsonObject argument) {
    ContractExecutionResponse response = clientService.executeContract(TRANSFER_ID, argument);
    logResponse("transfer", response);
    return response;
  }

  private void logResponse(String header, ContractExecutionResponse response) {
    logger.info(
        header
            + ": ("
            + response.getStatus()
            + ", "
            + response.getMessage()
            + ", "
            + response.getResult()
            + ")");
  }
}
