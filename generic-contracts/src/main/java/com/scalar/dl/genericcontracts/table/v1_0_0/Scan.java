package com.scalar.dl.genericcontracts.table.v1_0_0;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.AssetFilter.AgeOrder;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * `Scan` is a generic contract for internal use and is invoked by `Select` and `Update` contracts.
 * It takes a table name, a list of conditions, and options as arguments. In `Scan`, we do not check
 * the arguments aggressively except for the conditions. Make sure to validate them in the caller.
 */
public class Scan extends JacksonBasedContract {

  @Nullable
  @Override
  public JsonNode invoke(
      Ledger<JsonNode> ledger, JsonNode arguments, @Nullable JsonNode properties) {

    // Get the table information
    String tableName = arguments.get(Constants.QUERY_TABLE).asText();
    String tableAssetId = getAssetId(ledger, Constants.PREFIX_TABLE, TextNode.valueOf(tableName));
    JsonNode table =
        ledger
            .get(tableAssetId)
            .orElseThrow(() -> new ContractContextException(Constants.TABLE_NOT_EXIST + tableName))
            .data();

    validateConditions(arguments.get(Constants.QUERY_CONDITIONS));
    ListMultimap<String, JsonNode> conditionsMap =
        Multimaps.index(
            arguments.get(Constants.QUERY_CONDITIONS),
            condition -> condition.get(Constants.CONDITION_COLUMN).textValue());

    String tableReference = null;
    boolean includeMetadata = false;
    if (arguments.has(Constants.SCAN_OPTIONS)) {
      JsonNode options = arguments.get(Constants.SCAN_OPTIONS);
      tableReference =
          options.has(Constants.SCAN_OPTIONS_TABLE_REFERENCE)
              ? options.get(Constants.SCAN_OPTIONS_TABLE_REFERENCE).asText()
              : null;
      includeMetadata =
          options.has(Constants.SCAN_OPTIONS_INCLUDE_METADATA)
              && options.get(Constants.SCAN_OPTIONS_INCLUDE_METADATA).asBoolean();
    }

    // Scan records
    if (hasPrimaryKeyCondition(table, conditionsMap)) {
      return get(ledger, table, tableReference, conditionsMap, includeMetadata);
    } else if (hasIndexKeyCondition(table, conditionsMap)) {
      return scan(ledger, table, tableReference, conditionsMap, includeMetadata);
    } else {
      throw new ContractContextException(Constants.INVALID_KEY_SPECIFICATION);
    }
  }

  private void validateConditions(JsonNode conditions) {
    for (JsonNode condition : conditions) {
      if (!condition.isObject()
          || !condition.has(Constants.CONDITION_COLUMN)
          || !condition.get(Constants.CONDITION_COLUMN).isTextual()
          || !condition.has(Constants.CONDITION_OPERATOR)
          || !condition.get(Constants.CONDITION_OPERATOR).isTextual()) {
        throw new ContractContextException(Constants.INVALID_CONDITION_FORMAT + condition);
      }

      String column = condition.get(Constants.CONDITION_COLUMN).asText();
      if (!isSupportedObjectName(column)) {
        throw new ContractContextException(Constants.INVALID_OBJECT_NAME + column);
      }

      String operator = condition.get(Constants.CONDITION_OPERATOR).asText();
      if (!isSupportedOperator(operator)) {
        throw new ContractContextException(Constants.INVALID_OPERATOR + condition);
      }

      if (operator.equalsIgnoreCase(Constants.OPERATOR_IS_NULL)
          || operator.equalsIgnoreCase(Constants.OPERATOR_IS_NOT_NULL)) {
        // For IS_NULL or IS_NOT_NULL
        if (condition.size() != 2) {
          throw new ContractContextException(Constants.INVALID_CONDITION_FORMAT + condition);
        }
      } else {
        // For other operators
        if (condition.size() != 3
            || !condition.has(Constants.CONDITION_VALUE)
            || !isSupportedDataTypeForComparisonOperators(
                condition.get(Constants.CONDITION_VALUE).getNodeType().name())) {
          throw new ContractContextException(Constants.INVALID_CONDITION_FORMAT + condition);
        }
        if (condition.get(Constants.CONDITION_VALUE).isBoolean()
            && (!operator.equalsIgnoreCase(Constants.OPERATOR_EQ)
                && !operator.equalsIgnoreCase(Constants.OPERATOR_NE))) {
          throw new ContractContextException(Constants.INVALID_OPERATOR + condition);
        }
      }
    }
  }

  private boolean isSupportedObjectName(String name) {
    return Constants.OBJECT_NAME.matcher(name).matches();
  }

  private boolean isSupportedDataTypeForComparisonOperators(String type) {
    return type.toUpperCase().equals(JsonNodeType.BOOLEAN.name())
        || type.toUpperCase().equals(JsonNodeType.NUMBER.name())
        || type.toUpperCase().equals(JsonNodeType.STRING.name());
  }

  private boolean isSupportedOperator(String operator) {
    return operator.equalsIgnoreCase(Constants.OPERATOR_EQ)
        || operator.equalsIgnoreCase(Constants.OPERATOR_NE)
        || operator.equalsIgnoreCase(Constants.OPERATOR_LT)
        || operator.equalsIgnoreCase(Constants.OPERATOR_LTE)
        || operator.equalsIgnoreCase(Constants.OPERATOR_GT)
        || operator.equalsIgnoreCase(Constants.OPERATOR_GTE)
        || operator.equalsIgnoreCase(Constants.OPERATOR_IS_NULL)
        || operator.equalsIgnoreCase(Constants.OPERATOR_IS_NOT_NULL);
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
    String givenType = condition.get(Constants.CONDITION_VALUE).getNodeType().name();
    if (!givenType.equalsIgnoreCase(keyType)) {
      throw new ContractContextException(Constants.INVALID_KEY_TYPE + givenType);
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

    String givenType = condition.get(Constants.CONDITION_VALUE).getNodeType().name();
    if (!givenType.equalsIgnoreCase(keyType)) {
      throw new ContractContextException(Constants.INVALID_INDEX_KEY_TYPE + givenType);
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

  private Set<String> getRecordAssetIdsFromIndex(
      Ledger<JsonNode> ledger, String tableName, String indexAssetId, String keyColumnName) {
    AssetFilter filter = new AssetFilter(indexAssetId);
    List<Asset<JsonNode>> indexAssets = ledger.scan(filter.withAgeOrder(AgeOrder.ASC));
    Set<String> assetIds = new HashSet<>();

    for (Asset<JsonNode> indexAsset : indexAssets) {
      if (!indexAsset.data().isArray()) {
        throw new ContractContextException(Constants.ILLEGAL_INDEX_STATE);
      }

      for (JsonNode indexEntry : indexAsset.data()) {
        if (!indexEntry.has(keyColumnName)) {
          throw new ContractContextException(Constants.ILLEGAL_INDEX_STATE);
        }

        String assetId =
            getAssetId(
                ledger,
                Constants.PREFIX_RECORD,
                TextNode.valueOf(tableName),
                TextNode.valueOf(keyColumnName),
                indexEntry.get(keyColumnName));
        if (indexEntry.has(Constants.INDEX_ASSET_DELETE_MARKER)
            && indexEntry.get(Constants.INDEX_ASSET_DELETE_MARKER).asBoolean()) {
          assetIds.remove(assetId);
        } else {
          assetIds.add(assetId);
        }
      }
    }

    return assetIds;
  }

  private ArrayNode get(
      Ledger<JsonNode> ledger,
      JsonNode table,
      @Nullable String tableReference,
      ListMultimap<String, JsonNode> conditionsMap,
      boolean includeMetadata) {
    String tableName = table.get(Constants.TABLE_NAME).textValue();
    JsonNode condition = getKeyColumnCondition(table, conditionsMap);
    String assetId =
        getAssetId(
            ledger,
            Constants.PREFIX_RECORD,
            TextNode.valueOf(tableName),
            condition.get(Constants.CONDITION_COLUMN),
            condition.get(Constants.CONDITION_VALUE));
    ArrayNode results = getObjectMapper().createArrayNode();

    ledger.get(assetId).ifPresent(asset -> results.add(prepareRecord(asset, includeMetadata)));

    return filter(
        results,
        conditionsMap.values().stream()
            .filter(c -> !c.equals(condition))
            .collect(Collectors.toList()),
        tableReference);
  }

  private ArrayNode scan(
      Ledger<JsonNode> ledger,
      JsonNode table,
      @Nullable String tableReference,
      ListMultimap<String, JsonNode> conditionsMap,
      boolean includeMetadata) {
    String tableName = table.get(Constants.TABLE_NAME).textValue();
    String keyColumnName = table.get(Constants.TABLE_KEY).textValue();
    JsonNode condition = getIndexColumnCondition(table, conditionsMap);
    String indexAssetId = getAssetIdForIndex(ledger, tableName, condition);
    ArrayNode results = getObjectMapper().createArrayNode();

    for (String recordAssetId :
        getRecordAssetIdsFromIndex(ledger, tableName, indexAssetId, keyColumnName)) {
      Asset<JsonNode> asset =
          ledger
              .get(recordAssetId)
              .orElseThrow(() -> new ContractContextException(Constants.ILLEGAL_INDEX_STATE));
      results.add(prepareRecord(asset, includeMetadata));
    }

    return filter(
        results,
        conditionsMap.values().stream()
            .filter(c -> !c.equals(condition))
            .collect(Collectors.toList()),
        tableReference);
  }

  private String addTableReference(String tableReference, String columnName) {
    return tableReference + Constants.COLUMN_SEPARATOR + columnName;
  }

  private JsonNode addTableReferenceToColumns(JsonNode record, String tableReference) {
    ObjectNode renamed = getObjectMapper().createObjectNode();
    Set<Entry<String, JsonNode>> columns = record.properties();

    for (Entry<String, JsonNode> column : columns) {
      renamed.set(addTableReference(tableReference, column.getKey()), column.getValue());
    }

    return renamed;
  }

  private JsonNode prepareRecord(Asset<JsonNode> asset, boolean includeMetadata) {
    if (includeMetadata) {
      ObjectNode record = asset.data().deepCopy();
      record.put(Constants.SCAN_METADATA_AGE, asset.age());
      return record;
    } else {
      return asset.data();
    }
  }

  private ArrayNode filter(
      ArrayNode records, List<JsonNode> conditions, @Nullable String tableReference) {
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
        results.add(
            tableReference == null ? record : addTableReferenceToColumns(record, tableReference));
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

  private String getAssetIdForIndex(Ledger<JsonNode> ledger, String tableName, JsonNode condition) {
    String indexKey = condition.get(Constants.CONDITION_COLUMN).textValue();
    JsonNode indexValue = condition.get(Constants.CONDITION_VALUE);
    String operator = condition.get(Constants.CONDITION_OPERATOR).textValue().toUpperCase();
    return getAssetId(
        ledger,
        Constants.PREFIX_INDEX,
        TextNode.valueOf(tableName),
        TextNode.valueOf(indexKey),
        (operator.equals(Constants.OPERATOR_IS_NULL) ? NullNode.getInstance() : indexValue));
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
