package com.scalar.dl.hashstore.client.tool;

import static com.scalar.dl.client.tool.CommandLineTestUtils.createDefaultClientPropertiesFile;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

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
import picocli.CommandLine;

public class ObjectGetTest {
  private CommandLine commandLine;
  private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

  @BeforeEach
  void setup() throws Exception {
    commandLine = new CommandLine(new ObjectGet());
    System.setOut(new PrintStream(outputStreamCaptor, true, UTF_8.name()));
  }

  @Nested
  @DisplayName("#call()")
  class call {
    @Nested
    @DisplayName("where object get succeeds")
    class whereObjectGetSucceeds {
      @Test
      @DisplayName("returns 0 as exit code with result")
      void returns0AsExitCodeWithResult() throws Exception {
        // Arrange
        String[] args = new String[] {"--properties=PROPERTIES_FILE", "--object-id=obj123"};
        ObjectGet command = parseArgs(args);
        ClientService serviceMock = mock(ClientService.class);

        String resultJson = "{\"object_id\":\"obj123\",\"hash_value\":\"abc123hash\"}";
        ContractExecutionResult contractResult =
            new ContractExecutionResult(
                resultJson, null, Collections.emptyList(), Collections.emptyList());
        ExecutionResult result = new ExecutionResult(contractResult);
        when(serviceMock.getObject(anyString())).thenReturn(result);

        // Act
        int exitCode = command.execute(serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(0);
        verify(serviceMock).getObject("obj123");

        String stdout = outputStreamCaptor.toString(UTF_8.name());
        assertThat(stdout).contains("Result:");
        assertThat(stdout).contains("\"object_id\" : \"obj123\"");
        assertThat(stdout).contains("\"hash_value\" : \"abc123hash\"");
      }

      @Test
      @DisplayName("returns 0 as exit code when object not found")
      void returns0AsExitCodeWhenObjectNotFound() throws Exception {
        // Arrange
        String[] args = new String[] {"--properties=PROPERTIES_FILE", "--object-id=nonexistent"};
        ObjectGet command = parseArgs(args);
        ClientService serviceMock = mock(ClientService.class);

        ContractExecutionResult contractResult =
            new ContractExecutionResult(
                null, null, Collections.emptyList(), Collections.emptyList());
        ExecutionResult result = new ExecutionResult(contractResult);
        when(serviceMock.getObject(anyString())).thenReturn(result);

        // Act
        int exitCode = command.execute(serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(0);
        verify(serviceMock).getObject("nonexistent");

        String stdout = outputStreamCaptor.toString(UTF_8.name());
        assertThat(stdout).contains("Object not found.");
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
              "--properties=" + file.getAbsolutePath(), "--object-id=obj123", "--use-gateway"
            };
        ObjectGet command = parseArgs(args);
        ClientServiceFactory factory = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);

        ContractExecutionResult contractResult =
            new ContractExecutionResult(
                null, null, Collections.emptyList(), Collections.emptyList());
        ExecutionResult result = new ExecutionResult(contractResult);
        when(serviceMock.getObject(anyString())).thenReturn(result);
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
            new String[] {"--properties=" + file.getAbsolutePath(), "--object-id=obj123"};
        ObjectGet command = parseArgs(args);
        ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);

        when(factoryMock.create(any(ClientConfig.class), anyBoolean())).thenReturn(serviceMock);
        when(serviceMock.getObject(anyString()))
            .thenThrow(new ClientException("Failed to get object", StatusCode.RUNTIME_ERROR));

        // Act
        int exitCode = command.call(factoryMock);

        // Assert
        assertThat(exitCode).isEqualTo(1);
        verify(factoryMock).close();
        assertThat(outputStreamCaptor.toString(UTF_8.name())).contains("Failed to get object");
      }
    }
  }

  private ObjectGet parseArgs(String[] args) {
    return CommandLineTestUtils.parseArgs(commandLine, ObjectGet.class, args);
  }
}
