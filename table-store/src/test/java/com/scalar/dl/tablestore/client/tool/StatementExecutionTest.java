package com.scalar.dl.tablestore.client.tool;

import static com.scalar.dl.client.tool.CommandLineTestUtils.createDefaultClientPropertiesFile;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.GatewayClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.tool.CommandLineTestUtils;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.tablestore.client.model.StatementExecutionResult;
import com.scalar.dl.tablestore.client.service.ClientService;
import com.scalar.dl.tablestore.client.service.ClientServiceFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

public class StatementExecutionTest {
  private CommandLine commandLine;
  private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

  @BeforeEach
  void setup() throws Exception {
    commandLine = new CommandLine(new StatementExecution());

    // To verify the output to stdout, e.g., System.out.println(...).
    System.setOut(new PrintStream(outputStreamCaptor, true, UTF_8.name()));
  }

  @Nested
  @DisplayName("#call()")
  class call {
    @Nested
    @DisplayName("where statement execution succeeds")
    class whereStatementExecutionSucceeds {
      @Test
      @DisplayName("returns 0 as exit code with result")
      void returns0AsExitCodeWithResult() throws Exception {
        // Arrange
        String[] args =
            new String[] {
              "--properties=PROPERTIES_FILE",
              "--statement=INSERT INTO test_table VALUES {'id': '123', 'name': 'test'}"
            };
        StatementExecution command = parseArgs(args);
        ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);

        String resultJson = "{\"status\":\"success\"}";
        ContractExecutionResult contractResult =
            new ContractExecutionResult(
                resultJson, null, Collections.emptyList(), Collections.emptyList());
        StatementExecutionResult result = new StatementExecutionResult(contractResult);
        when(serviceMock.executeStatement(anyString())).thenReturn(result);

        // Act
        int exitCode = command.call(factoryMock, serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(0);
        verify(serviceMock)
            .executeStatement("INSERT INTO test_table VALUES {'id': '123', 'name': 'test'}");
        verify(factoryMock).close();

        String stdout = outputStreamCaptor.toString(UTF_8.name());
        assertThat(stdout).contains("Result:");
        assertThat(stdout).contains("\"status\" : \"success\"");
      }

      @Test
      @DisplayName("returns 0 as exit code without result")
      void returns0AsExitCodeWithoutResult() throws Exception {
        // Arrange
        String[] args =
            new String[] {
              "--properties=PROPERTIES_FILE",
              "--statement=CREATE TABLE test_table (id STRING PRIMARY KEY, name STRING)"
            };
        StatementExecution command = parseArgs(args);
        ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);

        ContractExecutionResult contractResult =
            new ContractExecutionResult(
                null, null, Collections.emptyList(), Collections.emptyList());
        StatementExecutionResult result = new StatementExecutionResult(contractResult);
        when(serviceMock.executeStatement(anyString())).thenReturn(result);

        // Act
        int exitCode = command.call(factoryMock, serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(0);
        verify(serviceMock)
            .executeStatement("CREATE TABLE test_table (id STRING PRIMARY KEY, name STRING)");
        verify(factoryMock).close();

        String stdout = outputStreamCaptor.toString(UTF_8.name());
        assertThat(stdout).doesNotContain("Result:");
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
              "--statement=SELECT * FROM test_table",
              "--use-gateway"
            };
        StatementExecution command = parseArgs(args);
        ClientServiceFactory factory = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);

        ContractExecutionResult contractResult =
            new ContractExecutionResult(
                null, null, java.util.Collections.emptyList(), java.util.Collections.emptyList());
        StatementExecutionResult result = new StatementExecutionResult(contractResult);
        when(serviceMock.executeStatement(anyString())).thenReturn(result);
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
            new String[] {"--properties=PROPERTIES_FILE", "--statement=INVALID STATEMENT"};
        StatementExecution command = parseArgs(args);
        ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);

        when(serviceMock.executeStatement(anyString()))
            .thenThrow(new ClientException("Invalid statement", StatusCode.RUNTIME_ERROR));

        // Act
        int exitCode = command.call(factoryMock, serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(1);
        verify(factoryMock).close();
        assertThat(outputStreamCaptor.toString(UTF_8.name())).contains("Invalid statement");
      }
    }
  }

  private StatementExecution parseArgs(String[] args) {
    return CommandLineTestUtils.parseArgs(commandLine, StatementExecution.class, args);
  }
}
