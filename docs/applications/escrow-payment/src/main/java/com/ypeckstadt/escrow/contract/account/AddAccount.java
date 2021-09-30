package com.ypeckstadt.escrow.contract.account;

import com.scalar.dl.ledger.asset.Asset;
import com.scalar.dl.ledger.contract.Contract;
import com.scalar.dl.ledger.database.Ledger;
import com.scalar.dl.ledger.exception.ContractContextException;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.Optional;

import static com.ypeckstadt.escrow.common.Constants.*;

public class AddAccount extends Contract {

    @Override
    public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> properties) {

        // Check input arguments
        if (!argument.containsKey(ACCOUNT_ID)
                || !argument.containsKey(ACCOUNT_NAME)
                || !argument.containsKey(ACCOUNT_TIMESTAMP)) {
            throw new ContractContextException(CONTRACT_ADD_ACCOUNT_MISSING_ARGUMENTS_ERROR);
        }

        // Get field data
        String name = argument.getString(ACCOUNT_NAME);
        long timestamp = argument.getJsonNumber(ACCOUNT_TIMESTAMP).longValue();
        String accountId = argument.getString(ACCOUNT_ID);

        // Determine asset id
        String assetId = ACCOUNT_ASSET_TYPE + "_" + accountId;

        // check if asset with id already exists
        Optional<Asset> asset = ledger.get(assetId);
        if (asset.isPresent()) {
            throw new ContractContextException(CONTRACT_ADD_ACCOUNT_DUPLICATE_ERROR);
        }

        // asset json object builder
        JsonObjectBuilder builder = Json.createObjectBuilder()
                .add(ACCOUNT_ID, accountId)
                .add(ACCOUNT_NAME, name)
                .add(ACCOUNT_BALANCE, 0)
                .add(ACCOUNT_BALANCE_CHANGE, 0)
                .add(ACCOUNT_TIMESTAMP, timestamp);

        // add account
        ledger.put(assetId, builder.build());

        return null;
    }
}