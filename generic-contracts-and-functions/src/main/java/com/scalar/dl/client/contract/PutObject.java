package com.scalar.dl.client.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scalar.dl.client.Constants;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Ledger;
import javax.annotation.Nullable;

public class PutObject extends JacksonBasedContract {

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

    if (arguments.has(Constants.PROPERTIES) && !arguments.get(Constants.PROPERTIES).isObject()) {
      throw new ContractContextException(Constants.INVALID_PROPERTIES_FORMAT);
    }

    String assetId = arguments.get(Constants.OBJECT_ID).asText();
    ObjectNode node =
        getObjectMapper()
            .createObjectNode()
            .put(Constants.OBJECT_ID, arguments.get(Constants.OBJECT_ID).asText())
            .put(Constants.HASH_VALUE, arguments.get(Constants.HASH_VALUE).asText());

    if (arguments.has(Constants.PROPERTIES)) {
      node.set(Constants.PROPERTIES, arguments.get(Constants.PROPERTIES));
    }

    ledger.get(assetId);
    ledger.put(assetId, node);

    return null;
  }
}
