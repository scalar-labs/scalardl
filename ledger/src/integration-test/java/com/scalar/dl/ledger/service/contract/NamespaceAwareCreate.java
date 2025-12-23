package com.scalar.dl.ledger.service.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.service.Constants;
import com.scalar.dl.ledger.statemachine.Ledger;
import javax.annotation.Nullable;

public class NamespaceAwareCreate extends JacksonBasedContract {

  @Nullable
  @Override
  public JsonNode invoke(
      Ledger<JsonNode> ledger, JsonNode argument, @Nullable JsonNode properties) {
    JsonNode json =
        getObjectMapper()
            .createObjectNode()
            .put(
                Constants.BALANCE_ATTRIBUTE_NAME,
                argument.get(Constants.AMOUNT_ATTRIBUTE_NAME).asInt());

    String[] assetKey =
        argument.get(Constants.ASSET_ATTRIBUTE_NAME).asText().split(Constants.ASSET_ID_SEPARATOR);
    String namespace = assetKey[0];
    String assetId = assetKey[1];
    ledger.put(namespace, assetId, json);

    return null;
  }
}
