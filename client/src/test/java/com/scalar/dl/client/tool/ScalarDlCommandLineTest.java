package com.scalar.dl.client.tool;

import static com.scalar.dl.client.tool.ScalarDlCommandLine.setupSections;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.UnmatchedArgumentException;

public class ScalarDlCommandLineTest {
  private CommandLine commandLine;

  @BeforeEach
  void setup() {
    commandLine = new CommandLine(new ScalarDlCommandLine());
    setupSections(commandLine);
  }

  @Nested
  @DisplayName("#getUsageCommand()")
  class getUsageCommand {
    @Test
    @DisplayName("displays the grouped subcommands")
    void displaysGroupedSubcommands() {
      // Act
      String actual = commandLine.getUsageMessage();

      // Assert
      String expected =
          String.join(
              System.lineSeparator(),
              "Usage: scalardl [COMMAND]",
              "These are ScalarDL commands used in various situations:",
              "",
              "bootstrap the ledger",
              "  bootstrap              Bootstrap the ledger by registering identity and",
              "                           system contracts.",
              "",
              "register identity information manually",
              "  register-cert          Register a specified certificate.",
              "  register-secret        Register a specified secret.",
              "",
              "register business logic",
              "  register-contract      Register a specified contract.",
              "  register-contracts     Register specified contracts.",
              "  register-function      Register a specified function.",
              "  register-functions     Register specified functions.",
              "",
              "execute and list the registered business logic",
              "  execute-contract       Execute a specified contract.",
              "  list-contracts         List registered contracts.",
              "",
              "validate ledger",
              "  validate-ledger        Validate a specified asset in a ledger.",
              "",
              "run commands for generic-contracts-based setup",
              "  generic-contracts, gc  Run commands for generic-contracts-based setup.",
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
      command = ScalarDlCommandLine.class.getAnnotation(Command.class);
    }

    @Test
    @DisplayName("member values are properly set")
    void memberValuesAreProperlySet() {
      assertThat(command.name()).isEqualTo("scalardl");
      assertThat(command.subcommands())
          .isEqualTo(
              new Class[] {
                Bootstrap.class,
                GenericContractCommandLine.class,
                CertificateRegistration.class,
                ContractExecution.class,
                ContractRegistration.class,
                ContractsListing.class,
                ContractsRegistration.class,
                FunctionRegistration.class,
                FunctionsRegistration.class,
                CommandLine.HelpCommand.class,
                LedgerValidation.class,
                SecretRegistration.class,
              });
    }
  }

  @Nested
  @DisplayName("#parseArgs(...)")
  class parseArgs {
    @Nested
    @DisplayName("without arguments")
    class withoutArguments {
      @Test
      public void parseCommandSucceeds() {
        // Arrange
        String[] args = {};

        // Act
        ParseResult parseResult = commandLine.parseArgs(args);
        List<CommandLine> parsed = parseResult.asCommandLineList();

        // Assert
        // Verify that the argument contains only the top-level command.
        assertThat(parsed.size()).isEqualTo(1);

        // Verify that the top-level command is "scalardl".
        assertThat(parsed.get(0).getCommand().getClass()).isEqualTo(ScalarDlCommandLine.class);
      }
    }

    @Nested
    @DisplayName("with invalid subcommand that is not configured")
    class withInvalidSubcommand {
      @Test
      void throwsUnmatchedArgumentException() {
        // Arrange
        String[] args = new String[] {"invalid-subcommand"};

        // Act & Assert
        assertThatThrownBy(() -> commandLine.parseArgs(args))
            .isInstanceOf(UnmatchedArgumentException.class)
            .hasMessageContaining("Unmatched argument at index 0: 'invalid-subcommand'");
      }
    }
  }
}
