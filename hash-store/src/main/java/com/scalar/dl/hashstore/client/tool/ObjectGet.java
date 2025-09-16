package com.scalar.dl.hashstore.client.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.tool.Common;
import com.scalar.dl.hashstore.client.service.HashStoreClientService;
import com.scalar.dl.ledger.model.ExecutionResult;
import com.scalar.dl.ledger.util.JacksonSerDe;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "get-object", description = "Get an object from the hash store.")
public class ObjectGet extends AbstractHashStoreCommand {

  @CommandLine.Option(
      names = {"--object-id"},
      required = true,
      paramLabel = "OBJECT_ID",
      description = "The ID of the object to retrieve.")
  private String objectId;

  public static void main(String[] args) {
    int exitCode = new CommandLine(new ObjectGet()).execute(args);
    System.exit(exitCode);
  }

  @Override
  protected Integer execute(HashStoreClientService service) throws ClientException {
    JacksonSerDe serde = new JacksonSerDe(new ObjectMapper());
    ExecutionResult result = service.getObject(objectId);
    if (result.getResult().isPresent()) {
      System.out.println("Result:");
      Common.printJson(serde.deserialize(result.getResult().get()));
    } else {
      System.out.println("Object not found.");
    }
    return 0;
  }
}
