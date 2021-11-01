package com.ypeckstadt.escrow.command.item;

import com.ypeckstadt.escrow.contract.item.AddItem;
import com.ypeckstadt.escrow.dl.LedgerClientExecutor;
import java.util.Date;
import java.util.concurrent.Callable;
import javax.json.Json;
import javax.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

@CommandLine.Command(name = "add")
public class AddItemCommand extends LedgerClientExecutor implements Callable {

  private static final Logger LOG = LogManager.getLogger(AddItemCommand.class);

  @CommandLine.Option(
      names = {"-id", "--id"},
      paramLabel = "ID",
      description = "item id",
      required = true)
  String itemId;

  @CommandLine.Option(
      names = {"-n", "--name"},
      paramLabel = "NAME",
      description = "item name",
      required = true)
  String name;

  @CommandLine.Option(
      names = {"-p", "--price"},
      paramLabel = "PRICE",
      description = "item price",
      required = true)
  int price;

  @CommandLine.Option(
      names = {"-s", "--seller"},
      paramLabel = "SELLER",
      description = "seller's account id",
      required = true)
  String seller;

  @Override
  public Integer call() throws Exception {
    JsonObject argument =
        Json.createObjectBuilder()
            .add(AddItem.NAME, name)
            .add(AddItem.SELLER_ACCOUNT_ID, seller)
            .add(AddItem.PRICE, price)
            .add(AddItem.TIMESTAMP, new Date().getTime())
            .add(AddItem.ID, itemId)
            .build();

    try {
      executeContract(AddItem.class.getSimpleName(), argument, true);
      LOG.info("The item has been added successfully");
    } catch (Exception e) {
      LOG.error(e.getMessage());
    }
    return 0;
  }
}
