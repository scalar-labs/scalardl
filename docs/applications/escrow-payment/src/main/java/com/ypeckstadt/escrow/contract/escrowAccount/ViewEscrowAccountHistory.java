package com.ypeckstadt.escrow.contract.escrowAccount;

import com.scalar.dl.ledger.asset.Asset;
import com.scalar.dl.ledger.contract.Contract;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.Ledger;
import com.scalar.dl.ledger.exception.ContractContextException;

import javax.json.*;
import java.util.List;
import java.util.Optional;

public class ViewEscrowAccountHistory extends Contract {

  public static final String SELLER_ACCOUNT_ID = "seller_account_id";
  public static final String BUYER_ACCOUNT_ID = "buyer_account_id";

  private static final String ESCROW_ACCOUNT_ASSET_TYPE = "escrow";
  private static final String BALANCE = "balance";
  private static final String TIMESTAMP = "timestamp";
  private static final String ORDER_ID = "order_id";
  private static final String HISTORY = "escrow_account_history";

  @Override
  public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> properties) {

    // Check input arguments
    if (!argument.containsKey(BUYER_ACCOUNT_ID) || !argument.containsKey(SELLER_ACCOUNT_ID)) {
      throw new ContractContextException("missing ID argument for escrow account lookup");
    }

    // Determine asset id
    String buyerAccountId = argument.getString(BUYER_ACCOUNT_ID);
    String sellerAccountId = argument.getString(SELLER_ACCOUNT_ID);
    String assetId = ESCROW_ACCOUNT_ASSET_TYPE + buyerAccountId + "_" + sellerAccountId;

    // Retrieve escrow account history
    AssetFilter filter = new AssetFilter(assetId);
    List<Asset> history = ledger.scan(filter);
    if (history.isEmpty()) {
      throw new ContractContextException(
          "no escrow account between this seller and buyer was found");
    }

    // build result
    JsonArrayBuilder builder = Json.createArrayBuilder();
    for (Asset h : history) {

      // read data
      JsonObject data = h.data();
      String orderId = data.getString(ORDER_ID);
      int balance = data.getInt(BALANCE);
      long timestamp = data.getJsonNumber(TIMESTAMP).longValue();

      // build json object result
      JsonObject account =
          Json.createObjectBuilder()
              .add(BALANCE, balance)
              .add(ORDER_ID, orderId)
              .add(SELLER_ACCOUNT_ID, sellerAccountId)
              .add(BUYER_ACCOUNT_ID, buyerAccountId)
              .add(TIMESTAMP, timestamp)
              .build();
      builder.add(account);
    }
    JsonArray orderHistory = builder.build();

    return Json.createObjectBuilder().add(HISTORY, orderHistory).build();
  }
}
