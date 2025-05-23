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
    if (arguments.size() < 2
        || arguments.size() > 3
        || !arguments.has(Constants.RECORD_TABLE)
        || !arguments.get(Constants.RECORD_TABLE).isTextual()
        || !arguments.has(Constants.RECORD_KEY)) {
      throw new ContractContextException(Constants.INVALID_CONTRACT_ARGUMENTS);
    }

    // Get the table metadata
    JsonNode tableName = arguments.get(Constants.RECORD_TABLE);
    JsonNode table =
        ledger
            .get(getAssetId(ledger, Constants.PREFIX_TABLE, tableName))
            .orElseThrow(() -> new ContractContextException(Constants.TABLE_NOT_EXIST))
            .data();

    // Check the key type
    JsonNode keyColumn = table.get(Constants.TABLE_KEY);
    JsonNode keyValue = arguments.get(Constants.RECORD_KEY);
    String keyType = table.get(Constants.TABLE_KEY_TYPE).asText();
    if (!keyType.toUpperCase().equals(keyValue.getNodeType().name())) {
      throw new ContractContextException(Constants.INVALID_KEY_TYPE);
    }

    // Prepare scan for the asset of the record
    String recordAssetId =
        getAssetId(ledger, Constants.PREFIX_RECORD, tableName, keyColumn, keyValue);
    AssetFilter filter = new AssetFilter(recordAssetId).withAgeOrder(AgeOrder.DESC);
    if (arguments.has(Constants.HISTORY_LIMIT)) {
      filter.withLimit(validateAndGetLimit(arguments.get(Constants.HISTORY_LIMIT)));
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

  private Integer validateAndGetLimit(JsonNode limit) {
    if (!limit.isInt() || limit.asInt() < 0) {
      throw new ContractContextException(Constants.INVALID_CONTRACT_ARGUMENTS);
    }
    return limit.asInt();
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
