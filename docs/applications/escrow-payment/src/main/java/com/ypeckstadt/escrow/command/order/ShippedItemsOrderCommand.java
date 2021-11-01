package com.ypeckstadt.escrow.command.order;

import com.ypeckstadt.escrow.contract.order.SetOrderToShippedStatus;
import com.ypeckstadt.escrow.dl.LedgerClientExecutor;
import java.util.Date;
import java.util.concurrent.Callable;
import javax.json.Json;
import javax.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

@CommandLine.Command(name = "shipped")
public class ShippedItemsOrderCommand extends LedgerClientExecutor implements Callable {

  private static final Logger LOG = LogManager.getLogger(ShippedItemsOrderCommand.class);

  @CommandLine.Option(
      names = {"-id", "--orderId"},
      paramLabel = "ORDER ID",
      description = "the order id",
      required = true)
  String orderId;

  @CommandLine.Option(
      names = {"-s", "--sellerId"},
      paramLabel = "SELLER ID",
      description = "the seller's account id",
      required = true)
  String sellerAccountId;

  @Override
  public Integer call() throws Exception {
    JsonObject argument =
        Json.createObjectBuilder()
            .add(SetOrderToShippedStatus.ID, orderId)
            .add(SetOrderToShippedStatus.SELLER_ACCOUNT_ID, sellerAccountId)
            .add(SetOrderToShippedStatus.TIMESTAMP, new Date().getTime())
            .build();

    try {
      executeContract(SetOrderToShippedStatus.class.getSimpleName(), argument, true);
      LOG.info(
          "The order has been updated successfully. Waiting for the buyer to mark the item as"
              + " received.");
    } catch (Exception e) {
      LOG.error(e.getMessage());
    }
    return 0;
  }
}
