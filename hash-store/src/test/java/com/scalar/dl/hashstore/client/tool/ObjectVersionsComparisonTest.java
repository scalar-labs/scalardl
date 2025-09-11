package com.scalar.dl.hashstore.client.tool;

import static com.scalar.dl.client.tool.CommandLineTestUtils.createDefaultClientPropertiesFile;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.GatewayClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.tool.CommandLineTestUtils;
import com.scalar.dl.hashstore.client.service.ClientService;
import com.scalar.dl.hashstore.client.service.ClientServiceFactory;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.scalar.dl.ledger.model.ExecutionResult;
import com.scalar.dl.ledger.service.StatusCode;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import picocli.CommandLine;

public class ObjectVersionsComparisonTest {
  private CommandLine commandLine;
  private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

  @BeforeEach
  void setup() throws Exception {
    commandLine = new CommandLine(new ObjectVersionsComparison());
    System.setOut(new PrintStream(outputStreamCaptor, true, UTF_8.name()));
  }

  @Nested
  @DisplayName("#call()")
  class call {
    @Nested
    @DisplayName("where object versions comparison succeeds")
    class whereObjectVersionsComparisonSucceeds {
      @Test
      @DisplayName("returns 0 as exit code with basic parameters")
      void returns0AsExitCodeWithBasicParameters() throws Exception {
        // Arrange
        String[] args =
            new String[] {
              "--properties=PROPERTIES_FILE", "--object-id=obj123", "--versions=[\"v1\",\"v2\"]"
            };
        ObjectVersionsComparison command = parseArgs(args);
        ClientService serviceMock = mock(ClientService.class);

        String resultJson = "{\"result\":\"versions match\"}";
        ContractExecutionResult contractResult =
            new ContractExecutionResult(
                resultJson, null, Collections.emptyList(), Collections.emptyList());
        ExecutionResult result = new ExecutionResult(contractResult);
        when(serviceMock.compareObjectVersions(any(ObjectNode.class))).thenReturn(result);

        // Act
        int exitCode = command.execute(serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(0);

        ArgumentCaptor<ObjectNode> argumentCaptor = ArgumentCaptor.forClass(ObjectNode.class);
        verify(serviceMock).compareObjectVersions(argumentCaptor.capture());

        ObjectNode capturedArgument = argumentCaptor.getValue();
        assertThat(capturedArgument.get("object_id").asText()).isEqualTo("obj123");
        assertThat(capturedArgument.has("versions")).isTrue();
        assertThat(capturedArgument.get("versions").isArray()).isTrue();
        assertThat(capturedArgument.get("versions").get(0).asText()).isEqualTo("v1");
        assertThat(capturedArgument.get("versions").get(1).asText()).isEqualTo("v2");
        assertThat(capturedArgument.has("options")).isFalse();

        String stdout = outputStreamCaptor.toString(UTF_8.name());
        assertThat(stdout).contains("Result:");
        assertThat(stdout).contains("\"result\" : \"versions match\"");
      }

      @Test
      @DisplayName("returns 0 as exit code with all option")
      void returns0AsExitCodeWithAllOption() throws Exception {
        // Arrange
        String[] args =
            new String[] {
              "--properties=PROPERTIES_FILE",
              "--object-id=obj123",
              "--versions=[\"v1\",\"v2\"]",
              "--all"
            };
        ObjectVersionsComparison command = parseArgs(args);
        ClientService serviceMock = mock(ClientService.class);

        ContractExecutionResult contractResult =
            new ContractExecutionResult(
                "{}", null, Collections.emptyList(), Collections.emptyList());
        ExecutionResult result = new ExecutionResult(contractResult);
        when(serviceMock.compareObjectVersions(any(ObjectNode.class))).thenReturn(result);

        // Act
        int exitCode = command.execute(serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(0);

        ArgumentCaptor<ObjectNode> argumentCaptor = ArgumentCaptor.forClass(ObjectNode.class);
        verify(serviceMock).compareObjectVersions(argumentCaptor.capture());

        ObjectNode capturedArgument = argumentCaptor.getValue();
        assertThat(capturedArgument.get("object_id").asText()).isEqualTo("obj123");
        assertThat(capturedArgument.has("options")).isTrue();
        assertThat(capturedArgument.get("options").get("all").asBoolean()).isTrue();
        assertThat(capturedArgument.get("options").has("verbose")).isFalse();
      }

      @Test
      @DisplayName("returns 0 as exit code with verbose option")
      void returns0AsExitCodeWithVerboseOption() throws Exception {
        // Arrange
        String[] args =
            new String[] {
              "--properties=PROPERTIES_FILE",
              "--object-id=obj123",
              "--versions=[\"v1\",\"v2\"]",
              "--verbose"
            };
        ObjectVersionsComparison command = parseArgs(args);
        ClientService serviceMock = mock(ClientService.class);

        ContractExecutionResult contractResult =
            new ContractExecutionResult(
                "{}", null, Collections.emptyList(), Collections.emptyList());
        ExecutionResult result = new ExecutionResult(contractResult);
        when(serviceMock.compareObjectVersions(any(ObjectNode.class))).thenReturn(result);

        // Act
        int exitCode = command.execute(serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(0);

        ArgumentCaptor<ObjectNode> argumentCaptor = ArgumentCaptor.forClass(ObjectNode.class);
        verify(serviceMock).compareObjectVersions(argumentCaptor.capture());

        ObjectNode capturedArgument = argumentCaptor.getValue();
        assertThat(capturedArgument.get("object_id").asText()).isEqualTo("obj123");
        assertThat(capturedArgument.has("options")).isTrue();
        assertThat(capturedArgument.get("options").get("verbose").asBoolean()).isTrue();
        assertThat(capturedArgument.get("options").has("all")).isFalse();
      }

      @Test
      @DisplayName("returns 0 as exit code with all options")
      void returns0AsExitCodeWithAllOptions() throws Exception {
        // Arrange
        String[] args =
            new String[] {
              "--properties=PROPERTIES_FILE",
              "--object-id=obj123",
              "--versions=[\"v1\",\"v2\"]",
              "--all",
              "--verbose"
            };
        ObjectVersionsComparison command = parseArgs(args);
        ClientService serviceMock = mock(ClientService.class);

        ContractExecutionResult contractResult =
            new ContractExecutionResult(
                "{}", null, Collections.emptyList(), Collections.emptyList());
        ExecutionResult result = new ExecutionResult(contractResult);
        when(serviceMock.compareObjectVersions(any(ObjectNode.class))).thenReturn(result);

        // Act
        int exitCode = command.execute(serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(0);

        ArgumentCaptor<ObjectNode> argumentCaptor = ArgumentCaptor.forClass(ObjectNode.class);
        verify(serviceMock).compareObjectVersions(argumentCaptor.capture());

        ObjectNode capturedArgument = argumentCaptor.getValue();
        assertThat(capturedArgument.get("object_id").asText()).isEqualTo("obj123");
        assertThat(capturedArgument.has("options")).isTrue();
        assertThat(capturedArgument.get("options").get("all").asBoolean()).isTrue();
        assertThat(capturedArgument.get("options").get("verbose").asBoolean()).isTrue();
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
        String[] args =
            new String[] {
              "--properties=" + file.getAbsolutePath(),
              "--object-id=obj123",
              "--versions=[\"v1\",\"v2\"]",
              "--use-gateway"
            };
        ObjectVersionsComparison command = parseArgs(args);
        ClientServiceFactory factory = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);

        ContractExecutionResult contractResult =
            new ContractExecutionResult(
                null, null, Collections.emptyList(), Collections.emptyList());
        ExecutionResult result = new ExecutionResult(contractResult);
        when(serviceMock.compareObjectVersions(any(ObjectNode.class))).thenReturn(result);
        when(factory.create(any(GatewayClientConfig.class), anyBoolean())).thenReturn(serviceMock);

        // Act
        command.call(factory);

        // Verify
        verify(factory).create(any(GatewayClientConfig.class), eq(false));
        verify(factory, never()).create(any(ClientConfig.class), anyBoolean());
      }
    }

    @Nested
    @DisplayName("where ClientService throws ClientException")
    class whereClientExceptionIsThrownByClientService {
      @Test
      @DisplayName("returns 1 as exit code")
      void returns1AsExitCode(@TempDir Path tempDir) throws Exception {
        // Arrange
        File file = createDefaultClientPropertiesFile(tempDir, "client.props");
        String[] args =
            new String[] {
              "--properties=" + file.getAbsolutePath(), "--object-id=obj123", "--versions=[\"v1\",\"v2\"]"
            };
        ObjectVersionsComparison command = parseArgs(args);
        ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);

        when(factoryMock.create(any(ClientConfig.class), anyBoolean())).thenReturn(serviceMock);
        when(serviceMock.compareObjectVersions(any(ObjectNode.class)))
            .thenThrow(new ClientException("Failed to compare versions", StatusCode.RUNTIME_ERROR));

        // Act
        int exitCode = command.call(factoryMock);

        // Assert
        assertThat(exitCode).isEqualTo(1);
        verify(factoryMock).close();
        assertThat(outputStreamCaptor.toString(UTF_8.name()))
            .contains("Failed to compare versions");
      }
    }
  }

  private ObjectVersionsComparison parseArgs(String[] args) {
    return CommandLineTestUtils.parseArgs(commandLine, ObjectVersionsComparison.class, args);
  }
}
