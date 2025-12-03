package com.scalar.dl.tablestore.client.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.tool.Common;
import com.scalar.dl.ledger.model.ExecutionResult;
import com.scalar.dl.ledger.util.JacksonSerDe;
import com.scalar.dl.tablestore.client.service.TableStoreClientService;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "execute-statement", description = "Execute a specified statement.")
public class StatementExecution extends AbstractTableStoreCommand {

  @CommandLine.Option(
      names = {"--statement"},
      required = true,
      paramLabel = "STATEMENT",
      description = "A statement to interact with the table store.")
  private String statement;

  @Override
  protected Integer execute(TableStoreClientService service) throws ClientException {
    JacksonSerDe serde = new JacksonSerDe(new ObjectMapper());
    ExecutionResult result = service.executeStatement(statement);
    result
        .getResult()
        .ifPresent(
            r -> {
              System.out.println("Result:");
              Common.printJson(serde.deserialize(r));
            });
    return 0;
  }
}
