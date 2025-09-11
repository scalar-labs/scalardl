package com.scalar.dl.hashstore.client.tool;

import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.hashstore.client.service.ClientService;
import java.util.List;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "remove-from-collection", description = "Remove objects from a collection.")
public class ObjectRemovalFromCollection extends AbstractHashStoreCommand {

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
      description = "Object IDs to remove from the collection.")
  private List<String> objectIds;

  @CommandLine.Option(
      names = {"--force"},
      description = "Skip validation for object IDs that are not in the collection.")
  private boolean force;

  public static void main(String[] args) {
    int exitCode = new CommandLine(new ObjectRemovalFromCollection()).execute(args);
    System.exit(exitCode);
  }

  @Override
  protected Integer execute(ClientService service) throws ClientException {
    if (force) {
      service.removeFromCollection(collectionId, objectIds, true);
    } else {
      service.removeFromCollection(collectionId, objectIds);
    }
    return 0;
  }
}
