package com.ypeckstadt.escrow.command.order;

import com.ypeckstadt.escrow.contract.account.AddAccount;
import com.ypeckstadt.escrow.contract.account.ChargeAccount;
import com.ypeckstadt.escrow.contract.order.CancelOrder;
import com.ypeckstadt.escrow.dl.LedgerClientExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.Date;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "cancel")
public class CancelOrderCommand extends LedgerClientExecutor implements Callable {

  private static final Logger LOG = LogManager.getLogger(CancelOrderCommand.class);

  @CommandLine.Option(
      names = {"-id", "--orderId"},
      paramLabel = "ORDER",
      description = "order id",
      required = true)
  String orderId;

  @CommandLine.Option(
      names = {"-a", "--accountId"},
      paramLabel = "ACCOUNT",
      description = "buyer or seller account id",
      required = true)
  String accountId;

  @Override
  public Integer call() throws Exception {
    JsonObject argument =
        Json.createObjectBuilder()
            .add(CancelOrder.ACCOUNT_ID, accountId)
            .add(CancelOrder.TIMESTAMP, new Date().getTime())
            .add(CancelOrder.ID, orderId)
            .build();

    try {
      executeContract(CancelOrder.class.getSimpleName(), argument, true);
      LOG.info("The order has been cancelled successfully");
    } catch (Exception e) {
      LOG.error(e.getMessage());
    }

    return 0;
  }
}
