package com.ypeckstadt.escrow.command.account;

import com.ypeckstadt.escrow.contract.account.AddAccount;
import com.ypeckstadt.escrow.contract.account.ChargeAccount;
import com.ypeckstadt.escrow.dl.LedgerClientExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.Date;
import java.util.concurrent.Callable;

import static com.ypeckstadt.escrow.common.Constants.ACCOUNT_ID;
import static com.ypeckstadt.escrow.common.Constants.ACCOUNT_TIMESTAMP;

@CommandLine.Command(name = "charge")
public class ChargeAccountCommand extends LedgerClientExecutor implements Callable {

  private static final Logger LOG = LogManager.getLogger(ChargeAccountCommand.class);

  @CommandLine.Option(
      names = {"-id", "--id"},
      paramLabel = "ID",
      description = "account id",
      required = true)
  String accountId;

  @CommandLine.Option(
      names = {"-a", "--amount"},
      paramLabel = "AMOUNT",
      description = "amount to charge",
      required = true)
  int amount;

  @Override
  public Integer call() throws Exception {
    // Charge account
    JsonObject argument =
        Json.createObjectBuilder()
            .add(ChargeAccount.AMOUNT, amount)
            .add(ACCOUNT_TIMESTAMP, new Date().getTime())
            .add(ACCOUNT_ID, accountId)
            .build();

    try {
      executeContract(ChargeAccount.class.getSimpleName(), argument, true);
      LOG.info("Funds have been added to the account successfully");
    } catch (Exception e) {
      LOG.error(e.getMessage());
    }

    return 0;
  }
}
