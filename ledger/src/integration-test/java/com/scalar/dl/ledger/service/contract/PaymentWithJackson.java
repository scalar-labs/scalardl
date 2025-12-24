package com.scalar.dl.ledger.service.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.service.Constants;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import javax.annotation.Nullable;

public class PaymentWithJackson extends JacksonBasedContract {

  @Nullable
  @Override
  public JsonNode invoke(
      Ledger<JsonNode> ledger, JsonNode argument, @Nullable JsonNode properties) {
    ArrayNode array = (ArrayNode) argument.get(Constants.ASSETS_ATTRIBUTE_NAME);
    int amount = argument.get(Constants.AMOUNT_ATTRIBUTE_NAME).asInt();
    String fromId = array.get(0).asText();
    String toId = array.get(1).asText();

    Asset<JsonNode> from = ledger.get(fromId).get();
    Asset<JsonNode> to = ledger.get(toId).get();
    JsonNode fromData = from.data();
    JsonNode toData = to.data();

    int fromBalance = fromData.get(Constants.BALANCE_ATTRIBUTE_NAME).asInt();
    int toBalance = toData.get(Constants.BALANCE_ATTRIBUTE_NAME).asInt();
    if (fromBalance - amount < 0) {
      throw new ContractContextException("not enough balance in from account");
    }

    ledger.put(
        fromId,
        ((ObjectNode) getObjectMapper().createObjectNode().setAll((ObjectNode) fromData))
            .put(Constants.BALANCE_ATTRIBUTE_NAME, fromBalance - amount));
    ledger.put(
        toId,
        ((ObjectNode) getObjectMapper().createObjectNode().setAll((ObjectNode) toData))
            .put(Constants.BALANCE_ATTRIBUTE_NAME, toBalance + amount));

    return null;
  }
}
