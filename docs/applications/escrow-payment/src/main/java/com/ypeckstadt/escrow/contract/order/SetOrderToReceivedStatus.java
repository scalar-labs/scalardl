package com.ypeckstadt.escrow.contract.order;

import com.scalar.dl.ledger.asset.Asset;
import com.scalar.dl.ledger.contract.Contract;
import com.scalar.dl.ledger.database.Ledger;
import com.scalar.dl.ledger.exception.ContractContextException;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.Optional;

public class SetOrderToReceivedStatus extends Contract {

  public static final String ID = "id";
  public static final String TIMESTAMP = "timestamp";
  public static final String BUYER_ACCOUNT_ID = "buyer_account_id";

  private static final String SELLER_ACCOUNT_ID = "seller_account_id";
  private static final String ITEM_ID = "item_id";
  private static final String STATUS = "status";
  private static final String ORDER_ASSET_TYPE = "order";
  private static final String ACCOUNT_ASSET_TYPE = "account";
  private static final String ORDER_STATUS_COMPLETE = "complete";
  private static final String DEBIT_ACCOUNT_CONTRACT = "DebitAccount_foo";
  private static final String CREDIT_ESCROW_ACCOUNT_CONTRACT = "CreditEscrowAccount_foo";
  private static final String ACCOUNT_ID = "account_id";
  private static final String TOTAL = "total";
  private static final String ORDER_ID = "order_id";
  private static final String AMOUNT = "amount";

  @Override
  public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> properties) {
    // Check input arguments
    if (!argument.containsKey(BUYER_ACCOUNT_ID)
        || !argument.containsKey(ID)
        || !argument.containsKey(TIMESTAMP)) {
      throw new ContractContextException("wrong or missing arguments to mark order as received");
    }

    // get field data from arguments
    String orderId = argument.getString(ID);
    String orderAssetId = ORDER_ASSET_TYPE + "_" + orderId;
    String buyerAccountId = argument.getString(BUYER_ACCOUNT_ID);
    String buyerAccountAssetId = ACCOUNT_ASSET_TYPE + "_" + buyerAccountId;
    long timestamp = argument.getJsonNumber(TIMESTAMP).longValue();

    // check if order exists
    Optional<Asset> optionalOrderAsset = ledger.get(orderAssetId);
    if (!optionalOrderAsset.isPresent()) {
      throw new ContractContextException("the order does not exist");
    }

    // check if buyer's account exists
    Optional<Asset> optionalAccountAsset = ledger.get(buyerAccountAssetId);
    if (!optionalAccountAsset.isPresent()) {
      throw new ContractContextException("the buyer's account does not exist");
    }

    // check if the order belongs to the buyer
    JsonObject order = optionalOrderAsset.get().data();
    String orderBuyerAccountId = order.getString(BUYER_ACCOUNT_ID);
    if (!buyerAccountId.equals(orderBuyerAccountId)) {
      throw new ContractContextException("Access denied. The order does not belong to the buyer");
    }

    // get data
    String itemId = order.getString(ITEM_ID);
    String sellerAccountId = order.getString(SELLER_ACCOUNT_ID);
    int orderTotal = order.getInt(TOTAL);

    // asset json object builder
    JsonObjectBuilder builder =
        Json.createObjectBuilder()
            .add(ID, orderId)
            .add(ITEM_ID, itemId)
            .add(BUYER_ACCOUNT_ID, buyerAccountId)
            .add(SELLER_ACCOUNT_ID, sellerAccountId)
            .add(STATUS, ORDER_STATUS_COMPLETE)
            .add(TOTAL, orderTotal)
            .add(TIMESTAMP, timestamp);

    // update order record
    ledger.put(orderAssetId, builder.build());

    // Credit Escrow account
    JsonObject escrowArguments =
        Json.createObjectBuilder()
            .add(BUYER_ACCOUNT_ID, buyerAccountId)
            .add(SELLER_ACCOUNT_ID, sellerAccountId)
            .add(AMOUNT, orderTotal)
            .add(ORDER_ID, orderId)
            .add(TIMESTAMP, timestamp)
            .build();
    invoke(CREDIT_ESCROW_ACCOUNT_CONTRACT, ledger, escrowArguments);

    // Debit Seller account
    JsonObject debitArguments =
        Json.createObjectBuilder()
            .add(ACCOUNT_ID, sellerAccountId)
            .add(AMOUNT, orderTotal)
            .add(TIMESTAMP, timestamp)
            .build();
    invoke(DEBIT_ACCOUNT_CONTRACT, ledger, debitArguments);

    return null;
  }
}
