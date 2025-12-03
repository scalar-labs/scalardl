package com.scalar.dl.client.tool;

import static com.scalar.dl.client.tool.CommandLineTestUtils.createDefaultClientPropertiesFile;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.GatewayClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.client.service.ClientServiceFactory;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.scalar.dl.ledger.service.StatusCode;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatchers;
import picocli.CommandLine;
import picocli.CommandLine.Option;

public class ContractExecutionTest {
  private CommandLine commandLine;
  private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

  @BeforeEach
  void setup() throws UnsupportedEncodingException {
    commandLine = new CommandLine(new ContractExecution());

    // To verify the output to stdout, e.g., System.out.println(...).
    System.setOut(new PrintStream(outputStreamCaptor, true, UTF_8.name()));
  }

  @Nested
  @DisplayName("#call()")
  class call {

    @Nested
    @DisplayName("with JSON deserialization format")
    class withJsonDeserializationFormat {
      @Test
      @DisplayName("returns 0 as exit code")
      void returns0AsExitCode() throws Exception {
        // Arrange
        String[] args =
            new String[] {
              // Set the required options.
              "--properties=PROPERTIES_FILE",
              "--contract-id=CONTRACT_ID",
              "--contract-argument=[\"CONTRACT_ARGUMENT\"]",
              // Set the optional options.
              "--function-id=FUNCTION_ID",
              "--function-argument=[\"FUNCTION_ARGUMENT\"]"
            };
        ContractExecution command = parseArgs(args);
        // Mock service that returns ContractExecutionResult.
        ClientService serviceMock = mock(ClientService.class);
        ContractExecutionResult result =
            new ContractExecutionResult(
                "[\"CONTRACT_RESULT\"]", "[\"FUNCTION_RESULT\"]", emptyList(), emptyList());
        when(serviceMock.executeContract(
                eq("CONTRACT_ID"), any(JsonNode.class), eq("FUNCTION_ID"), any(ArrayNode.class)))
            .thenReturn(result);

        // Act
        int exitCode = command.execute(serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(0);

        String stdout = outputStreamCaptor.toString(UTF_8.name()).trim();
        String separator = System.lineSeparator();
        assertThat(stdout).contains("Contract result:" + separator + "[ \"CONTRACT_RESULT\" ]");
        assertThat(stdout).contains("Function result:" + separator + "[ \"FUNCTION_RESULT\" ]");
      }
    }

    @Nested
    @DisplayName("with String deserialization format")
    class withStringDeserializationFormat {
      @Test
      @DisplayName("returns 0 as exit code")
      void returns0AsExitCode() throws Exception {
        // Arrange
        String[] args =
            new String[] {
              // Set the required options.
              "--properties=PROPERTIES_FILE",
              "--contract-id=CONTRACT_ID",
              "--contract-argument=CONTRACT_ARGUMENT",
              // Set the optional options.
              "--function-id=FUNCTION_ID",
              "--function-argument=FUNCTION_ARGUMENT",
              "--deserialization-format=STRING",
            };
        ContractExecution command = parseArgs(args);
        // Mock service that returns ContractExecutionResult.
        ClientService serviceMock = mock(ClientService.class);
        ContractExecutionResult result =
            new ContractExecutionResult(
                "CONTRACT_RESULT", "FUNCTION_RESULT", emptyList(), emptyList());
        when(serviceMock.executeContract(
                eq("CONTRACT_ID"),
                eq("CONTRACT_ARGUMENT"),
                eq("FUNCTION_ID"),
                eq("FUNCTION_ARGUMENT")))
            .thenReturn(result);

        // Act
        int exitCode = command.execute(serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(0);

        String stdout = outputStreamCaptor.toString(UTF_8.name()).trim();
        assertThat(stdout).contains("Contract result: CONTRACT_RESULT");
        assertThat(stdout).contains("Function result: FUNCTION_RESULT");
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
              "--contract-argument=[\"CONTRACT_ARGUMENT\"]",
              // Set the optional options.
              "--function-id=FUNCTION_ID",
              "--function-argument=[\"FUNCTION_ARGUMENT\"]",
              // Enable Gateway.
              "--use-gateway"
            };
        ContractExecution command = parseArgs(args);
        ClientServiceFactory factory = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);
        ContractExecutionResult result =
            new ContractExecutionResult(
                "[\"CONTRACT_RESULT\"]", "[\"FUNCTION_RESULT\"]", emptyList(), emptyList());
        when(serviceMock.executeContract(
                eq("CONTRACT_ID"), any(JsonNode.class), eq("FUNCTION_ID"), any(ArrayNode.class)))
            .thenReturn(result);
        doReturn(serviceMock).when(factory).create(any(GatewayClientConfig.class));

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
              "--contract-argument=[\"CONTRACT_ARGUMENT\"]",
              // Set the optional options.
              "--function-id=FUNCTION_ID",
              "--function-argument=[\"FUNCTION_ARGUMENT\"]",
              // Gateway is disabled by default.
            };
        ContractExecution command = parseArgs(args);
        ClientServiceFactory factory = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);
        ContractExecutionResult result =
            new ContractExecutionResult(
                "[\"CONTRACT_RESULT\"]", "[\"FUNCTION_RESULT\"]", emptyList(), emptyList());
        when(serviceMock.executeContract(
                eq("CONTRACT_ID"), any(JsonNode.class), eq("FUNCTION_ID"), any(ArrayNode.class)))
            .thenReturn(result);
        doReturn(serviceMock).when(factory).create(any(ClientConfig.class));

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
                "--contract-argument={}",
                // By default, JSON is the deserialization format.
              };
          ContractExecution command = parseArgs(args);
          // Mock service that throws an exception.
          ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
          ClientService serviceMock = mock(ClientService.class);
          when(factoryMock.create(any(ClientConfig.class))).thenReturn(serviceMock);
          when(serviceMock.executeContract(
                  eq("CONTRACT_ID"), any(JsonNode.class), isNull(), isNull()))
              .thenThrow(new ClientException("", StatusCode.RUNTIME_ERROR));

          // Act
          int exitCode = command.call(factoryMock);

          // Assert
          assertThat(exitCode).isEqualTo(1);
          verify(factoryMock).close();
        }
      }

      @Nested
      @DisplayName("with String deserialization format")
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
                "--contract-argument=CONTRACT_ARGUMENT",
                // Use String as deserialization format.
                "--deserialization-format=STRING",
              };
          ContractExecution command = parseArgs(args);
          // Mock service that throws an exception.
          ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
          ClientService serviceMock = mock(ClientService.class);
          when(factoryMock.create(any(ClientConfig.class))).thenReturn(serviceMock);
          when(serviceMock.executeContract(
                  eq("CONTRACT_ID"),
                  eq("CONTRACT_ARGUMENT"),
                  ArgumentMatchers.<String>isNull(),
                  isNull()))
              .thenThrow(new ClientException("", StatusCode.RUNTIME_ERROR));

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
        Option option = getOption("contractId");

        assertThat(option.required()).isTrue();
        assertThat(option.paramLabel()).isEqualTo("CONTRACT_ID");
        assertThat(option.names()).isEqualTo(new String[] {"--contract-id"});
      }
    }

    @Nested
    @DisplayName("--contract-argument")
    class contractArgument {
      @Test
      @DisplayName("member values are properly set")
      void memberValuesAreProperlySet() throws Exception {
        Option option = getOption("contractArgument");

        assertThat(option.required()).isTrue();
        assertThat(option.paramLabel()).isEqualTo("CONTRACT_ARGUMENT");
        assertThat(option.names()).isEqualTo(new String[] {"--contract-argument"});
      }
    }

    @Nested
    @DisplayName("--function-id")
    class functionId {
      @Test
      @DisplayName("member values are properly set")
      void memberValuesAreProperlySet() throws Exception {
        Option option = getOption("functionId");

        assertThat(option.required()).isFalse();
        assertThat(option.paramLabel()).isEqualTo("FUNCTION_ID");
        assertThat(option.names()).isEqualTo(new String[] {"--function-id"});
      }
    }

    @Nested
    @DisplayName("--function-argument")
    class functionArgument {
      @Test
      @DisplayName("member values are properly set")
      void memberValuesAreProperlySet() throws Exception {
        Option option = getOption("functionArgument");

        assertThat(option.required()).isFalse();
        assertThat(option.paramLabel()).isEqualTo("FUNCTION_ARGUMENT");
        assertThat(option.names()).isEqualTo(new String[] {"--function-argument"});
      }
    }

    @Nested
    @DisplayName("--deserialization-format")
    class deserializationFormat {
      @Test
      @DisplayName("member values are properly set")
      void memberValuesAreProperlySet() throws Exception {
        Option option = getOption("deserializationFormat");

        assertThat(option.required()).isFalse();
        assertThat(option.paramLabel()).isEqualTo("DESERIALIZATION_FORMAT");
        assertThat(option.names()).isEqualTo(new String[] {"--deserialization-format"});
      }
    }
  }

  private ContractExecution parseArgs(String[] args) {
    return CommandLineTestUtils.parseArgs(commandLine, ContractExecution.class, args);
  }

  private Option getOption(String fieldName) throws Exception {
    return CommandLineTestUtils.getOption(ContractExecution.class, fieldName);
  }
}
