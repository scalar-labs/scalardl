package com.scalar.dl.client.tool;

import static com.scalar.dl.client.tool.GenericContractCommandLine.setupSections;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

public class GenericContractCommandLineTest {
  private CommandLine commandLine;

  @BeforeEach
  void setup() {
    commandLine = new CommandLine(new GenericContractCommandLine());
    setupSections(commandLine);
  }

  @Nested
  @DisplayName("#getUsageCommand()")
  class getUsageCommand {
    @Test
    @DisplayName("displays the grouped sub-subcommands")
    void displaysGroupedSubcommands() {
      // Act
      String actual = commandLine.getUsageMessage();

      // Assert
      String expected =
          String.join(
              System.lineSeparator(),
              "Usage: generic-contracts [COMMAND]",
              "Run commands for generic-contracts-based setup.",
              "",
              "register identity information",
              "  register-cert       Register a specified certificate.",
              "  register-secret     Register a specified secret.",
              "",
              "register business logic",
              "  register-contract   Register a specified contract.",
              "  register-contracts  Register specified contracts.",
              "  register-function   Register a specified function.",
              "  register-functions  Register specified functions.",
              "",
              "execute and list the registered business logic",
              "  execute-contract    Execute a specified contract.",
              "  list-contracts      List registered contracts.",
              "",
              "validate ledger",
              "  validate-ledger     Validate a specified asset in a ledger.",
              "");
      assertThat(actual).isEqualTo(expected);
    }
  }

  @Nested
  @DisplayName("@Command annotation")
  @SuppressWarnings("ClassCanBeStatic")
  class CommandAnnotation {
    private Command command;

    @BeforeEach
    void setup() {
      command = GenericContractCommandLine.class.getAnnotation(Command.class);
    }

    @Test
    @DisplayName("member values are properly set")
    void memberValuesAreProperlySet() {
      assertThat(command.name()).isEqualTo("generic-contracts");
      assertThat(command.subcommands())
          .isEqualTo(
              new Class[] {
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
              });
    }
  }
}
