package com.scalar.dl.testing.contract;

import static com.scalar.dl.testing.schema.SchemaConstants.ASSET_AGE_COLUMN_NAME;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.List;
import javax.annotation.Nullable;

/** Contract that scans all ages of an asset and returns them. */
public class Scan extends JacksonBasedContract {

  public static final String SCAN_ATTRIBUTE_NAME = "scan";
  public static final String DATA_ATTRIBUTE_NAME = "data";

  @Nullable
  @Override
  public JsonNode invoke(
      Ledger<JsonNode> ledger, JsonNode argument, @Nullable JsonNode properties) {
    String assetId = argument.get(Constants.ASSET_ATTRIBUTE_NAME).asText();
    AssetFilter filter = new AssetFilter(assetId);
    List<Asset<JsonNode>> assets = ledger.scan(filter);

    ArrayNode scanned = getObjectMapper().createArrayNode();
    for (Asset<JsonNode> asset : assets) {
      ObjectNode entry = getObjectMapper().createObjectNode();
      entry.put(ASSET_AGE_COLUMN_NAME, asset.age());
      entry.set(DATA_ATTRIBUTE_NAME, asset.data());
      scanned.add(entry);
    }

    return getObjectMapper().createObjectNode().set(SCAN_ATTRIBUTE_NAME, scanned);
  }
}
