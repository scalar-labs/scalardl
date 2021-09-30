package com.ypeckstadt.escrow.command.item;

import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "item",
        description = "manage items",
        subcommands = {
                AddItemCommand.class,
                ViewItemCommand.class,
        }
)
public class ItemCommand implements Callable<Integer> {

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public Integer call() throws Exception {
        throw new CommandLine.ParameterException(spec.commandLine(), "Missing required subcommand");
    }
}

