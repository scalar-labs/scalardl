package com.ypeckstadt.escrow.command.escrowAccount;

import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(
    name = "escrow",
    description = "view escrow accounts history",
    subcommands = {
      ViewEscrowAccountHistoryCommand.class,
    })
public class EscrowAccountCommand implements Callable<Integer> {

  @CommandLine.Spec CommandLine.Model.CommandSpec spec;

  @Override
  public Integer call() throws Exception {
    throw new CommandLine.ParameterException(spec.commandLine(), "Missing required subcommand");
  }
}
