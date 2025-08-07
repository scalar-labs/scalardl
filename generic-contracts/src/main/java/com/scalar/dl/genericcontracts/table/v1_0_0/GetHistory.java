package com.scalar.dl.genericcontracts.table.v1_0_0;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.annotations.VisibleForTesting;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.AssetFilter.AgeOrder;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.Arrays;
import javax.annotation.Nullable;

public class GetHistory extends JacksonBasedContract {

  @Nullable
  @Override
  public JsonNode invoke(
      Ledger<JsonNode> ledger, JsonNode arguments, @Nullable JsonNode properties) {

    // Check the arguments
    validateQuery(arguments);

    JsonNode tableName = arguments.get(Constants.QUERY_TABLE);
    if (!isSupportedObjectName(tableName.asText())) {
      throw new ContractContextException(Constants.INVALID_OBJECT_NAME + tableName.asText());
    }

    JsonNode table =
        ledger
            .get(getAssetId(ledger, Constants.PREFIX_TABLE, tableName))
            .orElseThrow(
                () -> new ContractContextException(Constants.TABLE_NOT_EXIST + tableName.asText()))
            .data();

    // Check the key type
    JsonNode condition = arguments.get(Constants.QUERY_CONDITIONS).get(0);
    String conditionColumn = getColumnName(condition.get(Constants.CONDITION_COLUMN).asText());
    JsonNode conditionValue = condition.get(Constants.CONDITION_VALUE);
    JsonNode keyColumn = table.get(Constants.TABLE_KEY);
    if (!keyColumn.asText().equals(conditionColumn)) {
      throw new ContractContextException(Constants.INVALID_HISTORY_QUERY_CONDITION);
    }
    String keyType = table.get(Constants.TABLE_KEY_TYPE).asText();
    String givenType = conditionValue.getNodeType().name();
    if (!givenType.equalsIgnoreCase(keyType)) {
      throw new ContractContextException(Constants.INVALID_KEY_TYPE + givenType);
    }

    // Prepare scan for the asset of the record
    String recordAssetId =
        getAssetId(ledger, Constants.PREFIX_RECORD, tableName, keyColumn, conditionValue);
    AssetFilter filter = new AssetFilter(recordAssetId).withAgeOrder(AgeOrder.DESC);
    if (arguments.has(Constants.QUERY_LIMIT)) {
      filter.withLimit(validateAndGetLimit(arguments.get(Constants.QUERY_LIMIT)));
    }

    // Get history of the record
    ArrayNode history = getObjectMapper().createArrayNode();
    ledger
        .scan(filter)
        .forEach(
            asset ->
                history.add(
                    getObjectMapper()
                        .createObjectNode()
                        .put(Constants.HISTORY_ASSET_AGE, asset.age())
                        .set(Constants.RECORD_VALUES, asset.data())));

    return history;
  }

  private void validateQuery(JsonNode arguments) {
    // Check the required fields
    if (arguments.size() < 2
        || arguments.size() > 3
        || !arguments.has(Constants.QUERY_TABLE)
        || !arguments.get(Constants.QUERY_TABLE).isTextual()
        || !arguments.has(Constants.QUERY_CONDITIONS)) {
      throw new ContractContextException(Constants.INVALID_CONTRACT_ARGUMENTS);
    }

    String tableReference = arguments.get(Constants.QUERY_TABLE).asText();
    validateConditions(tableReference, arguments.get(Constants.QUERY_CONDITIONS));
  }

  private void validateConditions(String tableReference, JsonNode conditions) {
    if (!conditions.isArray() || conditions.size() != 1) {
      throw new ContractContextException(Constants.INVALID_HISTORY_QUERY_CONDITION);
    }

    JsonNode condition = conditions.get(0);
    if (!condition.isObject()
        || !condition.has(Constants.CONDITION_COLUMN)
        || !condition.get(Constants.CONDITION_COLUMN).isTextual()
        || !condition.has(Constants.CONDITION_OPERATOR)
        || !condition.get(Constants.CONDITION_OPERATOR).isTextual()) {
      throw new ContractContextException(Constants.INVALID_HISTORY_QUERY_CONDITION);
    }

    if (!condition
        .get(Constants.CONDITION_OPERATOR)
        .asText()
        .equalsIgnoreCase(Constants.OPERATOR_EQ)) {
      throw new ContractContextException(Constants.INVALID_HISTORY_QUERY_CONDITION);
    }

    // Check the column
    validateColumn(tableReference, condition.get(Constants.CONDITION_COLUMN).asText());
  }

  private boolean isColumnName(String column) {
    return Constants.OBJECT_NAME.matcher(column).matches();
  }

  private boolean isColumnReference(String column) {
    return Constants.COLUMN_REFERENCE.matcher(column).matches();
  }

  private String getColumnName(String column) {
    return isColumnReference(column)
        ? column.substring(column.indexOf(Constants.COLUMN_SEPARATOR) + 1)
        : column;
  }

  private String getTableReference(String column) {
    return column.substring(0, column.indexOf(Constants.COLUMN_SEPARATOR));
  }

  private void validateColumn(String tableReference, String column) {
    if (isColumnReference(column)) {
      String specifiedTableReference = getTableReference(column);
      if (!specifiedTableReference.equals(tableReference)) {
        throw new ContractContextException(Constants.UNKNOWN_TABLE + specifiedTableReference);
      }
    } else {
      if (!isColumnName(column)) {
        throw new ContractContextException(Constants.INVALID_COLUMN_FORMAT + column);
      }
    }
  }

  private Integer validateAndGetLimit(JsonNode limit) {
    if (!limit.isInt() || limit.asInt() < 0) {
      throw new ContractContextException(Constants.INVALID_CONTRACT_ARGUMENTS);
    }
    return limit.asInt();
  }

  private boolean isSupportedObjectName(String name) {
    return Constants.OBJECT_NAME.matcher(name).matches();
  }

  @VisibleForTesting
  String getAssetId(Ledger<JsonNode> ledger, String prefix, JsonNode... jsonNodes) {
    ArrayNode values = getObjectMapper().createArrayNode();
    Arrays.stream(jsonNodes).forEach(values::add);
    JsonNode arguments =
        getObjectMapper()
            .createObjectNode()
            .put(Constants.ASSET_ID_PREFIX, prefix)
            .set(Constants.ASSET_ID_VALUES, values);
    return invoke(Constants.CONTRACT_GET_ASSET_ID, ledger, arguments).asText();
  }
}
