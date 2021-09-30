package com.ypeckstadt.escrow.command.order;

import com.ypeckstadt.escrow.contract.order.SetOrderToReceivedStatus;
import com.ypeckstadt.escrow.contract.order.SetOrderToShippedStatus;
import com.ypeckstadt.escrow.dl.LedgerClientExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.Date;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "received")
public class ReceivedItemsOrderCommand extends LedgerClientExecutor implements Callable {

  private static final Logger LOG = LogManager.getLogger(ReceivedItemsOrderCommand.class);

  @CommandLine.Option(
      names = {"-id", "--orderId"},
      paramLabel = "ORDER ID",
      description = "the order id",
      required = true)
  String orderId;

  @CommandLine.Option(
      names = {"-b", "--buyerId"},
      paramLabel = "BUYER ID",
      description = "the buyer's account id",
      required = true)
  String buyerAccountId;

  @Override
  public Integer call() throws Exception {
    JsonObject argument =
        Json.createObjectBuilder()
            .add(SetOrderToReceivedStatus.ID, orderId)
            .add(SetOrderToReceivedStatus.BUYER_ACCOUNT_ID, buyerAccountId)
            .add(SetOrderToReceivedStatus.TIMESTAMP, new Date().getTime())
            .build();

    try {
      executeContract(SetOrderToReceivedStatus.class.getSimpleName(), argument, true);
      LOG.info("The order has completed.");
    } catch (Exception e) {
      LOG.error(e.getMessage());
    }
    return 0;
  }
}
