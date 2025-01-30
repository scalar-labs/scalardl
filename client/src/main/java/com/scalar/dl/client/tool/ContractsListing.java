package com.scalar.dl.client.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.client.service.ClientServiceFactory;
import com.scalar.dl.ledger.util.JacksonSerDe;
import java.io.File;
import java.util.concurrent.Callable;
import javax.json.JsonObject;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "list-contracts", description = "List registered contracts.")
public class ContractsListing implements Callable<Integer> {

  @CommandLine.Option(
      names = {"--properties", "--config"},
      required = true,
      paramLabel = "PROPERTIES_FILE",
      description = "A configuration file in properties format.")
  private String properties;

  @CommandLine.Option(
      names = {"--contract-id"},
      required = false,
      paramLabel = "CONTRACT_ID",
      description = "An ID of a contract to show.")
  private String contractId;

  @CommandLine.Option(
      names = {"-h", "--help"},
      usageHelp = true,
      description = "display the help message.")
  boolean helpRequested;

  public static void main(String[] args) {
    int exitCode = new CommandLine(new ContractsListing()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() throws Exception {
    ClientServiceFactory factory = new ClientServiceFactory();
    ClientService service = factory.create(new ClientConfig(new File(properties)));
    JacksonSerDe serde = new JacksonSerDe(new ObjectMapper());

    try {
      JsonObject result = service.listContracts(contractId);
      Common.printOutput(serde.deserialize(result.toString()));
      return 0;
    } catch (ClientException e) {
      Common.printError(e);
      return 1;
    } finally {
      factory.close();
    }
  }
}
