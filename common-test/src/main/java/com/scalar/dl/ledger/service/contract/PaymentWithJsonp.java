package com.scalar.dl.ledger.service.contract;

import com.scalar.dl.ledger.contract.JsonpBasedContract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.service.Constants;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import javax.annotation.Nullable;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

public class PaymentWithJsonp extends JsonpBasedContract {

  @Nullable
  @Override
  public JsonObject invoke(
      Ledger<JsonObject> ledger, JsonObject argument, @Nullable JsonObject properties) {
    JsonArray array = argument.getJsonArray(Constants.ASSETS_ATTRIBUTE_NAME);
    int amount = argument.getInt(Constants.AMOUNT_ATTRIBUTE_NAME);
    String fromId = array.getString(0);
    String toId = array.getString(1);

    Asset<JsonObject> from = ledger.get(fromId).get();
    Asset<JsonObject> to = ledger.get(toId).get();
    JsonObject fromData = from.data();
    JsonObject toData = to.data();

    int fromBalance = fromData.getInt(Constants.BALANCE_ATTRIBUTE_NAME);
    int toBalance = toData.getInt(Constants.BALANCE_ATTRIBUTE_NAME);
    if (fromBalance - amount < 0) {
      throw new ContractContextException("not enough balance in from account");
    }

    ledger.put(
        fromId,
        Json.createObjectBuilder(fromData)
            .add(Constants.BALANCE_ATTRIBUTE_NAME, fromBalance - amount)
            .build());
    ledger.put(
        toId,
        Json.createObjectBuilder(toData)
            .add(Constants.BALANCE_ATTRIBUTE_NAME, toBalance + amount)
            .build());

    return null;
  }
}
