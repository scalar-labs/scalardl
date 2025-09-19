package com.scalar.dl.tablestore.client.tool;

import static com.scalar.dl.client.tool.CommandLineTestUtils.createDefaultClientPropertiesFile;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.GatewayClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.tool.CommandLineTestUtils;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.tablestore.client.service.TableStoreClientService;
import com.scalar.dl.tablestore.client.service.TableStoreClientServiceFactory;
import java.io.File;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

public class LedgerValidationTest {
  private CommandLine commandLine;

  @BeforeEach
  void setup() {
    commandLine = new CommandLine(new LedgerValidation());
  }

  @Nested
  @DisplayName("#call()")
  class call {
    @Nested
    @DisplayName("where table-name is set")
    class whereTableNameIsSet {
      @Test
      @DisplayName("validates table schema when no key is specified and returns 0 as exit code")
      void validatesTableSchemaAndReturns0AsExitCode() {
        // Arrange
        String[] args =
            new String[] {
              "--properties=PROPERTIES_FILE",
              "--table-name=test_table",
              "--start-age=0",
              "--end-age=10"
            };
        LedgerValidation command = parseArgs(args);
        TableStoreClientServiceFactory factoryMock = mock(TableStoreClientServiceFactory.class);
        TableStoreClientService serviceMock = mock(TableStoreClientService.class);

        LedgerValidationResult result = new LedgerValidationResult(StatusCode.OK, null, null);
        when(serviceMock.validateTableSchema("test_table", 0, 10)).thenReturn(result);

        // Act
        int exitCode = command.call(factoryMock, serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(0);
        verify(serviceMock).validateTableSchema("test_table", 0, 10);
        verify(factoryMock).close();
      }

      @Test
      @DisplayName("validates record with primary key and returns 0 as exit code")
      void validatesRecordWithPrimaryKeyAndReturns0AsExitCode() {
        // Arrange
        String[] args =
            new String[] {
              "--properties=PROPERTIES_FILE",
              "--table-name=test_table",
              "--primary-key-column-name=id",
              "--column-value=\"123\"",
              "--start-age=0",
              "--end-age=10"
            };
        LedgerValidation command = parseArgs(args);
        TableStoreClientServiceFactory factoryMock = mock(TableStoreClientServiceFactory.class);
        TableStoreClientService serviceMock = mock(TableStoreClientService.class);

        LedgerValidationResult result = new LedgerValidationResult(StatusCode.OK, null, null);
        when(serviceMock.validateRecord("test_table", "id", "\"123\"", 0, 10)).thenReturn(result);

        // Act
        int exitCode = command.call(factoryMock, serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(0);
        verify(serviceMock).validateRecord("test_table", "id", "\"123\"", 0, 10);
        verify(factoryMock).close();
      }

      @Test
      @DisplayName("validates index record and returns 0 as exit code")
      void validatesIndexRecordAndReturns0AsExitCode() {
        // Arrange
        String[] args =
            new String[] {
              "--properties=PROPERTIES_FILE",
              "--table-name=test_table",
              "--index-key-column-name=email",
              "--column-value=\"test@example.com\"",
              "--start-age=0",
              "--end-age=10"
            };
        LedgerValidation command = parseArgs(args);
        TableStoreClientServiceFactory factoryMock = mock(TableStoreClientServiceFactory.class);
        TableStoreClientService serviceMock = mock(TableStoreClientService.class);

        LedgerValidationResult result = new LedgerValidationResult(StatusCode.OK, null, null);
        when(serviceMock.validateIndexRecord("test_table", "email", "\"test@example.com\"", 0, 10))
            .thenReturn(result);

        // Act
        int exitCode = command.call(factoryMock, serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(0);
        verify(serviceMock)
            .validateIndexRecord("test_table", "email", "\"test@example.com\"", 0, 10);
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
              "--properties=" + file.getAbsolutePath(), "--table-name=test_table", "--use-gateway"
            };
        LedgerValidation command = parseArgs(args);
        TableStoreClientServiceFactory factory = mock(TableStoreClientServiceFactory.class);
        TableStoreClientService serviceMock = mock(TableStoreClientService.class);

        LedgerValidationResult result = new LedgerValidationResult(StatusCode.OK, null, null);
        when(serviceMock.validateTableSchema(anyString(), anyInt(), anyInt())).thenReturn(result);
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
    class whereClientExceptionIsThrownByTableStoreClientService {
      @Test
      @DisplayName("returns 1 as exit code")
      void returns1AsExitCode() {
        // Arrange
        String[] args = new String[] {"--properties=PROPERTIES_FILE", "--table-name=test_table"};
        LedgerValidation command = parseArgs(args);
        TableStoreClientServiceFactory factoryMock = mock(TableStoreClientServiceFactory.class);
        TableStoreClientService serviceMock = mock(TableStoreClientService.class);

        when(serviceMock.validateTableSchema(anyString(), anyInt(), anyInt()))
            .thenThrow(new ClientException("Validation failed", StatusCode.RUNTIME_ERROR));

        // Act
        int exitCode = command.call(factoryMock, serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(1);
        verify(factoryMock).close();
      }
    }
  }

  private LedgerValidation parseArgs(String[] args) {
    return CommandLineTestUtils.parseArgs(commandLine, LedgerValidation.class, args);
  }
}
