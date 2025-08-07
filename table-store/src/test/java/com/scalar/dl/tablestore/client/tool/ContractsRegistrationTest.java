package com.scalar.dl.tablestore.client.tool;

import static com.scalar.dl.client.tool.CommandLineTestUtils.createDefaultClientPropertiesFile;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

import com.scalar.dl.client.config.GatewayClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.tool.CommandLineTestUtils;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.tablestore.client.service.ClientService;
import com.scalar.dl.tablestore.client.service.ClientServiceFactory;
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

public class ContractsRegistrationTest {
  private CommandLine commandLine;
  private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

  @BeforeEach
  void setup() throws Exception {
    commandLine = new CommandLine(new ContractsRegistration());

    // To verify the output to stdout, e.g., System.out.println(...).
    System.setOut(new PrintStream(outputStreamCaptor, true, UTF_8.name()));
  }

  @Nested
  @DisplayName("#call()")
  class call {
    @Nested
    @DisplayName("where register-contracts succeeds via ClientService")
    class whereRegisterContractsSucceedsViaClientService {
      @Test
      @DisplayName("returns 0 as exit code")
      void returns0AsExitCode() {
        // Arrange
        String[] args =
            new String[] {
              "--properties=PROPERTIES_FILE",
            };
        ContractsRegistration command = parseArgs(args);
        ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);

        // Act
        int exitCode = command.call(factoryMock, serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(0);
        verify(serviceMock).registerContracts();
        verify(factoryMock).close();
      }
    }

    @Nested
    @DisplayName("where register-contracts fails via ClientService")
    class whereRegisterContractsFailsViaClientService {
      @Test
      @DisplayName("returns 1 as exit code")
      void returns1AsExitCode() throws UnsupportedEncodingException {
        // Arrange
        String[] args =
            new String[] {
              "--properties=PROPERTIES_FILE",
            };
        ContractsRegistration command = parseArgs(args);
        ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);
        doThrow(new ClientException("Some error", StatusCode.RUNTIME_ERROR))
            .when(serviceMock)
            .registerContracts();

        // Act
        int exitCode = command.call(factoryMock, serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(1);
        verify(serviceMock).registerContracts();
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
      ContractsRegistration command = parseArgs(args);
      ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
      ClientService serviceMock = mock(ClientService.class);
      when(factoryMock.create(any(GatewayClientConfig.class), anyBoolean()))
          .thenReturn(serviceMock);

      // Act
      int exitCode = command.call(factoryMock);

      // Assert
      assertThat(exitCode).isEqualTo(0);
      verify(serviceMock).registerContracts();
      verify(factoryMock).close();
    }
  }

  private ContractsRegistration parseArgs(String[] args) {
    return CommandLineTestUtils.parseArgs(commandLine, ContractsRegistration.class, args);
  }
}
