package com.scalar.dl.hashstore.client.tool;

import static com.scalar.dl.hashstore.client.tool.HashStoreCommandLine.setupSections;
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

public class HashStoreCommandLineTest {
  private CommandLine commandLine;

  @BeforeEach
  void setup() {
    commandLine = new CommandLine(new HashStoreCommandLine());
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
              "Usage: scalardl-hash-store [COMMAND]",
              "These are ScalarDL Hash Store commands used in various situations:",
              "",
              "register identity information",
              "  register-identity        Register identity and contracts for the hash store.",
              "",
              "manage objects",
              "  get-object               Get an object from the hash store.",
              "  put-object               Put an object to the hash store.",
              "  compare-object-versions  Compare object versions.",
              "",
              "manage collections",
              "  create-collection        Create a new collection.",
              "  get-collection           Get a collection from the hash store.",
              "  add-to-collection        Add objects to a collection.",
              "  remove-from-collection   Remove objects from a collection.",
              "  get-collection-history   Get the history of a collection.",
              "",
              "validate ledger",
              "  validate-ledger          Validate a specified asset in the ledger.",
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
      command = HashStoreCommandLine.class.getAnnotation(Command.class);
    }

    @Test
    @DisplayName("member values are properly set")
    void memberValuesAreProperlySet() {
      assertThat(command.name()).isEqualTo("scalardl-hash-store");
      assertThat(command.subcommands())
          .isEqualTo(
              new Class[] {
                CommandLine.HelpCommand.class,
                IdentityRegistration.class,
                ObjectGet.class,
                ObjectPut.class,
                ObjectVersionsComparison.class,
                CollectionCreation.class,
                CollectionGet.class,
                ObjectAdditionToCollection.class,
                ObjectRemovalFromCollection.class,
                CollectionHistoryGet.class,
                LedgerValidation.class,
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

        // Verify that the top-level command is "scalardl-hash-store".
        assertThat(parsed.get(0).getCommand().getClass()).isEqualTo(HashStoreCommandLine.class);
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
      @DisplayName("register-identity subcommand is parsed correctly")
      void registerIdentitySubcommandIsParsedCorrectly() {
        // Arrange
        String[] args = new String[] {"register-identity", "--help"};

        // Act
        ParseResult parseResult = commandLine.parseArgs(args);
        List<CommandLine> parsed = parseResult.asCommandLineList();

        // Assert
        assertThat(parsed.size()).isEqualTo(2);
        assertThat(parsed.get(0).getCommand().getClass()).isEqualTo(HashStoreCommandLine.class);
        assertThat(parsed.get(1).getCommand().getClass()).isEqualTo(IdentityRegistration.class);
      }

      @Test
      @DisplayName("put-object subcommand is parsed correctly")
      void putObjectSubcommandIsParsedCorrectly() {
        // Arrange
        String[] args = new String[] {"put-object", "--help"};

        // Act
        ParseResult parseResult = commandLine.parseArgs(args);
        List<CommandLine> parsed = parseResult.asCommandLineList();

        // Assert
        assertThat(parsed.size()).isEqualTo(2);
        assertThat(parsed.get(0).getCommand().getClass()).isEqualTo(HashStoreCommandLine.class);
        assertThat(parsed.get(1).getCommand().getClass()).isEqualTo(ObjectPut.class);
      }

      @Test
      @DisplayName("get-object subcommand is parsed correctly")
      void getObjectSubcommandIsParsedCorrectly() {
        // Arrange
        String[] args = new String[] {"get-object", "--help"};

        // Act
        ParseResult parseResult = commandLine.parseArgs(args);
        List<CommandLine> parsed = parseResult.asCommandLineList();

        // Assert
        assertThat(parsed.size()).isEqualTo(2);
        assertThat(parsed.get(0).getCommand().getClass()).isEqualTo(HashStoreCommandLine.class);
        assertThat(parsed.get(1).getCommand().getClass()).isEqualTo(ObjectGet.class);
      }

      @Test
      @DisplayName("compare-object-versions subcommand is parsed correctly")
      void compareObjectVersionsSubcommandIsParsedCorrectly() {
        // Arrange
        String[] args = new String[] {"compare-object-versions", "--help"};

        // Act
        ParseResult parseResult = commandLine.parseArgs(args);
        List<CommandLine> parsed = parseResult.asCommandLineList();

        // Assert
        assertThat(parsed.size()).isEqualTo(2);
        assertThat(parsed.get(0).getCommand().getClass()).isEqualTo(HashStoreCommandLine.class);
        assertThat(parsed.get(1).getCommand().getClass()).isEqualTo(ObjectVersionsComparison.class);
      }

      @Test
      @DisplayName("create-collection subcommand is parsed correctly")
      void createCollectionSubcommandIsParsedCorrectly() {
        // Arrange
        String[] args = new String[] {"create-collection", "--help"};

        // Act
        ParseResult parseResult = commandLine.parseArgs(args);
        List<CommandLine> parsed = parseResult.asCommandLineList();

        // Assert
        assertThat(parsed.size()).isEqualTo(2);
        assertThat(parsed.get(0).getCommand().getClass()).isEqualTo(HashStoreCommandLine.class);
        assertThat(parsed.get(1).getCommand().getClass()).isEqualTo(CollectionCreation.class);
      }

      @Test
      @DisplayName("get-collection subcommand is parsed correctly")
      void getCollectionSubcommandIsParsedCorrectly() {
        // Arrange
        String[] args = new String[] {"get-collection", "--help"};

        // Act
        ParseResult parseResult = commandLine.parseArgs(args);
        List<CommandLine> parsed = parseResult.asCommandLineList();

        // Assert
        assertThat(parsed.size()).isEqualTo(2);
        assertThat(parsed.get(0).getCommand().getClass()).isEqualTo(HashStoreCommandLine.class);
        assertThat(parsed.get(1).getCommand().getClass()).isEqualTo(CollectionGet.class);
      }

      @Test
      @DisplayName("add-to-collection subcommand is parsed correctly")
      void addToCollectionSubcommandIsParsedCorrectly() {
        // Arrange
        String[] args = new String[] {"add-to-collection", "--help"};

        // Act
        ParseResult parseResult = commandLine.parseArgs(args);
        List<CommandLine> parsed = parseResult.asCommandLineList();

        // Assert
        assertThat(parsed.size()).isEqualTo(2);
        assertThat(parsed.get(0).getCommand().getClass()).isEqualTo(HashStoreCommandLine.class);
        assertThat(parsed.get(1).getCommand().getClass())
            .isEqualTo(ObjectAdditionToCollection.class);
      }

      @Test
      @DisplayName("remove-from-collection subcommand is parsed correctly")
      void removeFromCollectionSubcommandIsParsedCorrectly() {
        // Arrange
        String[] args = new String[] {"remove-from-collection", "--help"};

        // Act
        ParseResult parseResult = commandLine.parseArgs(args);
        List<CommandLine> parsed = parseResult.asCommandLineList();

        // Assert
        assertThat(parsed.size()).isEqualTo(2);
        assertThat(parsed.get(0).getCommand().getClass()).isEqualTo(HashStoreCommandLine.class);
        assertThat(parsed.get(1).getCommand().getClass())
            .isEqualTo(ObjectRemovalFromCollection.class);
      }

      @Test
      @DisplayName("get-collection-history subcommand is parsed correctly")
      void getCollectionHistorySubcommandIsParsedCorrectly() {
        // Arrange
        String[] args = new String[] {"get-collection-history", "--help"};

        // Act
        ParseResult parseResult = commandLine.parseArgs(args);
        List<CommandLine> parsed = parseResult.asCommandLineList();

        // Assert
        assertThat(parsed.size()).isEqualTo(2);
        assertThat(parsed.get(0).getCommand().getClass()).isEqualTo(HashStoreCommandLine.class);
        assertThat(parsed.get(1).getCommand().getClass()).isEqualTo(CollectionHistoryGet.class);
      }

      @Test
      @DisplayName("validate-ledger subcommand is parsed correctly")
      void validateObjectSubcommandIsParsedCorrectly() {
        // Arrange
        String[] args = new String[] {"validate-ledger", "--help"};

        // Act
        ParseResult parseResult = commandLine.parseArgs(args);
        List<CommandLine> parsed = parseResult.asCommandLineList();

        // Assert
        assertThat(parsed.size()).isEqualTo(2);
        assertThat(parsed.get(0).getCommand().getClass()).isEqualTo(HashStoreCommandLine.class);
        assertThat(parsed.get(1).getCommand().getClass()).isEqualTo(LedgerValidation.class);
      }
    }
  }
}
