package com.ypeckstadt.escrow.contract.account;

import com.scalar.dl.ledger.asset.Asset;
import com.scalar.dl.ledger.contract.Contract;
import com.scalar.dl.ledger.database.Ledger;
import com.scalar.dl.ledger.exception.ContractContextException;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.Optional;

public class ChargeAccount extends Contract {

  public static final String ID = "id";
  public static final String AMOUNT = "amount";
  public static final String ACCOUNT_ASSET_TYPE = "account";
  public static final String TIMESTAMP = "timestamp";
  public static final String BALANCE = "balance";
  public static final String BALANCE_CHANGE = "balance_change";
  public static final String NAME = "name";

  @Override
  public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> properties) {

    // Check input arguments
    if (!argument.containsKey(ID)
        || !argument.containsKey(AMOUNT)
        || !argument.containsKey(TIMESTAMP)) {
      throw new ContractContextException("wrong or missing arguments to charge an account");
    }

    // get input data
    String accountId = argument.getString(ID);
    int amount = argument.getInt(AMOUNT);
    long timestamp = argument.getJsonNumber(TIMESTAMP).longValue();

    // check if account exists
    String assetId = ACCOUNT_ASSET_TYPE + "_" + accountId;
    Optional<Asset> optionalAsset = ledger.get(assetId);
    if (!optionalAsset.isPresent()) {
      throw new ContractContextException("the provided account does not exist");
    }

    // calculate new balance
    JsonObject assetData = optionalAsset.get().data();
    int newBalance = assetData.getInt(BALANCE) + amount;

    // asset json object builder
    JsonObjectBuilder builder =
        Json.createObjectBuilder()
            .add(ID, accountId)
            .add(NAME, assetData.get(NAME))
            .add(BALANCE, newBalance)
            .add(BALANCE_CHANGE, amount)
            .add(TIMESTAMP, timestamp);

    // add account
    ledger.put(assetId, builder.build());

    return null;
  }
}
