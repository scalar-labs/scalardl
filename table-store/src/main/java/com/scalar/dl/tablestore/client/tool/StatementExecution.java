package com.scalar.dl.tablestore.client.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.GatewayClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.tool.Common;
import com.scalar.dl.client.tool.CommonOptions;
import com.scalar.dl.ledger.model.ExecutionResult;
import com.scalar.dl.ledger.util.JacksonSerDe;
import com.scalar.dl.tablestore.client.service.TableStoreClientService;
import com.scalar.dl.tablestore.client.service.TableStoreClientServiceFactory;
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

  @Override
  public Integer call() throws Exception {
    return call(new TableStoreClientServiceFactory());
  }

  @VisibleForTesting
  Integer call(TableStoreClientServiceFactory factory) throws Exception {
    TableStoreClientService service =
        useGateway
            ? factory.create(new GatewayClientConfig(new File(properties)), false)
            : factory.create(new ClientConfig(new File(properties)), false);
    return call(factory, service);
  }

  @VisibleForTesting
  Integer call(TableStoreClientServiceFactory factory, TableStoreClientService service) {
    JacksonSerDe serde = new JacksonSerDe(new ObjectMapper());
    try {
      ExecutionResult result = service.executeStatement(statement);
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
