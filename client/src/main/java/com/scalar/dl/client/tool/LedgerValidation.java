package com.scalar.dl.client.tool;

import com.google.common.base.Splitter;
import com.scalar.dl.client.error.ClientError;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import java.util.List;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "validate-ledger", description = "Validate a specified asset in a ledger.")
public class LedgerValidation extends AbstractClientCommand {

  @CommandLine.Option(
      names = {"--namespace"},
      paramLabel = "NAMESPACE",
      description = "The namespace of the asset.")
  private String namespace;

  @CommandLine.Option(
      names = {"--asset-id"},
      required = true,
      paramLabel = "ASSET_ID",
      description =
          "The ID (and the ages) of an asset. Format: 'asset_id' or 'asset_id,start_age,end_age'")
  private List<String> assetIds;

  public static void main(String[] args) {
    int exitCode = new CommandLine(new LedgerValidation()).execute(args);
    System.exit(exitCode);
  }

  @Override
  protected Integer execute(ClientService service) throws ClientException {
    try {
      assetIds.forEach(
          assetId -> {
            LedgerValidationResult result;
            List<String> idAndAges = Splitter.on(',').splitToList(assetId);
            if (idAndAges.size() == 1) {
              result = validateLedger(service, idAndAges.get(0));
            } else if (idAndAges.size() == 3) {
              result =
                  validateLedger(
                      service,
                      idAndAges.get(0),
                      Integer.parseInt(idAndAges.get(1)),
                      Integer.parseInt(idAndAges.get(2)));
            } else {
              throw new ClientException(ClientError.OPTION_ASSET_ID_IS_MALFORMED);
            }
            Common.printJson(Common.getValidationResult(result));
          });
      return 0;
    } catch (NumberFormatException e) {
      throw new ClientException(ClientError.OPTION_ASSET_ID_CONTAINS_INVALID_INTEGER, e);
    } catch (IndexOutOfBoundsException e) {
      throw new ClientException(ClientError.OPTION_ASSET_ID_IS_MALFORMED, e);
    }
  }

  private LedgerValidationResult validateLedger(ClientService service, String assetId) {
    if (namespace != null) {
      return service.validateLedger(namespace, assetId);
    }
    return service.validateLedger(assetId);
  }

  private LedgerValidationResult validateLedger(
      ClientService service, String assetId, int startAge, int endAge) {
    if (namespace != null) {
      return service.validateLedger(namespace, assetId, startAge, endAge);
    }
    return service.validateLedger(assetId, startAge, endAge);
  }
}
