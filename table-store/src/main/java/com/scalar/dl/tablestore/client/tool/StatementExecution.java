package com.scalar.dl.tablestore.client.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.GatewayClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.tool.Common;
import com.scalar.dl.client.tool.CommonOptions;
import com.scalar.dl.ledger.util.JacksonSerDe;
import com.scalar.dl.tablestore.client.model.StatementExecutionResult;
import com.scalar.dl.tablestore.client.service.ClientService;
import com.scalar.dl.tablestore.client.service.ClientServiceFactory;
import java.io.File;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "execute-statement", description = "Execute a specified statement.")
public class StatementExecution extends CommonOptions implements Callable<Integer> {

  @CommandLine.Option(
      names = {"--statement"},
      required = true,
      paramLabel = "STATEMENT",
      description = "A statement to interact with the table store.")
  private String statement;

  public static void main(String[] args) {
    int exitCode = new CommandLine(new StatementExecution()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() throws Exception {
    return call(new ClientServiceFactory());
  }

  @VisibleForTesting
  Integer call(ClientServiceFactory factory) throws Exception {
    ClientService service =
        useGateway
            ? factory.create(new GatewayClientConfig(new File(properties)), false)
            : factory.create(new ClientConfig(new File(properties)), false);
    return call(factory, service);
  }

  @VisibleForTesting
  Integer call(ClientServiceFactory factory, ClientService service) {
    JacksonSerDe serde = new JacksonSerDe(new ObjectMapper());
    try {
      StatementExecutionResult result = service.executeStatement(statement);
      result
          .getResult()
          .ifPresent(
              r -> {
                System.out.println("Result:");
                Common.printJson(serde.deserialize(r));
              });
      return 0;
    } catch (ClientException e) {
      Common.printError(e);
      printStackTrace(e);
      return 1;
    } finally {
      factory.close();
    }
  }
}
