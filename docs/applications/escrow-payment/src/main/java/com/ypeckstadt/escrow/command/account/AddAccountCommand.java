package com.ypeckstadt.escrow.command.account;

import com.ypeckstadt.escrow.contract.account.AddAccount;
import com.ypeckstadt.escrow.dl.LedgerClientExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.Date;
import java.util.concurrent.Callable;

import static com.ypeckstadt.escrow.common.Constants.*;

@CommandLine.Command(name = "add")
public class AddAccountCommand extends LedgerClientExecutor implements Callable {

  private static final Logger LOG = LogManager.getLogger(AddAccountCommand.class);

  @CommandLine.Option(
      names = {"-id", "--id"},
      paramLabel = "ACCOUNT ID",
      description = "the account id",
      required = true)
  String accountId;

  @CommandLine.Option(
      names = {"-n", "--name"},
      paramLabel = "NAME",
      description = "the account owner's name",
      required = true)
  String name;

  @Override
  public Integer call() throws Exception {

    // Add account
    JsonObject argument =
        Json.createObjectBuilder()
            .add(ACCOUNT_NAME, name)
            .add(ACCOUNT_TIMESTAMP, new Date().getTime())
            .add(ACCOUNT_ID, accountId)
            .build();

    try {
      executeContract(AddAccount.class.getSimpleName(), argument, true);
      LOG.info("The account has been added successfully");
    } catch (Exception e) {
      LOG.error(e.getMessage());
    }
    return 0;
  }
}
