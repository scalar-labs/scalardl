package com.scalar.dl.tablestore.client.tool;

import static com.scalar.dl.tablestore.client.tool.TableStoreCommandLine.setupSections;
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

public class TableStoreCommandLineTest {
  private CommandLine commandLine;

  @BeforeEach
  void setup() {
    commandLine = new CommandLine(new TableStoreCommandLine());
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
              "Usage: scalardl-tablestore [COMMAND]",
              "These are ScalarDL TableStore commands:",
              "",
              "bootstrap the table store",
              "  bootstrap          Bootstrap the table store by registering identity and",
              "                       contracts.",
              "",
              "execute a statement",
              "  execute-statement  Execute a specified statement.",
              "",
              "validate ledger",
              "  validate-ledger    Validate a specified asset in the ledger.",
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
      command = TableStoreCommandLine.class.getAnnotation(Command.class);
    }

    @Test
    @DisplayName("member values are properly set")
    void memberValuesAreProperlySet() {
      assertThat(command.name()).isEqualTo("scalardl-tablestore");
      assertThat(command.subcommands())
          .isEqualTo(
              new Class[] {
                Bootstrap.class,
                CommandLine.HelpCommand.class,
                LedgerValidation.class,
                StatementExecution.class,
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

        // Verify that the top-level command is "scalardl-tablestore".
        assertThat(parsed.get(0).getCommand().getClass()).isEqualTo(TableStoreCommandLine.class);
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

    @Nested
    @DisplayName("with valid subcommands")
    class withValidSubcommands {
      @Test
      @DisplayName("execute-statement subcommand is parsed correctly")
      void executeStatementSubcommandIsParsedCorrectly() {
        // Arrange
        String[] args = new String[] {"execute-statement", "--help"};

        // Act
        ParseResult parseResult = commandLine.parseArgs(args);
        List<CommandLine> parsed = parseResult.asCommandLineList();

        // Assert
        assertThat(parsed.size()).isEqualTo(2);
        assertThat(parsed.get(0).getCommand().getClass()).isEqualTo(TableStoreCommandLine.class);
        assertThat(parsed.get(1).getCommand().getClass()).isEqualTo(StatementExecution.class);
      }

      @Test
      @DisplayName("bootstrap subcommand is parsed correctly")
      void registerContractsSubcommandIsParsedCorrectly() {
        // Arrange
        String[] args = new String[] {"bootstrap", "--help"};

        // Act
        ParseResult parseResult = commandLine.parseArgs(args);
        List<CommandLine> parsed = parseResult.asCommandLineList();

        // Assert
        assertThat(parsed.size()).isEqualTo(2);
        assertThat(parsed.get(0).getCommand().getClass()).isEqualTo(TableStoreCommandLine.class);
        assertThat(parsed.get(1).getCommand().getClass()).isEqualTo(Bootstrap.class);
      }

      @Test
      @DisplayName("validate-ledger subcommand is parsed correctly")
      void validateLedgerSubcommandIsParsedCorrectly() {
        // Arrange
        String[] args = new String[] {"validate-ledger", "--help"};

        // Act
        ParseResult parseResult = commandLine.parseArgs(args);
        List<CommandLine> parsed = parseResult.asCommandLineList();

        // Assert
        assertThat(parsed.size()).isEqualTo(2);
        assertThat(parsed.get(0).getCommand().getClass()).isEqualTo(TableStoreCommandLine.class);
        assertThat(parsed.get(1).getCommand().getClass()).isEqualTo(LedgerValidation.class);
      }
    }
  }
}
