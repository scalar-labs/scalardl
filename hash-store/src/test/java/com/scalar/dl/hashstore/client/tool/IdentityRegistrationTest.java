package com.scalar.dl.hashstore.client.tool;

import static com.scalar.dl.client.tool.CommandLineTestUtils.createDefaultClientPropertiesFile;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.GatewayClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.tool.CommandLineTestUtils;
import com.scalar.dl.hashstore.client.service.HashStoreClientService;
import com.scalar.dl.hashstore.client.service.HashStoreClientServiceFactory;
import com.scalar.dl.ledger.service.StatusCode;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

public class IdentityRegistrationTest {
  private CommandLine commandLine;
  private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

  @BeforeEach
  void setup() throws Exception {
    commandLine = new CommandLine(new IdentityRegistration());
    System.setOut(new PrintStream(outputStreamCaptor, true, UTF_8.name()));
  }

  @Nested
  @DisplayName("#call()")
  class call {
    @Nested
    @DisplayName("where identity registration succeeds")
    class whereIdentityRegistrationSucceeds {
      @Test
      @DisplayName("returns 0 as exit code")
      void returns0AsExitCode() {
        // Arrange
        String[] args = new String[] {"--properties=PROPERTIES_FILE"};
        IdentityRegistration command = parseArgs(args);
        HashStoreClientService serviceMock = mock(HashStoreClientService.class);

        // Act
        int exitCode = command.execute(serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(0);
        verify(serviceMock).registerIdentity();
      }
    }

    @Nested
    @DisplayName("where useGateway option is true")
    class whereUseGatewayOptionIsTrue {
      @Test
      @DisplayName("create HashStoreClientService with GatewayClientConfig")
      public void createHashStoreClientServiceWithGatewayClientConfig(@TempDir Path tempDir)
          throws Exception {
        // Arrange
        File file = createDefaultClientPropertiesFile(tempDir, "client.props");
        String[] args = new String[] {"--properties=" + file.getAbsolutePath(), "--use-gateway"};
        IdentityRegistration command = parseArgs(args);
        HashStoreClientServiceFactory factory = mock(HashStoreClientServiceFactory.class);
        HashStoreClientService serviceMock = mock(HashStoreClientService.class);

        when(factory.create(any(GatewayClientConfig.class), anyBoolean())).thenReturn(serviceMock);

        // Act
        command.call(factory);

        // Verify
        verify(factory).create(any(GatewayClientConfig.class), eq(false));
        verify(factory, never()).create(any(ClientConfig.class), anyBoolean());
      }
    }

    @Nested
    @DisplayName("where HashStoreClientService throws ClientException")
    class whereClientExceptionIsThrownByHashStoreClientService {
      @Test
      @DisplayName("returns 1 as exit code")
      void returns1AsExitCode(@TempDir Path tempDir) throws Exception {
        // Arrange
        File file = createDefaultClientPropertiesFile(tempDir, "client.props");
        String[] args = new String[] {"--properties=" + file.getAbsolutePath()};
        IdentityRegistration command = parseArgs(args);
        HashStoreClientServiceFactory factoryMock = mock(HashStoreClientServiceFactory.class);
        HashStoreClientService serviceMock = mock(HashStoreClientService.class);

        when(factoryMock.create(any(ClientConfig.class), anyBoolean())).thenReturn(serviceMock);
        doThrow(new ClientException("Failed to register identity", StatusCode.RUNTIME_ERROR))
            .when(serviceMock)
            .registerIdentity();

        // Act
        int exitCode = command.call(factoryMock);

        // Assert
        assertThat(exitCode).isEqualTo(1);
        verify(factoryMock).close();
        assertThat(outputStreamCaptor.toString(UTF_8.name()))
            .contains("Failed to register identity");
      }
    }
  }

  private IdentityRegistration parseArgs(String[] args) {
    return CommandLineTestUtils.parseArgs(commandLine, IdentityRegistration.class, args);
  }
}
