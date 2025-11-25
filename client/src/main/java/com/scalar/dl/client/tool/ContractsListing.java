package com.scalar.dl.client.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.ledger.util.JacksonSerDe;
import javax.json.JsonObject;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "list-contracts", description = "List registered contracts.")
public class ContractsListing extends AbstractClientCommand {

  @CommandLine.Option(
      names = {"--contract-id"},
      required = false,
      paramLabel = "CONTRACT_ID",
      description = "An ID of a contract to show.")
  private String contractId;

  public static void main(String[] args) {
    int exitCode = new CommandLine(new ContractsListing()).execute(args);
    System.exit(exitCode);
  }

  @Override
  protected Integer execute(ClientService service) throws ClientException {
    JacksonSerDe serde = new JacksonSerDe(new ObjectMapper());
    JsonObject result = service.listContracts(contractId);
    Common.printOutput(serde.deserialize(result.toString()));
    return 0;
  }
}
