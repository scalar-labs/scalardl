package com.scalar.dl.tablestore.client.tool;

import com.google.common.annotations.VisibleForTesting;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.GatewayClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.tool.Common;
import com.scalar.dl.client.tool.CommonOptions;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.tablestore.client.service.TableStoreClientService;
import com.scalar.dl.tablestore.client.service.TableStoreClientServiceFactory;
import java.io.File;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;

@Command(name = "validate-ledger", description = "Validate a specified asset in the ledger.")
public class LedgerValidation extends CommonOptions implements Callable<Integer> {

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

  @CommandLine.Option(
      names = {"--table-name"},
      required = true,
      paramLabel = "TABLE_NAME",
      description = "The name of the table.")
  String tableName;

  @ArgGroup(exclusive = false, multiplicity = "0..1")
  private PrimaryOrIndexKey key;

  static class PrimaryOrIndexKey {
    @ArgGroup(exclusive = true, multiplicity = "1")
    private ColumnName name;

    @CommandLine.Option(
        names = {"--column-value"},
        required = true,
        paramLabel = "COLUMN_VALUE",
        description = "The column value of the primary key or the index key as a JSON string.")
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

  @Override
  public Integer call() throws Exception {
    return call(new TableStoreClientServiceFactory());
  }

  @VisibleForTesting
  Integer call(TableStoreClientServiceFactory factory) throws Exception {
    TableStoreClientService service =
        useGateway
            ? factory.create(new GatewayClientConfig(new File(properties)), false)
            : factory.create(new ClientConfig(new File(properties)), false);
    return call(factory, service);
  }

  @VisibleForTesting
  Integer call(TableStoreClientServiceFactory factory, TableStoreClientService service) {
    try {
      LedgerValidationResult result;
      if (key == null) {
        result = service.validateTableSchema(tableName, startAge, endAge);
      } else {
        if (key.name.primary != null) {
          result = service.validateRecord(tableName, key.name.primary, key.value, startAge, endAge);
        } else {
          result =
              service.validateIndexRecord(tableName, key.name.index, key.value, startAge, endAge);
        }
      }
      Common.printJson(Common.getValidationResult(result));
      return 0;
    } catch (ClientException e) {
      Common.printError(e);
      printStackTrace(e);
      return 1;
    } finally {
      factory.close();
    }
  }
}
