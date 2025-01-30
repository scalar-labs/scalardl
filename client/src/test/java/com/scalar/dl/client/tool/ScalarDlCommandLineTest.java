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
          new StringBuilder()
              .append("Usage: scalardl [COMMAND]\n")
              .append("These are ScalarDL commands used in various situations:\n")
              .append("\n")
              .append("register identity information\n")
              .append("  register-cert       Register a specified certificate.\n")
              .append("  register-secret     Register a specified secret.\n")
              .append("\n")
              .append("register business logic\n")
              .append("  register-contract   Register a specified contract.\n")
              .append("  register-contracts  Register specified contracts.\n")
              .append("  register-function   Register a specified function.\n")
              .append("  register-functions  Register specified functions.\n")
              .append("\n")
              .append("execute and list the registered business logic\n")
              .append("  execute-contract    Execute a specified contract.\n")
              .append("  list-contracts      List registered contracts.\n")
              .append("\n")
              .append("validate ledger\n")
              .append("  validate-ledger     Validate a specified asset in a ledger.\n")
              .toString();
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
