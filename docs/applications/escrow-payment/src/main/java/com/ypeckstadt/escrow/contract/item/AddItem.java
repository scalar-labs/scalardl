package com.ypeckstadt.escrow.contract.item;

import com.scalar.dl.ledger.asset.Asset;
import com.scalar.dl.ledger.contract.Contract;
import com.scalar.dl.ledger.database.Ledger;
import com.scalar.dl.ledger.exception.ContractContextException;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class AddItem extends Contract {

  public static final String ITEM_ASSET_TYPE = "item";
  public static final String ACCOUNT_ASSET_TYPE = "account";
  public static final String ID = "id";
  public static final String NAME = "name";
  public static final String TIMESTAMP = "timestamp";
  public static final String PRICE = "price";
  public static final String SELLER_ACCOUNT_ID = "seller_account_id";

  @Override
  public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> properties) {
    // Check input arguments
    if (!argument.containsKey(ID)
        || !argument.containsKey(NAME)
        || !argument.containsKey(PRICE)
        || !argument.containsKey(SELLER_ACCOUNT_ID)
        || !argument.containsKey(TIMESTAMP)) {
      throw new ContractContextException("wrong or missing arguments to create an item");
    }

    // get field data from arguments
    String itemId = argument.getString(ID);
    String name = argument.getString(NAME);
    int price = argument.getInt(PRICE);
    long timestamp = argument.getJsonNumber(TIMESTAMP).longValue();
    String sellerAccountId = argument.getString(SELLER_ACCOUNT_ID);

    // determine asset id
    String assetId = ITEM_ASSET_TYPE + "_" + itemId;

    // check if asset with id already exists
    Optional<Asset> asset = ledger.get(assetId);
    if (asset.isPresent()) {
      throw new ContractContextException("an item with this id already exists");
    }

    // validate the seller has an account
    String accountAssetId = ACCOUNT_ASSET_TYPE + "_" + sellerAccountId;
    asset = ledger.get(accountAssetId);
    if (!asset.isPresent()) {
      throw new ContractContextException("the provided seller does not have an account");
    }

    // asset json object builder
    JsonObjectBuilder builder =
        Json.createObjectBuilder()
            .add(ID, itemId)
            .add(NAME, name)
            .add(PRICE, price)
            .add(SELLER_ACCOUNT_ID, sellerAccountId)
            .add(TIMESTAMP, timestamp);

    // add item
    ledger.put(assetId, builder.build());

    return null;
  }
}
