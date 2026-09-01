package com.scalar.dl.testing.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.statemachine.Ledger;
import javax.annotation.Nullable;

/**
 * Contract that scans all ages of an asset and then puts a new entry. Used to verify scan+put in a
 * single contract execution.
 *
 * <p>The new entry's {@code state} is set to the number of ages returned by the scan, so callers
 * can assert that the scan ran and the put committed that result.
 */
public class ScanWithPut extends JacksonBasedContract {

  public static final String STATE_ATTRIBUTE_NAME = "state";

  @Nullable
  @Override
  public JsonNode invoke(
      Ledger<JsonNode> ledger, JsonNode argument, @Nullable JsonNode properties) {
    String assetId = argument.get(Constants.ASSET_ATTRIBUTE_NAME).asText();

    int ageCount = ledger.scan(new AssetFilter(assetId)).size();
    ledger.put(assetId, getObjectMapper().createObjectNode().put(STATE_ATTRIBUTE_NAME, ageCount));
    return null;
  }
}
