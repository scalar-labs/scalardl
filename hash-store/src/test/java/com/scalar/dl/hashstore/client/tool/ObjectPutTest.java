package com.scalar.dl.hashstore.client.tool;

import static com.scalar.dl.client.tool.CommandLineTestUtils.createDefaultClientPropertiesFile;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.GatewayClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.tool.CommandLineTestUtils;
import com.scalar.dl.hashstore.client.service.ClientService;
import com.scalar.dl.hashstore.client.service.ClientServiceFactory;
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
import org.mockito.ArgumentCaptor;
import picocli.CommandLine;

public class ObjectPutTest {
  private CommandLine commandLine;
  private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

  @BeforeEach
  void setup() throws Exception {
    commandLine = new CommandLine(new ObjectPut());
    System.setOut(new PrintStream(outputStreamCaptor, true, UTF_8.name()));
  }

  @Nested
  @DisplayName("#call()")
  class call {
    @Nested
    @DisplayName("where object put succeeds")
    class whereObjectPutSucceeds {
      @Test
      @DisplayName("returns 0 as exit code with basic parameters")
      void returns0AsExitCodeWithBasicParameters() throws Exception {
        // Arrange
        String[] args =
            new String[] {
              "--properties=PROPERTIES_FILE", "--object-id=obj123", "--hash=abc123hash"
            };
        ObjectPut command = parseArgs(args);
        ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);

        // Act
        int exitCode = command.call(factoryMock, serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(0);

        ArgumentCaptor<ObjectNode> argumentCaptor = ArgumentCaptor.forClass(ObjectNode.class);
        verify(serviceMock).putObject(argumentCaptor.capture());

        ObjectNode capturedArgument = argumentCaptor.getValue();
        assertThat(capturedArgument.get("object_id").asText()).isEqualTo("obj123");
        assertThat(capturedArgument.get("hash_value").asText()).isEqualTo("abc123hash");
        assertThat(capturedArgument.has("metadata")).isFalse();

        verify(factoryMock).close();
      }

      @Test
      @DisplayName("returns 0 as exit code with metadata")
      void returns0AsExitCodeWithMetadata() throws Exception {
        // Arrange
        String[] args =
            new String[] {
              "--properties=PROPERTIES_FILE",
              "--object-id=obj123",
              "--hash=abc123hash",
              "--metadata={\"key\":\"value\",\"timestamp\":\"2023-01-01\"}"
            };
        ObjectPut command = parseArgs(args);
        ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);

        // Act
        int exitCode = command.call(factoryMock, serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(0);

        ArgumentCaptor<ObjectNode> argumentCaptor = ArgumentCaptor.forClass(ObjectNode.class);
        verify(serviceMock).putObject(argumentCaptor.capture());

        ObjectNode capturedArgument = argumentCaptor.getValue();
        assertThat(capturedArgument.get("object_id").asText()).isEqualTo("obj123");
        assertThat(capturedArgument.get("hash_value").asText()).isEqualTo("abc123hash");
        assertThat(capturedArgument.has("metadata")).isTrue();
        JsonNode metadata = capturedArgument.get("metadata");
        assertThat(metadata.get("key").asText()).isEqualTo("value");
        assertThat(metadata.get("timestamp").asText()).isEqualTo("2023-01-01");

        verify(factoryMock).close();
      }

      @Test
      @DisplayName("returns 0 as exit code with put-to-mutable")
      void returns0AsExitCodeWithPutToMutable() throws Exception {
        // Arrange
        String[] args =
            new String[] {
              "--properties=PROPERTIES_FILE",
              "--object-id=obj123",
              "--hash=abc123hash",
              "--put-to-mutable={\"table\":\"test_table\",\"key\":\"key1\"}"
            };
        ObjectPut command = parseArgs(args);
        ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);

        // Act
        int exitCode = command.call(factoryMock, serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(0);

        ArgumentCaptor<ObjectNode> argumentCaptor = ArgumentCaptor.forClass(ObjectNode.class);
        ArgumentCaptor<JsonNode> putCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(serviceMock).putObject(argumentCaptor.capture(), putCaptor.capture());

        ObjectNode capturedArgument = argumentCaptor.getValue();
        assertThat(capturedArgument.get("object_id").asText()).isEqualTo("obj123");
        assertThat(capturedArgument.get("hash_value").asText()).isEqualTo("abc123hash");

        JsonNode capturedPut = putCaptor.getValue();
        assertThat(capturedPut.get("table").asText()).isEqualTo("test_table");
        assertThat(capturedPut.get("key").asText()).isEqualTo("key1");

        verify(factoryMock).close();
      }

      @Test
      @DisplayName("returns 0 as exit code with all optional parameters")
      void returns0AsExitCodeWithAllOptionalParameters() throws Exception {
        // Arrange
        String[] args =
            new String[] {
              "--properties=PROPERTIES_FILE",
              "--object-id=obj123",
              "--hash=abc123hash",
              "--metadata={\"key\":\"value\"}",
              "--put-to-mutable={\"table\":\"test_table\"}"
            };
        ObjectPut command = parseArgs(args);
        ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);

        // Act
        int exitCode = command.call(factoryMock, serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(0);

        ArgumentCaptor<ObjectNode> argumentCaptor = ArgumentCaptor.forClass(ObjectNode.class);
        ArgumentCaptor<JsonNode> putCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(serviceMock).putObject(argumentCaptor.capture(), putCaptor.capture());

        ObjectNode capturedArgument = argumentCaptor.getValue();
        assertThat(capturedArgument.get("object_id").asText()).isEqualTo("obj123");
        assertThat(capturedArgument.get("hash_value").asText()).isEqualTo("abc123hash");
        assertThat(capturedArgument.has("metadata")).isTrue();
        JsonNode metadata = capturedArgument.get("metadata");
        assertThat(metadata.get("key").asText()).isEqualTo("value");

        JsonNode capturedPut = putCaptor.getValue();
        assertThat(capturedPut.get("table").asText()).isEqualTo("test_table");

        verify(factoryMock).close();
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
              "--hash=abc123hash",
              "--use-gateway"
            };
        ObjectPut command = parseArgs(args);
        ClientServiceFactory factory = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);

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
      void returns1AsExitCode() throws UnsupportedEncodingException {
        // Arrange
        String[] args =
            new String[] {
              "--properties=PROPERTIES_FILE", "--object-id=obj123", "--hash=abc123hash"
            };
        ObjectPut command = parseArgs(args);
        ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);

        doThrow(new ClientException("Failed to put object", StatusCode.RUNTIME_ERROR))
            .when(serviceMock)
            .putObject(any(ObjectNode.class));

        // Act
        int exitCode = command.call(factoryMock, serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(1);
        verify(factoryMock).close();
        assertThat(outputStreamCaptor.toString(UTF_8.name())).contains("Failed to put object");
      }
    }
  }

  private ObjectPut parseArgs(String[] args) {
    return CommandLineTestUtils.parseArgs(commandLine, ObjectPut.class, args);
  }
}
