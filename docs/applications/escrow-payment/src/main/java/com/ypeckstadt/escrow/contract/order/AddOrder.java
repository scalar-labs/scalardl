package com.ypeckstadt.escrow.contract.order;

import com.scalar.dl.ledger.asset.Asset;
import com.scalar.dl.ledger.contract.Contract;
import com.scalar.dl.ledger.database.Ledger;
import com.scalar.dl.ledger.exception.ContractContextException;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.Optional;

public class AddOrder extends Contract {


    public static final String BUYER_ACCOUNT_ID = "buyer_account_id";
    public static final String ITEM_ID = "item_id";
    public static final String TIMESTAMP = "timestamp";
    public static final String ID = "id";

    private static final String ITEM_ASSET_TYPE = "item";
    private static final String ACCOUNT_ASSET_TYPE = "account";
    private static final String ORDER_ASSET_TYPE = "order";
    private static final String ACCOUNT_ID = "account_id";
    private static final String ITEM_PRICE = "price";
    private static final String TOTAL = "total";
    private static final String SELLER_ACCOUNT_ID = "seller_account_id";
    private static final String ORDER_ID = "order_id";
    private static final String STATUS = "status";
    private static final String DEBIT_ESCROW_ACCOUNT_CONTRACT = "DebitEscrowAccount_foo";
    private static final String CREDIT_ACCOUNT_CONTRACT = "CreditAccount_foo";
    private static final String ORDER_STATUS_OPEN = "open";
    private static final String AMOUNT = "amount";

    @Override
    public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> properties) {

        // Check input arguments
        if (!argument.containsKey(BUYER_ACCOUNT_ID)
                || !argument.containsKey(ITEM_ID)
                || !argument.containsKey(ID)
                || !argument.containsKey(TIMESTAMP)) {
            throw new ContractContextException("wrong or missing arguments to create an order");
        }

        // get field data from arguments
        String itemId = argument.getString(ITEM_ID);
        String buyerAccountId = argument.getString(BUYER_ACCOUNT_ID);
        long timestamp = argument.getJsonNumber(TIMESTAMP).longValue();
        String orderId = argument.getString(ID);

        // determine asset ids
        String itemAssetId = ITEM_ASSET_TYPE + "_" + itemId;
        String buyerAccountAssetId = ACCOUNT_ASSET_TYPE + "_" + buyerAccountId;

        // check if the item exists
        Optional<Asset> optionalItemAsset = ledger.get(itemAssetId);
        if (!optionalItemAsset.isPresent()) {
            throw new ContractContextException("the item does not exist");
        }

        // check if the buyer's account exists
        Optional<Asset> optionalBuyerAccountAsset = ledger.get(buyerAccountAssetId);
        if (!optionalBuyerAccountAsset.isPresent()) {
            throw new ContractContextException("the account does not exist");
        }

        // get json object data
        JsonObject item = optionalItemAsset.get().data();
        int itemPrice = item.getInt(ITEM_PRICE);
        String sellerAccountId = item.getString(SELLER_ACCOUNT_ID);


        // Debit escrow and credit are split into 2 separate contracts to not make the contract to heavy and test calling external contracts.
        // In a normal situation both actions will always follow each other so it makes more sense to put it into one contract

        // Debit escrow account
        JsonObject escrowArguments = Json.createObjectBuilder()
                .add(BUYER_ACCOUNT_ID, buyerAccountId)
                .add(SELLER_ACCOUNT_ID, sellerAccountId)
                .add(AMOUNT, itemPrice)
                .add(ORDER_ID, orderId)
                .add(TIMESTAMP, timestamp)
                .build();
        invoke(DEBIT_ESCROW_ACCOUNT_CONTRACT, ledger, escrowArguments);

        // Credit buyer's account
        JsonObject creditArguments = Json.createObjectBuilder()
                .add(ACCOUNT_ID, buyerAccountId)
                .add(AMOUNT, itemPrice)
                .add(TIMESTAMP, timestamp)
                .build();
        invoke(CREDIT_ACCOUNT_CONTRACT, ledger, creditArguments);

        // asset json object builder
        JsonObjectBuilder builder = Json.createObjectBuilder()
                .add(ID, orderId)
                .add(ITEM_ID, itemId)
                .add(BUYER_ACCOUNT_ID, buyerAccountId)
                .add(SELLER_ACCOUNT_ID, sellerAccountId)
                .add(STATUS, ORDER_STATUS_OPEN)
                .add(TOTAL, itemPrice)
                .add(TIMESTAMP, timestamp);

        // add order record
        String orderAssetId = ORDER_ASSET_TYPE + "_" + orderId;
        ledger.put(orderAssetId, builder.build());

        return null;
    }
}