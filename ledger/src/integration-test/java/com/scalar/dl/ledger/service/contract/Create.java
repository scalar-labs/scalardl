package com.scalar.dl.ledger.service.contract;

import static com.scalar.dl.ledger.service.Constants.EXECUTE_NESTED_ATTRIBUTE_NAME;

import com.scalar.dl.ledger.contract.Contract;
import com.scalar.dl.ledger.database.Ledger;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.service.Constants;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;

public class Create extends Contract {

  @Override
  public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> properties) {
    JsonObject json =
        Json.createObjectBuilder()
            .add(Constants.BALANCE_ATTRIBUTE_NAME, argument.getInt(Constants.AMOUNT_ATTRIBUTE_NAME))
            .build();

    String assetId = argument.getString(Constants.ASSET_ATTRIBUTE_NAME);
    ledger.put(assetId, json);

    if (properties.isPresent()
        && argument.containsKey(EXECUTE_NESTED_ATTRIBUTE_NAME)
        && argument.getBoolean(EXECUTE_NESTED_ATTRIBUTE_NAME)) {
      if (!properties
          .get()
          .getString(Constants.CONTRACT_ID_ATTRIBUTE_NAME)
          .equals(Constants.CREATE_CONTRACT_ID1)) {
        throw new ContractContextException("properties is not set properly.");
      }
      return invoke(Constants.GET_BALANCE_CONTRACT_ID1, ledger, argument);
    } else {
      return null;
    }
  }
}
