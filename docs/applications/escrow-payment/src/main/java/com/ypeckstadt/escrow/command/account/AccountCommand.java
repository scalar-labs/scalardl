package com.ypeckstadt.escrow.command.account;

import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "account",
        description = "manage accounts",
        subcommands = {
                AddAccountCommand.class,
                ViewAccountHistoryCommand.class,
                ChargeAccountCommand.class
        }
)
public class AccountCommand implements Callable<Integer> {

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public Integer call() throws Exception {
        throw new CommandLine.ParameterException(spec.commandLine(), "Missing required subcommand");
    }
}

