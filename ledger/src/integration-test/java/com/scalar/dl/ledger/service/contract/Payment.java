package com.scalar.dl.ledger.service.contract;

import com.scalar.dl.ledger.asset.Asset;
import com.scalar.dl.ledger.contract.Contract;
import com.scalar.dl.ledger.database.Ledger;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.service.Constants;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

public class Payment extends Contract {

  @Override
  public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> properties) {
    JsonArray array = argument.getJsonArray(Constants.ASSETS_ATTRIBUTE_NAME);
    int amount = argument.getInt(Constants.AMOUNT_ATTRIBUTE_NAME);
    String fromId = array.getString(0);
    String toId = array.getString(1);

    Asset from = ledger.get(fromId).get();
    Asset to = ledger.get(toId).get();
    JsonObject fromData = from.data();
    JsonObject toData = to.data();

    int fromBalance = fromData.getInt(Constants.BALANCE_ATTRIBUTE_NAME);
    int toBalance = toData.getInt(Constants.BALANCE_ATTRIBUTE_NAME);
    if (fromBalance - amount < 0) {
      throw new ContractContextException("not enough balance in from account");
    }

    ledger.put(
        fromId, Json.createObjectBuilder(fromData).add("balance", fromBalance - amount).build());
    ledger.put(toId, Json.createObjectBuilder(toData).add("balance", toBalance + amount).build());

    return null;
  }
}
