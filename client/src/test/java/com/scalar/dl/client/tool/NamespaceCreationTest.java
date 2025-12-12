package com.scalar.dl.client.tool;

import static com.scalar.dl.client.tool.CommandLineTestUtils.createDefaultClientPropertiesFile;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

public class NamespaceCreationTest {
  private CommandLine commandLine;

  @BeforeEach
  void setup() {
    commandLine = new CommandLine(new NamespaceCreation());
  }

  @Nested
  @DisplayName("#call()")
  class call {
    @Test
    @DisplayName("returns 0 as exit code")
    void returns0AsExitCode() throws ClientException {
      // Arrange
      String[] args =
          new String[] {
            // Set the required options.
            "--properties=PROPERTIES_FILE", "--namespace=test_namespace",
          };
      NamespaceCreation command = parseArgs(args);
      ClientService serviceMock = mock(ClientService.class);

      // Act
      int exitCode = command.execute(serviceMock);

      // Assert
      assertThat(exitCode).isEqualTo(0);
      verify(serviceMock).createNamespace("test_namespace");
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
              "--namespace=test-namespace",
              // Enable Gateway.
              "--use-gateway"
            };
        NamespaceCreation command = parseArgs(args);
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
              propertiesOption, "--namespace=test-namespace",
              // Gateway is disabled by default.
            };
        NamespaceCreation command = parseArgs(args);
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
      @Test
      @DisplayName("returns 1 as exit code")
      void returns1AsExitCode(@TempDir Path tempDir) throws Exception {
        // Arrange
        File file = createDefaultClientPropertiesFile(tempDir, "client.props");
        String[] args =
            new String[] {
              // Set the required options.
              "--properties=" + file.getAbsolutePath(), "--namespace=test-namespace",
            };
        NamespaceCreation command = parseArgs(args);
        // Mock service that throws an exception.
        ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);
        when(factoryMock.create(any(ClientConfig.class))).thenReturn(serviceMock);
        doThrow(new ClientException("", StatusCode.RUNTIME_ERROR))
            .when(serviceMock)
            .createNamespace("test-namespace");

        // Act
        int exitCode = command.call(factoryMock);

        // Assert
        assertThat(exitCode).isEqualTo(1);
        verify(factoryMock).close();
      }
    }
  }

  private NamespaceCreation parseArgs(String[] args) {
    return CommandLineTestUtils.parseArgs(commandLine, NamespaceCreation.class, args);
  }
}
