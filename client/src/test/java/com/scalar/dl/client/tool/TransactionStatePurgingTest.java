package com.scalar.dl.client.tool;

import static com.scalar.dl.client.tool.CommandLineTestUtils.createDefaultClientPropertiesFile;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.GatewayClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.client.service.ClientServiceFactory;
import com.scalar.dl.ledger.model.TransactionStatePurgeResult;
import com.scalar.dl.ledger.service.StatusCode;
import java.io.File;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

public class TransactionStatePurgingTest {
  private CommandLine commandLine;

  @BeforeEach
  void setup() {
    commandLine = new CommandLine(new TransactionStatePurging());
  }

  @Nested
  @DisplayName("#call()")
  class call {
    @Test
    @DisplayName("returns 0 as exit code")
    void returns0AsExitCode() throws ClientException {
      // Arrange
      String[] args = new String[] {"--properties=PROPERTIES_FILE"};
      TransactionStatePurging command = spy(parseArgs(args));
      doReturn(true).when(command).confirmPurging();
      ClientService serviceMock = mock(ClientService.class);
      when(serviceMock.purgeState()).thenReturn(new TransactionStatePurgeResult(3, 2, 1));

      // Act
      int exitCode = command.execute(serviceMock);

      // Assert
      assertThat(exitCode).isEqualTo(0);
      verify(serviceMock).purgeState();
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
              // Enable Gateway.
              "--use-gateway"
            };
        TransactionStatePurging command = spy(parseArgs(args));
        doReturn(true).when(command).confirmPurging();
        ClientServiceFactory factory = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);
        when(serviceMock.purgeState()).thenReturn(new TransactionStatePurgeResult(0, 0, 0));
        doReturn(serviceMock).when(factory).create(any(GatewayClientConfig.class), eq(false));

        // Act
        command.call(factory);

        // Verify
        verify(factory).create(any(GatewayClientConfig.class), eq(false));
        verify(factory, never()).create(any(ClientConfig.class), eq(false));
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
              // Gateway is disabled by default.
            };
        TransactionStatePurging command = spy(parseArgs(args));
        doReturn(true).when(command).confirmPurging();
        ClientServiceFactory factory = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);
        when(serviceMock.purgeState()).thenReturn(new TransactionStatePurgeResult(0, 0, 0));
        doReturn(serviceMock).when(factory).create(any(ClientConfig.class), eq(false));

        // Act
        command.call(factory);

        // Verify
        verify(factory).create(any(ClientConfig.class), eq(false));
        verify(factory, never()).create(any(GatewayClientConfig.class), eq(false));
      }
    }

    @Nested
    @DisplayName("where confirmation fails")
    class whereConfirmationFails {
      @Test
      @DisplayName("returns 1 as exit code and does not call purgeState")
      void returns1AsExitCodeAndDoesNotCallPurgeState() throws ClientException {
        // Arrange
        String[] args = new String[] {"--properties=PROPERTIES_FILE"};
        TransactionStatePurging command = spy(parseArgs(args));
        doReturn(false).when(command).confirmPurging();
        ClientService serviceMock = mock(ClientService.class);

        // Act
        int exitCode = command.execute(serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(1);
        verify(serviceMock, never()).purgeState();
      }
    }

    @Nested
    @DisplayName("where force option is true")
    class whereForceOptionIsTrue {
      @Test
      @DisplayName("returns 0 as exit code without confirmation prompt")
      void returns0AsExitCodeWithoutConfirmation() throws ClientException {
        // Arrange
        String[] args = new String[] {"--properties=PROPERTIES_FILE", "--force"};
        TransactionStatePurging command = parseArgs(args);
        ClientService serviceMock = mock(ClientService.class);
        when(serviceMock.purgeState()).thenReturn(new TransactionStatePurgeResult(0, 0, 0));

        // Act
        int exitCode = command.execute(serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(0);
        verify(serviceMock).purgeState();
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
        String[] args = new String[] {"--properties=" + file.getAbsolutePath()};
        TransactionStatePurging command = spy(parseArgs(args));
        doReturn(true).when(command).confirmPurging();
        // Mock service that throws an exception.
        ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);
        when(factoryMock.create(any(ClientConfig.class), eq(false))).thenReturn(serviceMock);
        doThrow(new ClientException("", StatusCode.RUNTIME_ERROR)).when(serviceMock).purgeState();

        // Act
        int exitCode = command.call(factoryMock);

        // Assert
        assertThat(exitCode).isEqualTo(1);
        verify(factoryMock).close();
      }
    }
  }

  private TransactionStatePurging parseArgs(String[] args) {
    return CommandLineTestUtils.parseArgs(commandLine, TransactionStatePurging.class, args);
  }
}
