package com.ypeckstadt.escrow.contract.account;

import com.scalar.dl.ledger.asset.Asset;
import com.scalar.dl.ledger.contract.Contract;
import com.scalar.dl.ledger.database.Ledger;
import com.scalar.dl.ledger.exception.ContractContextException;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.Optional;

public class CreditAccount extends Contract {

    public static final String ACCOUNT_ID = "account_id";
    public static final String AMOUNT = "amount";
    public static final String ACCOUNT_ASSET_TYPE = "account";
    public static final String BALANCE = "balance";
    public static final String TIMESTAMP = "timestamp";

    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String BALANCE_CHANGE = "balance_change";


    @Override
    public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> properties) {

        // Check input arguments
        if (!argument.containsKey(ACCOUNT_ID)
                || !argument.containsKey(AMOUNT)
                || !argument.containsKey(TIMESTAMP)) {
            throw new ContractContextException("wrong or missing arguments to credit an account");
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

        // check balance
        JsonObject account = asset.get().data();
        int balance = account.getInt(BALANCE);
        if (balance < amount) {
            throw new ContractContextException("the account has insufficient funds");
        }

        // credit account
        int newBalance = balance - amount;

        // asset json object builder
        JsonObjectBuilder builder = Json.createObjectBuilder()
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