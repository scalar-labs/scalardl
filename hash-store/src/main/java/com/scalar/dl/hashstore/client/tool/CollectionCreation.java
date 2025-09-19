package com.scalar.dl.hashstore.client.tool;

import com.scalar.dl.hashstore.client.service.HashStoreClientService;
import java.util.ArrayList;
import java.util.List;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "create-collection", description = "Create a new collection.")
public class CollectionCreation extends AbstractHashStoreCommand {

  @CommandLine.Option(
      names = {"--collection-id"},
      required = true,
      paramLabel = "COLLECTION_ID",
      description = "The ID of the collection to create.")
  private String collectionId;

  @CommandLine.Option(
      names = {"--object-ids"},
      paramLabel = "OBJECT_ID",
      description = "Object IDs to include in the collection.")
  private List<String> objectIds = new ArrayList<>();

  @Override
  protected Integer execute(HashStoreClientService service) {
    service.createCollection(collectionId, objectIds);
    return 0;
  }
}
