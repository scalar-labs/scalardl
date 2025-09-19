package com.scalar.dl.tablestore.client.tool;

import static picocli.CommandLine.Model.UsageMessageSpec.SECTION_KEY_COMMAND_LIST;
import static picocli.CommandLine.Model.UsageMessageSpec.SECTION_KEY_COMMAND_LIST_HEADING;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.scalar.dl.client.tool.CommandGroupRenderer;
import java.util.Collections;
import java.util.List;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

@Command(
    name = "scalardl-tablestore",
    subcommands = {
      Bootstrap.class,
      HelpCommand.class,
      LedgerValidation.class,
      StatementExecution.class,
    },
    description = {"These are ScalarDL TableStore commands:"})
public class TableStoreCommandLine {

  public static void main(String[] args) {
    CommandLine commandLine = new CommandLine(new TableStoreCommandLine());
    setupSections(commandLine);

    int exitCode = commandLine.execute(args);
    System.exit(exitCode);
  }

  /**
   * Changes the usage message (a.k.a. help information) of the {@link CommandLine} into the grouped
   * sections.
   *
   * <p>[Note]
   *
   * <p>Please set up all the sections in this method.
   *
   * <p>Please refer to the [Usage] comment described in {@link CommandGroupRenderer}.
   *
   * @param cmd command line of {@link TableStoreCommandLine}
   */
  @VisibleForTesting
  static void setupSections(CommandLine cmd) {
    // ref. Code to group subcommands from the official documentation.
    //      https://github.com/remkop/picocli/issues/978#issuecomment-604174211

    ImmutableMap.Builder<String, List<Class<?>>> sections = ImmutableMap.builder();
    // Section: bootstrap
    sections.put("%nbootstrap the table store%n", Collections.singletonList(Bootstrap.class));
    // Section: execute a statement.
    sections.put("%nexecute a statement%n", Collections.singletonList(StatementExecution.class));
    // Section: validate ledger.
    sections.put("%nvalidate ledger%n", Collections.singletonList(LedgerValidation.class));
    CommandGroupRenderer renderer = new CommandGroupRenderer(sections.build());

    cmd.getHelpSectionMap().remove(SECTION_KEY_COMMAND_LIST_HEADING);
    cmd.getHelpSectionMap().put(SECTION_KEY_COMMAND_LIST, renderer);
  }
}
