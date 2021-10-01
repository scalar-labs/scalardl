package com.ypeckstadt.escrow.contract.order;

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

public class ViewOrderHistory extends Contract {

  public static final String ID = "id";

  private static final String ASSET_TYPE = "order";
  private static final String TIMESTAMP = "timestamp";
  private static final String BUYER_ACCOUNT_ID = "buyer_account_id";
  private static final String SELLER_ACCOUNT_ID = "seller_account_id";
  private static final String ITEM_ID = "item_id";
  private static final String STATUS = "status";
  private static final String TOTAL = "total";

  private static final String HISTORY = "order_history";

  @Override
  public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> properties) {

    // Check input arguments
    if (!argument.containsKey(ID)) {
      throw new ContractContextException("missing ID argument for order lookup");
    }

    // Determine asset id
    String orderId = argument.getString(ID);
    String assetId = ASSET_TYPE + "_" + orderId;

    // Retrieve order history
    AssetFilter filter = new AssetFilter(assetId);
    List<Asset> history = ledger.scan(filter);
    if (history.isEmpty()) {
      throw new ContractContextException("no order with this id was found");
    }

    // build result
    JsonArrayBuilder builder = Json.createArrayBuilder();
    for (Asset h : history) {

      // read data
      JsonObject data = h.data();
      String id = data.getString(ID);
      String itemId = data.getString(ITEM_ID);
      String buyerAccountId = data.getString(BUYER_ACCOUNT_ID);
      String sellerAccountId = data.getString(SELLER_ACCOUNT_ID);
      String status = data.getString(STATUS);
      long timestamp = data.getJsonNumber(TIMESTAMP).longValue();
      int total = data.getInt(TOTAL);

      // build json object result
      JsonObject order =
          Json.createObjectBuilder()
              .add(ID, id)
              .add(ITEM_ID, itemId)
              .add(BUYER_ACCOUNT_ID, buyerAccountId)
              .add(SELLER_ACCOUNT_ID, sellerAccountId)
              .add(STATUS, status)
              .add(TOTAL, total)
              .add(TIMESTAMP, timestamp)
              .build();
      builder.add(order);
    }
    JsonArray orderHistory = builder.build();

    return Json.createObjectBuilder().add(HISTORY, orderHistory).build();
  }
}
