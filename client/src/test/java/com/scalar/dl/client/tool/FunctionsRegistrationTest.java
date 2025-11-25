package com.scalar.dl.client.tool;

import static com.scalar.dl.client.tool.CommandLineTestUtils.createDefaultClientPropertiesFile;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
import picocli.CommandLine.Option;

public class FunctionsRegistrationTest {
  private CommandLine commandLine;
  private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

  @BeforeEach
  void setup() throws Exception {
    commandLine = new CommandLine(new FunctionsRegistration());

    // To verify the output to stdout, e.g., System.out.println(...).
    System.setOut(new PrintStream(outputStreamCaptor, true, UTF_8.name()));
  }

  @Nested
  @DisplayName("#call()")
  class call {
    @Nested
    @DisplayName("where functions file is successfully parsed")
    class whereFunctionsFileIsSuccessfullyParsed {
      private File functionsFile;

      @BeforeEach
      void setup() throws Exception {
        // Create a temp(mock) functions file written in TOML format.
        functionsFile = File.createTempFile("functions-file", ".toml");
        // Assume that there are two functions listed in the file.
        String functionsAsTomlFormat =
            new StringBuilder()
                .append("[[functions]]\n")
                .append("function-id = \"FUNCTION_ID_1\"\n")
                .append("function-binary-name = \"FUNCTION_BINARY_NAME_1\"\n")
                .append("function-class-file = \"FUNCTION_CLASS_FILE_1\"\n")
                .append("\n")
                .append("[[functions]]\n")
                .append("function-id = \"FUNCTION_ID_2\"\n")
                .append("function-binary-name = \"FUNCTION_BINARY_NAME_2\"\n")
                .append("function-class-file = \"FUNCTION_CLASS_FILE_2\"\n")
                .toString();
        try (PrintWriter out = new PrintWriter(functionsFile.getAbsolutePath(), UTF_8.name())) {
          out.println(functionsAsTomlFormat);
        }
      }

      @Nested
      @DisplayName("where register-functions succeeds via ClientService")
      class whereRegisterFunctionsSucceedsViaClientService {
        @Test
        @DisplayName("returns 0 as exit code")
        void returns0AsExitCode() throws Exception {
          // Arrange
          String[] args =
              new String[] {
                // Set the required options.
                "--properties=PROPERTIES_FILE",
                "--functions-file=" + functionsFile.getAbsolutePath(),
              };
          FunctionsRegistration command = parseArgs(args);
          ClientService serviceMock = mock(ClientService.class);

          // Act
          int exitCode = command.execute(serviceMock);

          // Assert
          assertThat(exitCode).isEqualTo(0);

          verify(serviceMock)
              .registerFunction(
                  eq("FUNCTION_ID_1"), eq("FUNCTION_BINARY_NAME_1"), eq("FUNCTION_CLASS_FILE_1"));
          verify(serviceMock)
              .registerFunction(
                  eq("FUNCTION_ID_2"), eq("FUNCTION_BINARY_NAME_2"), eq("FUNCTION_CLASS_FILE_2"));

          String stdout = outputStreamCaptor.toString(UTF_8.name()).trim();
          assertThat(stdout).contains("Registration succeeded: 2");
          assertThat(stdout).contains("Registration failed: 0");

          verifyFailedFunctionsFileDoesNotExist();
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
                "--functions-file=" + functionsFile.getAbsolutePath(),
                // Enable Gateway.
                "--use-gateway"
              };
          FunctionsRegistration command = parseArgs(args);
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
                propertiesOption, "--functions-file=" + functionsFile.getAbsolutePath(),
                // Gateway is disabled by default.
              };
          FunctionsRegistration command = parseArgs(args);
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
                "--functions-file=" + functionsFile.getAbsolutePath(),
              };
          FunctionsRegistration command = parseArgs(args);
          // Mock service that throws an exception.
          ClientService serviceMock = mock(ClientService.class);
          doThrow(new ClientException("", StatusCode.RUNTIME_ERROR))
              .when(serviceMock)
              .registerFunction(
                  eq("FUNCTION_ID_1"), eq("FUNCTION_BINARY_NAME_1"), eq("FUNCTION_CLASS_FILE_1"));

          // Act
          int exitCode = command.execute(serviceMock);

          // Assert
          assertThat(exitCode).isEqualTo(1);

          // The other function is registered successfully.
          verify(serviceMock)
              .registerFunction(
                  eq("FUNCTION_ID_2"), eq("FUNCTION_BINARY_NAME_2"), eq("FUNCTION_CLASS_FILE_2"));

          String stdout = outputStreamCaptor.toString(UTF_8.name()).trim();
          assertThat(stdout).contains("Registration succeeded: 1");
          assertThat(stdout).contains("Registration failed: 1");

          verifyAndCleanUpFailedFunctionsFile();
        }
      }
    }

    @Nested
    @DisplayName("where functions file is malformed")
    class whereFunctionsFileIsMalformed {
      private File malformedFunctionsFile;

      @BeforeEach
      void setup() throws Exception {
        // Create a temp(mock) functions file written in malformed TOML format.
        malformedFunctionsFile = File.createTempFile("functions-file", ".toml");
        // Assume that there are three malformed functions listed in the file.
        String functionsAsMalformedTomlFormat =
            new StringBuilder()
                .append("[[functions]]\n")
                // Without function-id.
                // .append("function-id = \"FUNCTION_ID_1\"\n")
                .append("function-binary-name = \"FUNCTION_BINARY_NAME_1\"\n")
                .append("function-class-file = \"FUNCTION_CLASS_FILE_1\"\n")
                .append("\n")
                .append("[[functions]]\n")
                .append("function-id = \"FUNCTION_ID_2\"\n")
                // Without function-binary-name
                // .append("function-binary-name = \"FUNCTION_BINARY_NAME_2\"\n")
                .append("function-class-file = \"FUNCTION_CLASS_FILE_2\"\n")
                .append("\n")
                .append("[[functions]]\n")
                .append("function-id = \"FUNCTION_ID_3\"\n")
                .append("function-binary-name = \"FUNCTION_BINARY_NAME_3\"\n")
                // Without function-class-file
                // .append("function-class-file = \"FUNCTION_CLASS_FILE_3\"\n")
                .toString();
        try (PrintWriter out =
            new PrintWriter(malformedFunctionsFile.getAbsolutePath(), UTF_8.name())) {
          out.println(functionsAsMalformedTomlFormat);
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
              "--functions-file=" + malformedFunctionsFile.getAbsolutePath(),
            };
        FunctionsRegistration command = parseArgs(args);
        // Mock service that throws an exception.
        ClientService serviceMock = mock(ClientService.class);

        // Act
        int exitCode = command.execute(serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(1);

        // Verify that register-function was not called.
        verify(serviceMock, never())
            .registerFunction(
                anyString(), eq("FUNCTION_BINARY_NAME_1"), eq("FUNCTION_CLASS_FILE_1"));
        verify(serviceMock, never())
            .registerFunction(eq("FUNCTION_ID_2"), anyString(), eq("FUNCTION_CLASS_FILE_2"));
        verify(serviceMock, never())
            .registerFunction(eq("FUNCTION_ID_3"), eq("FUNCTION_BINARY_NAME_3"), anyString());

        String stdout = outputStreamCaptor.toString(UTF_8.name()).trim();
        assertThat(stdout).contains("Registration succeeded: 0");
        assertThat(stdout).contains("Registration failed: 3");

        verifyAndCleanUpFailedFunctionsFile();
      }
    }
  }

  @Nested
  @DisplayName("@Option annotation")
  class OptionAnnotation {
    @Nested
    @DisplayName("--functions-file")
    class functionsFile {
      @Test
      @DisplayName("member values are properly set")
      void memberValuesAreProperlySet() throws Exception {
        Option option = getOption("functionsFile");

        assertThat(option.required()).isTrue();
        assertThat(option.paramLabel()).isEqualTo("FUNCTIONS_FILE");
        assertThat(option.names()).isEqualTo(new String[] {"--functions-file"});
      }
    }
  }

  private FunctionsRegistration parseArgs(String[] args) {
    return CommandLineTestUtils.parseArgs(commandLine, FunctionsRegistration.class, args);
  }

  private Option getOption(String fieldName) throws Exception {
    return CommandLineTestUtils.getOption(FunctionsRegistration.class, fieldName);
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
      Path path = Paths.get("registration-failed-functions.toml");
      Files.delete(path);
    } catch (IllegalArgumentException e) {
      fail("registration-failed-functions.toml must be created, but it was not created.");
    }
  }

  private void verifyAndCleanUpFailedFunctionsFile() throws Exception {
    verifyAndCleanUpFailedFunctionsFile(true);
  }

  private void verifyFailedFunctionsFileDoesNotExist() throws Exception {
    verifyAndCleanUpFailedFunctionsFile(false);
  }
}
