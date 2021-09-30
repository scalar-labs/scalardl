package com.ypeckstadt.escrow.command.item;

import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.ypeckstadt.escrow.contract.item.ViewItem;
import com.ypeckstadt.escrow.dl.LedgerClientExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "view"
)
public class ViewItemCommand extends LedgerClientExecutor implements Callable {

    private static final Logger LOG = LogManager.getLogger(ViewItemCommand.class);

    @CommandLine.Option(names = {"-id", "--itemId"}, paramLabel = "ITEM", description = "the item id", required = true)
    String itemId;

    @Override
    public Integer call() throws Exception {
        JsonObject argument =
                Json.createObjectBuilder()
                        .add(ViewItem.ID, itemId)
                        .build();

        try {
            // Execute contract
            ContractExecutionResult result = executeContract(ViewItem.class.getSimpleName(), argument, true);

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
