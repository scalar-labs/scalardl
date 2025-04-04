package com.scalar.dl.genericcontracts.table.v1_0_0;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.Optional;
import javax.annotation.Nullable;

public class Insert extends JacksonBasedContract {

  @Nullable
  @Override
  public JsonNode invoke(
      Ledger<JsonNode> ledger, JsonNode arguments, @Nullable JsonNode properties) {

    // Check the arguments
    if (arguments.size() != 2
        || !arguments.has(Constants.RECORD_TABLE)
        || !arguments.get(Constants.RECORD_TABLE).isTextual()
        || !arguments.has(Constants.RECORD_VALUES)
        || !arguments.get(Constants.RECORD_VALUES).isObject()) {
      throw new ContractContextException(Constants.INVALID_RECORD_FORMAT);
    }

    // Get the table information
    String tableName = arguments.get(Constants.RECORD_TABLE).asText();
    String tableAssetId = getAssetIdForTable(tableName);
    Optional<Asset<JsonNode>> tableAsset = ledger.get(tableAssetId);
    if (!tableAsset.isPresent()) {
      throw new ContractContextException(Constants.TABLE_NOT_EXIST);
    }

    // Check the key existence and type in the argument
    String key = tableAsset.get().data().get(Constants.TABLE_KEY).textValue();
    String keyType = tableAsset.get().data().get(Constants.TABLE_KEY_TYPE).textValue();
    JsonNode values = arguments.get(Constants.RECORD_VALUES);
    if (!values.has(key)) {
      throw new ContractContextException(Constants.RECORD_KEY_NOT_EXIST);
    }
    if (!keyType.toUpperCase().equals(values.get(key).getNodeType().name())) {
      throw new ContractContextException(Constants.INVALID_KEY_TYPE);
    }

    // Check the record existence
    String recordAssetId = getAssetIdForRecord(tableName, key, values.get(key).asText());
    Optional<Asset<JsonNode>> recordAsset = ledger.get(recordAssetId);
    if (recordAsset.isPresent()) {
      throw new ContractContextException(Constants.RECORD_ALREADY_EXISTS);
    }

    // Put the asset records
    putIndexAssets(
        ledger,
        tableName,
        key,
        arguments.get(Constants.RECORD_VALUES),
        tableAsset.get().data().get(Constants.TABLE_INDEXES));
    ledger.put(recordAssetId, arguments.get(Constants.RECORD_VALUES));

    return null;
  }

  private void putIndexAssets(
      Ledger<JsonNode> ledger, String tableName, String key, JsonNode values, JsonNode indexes) {
    for (JsonNode index : indexes) {
      String indexKey = index.get(Constants.INDEX_KEY).asText();
      String indexKeyType = index.get(Constants.INDEX_KEY_TYPE).asText();
      String assetId;

      if (values.has(indexKey) && !values.get(indexKey).isNull()) {
        if (!indexKeyType.toUpperCase().equals(values.get(indexKey).getNodeType().name())) {
          throw new ContractContextException(Constants.INVALID_INDEX_KEY_TYPE);
        }
        assetId = getAssetIdForIndex(tableName, indexKey, values.get(indexKey).asText());
      } else {
        assetId = getAssetIdForNullIndex(tableName, indexKey);
      }

      ObjectNode node = getObjectMapper().createObjectNode();
      node.set(key, values.get(key));
      node.put(Constants.ASSET_AGE, 0);

      ledger.put(assetId, node);
    }
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
