package com.ypeckstadt.escrow.contract.item;

import com.scalar.dl.ledger.asset.Asset;
import com.scalar.dl.ledger.contract.Contract;
import com.scalar.dl.ledger.database.Ledger;
import com.scalar.dl.ledger.exception.ContractContextException;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.Optional;

public class ViewItem extends Contract {

  public static final String ASSET_TYPE = "item";
  public static final String ID = "id";
  public static final String NAME = "name";
  public static final String TIMESTAMP = "timestamp";
  public static final String PRICE = "price";
  public static final String SELLER_ACCOUNT_ID = "seller_account_id";

  @Override
  public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> properties) {

    // Check input arguments
    if (!argument.containsKey(ID)) {
      throw new ContractContextException("missing ID argument for item lookup");
    }

    // Determine asset id
    String itemId = argument.getString(ID);
    String assetId = ASSET_TYPE + "_" + itemId;

    // Retrieve item
    Optional<Asset> asset = ledger.get(assetId);
    if (!asset.isPresent()) {
      throw new ContractContextException("no item with this id was found");
    }

    // get field data
    JsonObject data = asset.get().data();
    String id = data.getString(ID);
    String name = data.getString(NAME);
    String seller = data.getString(SELLER_ACCOUNT_ID);
    int price = data.getInt(PRICE);
    long timestamp = data.getJsonNumber(TIMESTAMP).longValue();

    // asset json object builder
    return Json.createObjectBuilder()
        .add(NAME, name)
        .add(PRICE, price)
        .add(SELLER_ACCOUNT_ID, seller)
        .add(ID, id)
        .add(TIMESTAMP, timestamp)
        .build();
  }
}
