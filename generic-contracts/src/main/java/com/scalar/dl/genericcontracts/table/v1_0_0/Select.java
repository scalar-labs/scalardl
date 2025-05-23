package com.scalar.dl.genericcontracts.table.v1_0_0;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.Nullable;

public class Select extends JacksonBasedContract {

  @Nullable
  @Override
  public JsonNode invoke(
      Ledger<JsonNode> ledger, JsonNode arguments, @Nullable JsonNode properties) {

    // Check the arguments
    validateQuery(arguments);

    // Prepare conditions for each table
    JsonNode leftmostTable = arguments.get(Constants.QUERY_TABLE);
    String leftmostTableReference = getTableReference(leftmostTable);
    ListMultimap<String, JsonNode> conditionsMap =
        prepareConditionsMap(arguments.get(Constants.QUERY_CONDITIONS), leftmostTableReference);

    // Prepare joins
    List<JsonNode> joins = new ArrayList<>();
    if (arguments.has(Constants.QUERY_JOINS)) {
      arguments.get(Constants.QUERY_JOINS).forEach(joins::add);
    }

    // Prepare projections
    List<String> projections = new ArrayList<>();
    if (arguments.has(Constants.QUERY_PROJECTIONS)) {
      arguments
          .get(Constants.QUERY_PROJECTIONS)
          .forEach(
              projection -> {
                if (joins.isEmpty()) {
                  // Remove the table reference if it exists in a non-join query
                  projections.add(getColumnName(projection.asText()));
                } else {
                  // Projection columns in a join query should always have the table reference
                  projections.add(projection.asText());
                }
              });
    }

    // Scan the leftmost table
    JsonNode records =
        invokeSubContract(
            Constants.CONTRACT_SCAN,
            ledger,
            prepareScanArguments(
                getTableName(leftmostTable),
                leftmostTableReference,
                conditionsMap,
                !joins.isEmpty()));

    // Join tables
    for (JsonNode join : joins) {
      ArrayNode joinedRecords = getObjectMapper().createArrayNode();

      for (JsonNode leftRecord : records) {
        // Get right records whose join key matches that of the left record
        JsonNode rightTable = join.get(Constants.JOIN_TABLE);
        JsonNode rightRecords = scanRightTable(ledger, join, leftRecord, rightTable, conditionsMap);
        for (JsonNode rightRecord : rightRecords) {
          joinedRecords.add(join(leftRecord, rightRecord));
        }
      }

      records = joinedRecords;
    }

    // Project records
    return project(records, projections);
  }

  private JsonNode scanRightTable(
      Ledger<JsonNode> ledger,
      JsonNode join,
      JsonNode leftRecord,
      JsonNode rightTable,
      ListMultimap<String, JsonNode> conditionsMap) {
    String leftKey = join.get(Constants.JOIN_LEFT_KEY).asText();
    String rightTableName = getTableName(rightTable);
    String rightTableReference = getTableReference(rightTable);
    JsonNode rightTableMetadata = getTableMetadata(ledger, rightTableName);
    String rightKey = join.get(Constants.JOIN_RIGHT_KEY).asText();
    String rightKeyColumnName = getColumnName(rightKey);
    String rightKeyColumnType = getColumnType(rightTableMetadata, rightKeyColumnName);

    if (!leftRecord.has(leftKey)
        || leftRecord.get(leftKey).isNull()
        || !leftRecord.get(leftKey).getNodeType().name().equalsIgnoreCase(rightKeyColumnType)) {
      return getObjectMapper().createArrayNode();
    }

    // Prepare scan arguments
    JsonNode joinCondition =
        getObjectMapper()
            .createObjectNode()
            .put(Constants.CONDITION_COLUMN, rightKeyColumnName)
            .put(Constants.CONDITION_OPERATOR, Constants.OPERATOR_EQ)
            .set(Constants.CONDITION_VALUE, leftRecord.get(leftKey));

    return invokeSubContract(
        Constants.CONTRACT_SCAN,
        ledger,
        prepareScanArguments(
            rightTableName, rightTableReference, conditionsMap, true, joinCondition));
  }

  private JsonNode join(JsonNode leftRecord, JsonNode rightRecord) {
    ObjectNode joinedRecord = getObjectMapper().createObjectNode();
    Iterator<Entry<String, JsonNode>> leftColumns = leftRecord.fields();
    Iterator<Entry<String, JsonNode>> rightColumns = rightRecord.fields();

    while (leftColumns.hasNext()) {
      Map.Entry<String, JsonNode> column = leftColumns.next();
      joinedRecord.set(column.getKey(), column.getValue());
    }

    while (rightColumns.hasNext()) {
      Map.Entry<String, JsonNode> column = rightColumns.next();
      joinedRecord.set(column.getKey(), column.getValue());
    }

    return joinedRecord;
  }

  private JsonNode project(JsonNode records, List<String> projections) {
    if (projections.isEmpty()) {
      return records;
    } else {
      ArrayNode projectedResults = getObjectMapper().createArrayNode();
      for (JsonNode record : records) {
        projectedResults.add(projectRecord(record, projections));
      }
      return projectedResults;
    }
  }

  private JsonNode projectRecord(JsonNode record, List<String> projections) {
    ObjectNode newRecord = getObjectMapper().createObjectNode();
    for (String projection : projections) {
      if (record.has(projection)) {
        newRecord.set(projection, record.get(projection));
      }
    }
    return newRecord;
  }

  private ListMultimap<String, JsonNode> prepareConditionsMap(
      JsonNode conditions, String defaultTableReference) {
    ListMultimap<String, JsonNode> conditionsMap = ArrayListMultimap.create();
    conditions.forEach(
        condition -> {
          String column = condition.get(Constants.CONDITION_COLUMN).asText();
          if (isColumnReference(column)) {
            String tableReference = getTableReference(column);
            String columnName = getColumnName(column);
            ObjectNode newCondition = getObjectMapper().createObjectNode();
            newCondition.put(Constants.CONDITION_COLUMN, columnName);
            newCondition.set(Constants.CONDITION_VALUE, condition.get(Constants.CONDITION_VALUE));
            newCondition.set(
                Constants.CONDITION_OPERATOR, condition.get(Constants.CONDITION_OPERATOR));
            conditionsMap.put(tableReference, newCondition);
          } else {
            conditionsMap.put(defaultTableReference, condition);
          }
        });
    return conditionsMap;
  }

  private JsonNode prepareScanArguments(
      String tableName,
      String tableReference,
      ListMultimap<String, JsonNode> conditionsMap,
      boolean forJoin) {
    return prepareScanArguments(tableName, tableReference, conditionsMap, forJoin, null);
  }

  private JsonNode prepareScanArguments(
      String tableName,
      String tableReference,
      ListMultimap<String, JsonNode> conditionsMap,
      boolean forJoin,
      @Nullable JsonNode joinCondition) {
    ObjectNode scan = getObjectMapper().createObjectNode().put(Constants.QUERY_TABLE, tableName);

    ArrayNode conditionArray = getObjectMapper().createArrayNode();
    if (joinCondition != null) {
      conditionArray.add(joinCondition);
    }
    conditionsMap.get(tableReference).forEach(conditionArray::add);
    scan.set(Constants.QUERY_CONDITIONS, conditionArray);

    if (forJoin) {
      // Add table reference for each column of the result records if the scan is for a join
      scan.set(
          Constants.SCAN_OPTIONS,
          getObjectMapper()
              .createObjectNode()
              .put(Constants.SCAN_OPTIONS_TABLE_REFERENCE, tableReference));
    }

    return scan;
  }

  private JsonNode getTableMetadata(Ledger<JsonNode> ledger, String tableName) {
    String tableAssetId = getAssetIdForTable(tableName);
    return ledger
        .get(tableAssetId)
        .orElseThrow(() -> new ContractContextException(Constants.TABLE_NOT_EXIST))
        .data();
  }

  private Set<String> getJoinTableReferences(JsonNode joins) {
    Set<String> tables = new HashSet<>();

    for (JsonNode join : joins) {
      JsonNode table = join.get(Constants.JOIN_TABLE);
      tables.add(getTableReference(table));
    }

    return tables;
  }

  private boolean isColumnName(String column) {
    return Constants.COLUMN_NAME.matcher(column).matches();
  }

  private boolean isColumnReference(String column) {
    return Constants.COLUMN_REFERENCE.matcher(column).matches();
  }

  private String getTableName(JsonNode table) {
    return table.isTextual() ? table.asText() : table.get(Constants.ALIAS_NAME).asText();
  }

  private String getTableReference(JsonNode table) {
    return table.isTextual() ? table.asText() : table.get(Constants.ALIAS_AS).asText();
  }

  private String getTableReference(String column) {
    return column.substring(0, column.indexOf(Constants.COLUMN_SEPARATOR));
  }

  private String getColumnName(String column) {
    return isColumnReference(column)
        ? column.substring(column.indexOf(Constants.COLUMN_SEPARATOR) + 1)
        : column;
  }

  private String getColumnType(JsonNode tableMetadata, String columnName) {
    if (tableMetadata.get(Constants.TABLE_KEY).asText().equals(columnName)) {
      return tableMetadata.get(Constants.TABLE_KEY_TYPE).asText();
    }

    for (JsonNode index : tableMetadata.get(Constants.TABLE_INDEXES)) {
      if (index.get(Constants.INDEX_KEY).asText().equals(columnName)) {
        return index.get(Constants.INDEX_KEY_TYPE).asText();
      }
    }

    throw new ContractContextException(Constants.INVALID_JOIN_COLUMN + columnName);
  }

  private String getAssetIdForTable(String tableName) {
    return Constants.PREFIX_TABLE + tableName;
  }

  private void validateQuery(JsonNode arguments) {
    // Check the required fields
    if (!(arguments.size() >= 2 && arguments.size() <= 4)
        || !arguments.has(Constants.QUERY_TABLE)
        || !arguments.has(Constants.QUERY_CONDITIONS)) {
      throw new ContractContextException(Constants.INVALID_QUERY_FORMAT);
    }

    // Check the leftmost table
    validateTable(arguments.get(Constants.QUERY_TABLE));
    Set<String> tablesReferences = new HashSet<>();
    String leftmostTableReference = getTableReference(arguments.get(Constants.QUERY_TABLE));
    tablesReferences.add(leftmostTableReference);

    // Check joins
    if (arguments.has(Constants.QUERY_JOINS)) {
      validateJoins(leftmostTableReference, arguments.get(Constants.QUERY_JOINS));
      tablesReferences.addAll(getJoinTableReferences(arguments.get(Constants.QUERY_JOINS)));
    }

    // Check conditions
    validateConditions(arguments.get(Constants.QUERY_CONDITIONS), tablesReferences);

    // Check projections
    if (arguments.has(Constants.QUERY_PROJECTIONS)) {
      validateProjections(arguments.get(Constants.QUERY_PROJECTIONS), tablesReferences);
    }
  }

  private void validateColumn(String column, Set<String> tableReferences) {
    boolean hasTableReference = isColumnReference(column);

    if (!hasTableReference) {
      // Join queries must have a table reference
      if ((tableReferences.size() == 1 && !isColumnName(column)) || tableReferences.size() > 1) {
        throw new ContractContextException(Constants.INVALID_COLUMN_FORMAT + column);
      }
      return;
    }

    // Check if the table of the column is appeared in the query
    String tableReference = getTableReference(column);
    if (!tableReferences.contains(tableReference)) {
      throw new ContractContextException(Constants.UNKNOWN_TABLE + tableReference);
    }
  }

  private void validateTable(JsonNode table) {
    if ((!table.isTextual() && !table.isObject())
        || (table.isObject()
            && (table.size() != 2
                || !table.has(Constants.ALIAS_NAME)
                || !table.get(Constants.ALIAS_NAME).isTextual()
                || !table.has(Constants.ALIAS_AS)
                || !table.get(Constants.ALIAS_AS).isTextual()))) {
      throw new ContractContextException(Constants.INVALID_QUERY_TABLE_FORMAT + table);
    }
  }

  private void validateJoins(String leftmostTableReference, JsonNode joins) {
    Set<String> seenTables = new HashSet<>();
    seenTables.add(leftmostTableReference);

    if (!joins.isArray()) {
      throw new ContractContextException(Constants.INVALID_QUERY_FORMAT);
    }

    for (JsonNode join : joins) {
      // Check the join object format
      if (!join.isObject()
          || join.size() != 3
          || !join.has(Constants.JOIN_TABLE)
          || !join.has(Constants.JOIN_LEFT_KEY)
          || !join.has(Constants.JOIN_RIGHT_KEY)) {
        throw new ContractContextException(Constants.INVALID_JOIN_FORMAT + join);
      }

      // Check the join table format
      JsonNode table = join.get(Constants.JOIN_TABLE);
      validateTable(table);
      String tableReference = getTableReference(table);
      if (seenTables.contains(tableReference)) {
        throw new ContractContextException(Constants.TABLE_AMBIGUOUS + tableReference);
      }

      // Check the join key format
      JsonNode leftKey = join.get(Constants.JOIN_LEFT_KEY);
      JsonNode rightKey = join.get(Constants.JOIN_RIGHT_KEY);
      if (!leftKey.isTextual()
          || !rightKey.isTextual()
          || !isColumnReference(leftKey.asText())
          || !isColumnReference(rightKey.asText())) {
        throw new ContractContextException(Constants.INVALID_JOIN_FORMAT + join);
      }

      // Check the table existence for the join key
      String leftTable = getTableReference(leftKey.asText());
      String rightTable = getTableReference(rightKey.asText());
      if (!seenTables.contains(leftTable) || !tableReference.equals(rightTable)) {
        throw new ContractContextException(Constants.INVALID_JOIN_FORMAT + join);
      }

      seenTables.add(tableReference);
    }
  }

  private void validateConditions(JsonNode conditions, Set<String> tableReferences) {
    if (!conditions.isArray()) {
      throw new ContractContextException(Constants.INVALID_QUERY_FORMAT);
    }

    for (JsonNode condition : conditions) {
      if (!condition.isObject()
          || !condition.has(Constants.CONDITION_COLUMN)
          || !condition.get(Constants.CONDITION_COLUMN).isTextual()
          || !condition.has(Constants.CONDITION_OPERATOR)
          || !condition.get(Constants.CONDITION_OPERATOR).isTextual()) {
        throw new ContractContextException(Constants.INVALID_CONDITION_FORMAT + condition);
      }

      // Check the column
      validateColumn(condition.get(Constants.CONDITION_COLUMN).asText(), tableReferences);

      // Check the operator
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

  private void validateProjections(JsonNode projections, Set<String> tableReferences) {
    if (!projections.isArray()) {
      throw new ContractContextException(Constants.INVALID_QUERY_FORMAT);
    }

    for (JsonNode projection : projections) {
      if (!projection.isTextual()) {
        throw new ContractContextException(Constants.INVALID_PROJECTION_FORMAT + projection);
      }

      validateColumn(projection.asText(), tableReferences);
    }
  }

  @VisibleForTesting
  JsonNode invokeSubContract(String contractId, Ledger<JsonNode> ledger, JsonNode arguments) {
    return invoke(contractId, ledger, arguments);
  }
}
