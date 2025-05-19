package com.scalar.dl.client.tool;

import com.google.common.annotations.VisibleForTesting;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.GatewayClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientServiceFactory;
import com.scalar.dl.client.service.GenericContractClientService;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import java.io.File;
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

    @ArgGroup(exclusive = false, multiplicity = "1")
    TableArguments table;
  }

  static class TableArguments {
    @CommandLine.Option(
        names = {"--table-name"},
        required = true,
        paramLabel = "TABLE_NAME",
        description = "The name of a table created by table-oriented generic contracts.")
    String tableName;

    @ArgGroup(exclusive = false, multiplicity = "0..1")
    private PrimaryOrIndexKey key;
  }

  static class PrimaryOrIndexKey {
    @ArgGroup(exclusive = true, multiplicity = "1")
    private ColumnName name;

    @CommandLine.Option(
        names = {"--column-value"},
        required = true,
        paramLabel = "COLUMN_VALUE",
        description = "The column value of primary key or index key as JSON string.")
    String value;
  }

  static class ColumnName {
    @CommandLine.Option(
        names = {"--primary-key-column-name"},
        paramLabel = "PRIMARY_KEY_COLUMN_NAME",
        description =
            "The primary key column name of a record created by table-oriented generic contracts.")
    String primary;

    @CommandLine.Option(
        names = {"--index-key-column-name"},
        paramLabel = "INDEX_KEY_COLUMN_NAME",
        description =
            "The index key column name of a record created by table-oriented generic contracts.")
    String index;
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
      LedgerValidationResult result;
      if (arguments.objectId != null) {
        result = service.validateObject(arguments.objectId, startAge, endAge);
      } else if (arguments.collectionId != null) {
        result = service.validateCollection(arguments.collectionId, startAge, endAge);
      } else {
        if (arguments.table.key == null) {
          result = service.validateTableSchema(arguments.table.tableName, startAge, endAge);
        } else {
          String tableName = arguments.table.tableName;
          String value = arguments.table.key.value;
          if (arguments.table.key.name.primary != null) {
            result =
                service.validateRecord(
                    tableName, arguments.table.key.name.primary, value, startAge, endAge);
          } else {
            result =
                service.validateIndexEntry(
                    tableName, arguments.table.key.name.index, value, startAge, endAge);
          }
        }
      }
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
