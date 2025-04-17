package com.scalar.dl.genericcontracts.table.v1_0_0;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public class Scan extends JacksonBasedContract {

  @Nullable
  @Override
  public JsonNode invoke(
      Ledger<JsonNode> ledger, JsonNode arguments, @Nullable JsonNode properties) {

    // Get the table information
    String tableName = arguments.get(Constants.QUERY_TABLE).asText();
    String tableAssetId = getAssetIdForTable(tableName);
    JsonNode table =
        ledger
            .get(tableAssetId)
            .orElseThrow(() -> new ContractContextException(Constants.TABLE_NOT_EXIST))
            .data();

    // Scan records
    ListMultimap<String, JsonNode> conditionsMap =
        Multimaps.index(
            arguments.get(Constants.QUERY_CONDITIONS),
            condition -> condition.get(Constants.CONDITION_COLUMN).textValue());
    if (hasPrimaryKeyCondition(table, conditionsMap)) {
      return get(ledger, table, conditionsMap);
    } else if (hasIndexKeyCondition(table, conditionsMap)) {
      return scan(ledger, table, conditionsMap);
    } else {
      throw new ContractContextException(Constants.INVALID_KEY_SPECIFICATION);
    }
  }

  /**
   * Returns true if the conditions map has a primary key condition, which means the get operation
   * can be executed. The get operation gets a single record based on the primary key. It must
   * include an equality (EQ) condition for the primary key.
   *
   * @param table a table metadata object
   * @param conditionsMap a map of condition objects
   * @return boolean
   */
  private boolean hasPrimaryKeyCondition(
      JsonNode table, ListMultimap<String, JsonNode> conditionsMap) {
    String key = table.get(Constants.TABLE_KEY).textValue();
    String keyType = table.get(Constants.TABLE_KEY_TYPE).textValue();
    return conditionsMap.containsKey(key)
        && conditionsMap.get(key).stream()
            .anyMatch(condition -> isPrimaryKeyCondition(condition, keyType));
  }

  /**
   * Returns true if the conditions map has an index key condition, which means the index scan
   * operation can be executed. The index scan operation gets multiple records that the specified
   * index key matches. It must include an equality condition or IS_NULL condition for an index key.
   *
   * @param table a table metadata object
   * @param conditionsMap a map of condition objects
   * @return boolean
   */
  private boolean hasIndexKeyCondition(
      JsonNode table, ListMultimap<String, JsonNode> conditionsMap) {
    for (JsonNode index : table.get(Constants.TABLE_INDEXES)) {
      String indexKey = index.get(Constants.INDEX_KEY).asText();
      String indexKeyType = index.get(Constants.INDEX_KEY_TYPE).asText();
      if (conditionsMap.containsKey(indexKey)
          && conditionsMap.get(indexKey).stream()
              .anyMatch(condition -> isIndexKeyCondition(condition, indexKeyType))) {
        return true;
      }
    }
    return false;
  }

  private boolean isPrimaryKeyCondition(JsonNode condition, String keyType) {
    if (!condition.get(Constants.CONDITION_VALUE).getNodeType().name().equalsIgnoreCase(keyType)) {
      throw new ContractContextException(Constants.INVALID_KEY_TYPE);
    }

    return condition
        .get(Constants.CONDITION_OPERATOR)
        .textValue()
        .equalsIgnoreCase(Constants.OPERATOR_EQ);
  }

  private boolean isIndexKeyCondition(JsonNode condition, String keyType) {
    String operator = condition.get(Constants.CONDITION_OPERATOR).textValue().toUpperCase();
    if (operator.equals(Constants.OPERATOR_IS_NULL)) {
      return true;
    }

    if (!condition.get(Constants.CONDITION_VALUE).getNodeType().name().equalsIgnoreCase(keyType)) {
      throw new ContractContextException(Constants.INVALID_INDEX_KEY_TYPE);
    }

    return operator.equals(Constants.OPERATOR_EQ);
  }

  private JsonNode getKeyColumnCondition(
      JsonNode table, ListMultimap<String, JsonNode> conditionsMap) {
    String keyColumnName = table.get(Constants.TABLE_KEY).textValue();
    String keyColumnType = table.get(Constants.TABLE_KEY_TYPE).textValue();
    for (JsonNode condition : conditionsMap.get(keyColumnName)) {
      if (isPrimaryKeyCondition(condition, keyColumnType)) {
        return condition;
      }
    }

    throw new ContractContextException(Constants.INVALID_KEY_SPECIFICATION);
  }

  private JsonNode getIndexColumnCondition(
      JsonNode table, ListMultimap<String, JsonNode> conditionsMap) {
    for (JsonNode index : table.get(Constants.TABLE_INDEXES)) {
      String indexKey = index.get(Constants.INDEX_KEY).asText();
      String indexKeyType = index.get(Constants.INDEX_KEY_TYPE).asText();
      for (JsonNode condition : conditionsMap.get(indexKey)) {
        if (isIndexKeyCondition(condition, indexKeyType)) {
          return condition;
        }
      }
    }

    throw new ContractContextException(Constants.INVALID_KEY_SPECIFICATION);
  }

  private List<String> getRecordAssetIdsFromIndex(
      Ledger<JsonNode> ledger, String tableName, String indexAssetId, String keyColumnName) {
    List<Asset<JsonNode>> indexEntries = ledger.scan(new AssetFilter(indexAssetId));
    List<String> assetIds = new ArrayList<>();

    for (Asset<JsonNode> indexEntry : indexEntries) {
      if (!indexEntry.data().has(keyColumnName)) {
        throw new ContractContextException(Constants.ILLEGAL_INDEX_STATE);
      }
      assetIds.add(
          getAssetIdForRecord(
              tableName, keyColumnName, indexEntry.data().get(keyColumnName).asText()));
    }

    return assetIds;
  }

  private ArrayNode get(
      Ledger<JsonNode> ledger, JsonNode table, ListMultimap<String, JsonNode> conditionsMap) {
    String tableName = table.get(Constants.TABLE_NAME).textValue();
    JsonNode condition = getKeyColumnCondition(table, conditionsMap);
    String assetId =
        getAssetIdForRecord(
            tableName,
            condition.get(Constants.CONDITION_COLUMN).textValue(),
            condition.get(Constants.CONDITION_VALUE).asText());
    ArrayNode results = getObjectMapper().createArrayNode();

    ledger.get(assetId).ifPresent(asset -> results.add(asset.data()));

    return filter(
        results,
        conditionsMap.values().stream()
            .filter(c -> !c.equals(condition))
            .collect(Collectors.toList()));
  }

  private ArrayNode scan(
      Ledger<JsonNode> ledger, JsonNode table, ListMultimap<String, JsonNode> conditionsMap) {
    String tableName = table.get(Constants.TABLE_NAME).textValue();
    String keyColumnName = table.get(Constants.TABLE_KEY).textValue();
    JsonNode condition = getIndexColumnCondition(table, conditionsMap);
    String indexAssetId = getAssetIdForIndex(tableName, condition);
    ArrayNode results = getObjectMapper().createArrayNode();

    for (String recordAssetId :
        getRecordAssetIdsFromIndex(ledger, tableName, indexAssetId, keyColumnName)) {
      Asset<JsonNode> asset =
          ledger
              .get(recordAssetId)
              .orElseThrow(() -> new ContractContextException(Constants.ILLEGAL_INDEX_STATE));
      results.add(asset.data());
    }

    return filter(
        results,
        conditionsMap.values().stream()
            .filter(c -> !c.equals(condition))
            .collect(Collectors.toList()));
  }

  private ArrayNode filter(ArrayNode records, List<JsonNode> conditions) {
    ArrayNode results = getObjectMapper().createArrayNode();

    for (JsonNode record : records) {
      boolean allMatched = true;
      for (JsonNode condition : conditions) {
        if (!match(record, condition)) {
          allMatched = false;
          break;
        }
      }
      if (allMatched) {
        results.add(record);
      }
    }

    return results;
  }

  private boolean match(JsonNode record, JsonNode condition) {
    String column = condition.get(Constants.CONDITION_COLUMN).textValue();
    String operator = condition.get(Constants.CONDITION_OPERATOR).textValue().toUpperCase();
    JsonNode value = condition.get(Constants.CONDITION_VALUE);

    if (!operator.equals(Constants.OPERATOR_IS_NULL)
        && !operator.equals(Constants.OPERATOR_IS_NOT_NULL)
        && (!record.has(column) || !record.get(column).getNodeType().equals(value.getNodeType()))) {
      return false;
    }

    switch (operator) {
      case Constants.OPERATOR_EQ:
        return isEqual(record.get(column), value);
      case Constants.OPERATOR_NE:
        return !isEqual(record.get(column), value);
      case Constants.OPERATOR_LT:
        return isLessThan(record.get(column), value);
      case Constants.OPERATOR_LTE:
        return !isGreaterThan(record.get(column), value);
      case Constants.OPERATOR_GT:
        return isGreaterThan(record.get(column), value);
      case Constants.OPERATOR_GTE:
        return !isLessThan(record.get(column), value);
      case Constants.OPERATOR_IS_NULL:
        return isNull(record, column);
      case Constants.OPERATOR_IS_NOT_NULL:
        return !isNull(record, column);
      default:
        throw new ContractContextException(Constants.ILLEGAL_ARGUMENT);
    }
  }

  private boolean isEqual(JsonNode leftValue, JsonNode rightValue) {
    JsonNodeType type = leftValue.getNodeType();
    if (type == JsonNodeType.STRING) {
      return leftValue.asText().equals(rightValue.asText());
    } else if (type == JsonNodeType.NUMBER) {
      return leftValue.decimalValue().compareTo(rightValue.decimalValue()) == 0;
    } else {
      return leftValue.equals(rightValue);
    }
  }

  private boolean isLessThan(JsonNode leftValue, JsonNode rightValue) {
    JsonNodeType type = leftValue.getNodeType();
    if (type == JsonNodeType.STRING) {
      return leftValue.asText().compareTo(rightValue.asText()) < 0;
    } else if (type == JsonNodeType.NUMBER) {
      return leftValue.decimalValue().compareTo(rightValue.decimalValue()) < 0;
    } else {
      throw new ContractContextException(Constants.ILLEGAL_ARGUMENT);
    }
  }

  private boolean isGreaterThan(JsonNode leftValue, JsonNode rightValue) {
    JsonNodeType type = leftValue.getNodeType();
    if (type == JsonNodeType.STRING) {
      return leftValue.asText().compareTo(rightValue.asText()) > 0;
    } else if (type == JsonNodeType.NUMBER) {
      return leftValue.decimalValue().compareTo(rightValue.decimalValue()) > 0;
    } else {
      throw new ContractContextException(Constants.ILLEGAL_ARGUMENT);
    }
  }

  private boolean isNull(JsonNode record, String column) {
    return !record.has(column) || record.get(column).isNull();
  }

  private String getAssetIdForTable(String tableName) {
    return Constants.PREFIX_TABLE + tableName;
  }

  private String getAssetIdForRecord(String tableName, String keyColumnName, String key) {
    return Constants.PREFIX_RECORD
        + tableName
        + Constants.ASSET_ID_SEPARATOR
        + keyColumnName
        + Constants.ASSET_ID_SEPARATOR
        + key;
  }

  private String getAssetIdForIndex(String tableName, JsonNode condition) {
    String indexKey = condition.get(Constants.CONDITION_COLUMN).textValue();
    JsonNode indexValue = condition.get(Constants.CONDITION_VALUE);
    String operator = condition.get(Constants.CONDITION_OPERATOR).textValue().toUpperCase();

    if (operator.equals(Constants.OPERATOR_IS_NULL)) {
      return getAssetIdForNullIndex(tableName, indexKey);
    } else {
      return getAssetIdForIndex(tableName, indexKey, indexValue.asText());
    }
  }

  private String getAssetIdForIndex(String tableName, String indexKey, String value) {
    return Constants.PREFIX_INDEX
        + tableName
        + Constants.ASSET_ID_SEPARATOR
        + indexKey
        + Constants.ASSET_ID_SEPARATOR
        + value;
  }

  private String getAssetIdForNullIndex(String tableName, String indexKey) {
    return Constants.PREFIX_INDEX + tableName + Constants.ASSET_ID_SEPARATOR + indexKey;
  }
}
