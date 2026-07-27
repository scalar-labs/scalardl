package com.scalar.dl.testing.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import javax.annotation.Nullable;

/**
 * Contract that scans all ages of an asset and then puts a new entry. Used to verify scan+put in a
 * single contract execution (including under Auditor).
 */
public class ScanWithPut extends JacksonBasedContract {

  private static final String STATE_ATTRIBUTE_NAME = "state";

  @Nullable
  @Override
  public JsonNode invoke(
      Ledger<JsonNode> ledger, JsonNode argument, @Nullable JsonNode properties) {
    String assetId = argument.get(Constants.ASSET_ATTRIBUTE_NAME).asText();

    for (Asset<JsonNode> asset : ledger.scan(new AssetFilter(assetId))) {
      // Visit each age so scan is fully executed before put (Auditor path).
      asset.age();
    }

    ledger.put(assetId, getObjectMapper().createObjectNode().put(STATE_ATTRIBUTE_NAME, 0));
    return null;
  }
}
