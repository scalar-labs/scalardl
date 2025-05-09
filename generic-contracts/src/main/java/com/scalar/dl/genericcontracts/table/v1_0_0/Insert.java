package com.scalar.dl.genericcontracts.table.v1_0_0;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.annotations.VisibleForTesting;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.Arrays;
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
    String tableAssetId = getAssetId(ledger, Constants.PREFIX_TABLE, TextNode.valueOf(tableName));
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
    String recordAssetId =
        getAssetId(
            ledger,
            Constants.PREFIX_RECORD,
            TextNode.valueOf(tableName),
            TextNode.valueOf(key),
            values.get(key));
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
        assetId =
            getAssetId(
                ledger,
                Constants.PREFIX_INDEX,
                TextNode.valueOf(tableName),
                TextNode.valueOf(indexKey),
                values.get(indexKey));
      } else {
        assetId =
            getAssetId(
                ledger,
                Constants.PREFIX_INDEX,
                TextNode.valueOf(tableName),
                TextNode.valueOf(indexKey),
                NullNode.getInstance());
      }

      ObjectNode indexEntry = getObjectMapper().createObjectNode();
      indexEntry.set(key, values.get(key));
      indexEntry.put(Constants.INDEX_ASSET_ADDED_AGE, 0);

      ledger.put(assetId, getObjectMapper().createArrayNode().add(indexEntry));
    }
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
