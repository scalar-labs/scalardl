package com.ypeckstadt.escrow.contract.account;

import com.scalar.dl.ledger.asset.Asset;
import com.scalar.dl.ledger.contract.Contract;
import com.scalar.dl.ledger.database.Ledger;
import com.scalar.dl.ledger.exception.ContractContextException;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class DebitAccount extends Contract {

  private static final String ACCOUNT_ID = "account_id";
  private static final String AMOUNT = "amount";
  private static final String ACCOUNT_ASSET_TYPE = "account";
  private static final String BALANCE = "balance";
  private static final String TIMESTAMP = "timestamp";
  private static final String ID = "id";
  private static final String NAME = "name";
  private static final String BALANCE_CHANGE = "balance_change";

  @Override
  public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> properties) {

    // Check input arguments
    if (!argument.containsKey(ACCOUNT_ID)
        || !argument.containsKey(AMOUNT)
        || !argument.containsKey(TIMESTAMP)) {
      throw new ContractContextException("wrong or missing arguments to debit an account");
    }

    // get input data
    String accountId = argument.getString(ACCOUNT_ID);
    int amount = argument.getInt(AMOUNT);
    long timestamp = argument.getJsonNumber(TIMESTAMP).longValue();

    // Determine asset id
    String assetId = ACCOUNT_ASSET_TYPE + "_" + accountId;

    // check if account exists
    Optional<Asset> asset = ledger.get(assetId);
    if (!asset.isPresent()) {
      throw new ContractContextException("the account does not exist");
    }

    // debit balance
    JsonObject account = asset.get().data();
    int newBalance = account.getInt(BALANCE) + amount;

    // asset json object builder
    JsonObjectBuilder builder =
        Json.createObjectBuilder()
            .add(ID, accountId)
            .add(NAME, account.getString(NAME))
            .add(BALANCE, newBalance)
            .add(BALANCE_CHANGE, amount)
            .add(TIMESTAMP, timestamp);

    // update account
    ledger.put(assetId, builder.build());

    return null;
  }
}
