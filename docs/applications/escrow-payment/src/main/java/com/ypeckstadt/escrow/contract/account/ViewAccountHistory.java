package com.ypeckstadt.escrow.contract.account;

import com.scalar.dl.ledger.asset.Asset;
import com.scalar.dl.ledger.contract.Contract;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.Ledger;
import com.scalar.dl.ledger.exception.ContractContextException;
import java.util.List;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

public class ViewAccountHistory extends Contract {

  public static final String ASSET_TYPE = "account";
  public static final String ID = "id";
  public static final String NAME = "name";
  public static final String TIMESTAMP = "timestamp";
  public static final String BALANCE = "balance";
  public static final String BALANCE_CHANGE = "balance_change";
  private static final String HISTORY = "account_history";

  @Override
  public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> properties) {

    // Check input arguments
    if (!argument.containsKey(ID)) {
      throw new ContractContextException("missing ID argument for account lookup");
    }

    // Determine asset id
    String accountId = argument.getString(ID);
    String assetId = ASSET_TYPE + "_" + accountId;

    // Retrieve account history
    AssetFilter filter = new AssetFilter(assetId);
    List<Asset> history = ledger.scan(filter);
    if (history.isEmpty()) {
      throw new ContractContextException("no account with this id was found");
    }

    // build result
    JsonArrayBuilder builder = Json.createArrayBuilder();
    for (Asset h : history) {

      // read data
      JsonObject data = h.data();
      String id = data.getString(ID);
      String name = data.getString(NAME);
      int balance = data.getInt(BALANCE);
      int balanceChange = data.getInt(BALANCE_CHANGE);
      long timestamp = data.getJsonNumber(TIMESTAMP).longValue();

      // build json object result
      JsonObject account =
          Json.createObjectBuilder()
              .add(NAME, name)
              .add(BALANCE, balance)
              .add(BALANCE_CHANGE, balanceChange)
              .add(ID, id)
              .add(TIMESTAMP, timestamp)
              .build();
      builder.add(account);
    }
    JsonArray orderHistory = builder.build();

    return Json.createObjectBuilder().add(HISTORY, orderHistory).build();
  }
}
