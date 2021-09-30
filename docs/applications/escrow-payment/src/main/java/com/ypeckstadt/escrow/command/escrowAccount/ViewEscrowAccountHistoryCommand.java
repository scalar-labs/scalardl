package com.ypeckstadt.escrow.command.escrowAccount;

import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.ypeckstadt.escrow.contract.escrowAccount.ViewEscrowAccountHistory;
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
public class ViewEscrowAccountHistoryCommand extends LedgerClientExecutor implements Callable {

    private static final Logger LOG = LogManager.getLogger(ViewEscrowAccountHistoryCommand.class);

    @CommandLine.Option(names = {"-b", "--buyerId"}, paramLabel = "ID", description = "buyer account id", required = true)
    String buyerAccountId;

    @CommandLine.Option(names = {"-s", "--sellerId"}, paramLabel = "ID", description = "seller account id", required = true)
    String sellerAccountId;

    @Override
    public Integer call() throws Exception {
        JsonObject argument =
                Json.createObjectBuilder()
                        .add(ViewEscrowAccountHistory.BUYER_ACCOUNT_ID, buyerAccountId)
                        .add(ViewEscrowAccountHistory.SELLER_ACCOUNT_ID, sellerAccountId)
                        .build();

        try {
            // Execute contract
            ContractExecutionResult result = executeContract(ViewEscrowAccountHistory.class.getSimpleName(), argument, true);

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
