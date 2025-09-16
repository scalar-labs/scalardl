package com.scalar.dl.client.tool;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.GatewayClientConfig;
import com.scalar.dl.client.error.ClientError;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.client.service.ClientServiceFactory;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "validate-ledger", description = "Validate a specified asset in a ledger.")
public class LedgerValidation extends CommonOptions implements Callable<Integer> {

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
      assetIds.forEach(
          assetId -> {
            LedgerValidationResult result;
            List<String> idAndAges = Splitter.on(',').splitToList(assetId);
            if (idAndAges.size() == 1) {
              result = service.validateLedger(idAndAges.get(0));
            } else if (idAndAges.size() == 3) {
              result =
                  service.validateLedger(
                      idAndAges.get(0),
                      Integer.parseInt(idAndAges.get(1)),
                      Integer.parseInt(idAndAges.get(2)));
            } else {
              throw new ClientException(ClientError.OPTION_ASSET_ID_IS_MALFORMED);
            }
            Common.printJson(Common.getValidationResult(result));
          });
      return 0;
    } catch (ClientException e) {
      Common.printError(e);
      printStackTrace(e);
      return 1;
    } catch (NumberFormatException e) {
      System.out.println(ClientError.OPTION_ASSET_ID_CONTAINS_INVALID_INTEGER);
      printStackTrace(e);
      return 1;
    } catch (IndexOutOfBoundsException e) {
      System.out.println(ClientError.OPTION_ASSET_ID_IS_MALFORMED.buildMessage());
      printStackTrace(e);
      return 1;
    } finally {
      factory.close();
    }
  }
}
