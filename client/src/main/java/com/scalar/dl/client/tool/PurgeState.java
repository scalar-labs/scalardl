package com.scalar.dl.client.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.ledger.model.TransactionStatePurgeResult;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "purge-state", description = "Purge stale transaction states.")
public class PurgeState extends AbstractClientCommand {

  public static void main(String[] args) {
    int exitCode = new CommandLine(new PurgeState()).execute(args);
    System.exit(exitCode);
  }

  @Override
  protected Integer execute(ClientService service) throws ClientException {
    TransactionStatePurgeResult result = service.purgeState();
    ObjectNode output = new ObjectMapper().createObjectNode();
    output.put("total_targets", result.getTotalTargets());
    output.put("purged", result.getPurged());
    output.put("skipped", result.getSkipped());
    Common.printOutput(output);
    return 0;
  }
}
