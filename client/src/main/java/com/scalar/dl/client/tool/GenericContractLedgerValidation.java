package com.scalar.dl.client.tool;

import com.google.common.annotations.VisibleForTesting;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.GatewayClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientServiceFactory;
import com.scalar.dl.client.service.GenericContractClientService;
import com.scalar.dl.genericcontracts.AssetType;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;

@Command(name = "validate-ledger", description = "Validate a specified asset in a ledger.")
public class GenericContractLedgerValidation extends CommonOptions implements Callable<Integer> {

  @CommandLine.Option(
      names = {"--start-age"},
      paramLabel = "START_AGE",
      description = "The validation start age of the asset.")
  private int startAge = 0;

  @CommandLine.Option(
      names = {"--end-age"},
      paramLabel = "END_AGE",
      description = "The validation end age of the asset.")
  private int endAge = Integer.MAX_VALUE;

  static class Arguments {
    @CommandLine.Option(
        names = {"--object-id"},
        paramLabel = "OBJECT_ID",
        description = "The ID of an object created by the object.Put contract.")
    String objectId;

    @CommandLine.Option(
        names = {"--collection-id"},
        paramLabel = "COLLECTION_ID",
        description = "The ID of a collection created by the collection.Create contract.")
    String collectionId;
  }

  @ArgGroup(exclusive = true, multiplicity = "1")
  private Arguments arguments;

  @Override
  public Integer call() throws Exception {
    return call(new ClientServiceFactory());
  }

  @VisibleForTesting
  Integer call(ClientServiceFactory factory) throws Exception {
    GenericContractClientService service =
        useGateway
            ? factory.createForGenericContract(new GatewayClientConfig(new File(properties)))
            : factory.createForGenericContract(new ClientConfig(new File(properties)));
    return call(factory, service);
  }

  @VisibleForTesting
  Integer call(ClientServiceFactory factory, GenericContractClientService service) {
    try {
      AssetType type;
      List<String> keys = new ArrayList<>();
      if (arguments.objectId != null) {
        type = AssetType.OBJECT;
        keys.add(arguments.objectId);
      } else {
        type = AssetType.COLLECTION;
        keys.add(arguments.collectionId);
      }
      LedgerValidationResult result = service.validateLedger(type, keys, startAge, endAge);
      Common.printJson(Common.getValidationResult(result));
      return 0;
    } catch (IllegalArgumentException e) {
      printStackTrace(e);
      return 1;
    } catch (ClientException e) {
      Common.printError(e);
      printStackTrace(e);
      return 1;
    } finally {
      factory.close();
    }
  }
}
