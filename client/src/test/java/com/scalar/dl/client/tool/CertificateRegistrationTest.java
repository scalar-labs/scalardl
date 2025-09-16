package com.scalar.dl.client.tool;

import static com.scalar.dl.client.tool.CommandLineTestUtils.createDefaultClientPropertiesFile;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

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

public class CertificateRegistrationTest {
  private CommandLine commandLine;

  @BeforeEach
  void setup() {
    commandLine = new CommandLine(new CertificateRegistration());
  }

  @Nested
  @DisplayName("#call()")
  class call {
    @Test
    @DisplayName("returns 0 as exit code")
    void returns0AsExitCode() {
      // Arrange
      String[] args =
          new String[] {
            // Set the required options.
            "--properties=PROPERTIES_FILE",
          };
      CertificateRegistration command = parseArgs(args);
      // Mock service that returns ContractExecutionResult.
      ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
      ClientService serviceMock = mock(ClientService.class);

      // Act
      int exitCode = command.call(factoryMock, serviceMock);

      // Assert
      assertThat(exitCode).isEqualTo(0);
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
        CertificateRegistration command = parseArgs(args);
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
              propertiesOption,
              // Gateway is disabled by default.
            };
        CertificateRegistration command = parseArgs(args);
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
      void returns1AsExitCode() {
        // Arrange
        String[] args =
            new String[] {
              // Set the required options.
              "--properties=PROPERTIES_FILE",
            };
        CertificateRegistration command = parseArgs(args);
        // Mock service that throws an exception.
        ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);
        doThrow(new ClientException("", StatusCode.RUNTIME_ERROR))
            .when(serviceMock)
            .registerCertificate();

        // Act
        int exitCode = command.call(factoryMock, serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(1);
      }
    }
  }

  private CertificateRegistration parseArgs(String[] args) {
    return CommandLineTestUtils.parseArgs(commandLine, CertificateRegistration.class, args);
  }
}
