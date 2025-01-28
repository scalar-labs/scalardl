package com.scalar.dl.client.tool;

import static picocli.CommandLine.Model.UsageMessageSpec.SECTION_KEY_COMMAND_LIST;
import static picocli.CommandLine.Model.UsageMessageSpec.SECTION_KEY_COMMAND_LIST_HEADING;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

@Command(
    name = "scalardl",
    subcommands = {
      CertificateRegistration.class,
      ContractExecution.class,
      ContractRegistration.class,
      ContractsListing.class,
      ContractsRegistration.class,
      FunctionRegistration.class,
      FunctionsRegistration.class,
      HelpCommand.class,
      LedgerValidation.class,
      SecretRegistration.class,
    },
    description = {"These are ScalarDL commands used in various situations:"})
public class ScalarDlCommandLine {

  public static void main(String[] args) {
    CommandLine commandLine = new CommandLine(new ScalarDlCommandLine());
    setupSections(commandLine);

    int exitCode = commandLine.execute(args);
    System.exit(exitCode);
  }

  /**
   * Changes the usage message (a.k.a help information) of the {@link CommandLine} into the grouped
   * sections.
   *
   * <p>[Note]
   *
   * <p>Please set up all the sections in this method.
   *
   * <p>Please refer to the [Usage] comment described in {@link CommandGroupRenderer}.
   *
   * @param cmd command line of {@link ScalarDlCommandLine}
   */
  @VisibleForTesting
  static void setupSections(CommandLine cmd) {
    // ref. Code to group subcommands from the official documentation.
    //      https://github.com/remkop/picocli/issues/978#issuecomment-604174211

    ImmutableMap.Builder<String, List<Class<?>>> sections = ImmutableMap.builder();
    // Section: register identity information.
    sections.put(
        "%nregister identity information%n",
        Arrays.asList(CertificateRegistration.class, SecretRegistration.class));
    // Section: register business logic.
    sections.put(
        "%nregister business logic%n",
        Arrays.asList(
            ContractRegistration.class,
            ContractsRegistration.class,
            FunctionRegistration.class,
            FunctionsRegistration.class));
    // Section: execute and list the registered business logic.
    sections.put(
        "%nexecute and list the registered business logic%n",
        Arrays.asList(ContractExecution.class, ContractsListing.class));
    // Section: validate ledger.
    sections.put("%nvalidate ledger%n", Collections.singletonList(LedgerValidation.class));
    CommandGroupRenderer renderer = new CommandGroupRenderer(sections.build());

    cmd.getHelpSectionMap().remove(SECTION_KEY_COMMAND_LIST_HEADING);
    cmd.getHelpSectionMap().put(SECTION_KEY_COMMAND_LIST, renderer);
  }
}
