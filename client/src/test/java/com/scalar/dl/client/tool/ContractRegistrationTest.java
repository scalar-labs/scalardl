package com.scalar.dl.client.tool;

import static com.scalar.dl.client.tool.CommandLineTestUtils.createDefaultClientPropertiesFile;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.node.ArrayNode;
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

public class ContractRegistrationTest {
  private CommandLine commandLine;

  @BeforeEach
  void setup() {
    commandLine = new CommandLine(new ContractRegistration());
  }

  @Nested
  @DisplayName("#call()")
  class call {
    @Nested
    @DisplayName("with JSON deserialization format")
    class withJsonDeserializationFormat {
      @Test
      @DisplayName("returns 0 as exit code")
      void returns0AsExitCode() throws ClientException {
        // Arrange
        String[] args =
            new String[] {
              // Set the required options.
              "--properties=PROPERTIES_FILE",
              "--contract-id=CONTRACT_ID",
              "--contract-binary-name=CONTRACT_BINARY_NAME",
              "--contract-class-file=CONTRACT_CLASS_FILE",
              // Set the optional options.
              "--contract-properties=[\"CONTRACT_PROPERTIES\"]",
            };
        ContractRegistration command = parseArgs(args);
        ClientService serviceMock = mock(ClientService.class);

        // Act
        int exitCode = command.execute(serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(0);

        verify(serviceMock)
            .registerContract(
                eq("CONTRACT_ID"),
                eq("CONTRACT_BINARY_NAME"),
                eq("CONTRACT_CLASS_FILE"),
                any(ArrayNode.class));
      }
    }

    @Nested
    @DisplayName("with String deserialization format")
    class withStringDeserializationFormat {
      @Test
      @DisplayName("returns 0 as exit code")
      void returns0AsExitCode() throws ClientException {
        // Arrange
        String[] args =
            new String[] {
              // Set the required options.
              "--properties=PROPERTIES_FILE",
              "--contract-id=CONTRACT_ID",
              "--contract-binary-name=CONTRACT_BINARY_NAME",
              "--contract-class-file=CONTRACT_CLASS_FILE",
              // Set the optional options.
              "--contract-properties=CONTRACT_PROPERTIES",
              // Use String as deserialization format.
              "--deserialization-format=STRING",
            };
        ContractRegistration command = parseArgs(args);
        ClientService serviceMock = mock(ClientService.class);

        // Act
        int exitCode = command.execute(serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(0);

        verify(serviceMock)
            .registerContract(
                eq("CONTRACT_ID"),
                eq("CONTRACT_BINARY_NAME"),
                eq("CONTRACT_CLASS_FILE"),
                eq("CONTRACT_PROPERTIES"));
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
              "--contract-id=CONTRACT_ID",
              "--contract-binary-name=CONTRACT_BINARY_NAME",
              "--contract-class-file=CONTRACT_CLASS_FILE",
              // Enable Gateway.
              "--use-gateway"
            };
        ContractRegistration command = parseArgs(args);
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
              propertiesOption,
              "--contract-id=CONTRACT_ID",
              "--contract-binary-name=CONTRACT_BINARY_NAME",
              "--contract-class-file=CONTRACT_CLASS_FILE",
              // Gateway is disabled by default.
            };
        ContractRegistration command = parseArgs(args);
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
    @DisplayName("where ClientService throws ClientException")
    class whereClientExceptionIsThrownByClientService {

      @Nested
      @DisplayName("with JSON deserialization format")
      class withJsonDeserializationFormat {
        @Test
        @DisplayName("returns 1 as exit code")
        void returns1AsExitCode(@TempDir Path tempDir) throws Exception {
          // Arrange
          File file = createDefaultClientPropertiesFile(tempDir, "client.props");
          String[] args =
              new String[] {
                // Set the required options.
                "--properties=" + file.getAbsolutePath(),
                "--contract-id=CONTRACT_ID",
                "--contract-binary-name=CONTRACT_BINARY_NAME",
                "--contract-class-file=CONTRACT_CLASS_FILE",
                // Set the optional options.
                "--contract-properties=[\"CONTRACT_PROPERTIES\"]",
              };
          ContractRegistration command = parseArgs(args);
          // Mock service that throws an exception.
          ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
          ClientService serviceMock = mock(ClientService.class);
          when(factoryMock.create(any(ClientConfig.class))).thenReturn(serviceMock);
          doThrow(new ClientException("", StatusCode.RUNTIME_ERROR))
              .when(serviceMock)
              .registerContract(
                  eq("CONTRACT_ID"),
                  eq("CONTRACT_BINARY_NAME"),
                  eq("CONTRACT_CLASS_FILE"),
                  any(ArrayNode.class));

          // Act
          int exitCode = command.call(factoryMock);

          // Assert
          assertThat(exitCode).isEqualTo(1);
          verify(factoryMock).close();
        }
      }

      @Nested
      @DisplayName("with STRING deserialization format")
      class withStringDeserializationFormat {
        @Test
        @DisplayName("returns 1 as exit code")
        void returns1AsExitCode(@TempDir Path tempDir) throws Exception {
          // Arrange
          File file = createDefaultClientPropertiesFile(tempDir, "client.props");
          String[] args =
              new String[] {
                // Set the required options.
                "--properties=" + file.getAbsolutePath(),
                "--contract-id=CONTRACT_ID",
                "--contract-binary-name=CONTRACT_BINARY_NAME",
                "--contract-class-file=CONTRACT_CLASS_FILE",
                // Set the optional options.
                "--contract-properties=CONTRACT_PROPERTIES",
                // Use String as deserialization format.
                "--deserialization-format=STRING",
              };
          ContractRegistration command = parseArgs(args);
          // Mock service that throws an exception.
          ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
          ClientService serviceMock = mock(ClientService.class);
          when(factoryMock.create(any(ClientConfig.class))).thenReturn(serviceMock);
          doThrow(new ClientException("", StatusCode.RUNTIME_ERROR))
              .when(serviceMock)
              .registerContract(
                  eq("CONTRACT_ID"),
                  eq("CONTRACT_BINARY_NAME"),
                  eq("CONTRACT_CLASS_FILE"),
                  eq("CONTRACT_PROPERTIES"));

          // Act
          int exitCode = command.call(factoryMock);

          // Assert
          assertThat(exitCode).isEqualTo(1);
          verify(factoryMock).close();
        }
      }
    }
  }

  @Nested
  @DisplayName("@Option annotation")
  class OptionAnnotation {
    @Nested
    @DisplayName("--contract-id")
    class contractId {
      @Test
      @DisplayName("member values are properly set")
      void memberValuesAreProperlySet() throws Exception {
        CommandLine.Option option = getOption("contractId");

        assertThat(option.required()).isTrue();
        assertThat(option.paramLabel()).isEqualTo("CONTRACT_ID");
        assertThat(option.names()).isEqualTo(new String[] {"--contract-id"});
      }
    }

    @Nested
    @DisplayName("--contract-binary-name")
    class contractBinaryName {
      @Test
      @DisplayName("member values are properly set")
      void memberValuesAreProperlySet() throws Exception {
        CommandLine.Option option = getOption("contractBinaryName");

        assertThat(option.required()).isTrue();
        assertThat(option.paramLabel()).isEqualTo("CONTRACT_BINARY_NAME");
        assertThat(option.names()).isEqualTo(new String[] {"--contract-binary-name"});
      }
    }

    @Nested
    @DisplayName("--contract-class-file")
    class contractClassFile {
      @Test
      @DisplayName("member values are properly set")
      void memberValuesAreProperlySet() throws Exception {
        CommandLine.Option option = getOption("contractClassFile");

        assertThat(option.required()).isTrue();
        assertThat(option.paramLabel()).isEqualTo("CONTRACT_CLASS_FILE");
        assertThat(option.names()).isEqualTo(new String[] {"--contract-class-file"});
      }
    }

    @Nested
    @DisplayName("--contract-properties")
    class contractProperties {
      @Test
      @DisplayName("member values are properly set")
      void memberValuesAreProperlySet() throws Exception {
        CommandLine.Option option = getOption("contractProperties");

        assertThat(option.required()).isFalse();
        assertThat(option.paramLabel()).isEqualTo("CONTRACT_PROPERTIES");
        assertThat(option.names()).isEqualTo(new String[] {"--contract-properties"});
      }
    }

    @Nested
    @DisplayName("--deserialization-format")
    class deserializationFormat {
      @Test
      @DisplayName("member values are properly set")
      void memberValuesAreProperlySet() throws Exception {
        CommandLine.Option option = getOption("deserializationFormat");

        assertThat(option.required()).isFalse();
        assertThat(option.paramLabel()).isEqualTo("DESERIALIZATION_FORMAT");
        assertThat(option.names()).isEqualTo(new String[] {"--deserialization-format"});
      }
    }
  }

  private ContractRegistration parseArgs(String[] args) {
    return CommandLineTestUtils.parseArgs(commandLine, ContractRegistration.class, args);
  }

  private CommandLine.Option getOption(String fieldName) throws Exception {
    return CommandLineTestUtils.getOption(ContractRegistration.class, fieldName);
  }
}
