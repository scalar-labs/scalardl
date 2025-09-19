package com.scalar.dl.tablestore.client.tool;

import static com.scalar.dl.client.tool.CommandLineTestUtils.createDefaultClientPropertiesFile;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

import com.scalar.dl.client.config.GatewayClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.tool.CommandLineTestUtils;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.tablestore.client.service.TableStoreClientService;
import com.scalar.dl.tablestore.client.service.TableStoreClientServiceFactory;
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
import picocli.CommandLine;

public class BootstrapTest {
  private CommandLine commandLine;
  private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

  @BeforeEach
  void setup() throws Exception {
    commandLine = new CommandLine(new Bootstrap());

    // To verify the output to stdout, e.g., System.out.println(...).
    System.setOut(new PrintStream(outputStreamCaptor, true, UTF_8.name()));
  }

  @Nested
  @DisplayName("#call()")
  class call {
    @Nested
    @DisplayName("where bootstrap succeeds via ClientService")
    class whereBootstrapSucceedsViaTableStoreClientService {
      @Test
      @DisplayName("returns 0 as exit code")
      void returns0AsExitCode() {
        // Arrange
        String[] args =
            new String[] {
              "--properties=PROPERTIES_FILE",
            };
        Bootstrap command = parseArgs(args);
        TableStoreClientServiceFactory factoryMock = mock(TableStoreClientServiceFactory.class);
        TableStoreClientService serviceMock = mock(TableStoreClientService.class);

        // Act
        int exitCode = command.call(factoryMock, serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(0);
        verify(serviceMock).bootstrap();
        verify(factoryMock).close();
      }
    }

    @Nested
    @DisplayName("where bootstrap fails via ClientService")
    class whereBootstrapFailsViaTableStoreClientService {
      @Test
      @DisplayName("returns 1 as exit code")
      void returns1AsExitCode() throws UnsupportedEncodingException {
        // Arrange
        String[] args =
            new String[] {
              "--properties=PROPERTIES_FILE",
            };
        Bootstrap command = parseArgs(args);
        TableStoreClientServiceFactory factoryMock = mock(TableStoreClientServiceFactory.class);
        TableStoreClientService serviceMock = mock(TableStoreClientService.class);
        doThrow(new ClientException("Some error", StatusCode.RUNTIME_ERROR))
            .when(serviceMock)
            .bootstrap();

        // Act
        int exitCode = command.call(factoryMock, serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(1);
        verify(serviceMock).bootstrap();
        verify(factoryMock).close();
        assertThat(outputStreamCaptor.toString(UTF_8.name())).contains("Some error");
      }
    }

    @Test
    @DisplayName("with --use-gateway option returns 0 as exit code")
    void withGatewayOptionReturns0AsExitCode(@TempDir Path tempDir) throws Exception {
      // Arrange
      File propertiesFile = createDefaultClientPropertiesFile(tempDir, "client.properties");
      String[] args =
          new String[] {
            "--properties=" + propertiesFile.getAbsolutePath(), "--use-gateway",
          };
      Bootstrap command = parseArgs(args);
      TableStoreClientServiceFactory factoryMock = mock(TableStoreClientServiceFactory.class);
      TableStoreClientService serviceMock = mock(TableStoreClientService.class);
      when(factoryMock.create(any(GatewayClientConfig.class), anyBoolean()))
          .thenReturn(serviceMock);

      // Act
      int exitCode = command.call(factoryMock);

      // Assert
      assertThat(exitCode).isEqualTo(0);
      verify(serviceMock).bootstrap();
      verify(factoryMock).close();
    }
  }

  private Bootstrap parseArgs(String[] args) {
    return CommandLineTestUtils.parseArgs(commandLine, Bootstrap.class, args);
  }
}
