package com.ypeckstadt.escrow.contract.order;

import com.scalar.dl.ledger.asset.Asset;
import com.scalar.dl.ledger.contract.Contract;
import com.scalar.dl.ledger.database.Ledger;
import com.scalar.dl.ledger.exception.ContractContextException;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.Optional;

public class CancelOrder extends Contract {


    public static final String ID = "id";
    public static final String ACCOUNT_ID = "account_id";
    public static final String TIMESTAMP = "timestamp";

    private static final String ORDER_ASSET_TYPE = "order";
    private static final String STATUS = "status";
    private static final String ORDER_STATUS_OPEN = "open";
    private static final String ORDER_STATUS_CANCELLED = "cancelled";
    public static final String BUYER_ACCOUNT_ID = "buyer_account_id";
    private static final String SELLER_ACCOUNT_ID = "seller_account_id";
    private static final String TOTAL = "total";
    private static final String AMOUNT = "amount";
    private static final String ORDER_ID = "order_id";
    private static final String ITEM_ID = "item_id";
    private static final String DEBIT_ACCOUNT_CONTRACT = "DebitAccount_foo";
    private static final String CREDIT_ESCROW_ACCOUNT_CONTRACT = "CreditEscrowAccount_foo";



    @Override
    public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> properties) {

        // Check input arguments
        if (!argument.containsKey(ID)
                || !argument.containsKey(ACCOUNT_ID)
                || !argument.containsKey(TIMESTAMP)) {
            throw new ContractContextException("wrong or missing arguments to cancel an order");
        }

        // Get input data
        String orderId = argument.getString(ID);

        String accountId = argument.getString(ACCOUNT_ID);

        // Determine asset id
        String orderAssetId = ORDER_ASSET_TYPE + "_" + orderId;

        // check if order exists
        Optional<Asset> asset = ledger.get(orderAssetId);
        if (!asset.isPresent()) {
            throw new ContractContextException("no order with this id was found");
        }

        // Get order data
        JsonObject order = asset.get().data();
        String orderStatus = order.getString(STATUS);
        String buyerAccountId = order.getString(BUYER_ACCOUNT_ID);
        String sellerAccountId = order.getString(SELLER_ACCOUNT_ID);
        int orderTotal = order.getInt(TOTAL);
        long timestamp = order.getJsonNumber(TIMESTAMP).longValue();
        String itemId = order.getString(ITEM_ID);

        // check order status ,if still in open mode, cant be cancelled after
        if (!orderStatus.equals(ORDER_STATUS_OPEN)) {
            throw new ContractContextException("the order cannot be cancelled anymore");
        }

        // make sure the account id is a seller or buyer
        if (!accountId.equals(buyerAccountId) && !accountId.equals(sellerAccountId)) {
            throw new ContractContextException("the order does not belong to the account");
        }

        // Credit Escrow account
        JsonObject escrowArguments = Json.createObjectBuilder()
                .add(BUYER_ACCOUNT_ID, buyerAccountId)
                .add(SELLER_ACCOUNT_ID, sellerAccountId)
                .add(AMOUNT, orderTotal)
                .add(ORDER_ID, orderId)
                .add(TIMESTAMP, timestamp)
                .build();
        invoke(CREDIT_ESCROW_ACCOUNT_CONTRACT, ledger, escrowArguments);

        // Debit buyer account
        JsonObject debitArguments = Json.createObjectBuilder()
                .add(ACCOUNT_ID, buyerAccountId)
                .add(AMOUNT, orderTotal)
                .add(TIMESTAMP, timestamp)
                .build();
        invoke(DEBIT_ACCOUNT_CONTRACT, ledger, debitArguments);


        // update order, set to cancelled status
        JsonObjectBuilder builder = Json.createObjectBuilder()
                .add(ID, orderId)
                .add(ITEM_ID, itemId)
                .add(BUYER_ACCOUNT_ID, buyerAccountId)
                .add(SELLER_ACCOUNT_ID, sellerAccountId)
                .add(STATUS, ORDER_STATUS_CANCELLED)
                .add(TOTAL, orderTotal)
                .add(TIMESTAMP, timestamp);

        // update order record
        ledger.put(orderAssetId, builder.build());

        return null;

    }
}