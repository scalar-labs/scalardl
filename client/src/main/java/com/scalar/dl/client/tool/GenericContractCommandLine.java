package com.scalar.dl.client.tool;

import static picocli.CommandLine.Model.UsageMessageSpec.SECTION_KEY_COMMAND_LIST;
import static picocli.CommandLine.Model.UsageMessageSpec.SECTION_KEY_COMMAND_LIST_HEADING;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

@Command(
    name = Common.SCALARDL_GC_SUBCOMMAND_NAME,
    aliases = Common.SCALARDL_GC_ALIAS,
    subcommands = {
      CertificateRegistration.class,
      ContractExecution.class,
      ContractRegistration.class,
      ContractsListing.class,
      ContractsRegistration.class,
      FunctionRegistration.class,
      FunctionsRegistration.class,
      GenericContractLedgerValidation.class,
      HelpCommand.class,
      SecretRegistration.class,
    },
    description = {"Run commands for generic-contracts-based setup."})
public class GenericContractCommandLine {

  public static void main(String[] args) {
    CommandLine commandLine = new CommandLine(new GenericContractCommandLine());
    commandLine.setCommandName(Common.SCALARDL_GC_COMMAND_NAME);
    setupSections(commandLine);

    int exitCode = commandLine.execute(args);
    System.exit(exitCode);
  }

  static void setupSections(CommandLine cmd) {
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
