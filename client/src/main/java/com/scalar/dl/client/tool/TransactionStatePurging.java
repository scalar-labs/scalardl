package com.scalar.dl.client.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.ledger.model.TransactionStatePurgeResult;
import java.io.Console;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "purge-state", description = "Purge stale transaction states.")
public class TransactionStatePurging extends AbstractClientCommand {

  @Option(
      names = {"-f", "--force"},
      description = "Purge stale transaction states without the confirmation prompt.")
  private boolean force;

  @Override
  protected Integer execute(ClientService service) throws ClientException {
    if (!confirmPurging()) {
      return 1;
    }

    TransactionStatePurgeResult result = service.purgeState();
    ObjectNode output = new ObjectMapper().createObjectNode();
    output.put("total_targets", result.getTotalTargets());
    output.put("purged", result.getPurged());
    output.put("skipped", result.getSkipped());
    Common.printOutput(output);
    return 0;
  }

  protected boolean confirmPurging() {
    if (force) {
      return true;
    }

    Console console = System.console();
    if (console == null) {
      System.err.println("No console available. Use the --force option to skip the confirmation.");
      return false;
    }

    System.out.print("Are you sure you want to purge stale transaction states? [y/N]: ");
    System.out.flush();
    String confirmation = console.readLine();
    if (confirmation == null || !confirmation.trim().equalsIgnoreCase("y")) {
      System.err.println("Aborting.");
      return false;
    }
    return true;
  }
}
