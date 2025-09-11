package com.scalar.dl.hashstore.client.tool;

import static com.scalar.dl.genericcontracts.object.Constants.HASH_VALUE;
import static com.scalar.dl.genericcontracts.object.Constants.METADATA;
import static com.scalar.dl.genericcontracts.object.Constants.OBJECT_ID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.GatewayClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.tool.Common;
import com.scalar.dl.client.tool.CommonOptions;
import com.scalar.dl.hashstore.client.service.ClientService;
import com.scalar.dl.hashstore.client.service.ClientServiceFactory;
import com.scalar.dl.hashstore.client.util.HashStoreClientUtils;
import java.io.File;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "put-object", description = "Put an object to the hash store.")
public class ObjectPut extends CommonOptions implements Callable<Integer> {

  @CommandLine.Option(
      names = {"--object-id"},
      required = true,
      paramLabel = "OBJECT_ID",
      description = "The ID of the object to store.")
  private String objectId;

  @CommandLine.Option(
      names = {"--hash"},
      required = true,
      paramLabel = "HASH",
      description = "The hash value of the object.")
  private String hash;

  @CommandLine.Option(
      names = {"--metadata"},
      paramLabel = "METADATA_JSON",
      description = "Optional metadata as JSON string.")
  private String metadata;

  @CommandLine.Option(
      names = {"--put-to-mutable"},
      paramLabel = "PUT_TO_MUTABLE_JSON",
      description = "Optional Put operation for mutable database as JSON string.")
  private String putToMutable;

  public static void main(String[] args) {
    int exitCode = new CommandLine(new ObjectPut()).execute(args);
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
    try {
      // Build basic arguments with objectId and hash
      ObjectNode arguments =
          HashStoreClientUtils.createObjectNode().put(OBJECT_ID, objectId).put(HASH_VALUE, hash);

      // Add metadata if provided
      if (metadata != null) {
        JsonNode metadataJson = HashStoreClientUtils.convertToJsonNode(metadata);
        arguments.set(METADATA, metadataJson);
      }

      if (putToMutable != null) {
        // Use putObject with Put operation for the mutable database
        JsonNode putToMutableJson = HashStoreClientUtils.convertToJsonNode(putToMutable);
        service.putObject(arguments, putToMutableJson);
      } else {
        // Use simple putObject
        service.putObject(arguments);
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
