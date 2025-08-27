package com.scalar.dl.genericcontracts.table.v1_0_0;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.scalar.dl.genericcontracts.table.Constants;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

public class Create extends JacksonBasedContract {

  @Nullable
  @Override
  public JsonNode invoke(
      Ledger<JsonNode> ledger, JsonNode arguments, @Nullable JsonNode properties) {

    // Check the arguments
    if (arguments.size() < 3
        || arguments.size() > 4
        || !arguments.has(Constants.TABLE_NAME)
        || !arguments.get(Constants.TABLE_NAME).isTextual()
        || !arguments.has(Constants.TABLE_KEY)
        || !arguments.get(Constants.TABLE_KEY).isTextual()
        || !arguments.has(Constants.TABLE_KEY_TYPE)
        || !arguments.get(Constants.TABLE_KEY_TYPE).isTextual()) {
      throw new ContractContextException(Constants.INVALID_TABLE_FORMAT);
    }

    String tableName = arguments.get(Constants.TABLE_NAME).asText();
    if (!isSupportedObjectName(tableName)) {
      throw new ContractContextException(Constants.INVALID_OBJECT_NAME + tableName);
    }

    String key = arguments.get(Constants.TABLE_KEY).asText();
    if (!isSupportedObjectName(key)) {
      throw new ContractContextException(Constants.INVALID_OBJECT_NAME + key);
    }

    String keyType = arguments.get(Constants.TABLE_KEY_TYPE).asText();
    if (!isSupportedKeyType(keyType)) {
      throw new ContractContextException(Constants.INVALID_KEY_TYPE + keyType);
    }

    if (arguments.has(Constants.TABLE_INDEXES)) {
      validateIndexes(arguments.get(Constants.TABLE_INDEXES));
    }

    // Get the table existence
    String assetId = getAssetIdForTable(ledger, tableName);
    Optional<Asset<JsonNode>> asset = ledger.get(assetId);
    if (asset.isPresent()) {
      throw new ContractContextException(Constants.TABLE_ALREADY_EXISTS);
    }

    // Put the asset records
    JsonNode tableMetadata = prepareTableMetadata(arguments);
    ledger.put(Constants.ASSET_ID_METADATA_TABLES, tableMetadata);
    ledger.put(assetId, tableMetadata);

    return null;
  }

  private boolean isSupportedKeyType(String type) {
    return type.toUpperCase().equals(JsonNodeType.BOOLEAN.name())
        || type.toUpperCase().equals(JsonNodeType.NUMBER.name())
        || type.toUpperCase().equals(JsonNodeType.STRING.name());
  }

  private void validateIndexes(JsonNode indexes) {
    if (!indexes.isArray()) {
      throw new ContractContextException(Constants.INVALID_INDEX_FORMAT);
    }

    Set<String> seenColumns = new HashSet<>();
    for (JsonNode index : indexes) {
      if (!index.isObject()
          || index.size() != 2
          || !index.has(Constants.INDEX_KEY)
          || !index.get(Constants.INDEX_KEY).isTextual()
          || !index.has(Constants.INDEX_KEY_TYPE)
          || !index.get(Constants.INDEX_KEY_TYPE).isTextual()) {
        throw new ContractContextException(Constants.INVALID_INDEX_FORMAT);
      }

      String indexKey = index.get(Constants.INDEX_KEY).asText();
      if (!isSupportedObjectName(indexKey)) {
        throw new ContractContextException(Constants.INVALID_OBJECT_NAME + indexKey);
      }

      String indexKeyType = index.get(Constants.INDEX_KEY_TYPE).asText();
      if (!isSupportedKeyType(indexKeyType)) {
        throw new ContractContextException(Constants.INVALID_INDEX_KEY_TYPE + indexKeyType);
      }

      if (seenColumns.contains(indexKey)) {
        throw new ContractContextException(Constants.COLUMN_AMBIGUOUS + indexKey);
      }
      seenColumns.add(indexKey);
    }
  }

  private boolean isSupportedObjectName(String name) {
    return Constants.OBJECT_NAME.matcher(name).matches();
  }

  private JsonNode prepareTableMetadata(JsonNode arguments) {
    if (arguments.has(Constants.TABLE_INDEXES)) {
      return arguments;
    } else {
      ObjectNode tableMetadata = arguments.deepCopy();
      tableMetadata.set(Constants.TABLE_INDEXES, getObjectMapper().createArrayNode());
      return tableMetadata;
    }
  }

  @VisibleForTesting
  String getAssetIdForTable(Ledger<JsonNode> ledger, String tableName) {
    JsonNode arguments =
        getObjectMapper()
            .createObjectNode()
            .put(Constants.ASSET_ID_PREFIX, Constants.PREFIX_TABLE)
            .set(Constants.ASSET_ID_VALUES, getObjectMapper().createArrayNode().add(tableName));
    return invoke(Constants.CONTRACT_GET_ASSET_ID, ledger, arguments).asText();
  }
}
