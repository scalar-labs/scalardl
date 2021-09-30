package com.ypeckstadt.escrow;


import com.ypeckstadt.escrow.command.account.AccountCommand;
import com.ypeckstadt.escrow.command.escrowAccount.EscrowAccountCommand;
import com.ypeckstadt.escrow.command.item.ItemCommand;
import com.ypeckstadt.escrow.command.order.OrderCommand;
import picocli.CommandLine;

import java.io.IOException;

@CommandLine.Command(name = "app", description = "Scalar DL escrow demo CLI",
        mixinStandardHelpOptions = true, version = "1.0",
        subcommands = {AccountCommand.class, ItemCommand.class, OrderCommand.class, EscrowAccountCommand.class})
public class Main {
    public static void main(String[] args) throws IOException {
        // Display the help if no arguments are passed
        String[] commandArgs = args.length != 0 ? args : new String[] {"--help"};
        int exitCode = new CommandLine(new Main()).execute(commandArgs);
        System.exit(exitCode);
    }
}
