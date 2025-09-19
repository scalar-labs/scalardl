package com.scalar.dl.hashstore.client.tool;

import static com.scalar.dl.genericcontracts.object.Constants.OBJECT_ID;
import static com.scalar.dl.genericcontracts.object.Constants.OPTIONS;
import static com.scalar.dl.genericcontracts.object.Constants.OPTION_ALL;
import static com.scalar.dl.genericcontracts.object.Constants.OPTION_VERBOSE;
import static com.scalar.dl.genericcontracts.object.Constants.VERSIONS;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.tool.Common;
import com.scalar.dl.hashstore.client.service.HashStoreClientService;
import com.scalar.dl.hashstore.client.util.HashStoreClientUtils;
import com.scalar.dl.ledger.model.ExecutionResult;
import com.scalar.dl.ledger.util.JacksonSerDe;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "compare-object-versions", description = "Compare object versions.")
public class ObjectVersionsComparison extends AbstractHashStoreCommand {

  @CommandLine.Option(
      names = {"--object-id"},
      required = true,
      paramLabel = "OBJECT_ID",
      description = "The ID of the object to compare versions.")
  private String objectId;

  @CommandLine.Option(
      names = {"--versions"},
      required = true,
      paramLabel = "VERSIONS_JSON",
      description = "Object versions to compare as JSON array.")
  private String versions;

  @CommandLine.Option(
      names = {"--all"},
      description = "Compare all versions including stored versions in the ledger.")
  private boolean all;

  @CommandLine.Option(
      names = {"--verbose"},
      description = "Show detailed validation information.")
  private boolean verbose;

  @Override
  protected Integer execute(HashStoreClientService service) throws ClientException {
    JacksonSerDe serde = new JacksonSerDe(new ObjectMapper());

    // Build arguments with objectId and versions
    ObjectNode arguments = HashStoreClientUtils.createObjectNode().put(OBJECT_ID, objectId);

    // Parse and add versions
    JsonNode versionsJson = HashStoreClientUtils.convertToJsonNode(versions);
    arguments.set(VERSIONS, versionsJson);

    // Add options if specified
    if (all || verbose) {
      ObjectNode options = HashStoreClientUtils.createObjectNode();
      if (all) {
        options.put(OPTION_ALL, true);
      }
      if (verbose) {
        options.put(OPTION_VERBOSE, true);
      }
      arguments.set(OPTIONS, options);
    }

    ExecutionResult result = service.compareObjectVersions(arguments);
    System.out.println("Result:");
    if (result.getResult().isPresent()) {
      Common.printJson(serde.deserialize(result.getResult().get()));
    }
    return 0;
  }
}
