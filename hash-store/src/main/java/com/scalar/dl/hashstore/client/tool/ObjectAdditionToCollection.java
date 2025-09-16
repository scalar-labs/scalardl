package com.scalar.dl.hashstore.client.tool;

import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.hashstore.client.service.HashStoreClientService;
import java.util.List;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "add-to-collection", description = "Add objects to a collection.")
public class ObjectAdditionToCollection extends AbstractHashStoreCommand {

  @CommandLine.Option(
      names = {"--collection-id"},
      required = true,
      paramLabel = "COLLECTION_ID",
      description = "The ID of the collection.")
  private String collectionId;

  @CommandLine.Option(
      names = {"--object-ids"},
      required = true,
      paramLabel = "OBJECT_ID",
      description = "Object IDs to add to the collection.")
  private List<String> objectIds;

  @CommandLine.Option(
      names = {"--force"},
      description = "Skip validation for duplicate object IDs already in the collection.")
  private boolean force;

  public static void main(String[] args) {
    int exitCode = new CommandLine(new ObjectAdditionToCollection()).execute(args);
    System.exit(exitCode);
  }

  @Override
  protected Integer execute(HashStoreClientService service) throws ClientException {
    service.addToCollection(collectionId, objectIds, force);
    return 0;
  }
}
