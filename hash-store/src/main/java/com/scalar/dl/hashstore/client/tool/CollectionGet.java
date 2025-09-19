package com.scalar.dl.hashstore.client.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.tool.Common;
import com.scalar.dl.hashstore.client.service.HashStoreClientService;
import com.scalar.dl.ledger.model.ExecutionResult;
import com.scalar.dl.ledger.util.JacksonSerDe;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "get-collection", description = "Get a collection from the hash store.")
public class CollectionGet extends AbstractHashStoreCommand {

  @CommandLine.Option(
      names = {"--collection-id"},
      required = true,
      paramLabel = "COLLECTION_ID",
      description = "The ID of the collection to retrieve.")
  private String collectionId;

  @Override
  protected Integer execute(HashStoreClientService service) throws ClientException {
    JacksonSerDe serde = new JacksonSerDe(new ObjectMapper());
    ExecutionResult result = service.getCollection(collectionId);
    if (result.getResult().isPresent()) {
      System.out.println("Result:");
      Common.printJson(serde.deserialize(result.getResult().get()));
    } else {
      System.out.println("Collection not found.");
    }
    return 0;
  }
}
