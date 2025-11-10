package com.scalar.dl.client.tool;

import static com.scalar.dl.client.tool.CommandLineTestUtils.createDefaultClientPropertiesFile;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.GatewayClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.client.service.ClientServiceFactory;
import com.scalar.dl.ledger.service.StatusCode;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

public class ContractsRegistrationTest {
  private CommandLine commandLine;
  private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

  @BeforeEach
  void setup() throws Exception {
    commandLine = new CommandLine(new ContractsRegistration());

    // To verify the output to stdout, e.g., System.out.println(...).
    System.setOut(new PrintStream(outputStreamCaptor, true, UTF_8.name()));
  }

  @Nested
  @DisplayName("#call()")
  class call {
    @Nested
    @DisplayName("where contracts file is successfully parsed")
    class whereContractsFileIsSuccessfullyParsed {
      private File contractsFile;

      @BeforeEach
      void setup() throws Exception {
        // Create a temp(mock) contracts file written in TOML format.
        contractsFile = File.createTempFile("contracts-file", ".toml");
        // Assume that there are two contracts listed in the file.
        String contractsAsTomlFormat =
            new StringBuilder()
                .append("[[contracts]]\n")
                .append("contract-id = \"CONTRACT_ID_1\"\n")
                .append("contract-binary-name = \"CONTRACT_BINARY_NAME_1\"\n")
                .append("contract-class-file = \"CONTRACT_CLASS_FILE_1\"\n")
                .append("contract-properties=\"\"\"[\"CONTRACT_PROPERTIES_1\"]\"\"\"")
                .append("\n")
                .append("[[contracts]]\n")
                .append("contract-id = \"CONTRACT_ID_2\"\n")
                .append("contract-binary-name = \"CONTRACT_BINARY_NAME_2\"\n")
                .append("contract-class-file = \"CONTRACT_CLASS_FILE_2\"\n")
                .append("contract-properties=\"\"\"[\"CONTRACT_PROPERTIES_2\"]\"\"\"")
                .toString();
        try (PrintWriter out = new PrintWriter(contractsFile.getAbsolutePath(), UTF_8.name())) {
          out.println(contractsAsTomlFormat);
        }
      }

      @Nested
      @DisplayName("where register-contracts succeeds via ClientService")
      class whereRegisterContractsSucceedsViaClientService {
        @Test
        @DisplayName("returns 0 as exit code")
        void returns0AsExitCode() throws Exception {
          // Arrange
          String[] args =
              new String[] {
                // Set the required options.
                "--properties=PROPERTIES_FILE",
                "--contracts-file=" + contractsFile.getAbsolutePath(),
              };
          ContractsRegistration command = parseArgs(args);
          ClientService serviceMock = mock(ClientService.class);

          // Act
          int exitCode = command.execute(serviceMock);

          // Assert
          assertThat(exitCode).isEqualTo(0);

          verify(serviceMock)
              .registerContract(
                  eq("CONTRACT_ID_1"),
                  eq("CONTRACT_BINARY_NAME_1"),
                  eq("CONTRACT_CLASS_FILE_1"),
                  any(JsonNode.class));
          verify(serviceMock)
              .registerContract(
                  eq("CONTRACT_ID_2"),
                  eq("CONTRACT_BINARY_NAME_2"),
                  eq("CONTRACT_CLASS_FILE_2"),
                  any(JsonNode.class));

          String stdout = outputStreamCaptor.toString(UTF_8.name()).trim();
          assertThat(stdout).contains("Registration succeeded: 2");
          assertThat(stdout).contains("Already registered: 0");
          assertThat(stdout).contains("Registration failed: 0");

          verifyFailedFunctionsFileDoesNotExist();
        }

        @Nested
        @DisplayName("with an already-registered contract")
        class withAlreadyRegisteredContract {
          @Test
          @DisplayName("returns 0 as exit code")
          void returns0AsExitCode() throws Exception {
            // Arrange
            String[] args =
                new String[] {
                  // Set the required options.
                  "--properties=PROPERTIES_FILE",
                  "--contracts-file=" + contractsFile.getAbsolutePath(),
                };
            ContractsRegistration command = parseArgs(args);
            ClientService serviceMock = mock(ClientService.class);
            // Mock that one of the contracts is already registered.
            doThrow(new ClientException("", StatusCode.CONTRACT_ALREADY_REGISTERED))
                .when(serviceMock)
                .registerContract(
                    eq("CONTRACT_ID_1"),
                    eq("CONTRACT_BINARY_NAME_1"),
                    eq("CONTRACT_CLASS_FILE_1"),
                    any(JsonNode.class));

            // Act
            int exitCode = command.execute(serviceMock);

            // Assert
            assertThat(exitCode).isEqualTo(0);

            verify(serviceMock)
                .registerContract(
                    eq("CONTRACT_ID_1"),
                    eq("CONTRACT_BINARY_NAME_1"),
                    eq("CONTRACT_CLASS_FILE_1"),
                    any(JsonNode.class));
            verify(serviceMock)
                .registerContract(
                    eq("CONTRACT_ID_2"),
                    eq("CONTRACT_BINARY_NAME_2"),
                    eq("CONTRACT_CLASS_FILE_2"),
                    any(JsonNode.class));

            String stdout = outputStreamCaptor.toString(UTF_8.name()).trim();
            assertThat(stdout).contains("Registration succeeded: 1");
            assertThat(stdout).contains("Already registered: 1");
            assertThat(stdout).contains("Registration failed: 0");

            verifyFailedFunctionsFileDoesNotExist();
          }
        }
      }

      @Nested
      @DisplayName("where useGateway option is true")
      class whereUseGatewayOptionIsTrue {
        @Test
        @DisplayName("create ClientService with GatewayClientConfig")
        public void createClientServiceWithGatewayClientConfig(@TempDir Path tempDir)
            throws Exception {
          // Arrange
          File file = createDefaultClientPropertiesFile(tempDir, "client.props");
          String propertiesOption = String.format("--properties=%s", file.getAbsolutePath());
          String[] args =
              new String[] {
                // Set the required options.
                propertiesOption,
                "--contracts-file=" + contractsFile.getAbsolutePath(),
                // Enable Gateway.
                "--use-gateway"
              };
          ContractsRegistration command = parseArgs(args);
          ClientServiceFactory factory = mock(ClientServiceFactory.class);
          doReturn(mock(ClientService.class)).when(factory).create(any(GatewayClientConfig.class));

          // Act
          command.call(factory);

          // Verify
          verify(factory).create(any(GatewayClientConfig.class));
          verify(factory, never()).create(any(ClientConfig.class));
        }
      }

      @Nested
      @DisplayName("where useGateway option is false")
      class whereUseGatewayOptionIsFalse {
        @Test
        @DisplayName("create ClientService with ClientConfig")
        public void createClientServiceWithClientConfig(@TempDir Path tempDir) throws Exception {
          // Arrange
          File file = createDefaultClientPropertiesFile(tempDir, "client.props");
          String propertiesOption = String.format("--properties=%s", file.getAbsolutePath());
          String[] args =
              new String[] {
                // Set the required options.
                propertiesOption, "--contracts-file=" + contractsFile.getAbsolutePath(),
                // Gateway is disabled by default.
              };
          ContractsRegistration command = parseArgs(args);
          ClientServiceFactory factory = mock(ClientServiceFactory.class);
          doReturn(mock(ClientService.class)).when(factory).create(any(ClientConfig.class));

          // Act
          command.call(factory);

          // Verify
          verify(factory).create(any(ClientConfig.class));
          verify(factory, never()).create(any(GatewayClientConfig.class));
        }
      }

      @Nested
      @DisplayName("where ClientException is thrown by ClientService")
      class whereClientExceptionIsThrownByClientService {
        @Test
        @DisplayName("returns 1 as exit code")
        void returns1AsExitCode() throws Exception {
          // Arrange
          String[] args =
              new String[] {
                // Set the required options.
                "--properties=PROPERTIES_FILE",
                "--contracts-file=" + contractsFile.getAbsolutePath(),
              };
          ContractsRegistration command = parseArgs(args);
          // Mock service that throws an exception.
          ClientService serviceMock = mock(ClientService.class);
          doThrow(new ClientException("", StatusCode.RUNTIME_ERROR))
              .when(serviceMock)
              .registerContract(
                  eq("CONTRACT_ID_1"),
                  eq("CONTRACT_BINARY_NAME_1"),
                  eq("CONTRACT_CLASS_FILE_1"),
                  any(JsonNode.class));

          // Act
          int exitCode = command.execute(serviceMock);

          // Assert
          assertThat(exitCode).isEqualTo(1);

          // The other function is registered successfully.
          verify(serviceMock)
              .registerContract(
                  eq("CONTRACT_ID_2"),
                  eq("CONTRACT_BINARY_NAME_2"),
                  eq("CONTRACT_CLASS_FILE_2"),
                  any(JsonNode.class));

          String stdout = outputStreamCaptor.toString(UTF_8.name()).trim();
          assertThat(stdout).contains("Registration succeeded: 1");
          assertThat(stdout).contains("Already registered: 0");
          assertThat(stdout).contains("Registration failed: 1");

          verifyAndCleanUpFailedFunctionsFile();
        }
      }
    }

    @Nested
    @DisplayName("where contracts file is malformed")
    class whereContractsFileIsMalformed {
      private File malformedContractsFile;

      @BeforeEach
      void setup() throws Exception {
        // Create a temp(mock) contracts file written in malformed TOML format.
        malformedContractsFile = File.createTempFile("contracts-file", ".toml");
        // Assume that there are three malformed contracts listed in the file.
        String contractsAsMalformedTomlFormat =
            new StringBuilder()
                .append("[[contracts]]\n")
                // Without contract-id.
                // .append("contract-id = \"CONTRACT_ID_1\"\n")
                .append("contract-binary-name = \"CONTRACT_BINARY_NAME_1\"\n")
                .append("contract-class-file = \"CONTRACT_CLASS_FILE_1\"\n")
                .append("\n")
                .append("[[contracts]]\n")
                .append("contract-id = \"CONTRACT_ID_2\"\n")
                // Without contract-binary-name
                // .append("contract-binary-name = \"CONTRACT_BINARY_NAME_2\"\n")
                .append("contract-class-file = \"CONTRACT_CLASS_FILE_2\"\n")
                .append("\n")
                .append("[[contracts]]\n")
                .append("contract-id = \"CONTRACT_ID_3\"\n")
                .append("contract-binary-name = \"CONTRACT_BINARY_NAME_3\"\n")
                // Without contract-class-file
                // .append("contract-class-file = \"CONTRACT_CLASS_FILE_3\"\n")
                .toString();
        try (PrintWriter out =
            new PrintWriter(malformedContractsFile.getAbsolutePath(), UTF_8.name())) {
          out.println(contractsAsMalformedTomlFormat);
        }
      }

      @Test
      @DisplayName("returns 1 as exit code")
      void returns1AsExitCode() throws Exception {
        // Arrange
        String[] args =
            new String[] {
              // Set the required options.
              "--properties=PROPERTIES_FILE",
              "--contracts-file=" + malformedContractsFile.getAbsolutePath(),
            };
        ContractsRegistration command = parseArgs(args);
        // Mock service that throws an exception.
        ClientService serviceMock = mock(ClientService.class);

        // Act
        int exitCode = command.execute(serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(1);

        // Verify that register-function was not called.
        verify(serviceMock, never())
            .registerFunction(
                anyString(), eq("CONTRACT_BINARY_NAME_1"), eq("CONTRACT_CLASS_FILE_1"));
        verify(serviceMock, never())
            .registerFunction(eq("CONTRACT_ID_2"), anyString(), eq("CONTRACT_CLASS_FILE_2"));
        verify(serviceMock, never())
            .registerFunction(eq("CONTRACT_ID_3"), eq("CONTRACT_BINARY_NAME_3"), anyString());

        String stdout = outputStreamCaptor.toString(UTF_8.name()).trim();
        assertThat(stdout).contains("Registration succeeded: 0");
        assertThat(stdout).contains("Already registered: 0");
        assertThat(stdout).contains("Registration failed: 3");

        verifyAndCleanUpFailedFunctionsFile();
      }
    }
  }

  @Nested
  @DisplayName("@Option annotation")
  class OptionAnnotation {
    @Nested
    @DisplayName("--contracts-file")
    class contractsFile {
      @Test
      @DisplayName("member values are properly set")
      void memberValuesAreProperlySet() throws Exception {
        CommandLine.Option option = getOption("contractsFile");

        assertThat(option.required()).isTrue();
        assertThat(option.paramLabel()).isEqualTo("CONTRACTS_FILE");
        assertThat(option.names()).isEqualTo(new String[] {"--contracts-file"});
      }
    }
  }

  private ContractsRegistration parseArgs(String[] args) {
    return CommandLineTestUtils.parseArgs(commandLine, ContractsRegistration.class, args);
  }

  private CommandLine.Option getOption(String fieldName) throws Exception {
    return CommandLineTestUtils.getOption(ContractsRegistration.class, fieldName);
  }

  /**
   * Verify that a failed file was created if {@code shouldExist} is true. Otherwise, the file
   * existence is not checked.
   *
   * <p>If the file was created, remove it from the local environment as well.
   *
   * @param shouldExist true to verify the failed file was created.
   * @throws Exception if file operation failed somehow.
   */
  private void verifyAndCleanUpFailedFunctionsFile(boolean shouldExist) throws Exception {
    if (!shouldExist) {
      return;
    }

    try {
      Path path = Paths.get("registration-failed-contracts.toml");
      Files.delete(path);
    } catch (IllegalArgumentException e) {
      fail("registration-failed-contracts.toml must be created, but it was not created.");
    }
  }

  private void verifyAndCleanUpFailedFunctionsFile() throws Exception {
    verifyAndCleanUpFailedFunctionsFile(true);
  }

  private void verifyFailedFunctionsFileDoesNotExist() throws Exception {
    verifyAndCleanUpFailedFunctionsFile(false);
  }
}
