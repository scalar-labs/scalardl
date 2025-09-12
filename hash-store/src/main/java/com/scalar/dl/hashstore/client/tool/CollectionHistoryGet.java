package com.scalar.dl.hashstore.client.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.tool.Common;
import com.scalar.dl.hashstore.client.service.HashStoreClientService;
import com.scalar.dl.ledger.model.ExecutionResult;
import com.scalar.dl.ledger.util.JacksonSerDe;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "get-collection-history", description = "Get the history of a collection.")
public class CollectionHistoryGet extends AbstractHashStoreCommand {

  @CommandLine.Option(
      names = {"--collection-id"},
      required = true,
      paramLabel = "COLLECTION_ID",
      description = "The ID of the collection.")
  private String collectionId;

  @CommandLine.Option(
      names = {"--limit"},
      paramLabel = "LIMIT",
      description = "Maximum number of recent history entries to return.")
  private Integer limit;

  public static void main(String[] args) {
    int exitCode = new CommandLine(new CollectionHistoryGet()).execute(args);
    System.exit(exitCode);
  }

  @Override
  protected Integer execute(HashStoreClientService service) throws ClientException {
    JacksonSerDe serde = new JacksonSerDe(new ObjectMapper());
    ExecutionResult result;
    if (limit != null) {
      result = service.getCollectionHistory(collectionId, limit);
    } else {
      result = service.getCollectionHistory(collectionId);
    }

    if (result.getResult().isPresent()) {
      System.out.println("Result:");
      Common.printJson(serde.deserialize(result.getResult().get()));
    }
    return 0;
  }
}
