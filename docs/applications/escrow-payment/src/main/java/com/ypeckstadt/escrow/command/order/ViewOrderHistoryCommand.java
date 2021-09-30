package com.ypeckstadt.escrow.command.order;

import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.ypeckstadt.escrow.contract.order.ViewOrderHistory;
import com.ypeckstadt.escrow.dl.LedgerClientExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "view")
public class ViewOrderHistoryCommand extends LedgerClientExecutor implements Callable {

  private static final Logger LOG = LogManager.getLogger(ViewOrderHistoryCommand.class);

  @CommandLine.Option(
      names = {"-id", "--orderId"},
      paramLabel = "ID",
      description = "the order id",
      required = true)
  String orderId;

  @Override
  public Integer call() throws Exception {

    // prepare contract arguments
    JsonObject argument = Json.createObjectBuilder().add(ViewOrderHistory.ID, orderId).build();

    try {
      // Execute contract
      ContractExecutionResult result =
          executeContract(ViewOrderHistory.class.getSimpleName(), argument, true);

      // parse result
      if (result.getResult().isPresent()) {
        JsonObject jsonObject = result.getResult().get();
        prettyPrintJson(jsonObject);
      }
    } catch (Exception e) {
      LOG.error(e.getMessage());
    }

    return 0;
  }
}
