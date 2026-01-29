package com.scalar.dl.ledger.service.contract;

import static com.scalar.dl.ledger.service.Constants.ASSET_AGE_COLUMN_NAME;
import static com.scalar.dl.ledger.service.Constants.BALANCE_ATTRIBUTE_NAME;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.AssetFilter.AgeOrder;
import com.scalar.dl.ledger.service.Constants;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.List;
import javax.annotation.Nullable;

@SuppressWarnings("StringSplitter")
public class NamespaceAwareGetHistory extends JacksonBasedContract {

  @Nullable
  @Override
  public JsonNode invoke(
      Ledger<JsonNode> ledger, JsonNode argument, @Nullable JsonNode properties) {
    String[] assetKey =
        argument.get(Constants.ASSET_ATTRIBUTE_NAME).asText().split(Constants.ASSET_ID_SEPARATOR);
    String namespace = assetKey[0];
    String assetId = assetKey[1];
    AssetFilter filter = new AssetFilter(namespace, assetId).withAgeOrder(AgeOrder.DESC);
    List<Asset<JsonNode>> assetRecords = ledger.scan(filter);

    ArrayNode histories = getObjectMapper().createArrayNode();
    for (Asset<JsonNode> assetRecord : assetRecords) {
      JsonNode balance =
          getObjectMapper()
              .createObjectNode()
              .put(
                  BALANCE_ATTRIBUTE_NAME, assetRecord.data().get(BALANCE_ATTRIBUTE_NAME).intValue())
              .put(ASSET_AGE_COLUMN_NAME, assetRecord.age());
      histories.add(balance);
    }

    return histories;
  }
}
