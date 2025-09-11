package com.scalar.dl.hashstore.client.tool;

import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.tool.Common;
import com.scalar.dl.hashstore.client.service.ClientService;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;

@Command(name = "validate-ledger", description = "Validate a specified asset in the ledger.")
public class LedgerValidation extends AbstractHashStoreCommand {

  @CommandLine.Option(
      names = {"--start-age"},
      paramLabel = "START_AGE",
      description = "Start age for validation range.")
  private int startAge = 0;

  @CommandLine.Option(
      names = {"--end-age"},
      paramLabel = "END_AGE",
      description = "End age for validation range.")
  private int endAge = Integer.MAX_VALUE;

  @ArgGroup(exclusive = true, multiplicity = "1")
  private Target target;

  static class Target {
    @CommandLine.Option(
        names = {"--object-id"},
        paramLabel = "OBJECT_ID",
        description = "The ID of the object to validate.")
    String objectId;

    @CommandLine.Option(
        names = {"--collection-id"},
        paramLabel = "COLLECTION_ID",
        description = "The ID of the collection to validate.")
    String collectionId;
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new LedgerValidation()).execute(args);
    System.exit(exitCode);
  }

  @Override
  protected Integer execute(ClientService service) throws ClientException {
    LedgerValidationResult result;
    if (target.objectId != null) {
      result = service.validateObject(target.objectId, startAge, endAge);
    } else {
      result = service.validateCollection(target.collectionId, startAge, endAge);
    }
    Common.printJson(Common.getValidationResult(result));
    return 0;
  }
}
