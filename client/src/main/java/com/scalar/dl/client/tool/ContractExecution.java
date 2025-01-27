package com.scalar.dl.client.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.client.service.ClientServiceFactory;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.scalar.dl.ledger.util.JacksonSerDe;
import java.io.File;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "execute-contract", description = "Execute a specified contract.")
public class ContractExecution implements Callable<Integer> {

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
      description = "An ID of a contract to execute.")
  private String contractId;

  @CommandLine.Option(
      names = {"--contract-argument"},
      required = true,
      paramLabel = "CONTRACT_ARGUMENT",
      description = "An argument for a contract to execute in a serialized format.")
  private String contractArgument;

  @CommandLine.Option(
      names = {"--function-id"},
      required = false,
      paramLabel = "FUNCTION_ID",
      description = "An ID of a function to execute.")
  private String functionId;

  @CommandLine.Option(
      names = {"--function-argument"},
      required = false,
      paramLabel = "FUNCTION_ARGUMENT",
      description = "An argument for a function to execute in a serialized format.")
  private String functionArgument;

  @CommandLine.Option(
      names = {"--deserialization-format"},
      required = false,
      paramLabel = "DESERIALIZATION_FORMAT",
      description =
          "A deserialization format for contract and function arguments. "
              + "Valid values: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})")
  private DeserializationFormat deserializationFormat = DeserializationFormat.JSON;

  @CommandLine.Option(
      names = {"-h", "--help"},
      usageHelp = true,
      description = "display the help message.")
  boolean helpRequested;

  public static void main(String[] args) {
    int exitCode = new CommandLine(new ContractExecution()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() throws Exception {
    ClientServiceFactory factory = new ClientServiceFactory();
    ClientService service = factory.create(new ClientConfig(new File(properties)));
    JacksonSerDe serde = new JacksonSerDe(new ObjectMapper());

    try {
      if (deserializationFormat == DeserializationFormat.JSON) {
        JsonNode jsonContractArgument = serde.deserialize(contractArgument);
        JsonNode jsonFunctionArgument = null;
        if (functionArgument != null) {
          jsonFunctionArgument = serde.deserialize(functionArgument);
        }
        ContractExecutionResult result =
            service.executeContract(
                contractId, jsonContractArgument, functionId, jsonFunctionArgument);

        result
            .getContractResult()
            .ifPresent(
                r -> {
                  System.out.println("Contract result:");
                  Common.printJson(serde.deserialize(r));
                });
        result
            .getFunctionResult()
            .ifPresent(
                r -> {
                  System.out.println("Function result:");
                  Common.printJson(serde.deserialize(r));
                });
      } else if (deserializationFormat == DeserializationFormat.STRING) {
        ContractExecutionResult result =
            service.executeContract(contractId, contractArgument, functionId, functionArgument);

        result.getContractResult().ifPresent(r -> System.out.println("Contract result: " + r));
        result.getFunctionResult().ifPresent(r -> System.out.println("Function result:" + r));
      }

      return 0;
    } catch (ClientException e) {
      Common.printError(e);
      return 1;
    } finally {
      factory.close();
    }
  }
}
