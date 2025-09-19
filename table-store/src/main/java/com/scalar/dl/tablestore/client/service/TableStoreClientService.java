package com.scalar.dl.tablestore.client.service;

import static com.scalar.dl.genericcontracts.table.Constants.ASSET_ID_SEPARATOR;
import static com.scalar.dl.genericcontracts.table.Constants.CONTRACT_CREATE;
import static com.scalar.dl.genericcontracts.table.Constants.CONTRACT_GET_ASSET_ID;
import static com.scalar.dl.genericcontracts.table.Constants.CONTRACT_GET_HISTORY;
import static com.scalar.dl.genericcontracts.table.Constants.CONTRACT_INSERT;
import static com.scalar.dl.genericcontracts.table.Constants.CONTRACT_SCAN;
import static com.scalar.dl.genericcontracts.table.Constants.CONTRACT_SELECT;
import static com.scalar.dl.genericcontracts.table.Constants.CONTRACT_SHOW_TABLES;
import static com.scalar.dl.genericcontracts.table.Constants.CONTRACT_UPDATE;
import static com.scalar.dl.genericcontracts.table.Constants.PREFIX_INDEX;
import static com.scalar.dl.genericcontracts.table.Constants.PREFIX_RECORD;
import static com.scalar.dl.genericcontracts.table.Constants.PREFIX_TABLE;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.google.common.collect.ImmutableMap;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.client.util.Common;
import com.scalar.dl.genericcontracts.table.v1_0_0.Create;
import com.scalar.dl.genericcontracts.table.v1_0_0.GetAssetId;
import com.scalar.dl.genericcontracts.table.v1_0_0.GetHistory;
import com.scalar.dl.genericcontracts.table.v1_0_0.Insert;
import com.scalar.dl.genericcontracts.table.v1_0_0.Scan;
import com.scalar.dl.genericcontracts.table.v1_0_0.Select;
import com.scalar.dl.genericcontracts.table.v1_0_0.ShowTables;
import com.scalar.dl.genericcontracts.table.v1_0_0.Update;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.scalar.dl.ledger.model.ExecutionResult;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.util.JacksonSerDe;
import com.scalar.dl.tablestore.client.error.TableStoreClientError;
import com.scalar.dl.tablestore.client.partiql.parser.ScalarPartiqlParser;
import com.scalar.dl.tablestore.client.partiql.statement.ContractStatement;
import java.util.List;
import java.util.Map;
import javax.annotation.concurrent.Immutable;
import javax.json.JsonObject;
import javax.json.JsonValue;

/**
 * A thread-safe client for a table store. The client interacts with Ledger and Auditor components
 * to bootstrap the table store, execute statements, and validate data.
 *
 * <h3>Usage Examples</h3>
 *
 * Here is a simple example to demonstrate how to use {@code TableStoreClientService}. {@code
 * TableStoreClientService} should always be created with {@link TableStoreClientServiceFactory},
 * which reuses internal instances as much as possible for better performance and less resource
 * usage. When you create {@code TableStoreClientService}, the client certificate or secret key and
 * the necessary contracts for using a table store are automatically registered by default based on
 * the configuration in {@code ClientConfig}.
 *
 * <pre>{@code
 * TableStoreClientServiceFactory factory = new TableStoreClientServiceFactory(); // the factory should be reused
 *
 * TableStoreClientService service = factory.create(new ClientConfig(new File(properties));
 * try {
 *   String statement = ...; // prepare a PartiQL statement
 *   StatementExecutionResult result = service.executeStatement(statement);
 *   result.getResult().ifPresent(System.out::println);
 * } catch (ClientException e) {
 *   System.err.println(e.getStatusCode());
 *   System.err.println(e.getMessage());
 * }
 *
 * factory.close();
 * }</pre>
 */
@Immutable
public class TableStoreClientService {
  private static final ImmutableMap<String, Class<?>> CONTRACTS =
      ImmutableMap.<String, Class<?>>builder()
          .put(CONTRACT_CREATE, Create.class)
          .put(CONTRACT_INSERT, Insert.class)
          .put(CONTRACT_SELECT, Select.class)
          .put(CONTRACT_UPDATE, Update.class)
          .put(CONTRACT_SHOW_TABLES, ShowTables.class)
          .put(CONTRACT_GET_HISTORY, GetHistory.class)
          .put(CONTRACT_GET_ASSET_ID, GetAssetId.class)
          .put(CONTRACT_SCAN, Scan.class)
          .build();
  private static final JacksonSerDe jacksonSerDe = new JacksonSerDe(new ObjectMapper());
  private final ClientService clientService;

  /**
   * Constructs a {@code TableStoreClientService} with the specified {@link ClientService}.
   *
   * @param clientService a client service
   */
  public TableStoreClientService(ClientService clientService) {
    this.clientService = clientService;
  }

  /**
   * Bootstraps the table store by registering the identity (certificate or secret key) and
   * necessary contracts based on {@code ClientConfig}. The authentication method (digital signature
   * or HMAC) is determined by the configuration. If the identity or contract is already registered,
   * it is simply skipped without throwing an exception.
   *
   * <p>This method is primarily for internal use, and users don't need to call it because the
   * identity and contracts are automatically registered when creating {@code
   * TableStoreClientService}. Breaking changes can and will be introduced to this method. Users
   * should not depend on it.
   *
   * @throws ClientException if a request fails for some reason
   */
  public void bootstrap() {
    clientService.bootstrap();
    registerContracts();
  }

  private void registerContracts() {
    for (Map.Entry<String, Class<?>> entry : CONTRACTS.entrySet()) {
      String contractId = entry.getKey();
      Class<?> clazz = entry.getValue();
      String contractBinaryName = clazz.getName();
      byte[] bytes = Common.getClassBytes(clazz);
      try {
        clientService.registerContract(contractId, contractBinaryName, bytes, (String) null);
      } catch (ClientException e) {
        if (!e.getStatusCode().equals(StatusCode.CONTRACT_ALREADY_REGISTERED)) {
          throw e;
        }
      }
    }
  }

  /**
   * Retrieves a list of contracts for the certificate holder specified in {@code ClientConfig}. If
   * specified with a contract ID, it will return the matching contract only.
   *
   * @param id a contract ID
   * @return {@link JsonObject}
   * @throws ClientException if a request fails for some reason
   */
  public JsonObject listContracts(String id) {
    return clientService.listContracts(id);
  }

  /**
   * Executes the specified statement.
   *
   * @param statement a PartiQL statement
   * @return {@link ContractExecutionResult}
   * @throws IllegalArgumentException if the specified statement is invalid
   * @throws ClientException if a request fails for some reason
   */
  public ExecutionResult executeStatement(String statement) {
    List<ContractStatement> contractStatements = ScalarPartiqlParser.parse(statement);
    assert !contractStatements.isEmpty();

    if (contractStatements.size() > 1) {
      throw new IllegalArgumentException(
          TableStoreClientError.MULTIPLE_STATEMENTS_NOT_SUPPORTED.buildMessage());
    }

    ContractStatement contractStatement = contractStatements.get(0);
    ContractExecutionResult contractExecutionResult =
        clientService.executeContract(
            contractStatement.getContractId(), contractStatement.getArguments());
    return new ExecutionResult(contractExecutionResult);
  }

  /**
   * Validates the schema of the specified table in the ledger.
   *
   * @param tableName a table name
   * @return {@link LedgerValidationResult}
   * @throws ClientException if a request fails for some reason
   */
  public LedgerValidationResult validateTableSchema(String tableName) {
    return clientService.validateLedger(buildTableSchemaAssetId(tableName));
  }

  /**
   * Validates the schema of the specified table in the ledger.
   *
   * @param tableName a table name
   * @param startAge an age to be validated from (inclusive)
   * @param endAge an age to be validated to (inclusive)
   * @return {@link LedgerValidationResult}
   * @throws ClientException if a request fails for some reason
   */
  public LedgerValidationResult validateTableSchema(String tableName, int startAge, int endAge) {
    return clientService.validateLedger(buildTableSchemaAssetId(tableName), startAge, endAge);
  }

  /**
   * Validates the specified record in the ledger.
   *
   * @param tableName a table name
   * @param columnName a primary key column name
   * @param value a primary key column value
   * @return {@link LedgerValidationResult}
   * @throws ClientException if a request fails for some reason
   */
  public LedgerValidationResult validateRecord(
      String tableName, String columnName, JsonValue value) {
    return clientService.validateLedger(
        buildRecordAssetId(tableName, columnName, toStringFrom(value)));
  }

  /**
   * Validates the specified record in the ledger.
   *
   * @param tableName a table name
   * @param columnName a primary key column name
   * @param value a primary key column value
   * @param startAge an age to be validated from (inclusive)
   * @param endAge an age to be validated to (inclusive)
   * @return {@link LedgerValidationResult}
   * @throws ClientException if a request fails for some reason
   */
  public LedgerValidationResult validateRecord(
      String tableName, String columnName, JsonValue value, int startAge, int endAge) {
    return clientService.validateLedger(
        buildRecordAssetId(tableName, columnName, toStringFrom(value)), startAge, endAge);
  }

  /**
   * Validates the specified record in the ledger.
   *
   * @param tableName a table name
   * @param columnName a primary key column name
   * @param value a primary key column value
   * @return {@link LedgerValidationResult}
   * @throws ClientException if a request fails for some reason
   */
  public LedgerValidationResult validateRecord(
      String tableName, String columnName, ValueNode value) {
    return clientService.validateLedger(
        buildRecordAssetId(tableName, columnName, toStringFrom(value)));
  }

  /**
   * Validates the specified record in the ledger.
   *
   * @param tableName a table name
   * @param columnName a primary key column name
   * @param value a primary key column value
   * @param startAge an age to be validated from (inclusive)
   * @param endAge an age to be validated to (inclusive)
   * @return {@link LedgerValidationResult}
   * @throws ClientException if a request fails for some reason
   */
  public LedgerValidationResult validateRecord(
      String tableName, String columnName, ValueNode value, int startAge, int endAge) {
    return clientService.validateLedger(
        buildRecordAssetId(tableName, columnName, toStringFrom(value)), startAge, endAge);
  }

  /**
   * Validates the specified record in the ledger.
   *
   * @param tableName a table name
   * @param columnName a primary key column name
   * @param value a primary key column value in a JSON format
   * @return {@link LedgerValidationResult}
   * @throws ClientException if a request fails for some reason
   */
  public LedgerValidationResult validateRecord(String tableName, String columnName, String value) {
    return clientService.validateLedger(
        buildRecordAssetId(tableName, columnName, toStringFrom(jacksonSerDe.deserialize(value))));
  }

  /**
   * Validates the specified record in the ledger.
   *
   * @param tableName a table name
   * @param columnName a primary key column name
   * @param value a primary key column value in a JSON format
   * @param startAge an age to be validated from (inclusive)
   * @param endAge an age to be validated to (inclusive)
   * @return {@link LedgerValidationResult}
   * @throws ClientException if a request fails for some reason
   */
  public LedgerValidationResult validateRecord(
      String tableName, String columnName, String value, int startAge, int endAge) {
    return clientService.validateLedger(
        buildRecordAssetId(tableName, columnName, toStringFrom(jacksonSerDe.deserialize(value))),
        startAge,
        endAge);
  }

  /**
   * Validates the specified index record in the ledger.
   *
   * @param tableName a table name
   * @param columnName an index key column name
   * @param value an index key column value
   * @return {@link LedgerValidationResult}
   * @throws ClientException if a request fails for some reason
   */
  public LedgerValidationResult validateIndexRecord(
      String tableName, String columnName, JsonValue value) {
    return clientService.validateLedger(
        buildIndexRecordAssetId(tableName, columnName, toStringFrom(value)));
  }

  /**
   * Validates the specified index record in the ledger.
   *
   * @param tableName a table name
   * @param columnName an index key column name
   * @param value an index key column value
   * @param startAge an age to be validated from (inclusive)
   * @param endAge an age to be validated to (inclusive)
   * @return {@link LedgerValidationResult}
   * @throws ClientException if a request fails for some reason
   */
  public LedgerValidationResult validateIndexRecord(
      String tableName, String columnName, JsonValue value, int startAge, int endAge) {
    return clientService.validateLedger(
        buildIndexRecordAssetId(tableName, columnName, toStringFrom(value)), startAge, endAge);
  }

  /**
   * Validates the specified index record in the ledger.
   *
   * @param tableName a table name
   * @param columnName an index key column name
   * @param value an index key column value
   * @return {@link LedgerValidationResult}
   * @throws ClientException if a request fails for some reason
   */
  public LedgerValidationResult validateIndexRecord(
      String tableName, String columnName, ValueNode value) {
    return clientService.validateLedger(
        buildIndexRecordAssetId(tableName, columnName, toStringFrom(value)));
  }

  /**
   * Validates the specified index record in the ledger.
   *
   * @param tableName a table name
   * @param columnName an index key column name
   * @param value an index key column value
   * @param startAge an age to be validated from (inclusive)
   * @param endAge an age to be validated to (inclusive)
   * @return {@link LedgerValidationResult}
   * @throws ClientException if a request fails for some reason
   */
  public LedgerValidationResult validateIndexRecord(
      String tableName, String columnName, ValueNode value, int startAge, int endAge) {
    return clientService.validateLedger(
        buildIndexRecordAssetId(tableName, columnName, toStringFrom(value)), startAge, endAge);
  }

  /**
   * Validates the specified index record in the ledger.
   *
   * @param tableName a table name
   * @param columnName an index key column name
   * @param value an index key column value in a JSON format
   * @return {@link LedgerValidationResult}
   * @throws ClientException if a request fails for some reason
   */
  public LedgerValidationResult validateIndexRecord(
      String tableName, String columnName, String value) {
    return clientService.validateLedger(
        buildIndexRecordAssetId(
            tableName, columnName, toStringFrom(jacksonSerDe.deserialize(value))));
  }

  /**
   * Validates the specified index record in the ledger.
   *
   * @param tableName a table name
   * @param columnName an index key column name
   * @param value an index key column value in a JSON format
   * @param startAge an age to be validated from (inclusive)
   * @param endAge an age to be validated to (inclusive)
   * @return {@link LedgerValidationResult}
   * @throws ClientException if a request fails for some reason
   */
  public LedgerValidationResult validateIndexRecord(
      String tableName, String columnName, String value, int startAge, int endAge) {
    return clientService.validateLedger(
        buildIndexRecordAssetId(
            tableName, columnName, toStringFrom(jacksonSerDe.deserialize(value))),
        startAge,
        endAge);
  }

  private String buildTableSchemaAssetId(String tableName) {
    return PREFIX_TABLE + tableName;
  }

  private String buildRecordAssetId(String tableName, String columnName, String value) {
    return PREFIX_RECORD + String.join(ASSET_ID_SEPARATOR, tableName, columnName, value);
  }

  private String buildIndexRecordAssetId(String tableName, String columnName, String value) {
    return PREFIX_INDEX + String.join(ASSET_ID_SEPARATOR, tableName, columnName, value);
  }

  private String toStringFrom(JsonValue value) {
    return toStringFrom(jacksonSerDe.deserialize(value.toString()));
  }

  private String toStringFrom(JsonNode value) {
    if (value.canConvertToExactIntegral()) {
      return value.bigIntegerValue().toString();
    } else if (value.isNumber()) {
      return String.valueOf(value.doubleValue());
    } else {
      return value.asText();
    }
  }
}
