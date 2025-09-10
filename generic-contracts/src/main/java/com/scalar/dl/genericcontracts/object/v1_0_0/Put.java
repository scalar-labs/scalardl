package com.scalar.dl.genericcontracts.object.v1_0_0;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scalar.dl.genericcontracts.object.Constants;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Ledger;
import javax.annotation.Nullable;

public class Put extends JacksonBasedContract {

  @Nullable
  @Override
  public JsonNode invoke(
      Ledger<JsonNode> ledger, JsonNode arguments, @Nullable JsonNode properties) {

    if (!arguments.has(Constants.OBJECT_ID) || !arguments.get(Constants.OBJECT_ID).isTextual()) {
      throw new ContractContextException(Constants.OBJECT_ID_IS_MISSING_OR_INVALID);
    }

    if (!arguments.has(Constants.HASH_VALUE) || !arguments.get(Constants.HASH_VALUE).isTextual()) {
      throw new ContractContextException(Constants.HASH_VALUE_IS_MISSING_OR_INVALID);
    }

    if (arguments.has(Constants.METADATA) && !arguments.get(Constants.METADATA).isObject()) {
      throw new ContractContextException(Constants.INVALID_METADATA_FORMAT);
    }

    String assetId = getAssetIdForObject(arguments.get(Constants.OBJECT_ID).asText());
    ObjectNode node =
        getObjectMapper()
            .createObjectNode()
            .put(Constants.OBJECT_ID, arguments.get(Constants.OBJECT_ID).asText())
            .put(Constants.HASH_VALUE, arguments.get(Constants.HASH_VALUE).asText());

    if (arguments.has(Constants.METADATA)) {
      node.set(Constants.METADATA, arguments.get(Constants.METADATA));
    }

    ledger.get(assetId);
    ledger.put(assetId, node);

    return null;
  }

  private String getAssetIdForObject(String objectId) {
    return Constants.OBJECT_ID_PREFIX + objectId;
  }
}
