package com.scalar.dl.genericcontracts.table.v1_0_0;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.annotations.VisibleForTesting;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.AssetFilter.AgeOrder;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;

public class ShowTables extends JacksonBasedContract {

  @Override
  public JsonNode invoke(
      Ledger<JsonNode> ledger, JsonNode arguments, @Nullable JsonNode properties) {

    if (!arguments.isEmpty() && arguments.size() != 1) {
      throw new ContractContextException(Constants.INVALID_CONTRACT_ARGUMENTS);
    }

    if (arguments.size() == 1 && !arguments.has(Constants.TABLE_NAME)) {
      throw new ContractContextException(Constants.INVALID_CONTRACT_ARGUMENTS);
    }

    ArrayNode tables = getObjectMapper().createArrayNode();
    if (arguments.has(Constants.TABLE_NAME)) {
      if (!arguments.get(Constants.TABLE_NAME).isTextual()) {
        throw new ContractContextException(Constants.INVALID_CONTRACT_ARGUMENTS);
      }

      String assetId =
          getAssetId(ledger, Constants.PREFIX_TABLE, arguments.get(Constants.TABLE_NAME));
      Asset<JsonNode> table =
          ledger
              .get(assetId)
              .orElseThrow(() -> new ContractContextException(Constants.TABLE_NOT_EXIST));
      tables.add(table.data());
    } else {
      AssetFilter filter = new AssetFilter(Constants.ASSET_ID_METADATA_TABLES);
      filter.withAgeOrder(AgeOrder.ASC);
      List<Asset<JsonNode>> results = ledger.scan(filter);
      results.forEach(asset -> tables.add(asset.data()));
    }

    return tables;
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
