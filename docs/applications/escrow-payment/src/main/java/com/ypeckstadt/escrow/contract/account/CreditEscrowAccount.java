package com.ypeckstadt.escrow.contract.account;

import com.scalar.dl.ledger.asset.Asset;
import com.scalar.dl.ledger.contract.Contract;
import com.scalar.dl.ledger.database.Ledger;
import com.scalar.dl.ledger.exception.ContractContextException;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class CreditEscrowAccount extends Contract {

  public static final String BUYER_ACCOUNT_ID = "buyer_account_id";
  public static final String SELLER_ACCOUNT_ID = "seller_account_id";
  public static final String AMOUNT = "amount";
  public static final String ACCOUNT_ASSET_TYPE = "account";
  public static final String BALANCE = "balance";
  public static final String TIMESTAMP = "timestamp";
  public static final String ORDER_ID = "order_id";
  private static final String ESCROW_ACCOUNT_ASSET_TYPE = "escrow";

  @Override
  public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> properties) {

    // Check input arguments
    if (!argument.containsKey(BUYER_ACCOUNT_ID)
        || !argument.containsKey(SELLER_ACCOUNT_ID)
        || !argument.containsKey(TIMESTAMP)
        || !argument.containsKey(ORDER_ID)
        || !argument.containsKey(AMOUNT)) {
      throw new ContractContextException("wrong or missing arguments to credit an escrow account");
    }

    // get input data
    String buyerAccountId = argument.getString(BUYER_ACCOUNT_ID);
    String sellerAccountId = argument.getString(SELLER_ACCOUNT_ID);
    String orderId = argument.getString(ORDER_ID);
    int amount = argument.getInt(AMOUNT);
    long timestamp = argument.getJsonNumber(TIMESTAMP).longValue();

    // determine asset ids
    String buyerAccountAssetId = ACCOUNT_ASSET_TYPE + "_" + buyerAccountId;
    String sellerAccountAssetId = ACCOUNT_ASSET_TYPE + "_" + sellerAccountId;

    // check if buyer's account exits
    Optional<Asset> optionalBuyerAsset = ledger.get(buyerAccountAssetId);
    if (!optionalBuyerAsset.isPresent()) {
      throw new ContractContextException("the buyer's account does not exist");
    }

    // check if seller's account exists
    Optional<Asset> optionalSellerAsset = ledger.get(sellerAccountAssetId);
    if (!optionalSellerAsset.isPresent()) {
      throw new ContractContextException("the seller's account does not exist");
    }

    // retrieve escrow account
    String escrowAccountAssetId =
        ESCROW_ACCOUNT_ASSET_TYPE + buyerAccountId + "_" + sellerAccountId;
    Optional<Asset> optionalEscrowAccount = ledger.get(escrowAccountAssetId);
    if (!optionalEscrowAccount.isPresent()) {
      throw new ContractContextException("no escrow account was found");
    }

    // calculate new balance
    int newBalance = optionalEscrowAccount.get().data().getInt(BALANCE) - amount;

    // update escrow account
    JsonObjectBuilder builder =
        Json.createObjectBuilder()
            .add(BALANCE, newBalance)
            .add(ORDER_ID, orderId)
            .add(TIMESTAMP, timestamp);
    ledger.put(escrowAccountAssetId, builder.build());

    return null;
  }
}
