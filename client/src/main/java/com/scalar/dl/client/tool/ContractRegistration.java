package com.scalar.dl.client.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.client.service.ClientServiceFactory;
import com.scalar.dl.ledger.util.JacksonSerDe;
import java.io.File;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "register-contract", description = "Register a specified contract.")
public class ContractRegistration implements Callable<Integer> {

  @CommandLine.Option(
      names = {"--properties", "--config"},
      required = true,
      paramLabel = "PROPERTIES_FILE",
      description = "A configuration file in properties format.")
  private String properties;

  @CommandLine.Option(
      names = {"--contract-id"},
      required = true,
      paramLabel = "CONTRACT_ID",
      description = "An ID of a contract to register.")
  private String contractId;

  @CommandLine.Option(
      names = {"--contract-binary-name"},
      required = true,
      paramLabel = "CONTRACT_BINARY_NAME",
      description = "A binary name of a contract to register.")
  private String contractBinaryName;

  @CommandLine.Option(
      names = {"--contract-class-file"},
      required = true,
      paramLabel = "CONTRACT_CLASS_FILE",
      description = "A contract class file to register.")
  private String contractClassFile;

  @CommandLine.Option(
      names = {"--contract-properties"},
      required = false,
      paramLabel = "CONTRACT_PROPERTIES",
      description = "A contract properties in a serialized format.")
  private String contractProperties;

  @CommandLine.Option(
      names = {"--deserialization-format"},
      required = false,
      paramLabel = "DESERIALIZATION_FORMAT",
      description =
          "A deserialization format for a contract properties. "
              + "Valid values: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})")
  private DeserializationFormat deserializationFormat = DeserializationFormat.JSON;

  @CommandLine.Option(
      names = {"-h", "--help"},
      usageHelp = true,
      description = "display the help message.")
  boolean helpRequested;

  public static void main(String[] args) {
    int exitCode = new CommandLine(new ContractRegistration()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() throws Exception {
    ClientServiceFactory factory = new ClientServiceFactory();
    ClientService service = factory.create(new ClientConfig(new File(properties)));
    JacksonSerDe serde = new JacksonSerDe(new ObjectMapper());

    try {
      if (deserializationFormat == DeserializationFormat.JSON) {
        JsonNode jsonContractProperties = null;
        if (contractProperties != null) {
          jsonContractProperties = serde.deserialize(contractProperties);
        }
        service.registerContract(
            contractId, contractBinaryName, contractClassFile, jsonContractProperties);
      } else if (deserializationFormat == DeserializationFormat.STRING) {
        service.registerContract(
            contractId, contractBinaryName, contractClassFile, contractProperties);
      }
      Common.printOutput(null);
      return 0;
    } catch (ClientException e) {
      Common.printError(e);
      return 1;
    } finally {
      factory.close();
    }
  }
}
