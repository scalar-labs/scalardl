package com.ypeckstadt.escrow.command.account;

import static com.ypeckstadt.escrow.common.Constants.ACCOUNT_ID;

import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.ypeckstadt.escrow.contract.account.ViewAccountHistory;
import com.ypeckstadt.escrow.dl.LedgerClientExecutor;
import java.util.concurrent.Callable;
import javax.json.Json;
import javax.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

@CommandLine.Command(name = "view")
public class ViewAccountHistoryCommand extends LedgerClientExecutor implements Callable {

  private static final Logger LOG = LogManager.getLogger(ViewAccountHistoryCommand.class);

  @CommandLine.Option(
      names = {"-id", "--id"},
      paramLabel = "ID",
      description = "the account id",
      required = true)
  String accountId;

  @Override
  public Integer call() throws Exception {
    JsonObject argument = Json.createObjectBuilder().add(ACCOUNT_ID, accountId).build();

    try {
      // Execute contract
      ContractExecutionResult result =
          executeContract(ViewAccountHistory.class.getSimpleName(), argument, true);

      // pretty print result
      result.getResult().ifPresent(this::prettyPrintJson);
    } catch (Exception e) {
      LOG.error(e.getMessage());
    }
    return 0;
  }
}
