package com.ypeckstadt.escrow.contract.account;

import com.scalar.dl.ledger.asset.Asset;
import com.scalar.dl.ledger.contract.Contract;
import com.scalar.dl.ledger.database.Ledger;
import com.scalar.dl.ledger.exception.ContractContextException;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.Optional;

public class DebitEscrowAccount extends Contract {

  private static final String BUYER_ACCOUNT_ID = "buyer_account_id";
  private static final String SELLER_ACCOUNT_ID = "seller_account_id";
  private static final String AMOUNT = "amount";
  private static final String ACCOUNT_ASSET_TYPE = "account";
  private static final String BALANCE = "balance";
  private static final String TIMESTAMP = "timestamp";
  private static final String ORDER_ID = "order_id";
  private static final String ESCROW_ACCOUNT_ASSET_TYPE = "escrow";

  @Override
  public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> properties) {

    // Check input arguments
    if (!argument.containsKey(BUYER_ACCOUNT_ID)
        || !argument.containsKey(SELLER_ACCOUNT_ID)
        || !argument.containsKey(TIMESTAMP)
        || !argument.containsKey(ORDER_ID)
        || !argument.containsKey(AMOUNT)) {
      throw new ContractContextException("wrong or missing arguments to debit an escrow account");
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

    // check buyer's account sufficient funds
    JsonObject buyerAccount = optionalBuyerAsset.get().data();
    int balance = buyerAccount.getInt(BALANCE);
    if (balance < amount) {
      throw new ContractContextException("the buyer's account has insufficient funds");
    }

    // retrieve escrow account
    String escrowAccountAssetId =
        ESCROW_ACCOUNT_ASSET_TYPE + buyerAccountId + "_" + sellerAccountId;
    Optional<Asset> optionalEscrowAccount = ledger.get(escrowAccountAssetId);

    // calculate new balance
    int newBalance = amount;
    if (optionalEscrowAccount.isPresent()) {
      newBalance += optionalEscrowAccount.get().data().getInt(BALANCE);
    }

    // update escrow account
    JsonObjectBuilder builder =
        Json.createObjectBuilder()
            .add(BALANCE, newBalance)
            .add(ORDER_ID, orderId)
            .add(SELLER_ACCOUNT_ID, sellerAccountId)
            .add(BUYER_ACCOUNT_ID, buyerAccountId)
            .add(TIMESTAMP, timestamp);
    ledger.put(escrowAccountAssetId, builder.build());

    return null;
  }
}
