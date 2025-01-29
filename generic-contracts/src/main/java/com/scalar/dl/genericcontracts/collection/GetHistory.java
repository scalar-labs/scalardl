package com.scalar.dl.genericcontracts.collection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.AssetFilter.AgeOrder;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.List;
import javax.annotation.Nullable;

/**
 * `GetHistory` gets a history of modifying the specified collection. See the comment in `Create`
 * for details about the internal data structure of the collection.
 */
public class GetHistory extends JacksonBasedContract {

  @Nullable
  @Override
  public JsonNode invoke(
      Ledger<JsonNode> ledger, JsonNode arguments, @Nullable JsonNode properties) {

    if (!arguments.has(Constants.COLLECTION_ID)
        || !arguments.get(Constants.COLLECTION_ID).isTextual()) {
      throw new ContractContextException(Constants.COLLECTION_ID_IS_MISSING_OR_INVALID);
    }

    String collectionId = arguments.get(Constants.COLLECTION_ID).asText();
    String assetId = getAssetIdForCollection(collectionId);
    AssetFilter filter = new AssetFilter(assetId).withAgeOrder(AgeOrder.DESC);
    if (arguments.has(Constants.OPTIONS)
        && arguments.get(Constants.OPTIONS).isObject()
        && arguments.get(Constants.OPTIONS).has(Constants.OPTION_LIMIT)) {
      filter.withLimit(validateAndGetLimit(arguments));
    }

    List<Asset<JsonNode>> history = ledger.scan(filter);

    ArrayNode events = getObjectMapper().createArrayNode();
    for (Asset<JsonNode> asset : history) {
      JsonNode data = asset.data();
      ObjectNode event =
          getObjectMapper()
              .createObjectNode()
              .put(Constants.OPERATION_TYPE, data.get(Constants.OPERATION_TYPE).asText())
              .put(Constants.COLLECTION_AGE, asset.age())
              .set(Constants.OBJECT_IDS, data.get(Constants.OBJECT_IDS));
      events.add(event);
    }

    return getObjectMapper()
        .createObjectNode()
        .put(Constants.COLLECTION_ID, collectionId)
        .set(Constants.COLLECTION_EVENTS, events);
  }

  private String getAssetIdForCollection(String collectionId) {
    return Constants.COLLECTION_ID_PREFIX + collectionId;
  }

  private Integer validateAndGetLimit(JsonNode arguments) {
    JsonNode limit = arguments.get(Constants.OPTIONS).get(Constants.OPTION_LIMIT);
    if (!limit.isInt() || limit.asInt() < 0) {
      throw new ContractContextException(Constants.INVALID_OPTIONS_FORMAT);
    }
    return limit.asInt();
  }
}
