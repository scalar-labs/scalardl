package com.scalar.dl.genericcontracts.table.v1_0_0;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

public class Update extends JacksonBasedContract {

  @Nullable
  @Override
  public JsonNode invoke(
      Ledger<JsonNode> ledger, JsonNode arguments, @Nullable JsonNode properties) {

    // Check the arguments
    if (arguments.size() != 3
        || !arguments.has(Constants.UPDATE_TABLE)
        || !arguments.get(Constants.UPDATE_TABLE).isTextual()
        || !arguments.has(Constants.UPDATE_VALUES)
        || !arguments.get(Constants.UPDATE_VALUES).isObject()
        || !arguments.has(Constants.UPDATE_CONDITIONS)
        || !arguments.get(Constants.UPDATE_CONDITIONS).isArray()) {
      throw new ContractContextException(Constants.INVALID_UPDATE_FORMAT);
    }
    JsonNode values = arguments.get(Constants.UPDATE_VALUES);

    String tableName = arguments.get(Constants.UPDATE_TABLE).asText();
    if (!isSupportedObjectName(tableName)) {
      throw new ContractContextException(Constants.INVALID_OBJECT_NAME + tableName);
    }

    // Get the table information
    String tableAssetId = getAssetId(ledger, Constants.PREFIX_TABLE, TextNode.valueOf(tableName));
    Asset<JsonNode> tableAsset =
        ledger
            .get(tableAssetId)
            .orElseThrow(() -> new ContractContextException(Constants.TABLE_NOT_EXIST + tableName));
    String key = tableAsset.data().get(Constants.TABLE_KEY).asText();

    // Prepare index map
    Map<String, String> indexes = new HashMap<>();
    tableAsset
        .data()
        .get(Constants.TABLE_INDEXES)
        .forEach(
            index ->
                indexes.put(
                    index.get(Constants.INDEX_KEY).asText(),
                    index.get(Constants.INDEX_KEY_TYPE).asText()));

    // Check the specified values
    validateValues(values, key, indexes);

    // Scan target records
    ObjectNode scan = getObjectMapper().createObjectNode();
    scan.put(Constants.QUERY_TABLE, tableName);
    scan.set(Constants.QUERY_CONDITIONS, arguments.get(Constants.UPDATE_CONDITIONS));
    scan.set(
        Constants.SCAN_OPTIONS,
        getObjectMapper().createObjectNode().put(Constants.SCAN_OPTIONS_INCLUDE_METADATA, true));
    JsonNode records = invokeSubContract(Constants.CONTRACT_SCAN, ledger, scan);

    // A map from old and new index asset IDs to a list of index entries
    ListMultimap<String, JsonNode> indexEntriesMap = ArrayListMultimap.create();

    for (JsonNode record : records) {
      ObjectNode newRecord = record.deepCopy();
      // Prepare new record while adding index entries to the map
      values
          .properties()
          .forEach(
              entry -> {
                String column = entry.getKey();
                JsonNode newValue = entry.getValue();
                if (indexes.containsKey(column)) {
                  addIndexEntries(
                      ledger, indexEntriesMap, record, tableName, key, column, newValue);
                }
                newRecord.set(column, newValue);
              });

      // Update record after removing internal metadata
      if (!newRecord.equals(record)) {
        String assetId =
            getAssetId(
                ledger,
                Constants.PREFIX_RECORD,
                TextNode.valueOf(tableName),
                TextNode.valueOf(key),
                newRecord.get(key));
        newRecord.remove(Constants.SCAN_METADATA_AGE);
        ledger.put(assetId, newRecord);
      }
    }

    // Put index assets
    indexEntriesMap
        .asMap()
        .forEach(
            (indexAssetId, indexEntries) -> {
              ArrayNode indexEntriesJson = getObjectMapper().createArrayNode();
              indexEntries.forEach(indexEntriesJson::add);
              ledger.put(indexAssetId, indexEntriesJson);
            });

    return null;
  }

  private void validateValues(JsonNode values, String primaryKey, Map<String, String> indexes) {
    values
        .properties()
        .forEach(
            entry -> {
              String column = entry.getKey();
              JsonNode value = entry.getValue();

              if (!isSupportedObjectName(column)) {
                throw new ContractContextException(Constants.INVALID_OBJECT_NAME + column);
              }

              if (column.equals(primaryKey)) {
                throw new ContractContextException(Constants.CANNOT_UPDATE_KEY);
              }

              if (indexes.containsKey(column)
                  && !value.isNull()
                  && !indexes.get(column).equalsIgnoreCase(value.getNodeType().name())) {
                throw new ContractContextException(
                    Constants.INVALID_INDEX_KEY_TYPE + value.getNodeType().name());
              }
            });
  }

  private void addIndexEntries(
      Ledger<JsonNode> ledger,
      ListMultimap<String, JsonNode> indexEntriesMap,
      JsonNode record,
      String tableName,
      String primaryKey,
      String indexKey,
      JsonNode value) {
    JsonNode oldIndexValue =
        record.has(indexKey) && !record.get(indexKey).isNull()
            ? record.get(indexKey)
            : NullNode.getInstance();
    String oldIndexAssetId = getAssetIdForIndex(ledger, tableName, indexKey, oldIndexValue);
    JsonNode newIndexValue = value.isNull() ? NullNode.getInstance() : value;
    String newIndexAssetId = getAssetIdForIndex(ledger, tableName, indexKey, newIndexValue);
    int nextAge = record.get(Constants.SCAN_METADATA_AGE).intValue() + 1;

    if (!oldIndexAssetId.equals(newIndexAssetId)) {
      indexEntriesMap.put(
          oldIndexAssetId, createIndexEntry(primaryKey, record.get(primaryKey), nextAge, true));
      indexEntriesMap.put(
          newIndexAssetId, createIndexEntry(primaryKey, record.get(primaryKey), nextAge, false));
    }
  }

  private JsonNode createIndexEntry(String key, JsonNode value, int nextAge, boolean deleted) {
    ObjectNode indexEntry = getObjectMapper().createObjectNode();
    indexEntry.set(key, value);
    indexEntry.put(Constants.INDEX_ASSET_ADDED_AGE, nextAge);
    if (deleted) {
      indexEntry.put(Constants.INDEX_ASSET_DELETE_MARKER, true);
    }
    return indexEntry;
  }

  private String getAssetIdForIndex(
      Ledger<JsonNode> ledger, String tableName, String indexKey, JsonNode indexValue) {
    return getAssetId(
        ledger,
        Constants.PREFIX_INDEX,
        TextNode.valueOf(tableName),
        TextNode.valueOf(indexKey),
        indexValue);
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
    return invokeSubContract(Constants.CONTRACT_GET_ASSET_ID, ledger, arguments).asText();
  }

  private boolean isSupportedObjectName(String name) {
    return Constants.OBJECT_NAME.matcher(name).matches();
  }

  @VisibleForTesting
  JsonNode invokeSubContract(String contractId, Ledger<JsonNode> ledger, JsonNode arguments) {
    return invoke(contractId, ledger, arguments);
  }
}
