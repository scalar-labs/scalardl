package com.scalar.dl.ledger.service.contract;

import static com.scalar.dl.ledger.service.Constants.EXECUTE_NESTED_ATTRIBUTE_NAME;

import com.scalar.dl.ledger.contract.JsonpBasedContract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.service.Constants;
import com.scalar.dl.ledger.statemachine.Ledger;
import javax.annotation.Nullable;
import javax.json.Json;
import javax.json.JsonObject;

public class CreateWithJsonp extends JsonpBasedContract {

  @Nullable
  @Override
  public JsonObject invoke(
      Ledger<JsonObject> ledger, JsonObject argument, @Nullable JsonObject properties) {
    JsonObject json =
        Json.createObjectBuilder()
            .add(Constants.BALANCE_ATTRIBUTE_NAME, argument.getInt(Constants.AMOUNT_ATTRIBUTE_NAME))
            .build();

    String assetId = argument.getString(Constants.ASSET_ATTRIBUTE_NAME);
    ledger.put(assetId, json);

    if (properties != null
        && argument.containsKey(EXECUTE_NESTED_ATTRIBUTE_NAME)
        && argument.getBoolean(EXECUTE_NESTED_ATTRIBUTE_NAME)) {
      if (!properties
          .getString(Constants.CONTRACT_ID_ATTRIBUTE_NAME)
          .equals(Constants.CREATE_CONTRACT_ID2)) {
        throw new ContractContextException("properties is not set properly.");
      }
      return invoke(Constants.GET_BALANCE_CONTRACT_ID2, ledger, argument);
    } else {
      return null;
    }
  }
}
