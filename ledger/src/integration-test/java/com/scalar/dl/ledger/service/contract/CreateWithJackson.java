package com.scalar.dl.ledger.service.contract;

import static com.scalar.dl.ledger.service.Constants.EXECUTE_NESTED_ATTRIBUTE_NAME;

import com.fasterxml.jackson.databind.JsonNode;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.service.Constants;
import com.scalar.dl.ledger.statemachine.Ledger;
import javax.annotation.Nullable;

public class CreateWithJackson extends JacksonBasedContract {

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

    String assetId = argument.get(Constants.ASSET_ATTRIBUTE_NAME).asText();
    ledger.put(assetId, json);

    if (properties != null
        && argument.has(EXECUTE_NESTED_ATTRIBUTE_NAME)
        && argument.get(EXECUTE_NESTED_ATTRIBUTE_NAME).asBoolean()) {
      if (!properties
          .get(Constants.CONTRACT_ID_ATTRIBUTE_NAME)
          .asText()
          .equals(Constants.CREATE_CONTRACT_ID3)) {
        throw new ContractContextException("properties is not set properly.");
      }
      return invoke(Constants.GET_BALANCE_CONTRACT_ID3, ledger, argument);
    } else {
      return null;
    }
  }
}
