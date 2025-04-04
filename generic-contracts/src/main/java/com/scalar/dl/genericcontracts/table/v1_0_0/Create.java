package com.scalar.dl.genericcontracts.table.v1_0_0;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.Optional;
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

    if (!isSupportedKeyType(arguments.get(Constants.TABLE_KEY_TYPE).asText())) {
      throw new ContractContextException(Constants.INVALID_KEY_TYPE);
    }

    if (arguments.has(Constants.TABLE_INDEXES)) {
      validateIndexes(arguments.get(Constants.TABLE_INDEXES));
    }

    // Get the table existence
    String assetId = getAssetIdForTable(arguments.get(Constants.TABLE_NAME).asText());
    Optional<Asset<JsonNode>> asset = ledger.get(assetId);
    if (asset.isPresent()) {
      throw new ContractContextException(Constants.TABLE_ALREADY_EXISTS);
    }

    // Put the asset records
    ledger.put(Constants.ASSET_ID_METADATA_TABLES, arguments);
    ledger.put(assetId, arguments);

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

    for (JsonNode index : indexes) {
      if (!index.isObject()
          || index.size() != 2
          || !index.has(Constants.INDEX_KEY)
          || !index.get(Constants.INDEX_KEY).isTextual()
          || !index.has(Constants.INDEX_KEY_TYPE)
          || !index.get(Constants.INDEX_KEY_TYPE).isTextual()) {
        throw new ContractContextException(Constants.INVALID_INDEX_FORMAT);
      }
      if (!isSupportedKeyType(index.get(Constants.INDEX_KEY_TYPE).asText())) {
        throw new ContractContextException(Constants.INVALID_KEY_TYPE);
      }
    }
  }

  private String getAssetIdForTable(String tableName) {
    return Constants.PREFIX_TABLE + tableName;
  }
}
