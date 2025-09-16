package com.scalar.dl.client.tool;

import static com.scalar.dl.client.tool.CommandLineTestUtils.createDefaultClientPropertiesFile;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.GatewayClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.client.service.ClientServiceFactory;
import com.scalar.dl.ledger.service.StatusCode;
import java.io.File;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

public class FunctionRegistrationTest {
  private CommandLine commandLine;

  @BeforeEach
  void setup() {
    commandLine = new CommandLine(new FunctionRegistration());
  }

  @Nested
  @DisplayName("#call()")
  class call {
    @Test
    @DisplayName("returns 0 as exit code")
    void returns0AsExitCode() {
      // Arrange
      String[] args =
          new String[] {
            // Set the required options.
            "--properties=PROPERTIES_FILE",
            "--function-id=FUNCTION_ID",
            "--function-binary-name=FUNCTION_BINARY_NAME",
            "--function-class-file=FUNCTION_CLASS_FILE",
          };
      FunctionRegistration command = parseArgs(args);
      ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
      ClientService serviceMock = mock(ClientService.class);

      // Act
      int exitCode = command.call(factoryMock, serviceMock);

      // Assert
      assertThat(exitCode).isEqualTo(0);

      verify(serviceMock)
          .registerFunction(
              eq("FUNCTION_ID"), eq("FUNCTION_BINARY_NAME"), eq("FUNCTION_CLASS_FILE"));
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
              "--function-id=FUNCTION_ID",
              "--function-binary-name=FUNCTION_BINARY_NAME",
              "--function-class-file=FUNCTION_CLASS_FILE",
              // Enable Gateway.
              "--use-gateway"
            };
        FunctionRegistration command = parseArgs(args);
        ClientServiceFactory factory = mock(ClientServiceFactory.class);
        doReturn(mock(ClientService.class))
            .when(factory)
            .create(any(GatewayClientConfig.class), anyBoolean());

        // Act
        command.call(factory);

        // Verify
        verify(factory).create(any(GatewayClientConfig.class), eq(false));
        verify(factory, never()).create(any(ClientConfig.class), anyBoolean());
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
              propertiesOption,
              "--function-id=FUNCTION_ID",
              "--function-binary-name=FUNCTION_BINARY_NAME",
              "--function-class-file=FUNCTION_CLASS_FILE",
              // Gateway is disabled by default.
            };
        FunctionRegistration command = parseArgs(args);
        ClientServiceFactory factory = mock(ClientServiceFactory.class);
        doReturn(mock(ClientService.class))
            .when(factory)
            .create(any(ClientConfig.class), anyBoolean());

        // Act
        command.call(factory);

        // Verify
        verify(factory).create(any(ClientConfig.class), eq(false));
        verify(factory, never()).create(any(GatewayClientConfig.class), anyBoolean());
      }
    }

    @Nested
    @DisplayName("where ClientService throws ClientException")
    class whereClientExceptionIsThrownByClientService {
      @Test
      @DisplayName("returns 1 as exit code")
      void returns1AsExitCode() {
        // Arrange
        String[] args =
            new String[] {
              // Set the required options.
              "--properties=PROPERTIES_FILE",
              "--function-id=FUNCTION_ID",
              "--function-binary-name=FUNCTION_BINARY_NAME",
              "--function-class-file=FUNCTION_CLASS_FILE",
            };
        FunctionRegistration command = parseArgs(args);
        // Mock service that throws an exception.
        ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);
        doThrow(new ClientException("", StatusCode.RUNTIME_ERROR))
            .when(serviceMock)
            .registerFunction(
                eq("FUNCTION_ID"), eq("FUNCTION_BINARY_NAME"), eq("FUNCTION_CLASS_FILE"));

        // Act
        int exitCode = command.call(factoryMock, serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(1);
      }
    }
  }

  @Nested
  @DisplayName("@Option annotation")
  class OptionAnnotation {
    @Nested
    @DisplayName("--function-id")
    class contractId {
      @Test
      @DisplayName("member values are properly set")
      void memberValuesAreProperlySet() throws Exception {
        CommandLine.Option option = getOption("functionId");

        assertThat(option.required()).isTrue();
        assertThat(option.paramLabel()).isEqualTo("FUNCTION_ID");
        assertThat(option.names()).isEqualTo(new String[] {"--function-id"});
      }
    }

    @Nested
    @DisplayName("--function-binary-name")
    class contractBinaryName {
      @Test
      @DisplayName("member values are properly set")
      void memberValuesAreProperlySet() throws Exception {
        CommandLine.Option option = getOption("functionBinaryName");

        assertThat(option.required()).isTrue();
        assertThat(option.paramLabel()).isEqualTo("FUNCTION_BINARY_NAME");
        assertThat(option.names()).isEqualTo(new String[] {"--function-binary-name"});
      }
    }

    @Nested
    @DisplayName("--function-class-file")
    class functionClassFile {
      @Test
      @DisplayName("member values are properly set")
      void memberValuesAreProperlySet() throws Exception {
        CommandLine.Option option = getOption("functionClassFile");

        assertThat(option.required()).isTrue();
        assertThat(option.paramLabel()).isEqualTo("FUNCTION_CLASS_FILE");
        assertThat(option.names()).isEqualTo(new String[] {"--function-class-file"});
      }
    }
  }

  private FunctionRegistration parseArgs(String[] args) {
    return CommandLineTestUtils.parseArgs(commandLine, FunctionRegistration.class, args);
  }

  private CommandLine.Option getOption(String fieldName) throws Exception {
    return CommandLineTestUtils.getOption(FunctionRegistration.class, fieldName);
  }
}
