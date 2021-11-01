package com.ypeckstadt.escrow.contract.order;

import com.scalar.dl.ledger.asset.Asset;
import com.scalar.dl.ledger.contract.Contract;
import com.scalar.dl.ledger.database.Ledger;
import com.scalar.dl.ledger.exception.ContractContextException;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class SetOrderToShippedStatus extends Contract {

  public static final String SELLER_ACCOUNT_ID = "seller_account_id";
  public static final String ID = "id";
  public static final String TIMESTAMP = "timestamp";

  private static final String BUYER_ACCOUNT_ID = "buyer_account_id";
  private static final String ITEM_ID = "item_id";
  private static final String STATUS = "status";
  private static final String ORDER_ASSET_TYPE = "order";
  private static final String ACCOUNT_ASSET_TYPE = "account";
  private static final String ORDER_STATUS_SHIPPED = "shipped";
  private static final String TOTAL = "total";

  @Override
  public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> properties) {

    // Check input arguments
    if (!argument.containsKey(SELLER_ACCOUNT_ID)
        || !argument.containsKey(ID)
        || !argument.containsKey(TIMESTAMP)) {
      throw new ContractContextException("wrong or missing arguments to mark order as shipped");
    }

    // get field data from arguments
    String orderId = argument.getString(ID);
    String orderAssetId = ORDER_ASSET_TYPE + "_" + orderId;
    String sellerAccountId = argument.getString(SELLER_ACCOUNT_ID);
    String sellerAccountAssetId = ACCOUNT_ASSET_TYPE + "_" + sellerAccountId;
    long timestamp = argument.getJsonNumber(TIMESTAMP).longValue();

    // check if order exists
    Optional<Asset> optionalOrderAsset = ledger.get(orderAssetId);
    if (!optionalOrderAsset.isPresent()) {
      throw new ContractContextException("the order does not exist");
    }

    // check if seller's account exists
    Optional<Asset> optionalAccountAsset = ledger.get(sellerAccountAssetId);
    if (!optionalAccountAsset.isPresent()) {
      throw new ContractContextException("the seller's account does not exist");
    }

    // check if the order belongs to the seller
    JsonObject order = optionalOrderAsset.get().data();
    String orderSellerAccountId = order.getString(SELLER_ACCOUNT_ID);
    if (!sellerAccountId.equals(orderSellerAccountId)) {
      throw new ContractContextException("Access denied. The order does not belong to the seller");
    }

    // get data
    String itemId = order.getString(ITEM_ID);
    String buyerAccountId = order.getString(BUYER_ACCOUNT_ID);
    int orderTotal = order.getInt(TOTAL);

    // asset json object builder
    JsonObjectBuilder builder =
        Json.createObjectBuilder()
            .add(ID, orderId)
            .add(ITEM_ID, itemId)
            .add(BUYER_ACCOUNT_ID, buyerAccountId)
            .add(SELLER_ACCOUNT_ID, sellerAccountId)
            .add(STATUS, ORDER_STATUS_SHIPPED)
            .add(TOTAL, orderTotal)
            .add(TIMESTAMP, timestamp);

    // update order record
    ledger.put(orderAssetId, builder.build());

    return null;
  }
}
