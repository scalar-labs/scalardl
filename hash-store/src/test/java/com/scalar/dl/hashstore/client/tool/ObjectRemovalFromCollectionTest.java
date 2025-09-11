package com.scalar.dl.hashstore.client.tool;

import static com.scalar.dl.client.tool.CommandLineTestUtils.createDefaultClientPropertiesFile;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.GatewayClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.tool.CommandLineTestUtils;
import com.scalar.dl.hashstore.client.service.ClientService;
import com.scalar.dl.hashstore.client.service.ClientServiceFactory;
import com.scalar.dl.ledger.service.StatusCode;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import picocli.CommandLine;

public class ObjectRemovalFromCollectionTest {
  private CommandLine commandLine;
  private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

  @BeforeEach
  void setup() throws Exception {
    commandLine = new CommandLine(new ObjectRemovalFromCollection());
    System.setOut(new PrintStream(outputStreamCaptor, true, UTF_8.name()));
  }

  @Nested
  @DisplayName("#call()")
  class call {
    @Nested
    @DisplayName("where object removal from collection succeeds")
    class whereObjectRemovalSucceeds {
      @Test
      @DisplayName("returns 0 as exit code with single object ID")
      void returns0AsExitCodeWithSingleObjectId() throws Exception {
        // Arrange
        String[] args =
            new String[] {
              "--properties=PROPERTIES_FILE", "--collection-id=col123", "--object-ids=obj1"
            };
        ObjectRemovalFromCollection command = parseArgs(args);
        ClientService serviceMock = mock(ClientService.class);

        // Act
        int exitCode = command.execute(serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(0);

        ArgumentCaptor<String> collectionIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List> objectIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(serviceMock)
            .removeFromCollection(collectionIdCaptor.capture(), objectIdsCaptor.capture());

        assertThat(collectionIdCaptor.getValue()).isEqualTo("col123");
        assertThat(objectIdsCaptor.getValue()).isEqualTo(Collections.singletonList("obj1"));
      }

      @Test
      @DisplayName("returns 0 as exit code with multiple object IDs")
      void returns0AsExitCodeWithMultipleObjectIds() throws Exception {
        // Arrange
        String[] args =
            new String[] {
              "--properties=PROPERTIES_FILE",
              "--collection-id=col123",
              "--object-ids=obj1",
              "--object-ids=obj2",
              "--object-ids=obj3"
            };
        ObjectRemovalFromCollection command = parseArgs(args);
        ClientService serviceMock = mock(ClientService.class);

        // Act
        int exitCode = command.execute(serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(0);

        ArgumentCaptor<String> collectionIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List> objectIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(serviceMock)
            .removeFromCollection(collectionIdCaptor.capture(), objectIdsCaptor.capture());

        assertThat(collectionIdCaptor.getValue()).isEqualTo("col123");
        assertThat(objectIdsCaptor.getValue()).isEqualTo(Arrays.asList("obj1", "obj2", "obj3"));
      }

      @Test
      @DisplayName("returns 0 as exit code with force option")
      void returns0AsExitCodeWithForceOption() throws Exception {
        // Arrange
        String[] args =
            new String[] {
              "--properties=PROPERTIES_FILE",
              "--collection-id=col123",
              "--object-ids=obj1",
              "--force"
            };
        ObjectRemovalFromCollection command = parseArgs(args);
        ClientService serviceMock = mock(ClientService.class);

        // Act
        int exitCode = command.execute(serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(0);

        ArgumentCaptor<String> collectionIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List> objectIdsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Boolean> forceCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(serviceMock)
            .removeFromCollection(
                collectionIdCaptor.capture(), objectIdsCaptor.capture(), forceCaptor.capture());

        assertThat(collectionIdCaptor.getValue()).isEqualTo("col123");
        assertThat(objectIdsCaptor.getValue()).isEqualTo(Collections.singletonList("obj1"));
        assertThat(forceCaptor.getValue()).isTrue();
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
              "--collection-id=col123",
              "--object-ids=obj1",
              "--use-gateway"
            };
        ObjectRemovalFromCollection command = parseArgs(args);
        ClientServiceFactory factory = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);

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
            new String[] {
              "--properties=" + file.getAbsolutePath(),
              "--collection-id=col123",
              "--object-ids=obj1"
            };
        ObjectRemovalFromCollection command = parseArgs(args);
        ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);

        when(factoryMock.create(any(ClientConfig.class), anyBoolean())).thenReturn(serviceMock);
        doThrow(new ClientException("Failed to remove objects", StatusCode.RUNTIME_ERROR))
            .when(serviceMock)
            .removeFromCollection(anyString(), anyList());

        // Act
        int exitCode = command.call(factoryMock);

        // Assert
        assertThat(exitCode).isEqualTo(1);
        verify(factoryMock).close();
        assertThat(outputStreamCaptor.toString(UTF_8.name())).contains("Failed to remove objects");
      }
    }
  }

  private ObjectRemovalFromCollection parseArgs(String[] args) {
    return CommandLineTestUtils.parseArgs(commandLine, ObjectRemovalFromCollection.class, args);
  }
}
