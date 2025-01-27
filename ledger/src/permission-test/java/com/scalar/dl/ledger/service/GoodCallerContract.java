package com.scalar.dl.ledger.service;

import static com.scalar.dl.ledger.service.LedgerServicePermissionTest.AMOUNT_ATTRIBUTE_NAME;
import static com.scalar.dl.ledger.service.LedgerServicePermissionTest.ASSET_ATTRIBUTE_NAME;
import static com.scalar.dl.ledger.service.LedgerServicePermissionTest.BALANCE_ATTRIBUTE_NAME;
import static com.scalar.dl.ledger.service.LedgerServicePermissionTest.CONTRACT_ID_ATTRIBUTE_NAME;

import com.scalar.dl.ledger.contract.Contract;
import com.scalar.dl.ledger.database.Ledger;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;

public class GoodCallerContract extends Contract {

  @Override
  public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> properties) {
    JsonObject json =
        Json.createObjectBuilder()
            .add(BALANCE_ATTRIBUTE_NAME, argument.getInt(AMOUNT_ATTRIBUTE_NAME))
            .build();
    String assetId = argument.getString(ASSET_ATTRIBUTE_NAME);
    String contractId = argument.getString(CONTRACT_ID_ATTRIBUTE_NAME);

    ledger.put(assetId, json);
    ledger.get(assetId);

    return invoke(contractId, ledger, argument);
  }
}
