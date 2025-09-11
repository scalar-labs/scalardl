package com.scalar.dl.hashstore.client.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.GatewayClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.tool.Common;
import com.scalar.dl.client.tool.CommonOptions;
import com.scalar.dl.hashstore.client.service.ClientService;
import com.scalar.dl.hashstore.client.service.ClientServiceFactory;
import com.scalar.dl.ledger.model.ExecutionResult;
import com.scalar.dl.ledger.util.JacksonSerDe;
import java.io.File;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "get-collection-history", description = "Get the history of a collection.")
public class CollectionHistoryGet extends CommonOptions implements Callable<Integer> {

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
    } catch (ClientException e) {
      Common.printError(e);
      printStackTrace(e);
      return 1;
    } finally {
      factory.close();
    }
  }
}
