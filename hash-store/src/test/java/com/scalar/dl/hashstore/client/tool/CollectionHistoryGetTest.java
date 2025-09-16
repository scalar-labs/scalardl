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
import com.scalar.dl.ledger.model.ExecutionResult;
import com.scalar.dl.ledger.service.StatusCode;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

public class CollectionHistoryGetTest {
  private CommandLine commandLine;
  private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

  @BeforeEach
  void setup() throws Exception {
    commandLine = new CommandLine(new CollectionHistoryGet());
    System.setOut(new PrintStream(outputStreamCaptor, true, UTF_8.name()));
  }

  @Nested
  @DisplayName("#call()")
  class call {
    @Nested
    @DisplayName("where collection history get succeeds")
    class whereCollectionHistoryGetSucceeds {
      @Test
      @DisplayName("returns 0 as exit code without limit")
      void returns0AsExitCodeWithoutLimit() throws Exception {
        // Arrange
        String[] args = new String[] {"--properties=PROPERTIES_FILE", "--collection-id=coll123"};
        CollectionHistoryGet command = parseArgs(args);
        HashStoreClientService serviceMock = mock(HashStoreClientService.class);
        ExecutionResult resultMock = mock(ExecutionResult.class);

        when(serviceMock.getCollectionHistory("coll123")).thenReturn(resultMock);
        when(resultMock.getResult()).thenReturn(Optional.of("[{\"entry1\":\"data\"}]"));

        // Act
        int exitCode = command.execute(serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(0);
        verify(serviceMock).getCollectionHistory("coll123");
        verify(serviceMock, never()).getCollectionHistory(eq("coll123"), anyInt());
        assertThat(outputStreamCaptor.toString(UTF_8.name())).contains("Result:");
      }

      @Test
      @DisplayName("returns 0 as exit code with limit")
      void returns0AsExitCodeWithLimit() throws Exception {
        // Arrange
        String[] args =
            new String[] {"--properties=PROPERTIES_FILE", "--collection-id=coll123", "--limit=10"};
        CollectionHistoryGet command = parseArgs(args);
        HashStoreClientService serviceMock = mock(HashStoreClientService.class);
        ExecutionResult resultMock = mock(ExecutionResult.class);

        when(serviceMock.getCollectionHistory("coll123", 10)).thenReturn(resultMock);
        when(resultMock.getResult()).thenReturn(Optional.of("[{\"entry1\":\"data\"}]"));

        // Act
        int exitCode = command.execute(serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(0);
        verify(serviceMock).getCollectionHistory("coll123", 10);
        verify(serviceMock, never()).getCollectionHistory("coll123");
        assertThat(outputStreamCaptor.toString(UTF_8.name())).contains("Result:");
      }

      @Test
      @DisplayName("returns 0 as exit code with empty result")
      void returns0AsExitCodeWithEmptyResult() throws Exception {
        // Arrange
        String[] args = new String[] {"--properties=PROPERTIES_FILE", "--collection-id=coll123"};
        CollectionHistoryGet command = parseArgs(args);
        HashStoreClientService serviceMock = mock(HashStoreClientService.class);
        ExecutionResult resultMock = mock(ExecutionResult.class);

        when(serviceMock.getCollectionHistory("coll123")).thenReturn(resultMock);
        when(resultMock.getResult()).thenReturn(Optional.empty());

        // Act
        int exitCode = command.execute(serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(0);
        verify(serviceMock).getCollectionHistory("coll123");
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
        String[] args =
            new String[] {
              "--properties=" + file.getAbsolutePath(), "--collection-id=coll123", "--use-gateway"
            };
        CollectionHistoryGet command = parseArgs(args);
        HashStoreClientServiceFactory factory = mock(HashStoreClientServiceFactory.class);
        HashStoreClientService serviceMock = mock(HashStoreClientService.class);
        ExecutionResult resultMock = mock(ExecutionResult.class);

        when(factory.create(any(GatewayClientConfig.class), anyBoolean())).thenReturn(serviceMock);
        when(serviceMock.getCollectionHistory("coll123")).thenReturn(resultMock);
        when(resultMock.getResult()).thenReturn(Optional.empty());

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
        String[] args =
            new String[] {"--properties=" + file.getAbsolutePath(), "--collection-id=coll123"};
        CollectionHistoryGet command = parseArgs(args);
        HashStoreClientServiceFactory factoryMock = mock(HashStoreClientServiceFactory.class);
        HashStoreClientService serviceMock = mock(HashStoreClientService.class);

        when(factoryMock.create(any(ClientConfig.class), anyBoolean())).thenReturn(serviceMock);
        doThrow(new ClientException("Failed to get collection history", StatusCode.RUNTIME_ERROR))
            .when(serviceMock)
            .getCollectionHistory("coll123");

        // Act
        int exitCode = command.call(factoryMock);

        // Assert
        assertThat(exitCode).isEqualTo(1);
        verify(factoryMock).close();
        assertThat(outputStreamCaptor.toString(UTF_8.name()))
            .contains("Failed to get collection history");
      }
    }
  }

  private CollectionHistoryGet parseArgs(String[] args) {
    return CommandLineTestUtils.parseArgs(commandLine, CollectionHistoryGet.class, args);
  }
}
