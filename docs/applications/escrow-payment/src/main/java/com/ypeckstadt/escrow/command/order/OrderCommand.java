package com.ypeckstadt.escrow.command.order;

import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
    name = "order",
    description = "manage orders",
    subcommands = {
      ViewOrderHistoryCommand.class,
      AddOrderCommand.class,
      ShippedItemsOrderCommand.class,
      ReceivedItemsOrderCommand.class,
      CancelOrderCommand.class,
    })
public class OrderCommand implements Callable<Integer> {

  @CommandLine.Spec CommandLine.Model.CommandSpec spec;

  @Override
  public Integer call() throws Exception {
    throw new CommandLine.ParameterException(spec.commandLine(), "Missing required subcommand");
  }
}
