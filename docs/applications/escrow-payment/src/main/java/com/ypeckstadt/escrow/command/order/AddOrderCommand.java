package com.ypeckstadt.escrow.command.order;

import com.ypeckstadt.escrow.contract.order.AddOrder;
import com.ypeckstadt.escrow.dl.LedgerClientExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.Date;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "add")
public class AddOrderCommand extends LedgerClientExecutor implements Callable {

  private static final Logger LOG = LogManager.getLogger(AddOrderCommand.class);

  @CommandLine.Option(
      names = {"-id", "--id"},
      paramLabel = "ID",
      description = "order id",
      required = true)
  String orderId;

  @CommandLine.Option(
      names = {"-b", "--buyer"},
      paramLabel = "BUYER",
      description = "buyer's account id",
      required = true)
  String buyerAccountId;

  @CommandLine.Option(
      names = {"-i", "--item"},
      paramLabel = "ITEM",
      description = "item id",
      required = true)
  String itemId;

  @Override
  public Integer call() throws Exception {
    // Add order
    JsonObject argument =
        Json.createObjectBuilder()
            .add(AddOrder.ID, orderId)
            .add(AddOrder.BUYER_ACCOUNT_ID, buyerAccountId)
            .add(AddOrder.ITEM_ID, itemId)
            .add(AddOrder.TIMESTAMP, new Date().getTime())
            .build();

    try {
      executeContract(AddOrder.class.getSimpleName(), argument, true);
      LOG.info(
          "The order has been added successfully. Waiting for the seller to mark the item as shipped.");
    } catch (Exception e) {
      LOG.error(e.getMessage());
    }
    return 0;
  }
}
