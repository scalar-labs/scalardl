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
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.ledger.service.StatusCode;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import picocli.CommandLine;

public class LedgerValidationTest {
  private CommandLine commandLine;
  private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

  @BeforeEach
  void setup() throws Exception {
    commandLine = new CommandLine(new LedgerValidation());
    System.setOut(new PrintStream(outputStreamCaptor, true, UTF_8.name()));
  }

  @Nested
  @DisplayName("#call()")
  class call {
    @Nested
    @DisplayName("where object validation succeeds")
    class whereObjectValidationSucceeds {
      @Test
      @DisplayName("returns 0 as exit code with object ID")
      void returns0AsExitCodeWithObjectId() throws Exception {
        // Arrange
        String[] args = new String[] {"--properties=PROPERTIES_FILE", "--object-id=obj123"};
        LedgerValidation command = parseArgs(args);
        ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);
        LedgerValidationResult result = mock(LedgerValidationResult.class);

        when(result.getCode()).thenReturn(StatusCode.OK);
        when(result.getLedgerProof()).thenReturn(Optional.empty());
        when(result.getAuditorProof()).thenReturn(Optional.empty());
        when(serviceMock.validateObject(anyString(), anyInt(), anyInt())).thenReturn(result);

        // Act
        int exitCode = command.call(factoryMock, serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(0);

        ArgumentCaptor<String> objectIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> startAgeCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> endAgeCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(serviceMock)
            .validateObject(
                objectIdCaptor.capture(), startAgeCaptor.capture(), endAgeCaptor.capture());

        assertThat(objectIdCaptor.getValue()).isEqualTo("obj123");
        assertThat(startAgeCaptor.getValue()).isEqualTo(0);
        assertThat(endAgeCaptor.getValue()).isEqualTo(Integer.MAX_VALUE);

        verify(factoryMock).close();
      }

      @Test
      @DisplayName("returns 0 as exit code with custom start-age and end-age")
      void returns0AsExitCodeWithCustomAges() throws Exception {
        // Arrange
        String[] args =
            new String[] {
              "--properties=PROPERTIES_FILE", "--object-id=obj123", "--start-age=10", "--end-age=20"
            };
        LedgerValidation command = parseArgs(args);
        ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);
        LedgerValidationResult result = mock(LedgerValidationResult.class);

        when(result.getCode()).thenReturn(StatusCode.OK);
        when(result.getLedgerProof()).thenReturn(Optional.empty());
        when(result.getAuditorProof()).thenReturn(Optional.empty());
        when(serviceMock.validateObject(anyString(), anyInt(), anyInt())).thenReturn(result);

        // Act
        int exitCode = command.call(factoryMock, serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(0);

        ArgumentCaptor<String> objectIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> startAgeCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> endAgeCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(serviceMock)
            .validateObject(
                objectIdCaptor.capture(), startAgeCaptor.capture(), endAgeCaptor.capture());

        assertThat(objectIdCaptor.getValue()).isEqualTo("obj123");
        assertThat(startAgeCaptor.getValue()).isEqualTo(10);
        assertThat(endAgeCaptor.getValue()).isEqualTo(20);

        verify(factoryMock).close();
      }
    }

    @Nested
    @DisplayName("where collection validation succeeds")
    class whereCollectionValidationSucceeds {
      @Test
      @DisplayName("returns 0 as exit code with collection ID")
      void returns0AsExitCodeWithCollectionId() throws Exception {
        // Arrange
        String[] args = new String[] {"--properties=PROPERTIES_FILE", "--collection-id=col123"};
        LedgerValidation command = parseArgs(args);
        ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);
        LedgerValidationResult result = mock(LedgerValidationResult.class);

        when(result.getCode()).thenReturn(StatusCode.OK);
        when(result.getLedgerProof()).thenReturn(Optional.empty());
        when(result.getAuditorProof()).thenReturn(Optional.empty());
        when(serviceMock.validateCollection(anyString(), anyInt(), anyInt())).thenReturn(result);

        // Act
        int exitCode = command.call(factoryMock, serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(0);

        ArgumentCaptor<String> collectionIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> startAgeCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> endAgeCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(serviceMock)
            .validateCollection(
                collectionIdCaptor.capture(), startAgeCaptor.capture(), endAgeCaptor.capture());

        assertThat(collectionIdCaptor.getValue()).isEqualTo("col123");
        assertThat(startAgeCaptor.getValue()).isEqualTo(0);
        assertThat(endAgeCaptor.getValue()).isEqualTo(Integer.MAX_VALUE);

        verify(factoryMock).close();
      }

      @Test
      @DisplayName("returns 0 as exit code with custom start-age and end-age")
      void returns0AsExitCodeWithCustomAges() throws Exception {
        // Arrange
        String[] args =
            new String[] {
              "--properties=PROPERTIES_FILE",
              "--collection-id=col123",
              "--start-age=5",
              "--end-age=15"
            };
        LedgerValidation command = parseArgs(args);
        ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);
        LedgerValidationResult result = mock(LedgerValidationResult.class);

        when(result.getCode()).thenReturn(StatusCode.OK);
        when(result.getLedgerProof()).thenReturn(Optional.empty());
        when(result.getAuditorProof()).thenReturn(Optional.empty());
        when(serviceMock.validateCollection(anyString(), anyInt(), anyInt())).thenReturn(result);

        // Act
        int exitCode = command.call(factoryMock, serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(0);

        ArgumentCaptor<String> collectionIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> startAgeCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> endAgeCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(serviceMock)
            .validateCollection(
                collectionIdCaptor.capture(), startAgeCaptor.capture(), endAgeCaptor.capture());

        assertThat(collectionIdCaptor.getValue()).isEqualTo("col123");
        assertThat(startAgeCaptor.getValue()).isEqualTo(5);
        assertThat(endAgeCaptor.getValue()).isEqualTo(15);

        verify(factoryMock).close();
      }
    }

    @Nested
    @DisplayName("where useGateway option is true")
    class whereUseGatewayOptionIsTrue {
      @Test
      @DisplayName("create ClientService with GatewayClientConfig for object validation")
      public void createClientServiceWithGatewayClientConfigForObject(@TempDir Path tempDir)
          throws Exception {
        // Arrange
        File file = createDefaultClientPropertiesFile(tempDir, "client.props");
        String[] args =
            new String[] {
              "--properties=" + file.getAbsolutePath(), "--object-id=obj123", "--use-gateway"
            };
        LedgerValidation command = parseArgs(args);
        ClientServiceFactory factory = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);
        LedgerValidationResult result = mock(LedgerValidationResult.class);

        when(result.getCode()).thenReturn(StatusCode.OK);
        when(result.getLedgerProof()).thenReturn(Optional.empty());
        when(result.getAuditorProof()).thenReturn(Optional.empty());
        when(factory.create(any(GatewayClientConfig.class), anyBoolean())).thenReturn(serviceMock);
        when(serviceMock.validateObject(anyString(), anyInt(), anyInt())).thenReturn(result);

        // Act
        command.call(factory);

        // Verify
        verify(factory).create(any(GatewayClientConfig.class), eq(false));
        verify(factory, never()).create(any(ClientConfig.class), anyBoolean());
      }

      @Test
      @DisplayName("create ClientService with GatewayClientConfig for collection validation")
      public void createClientServiceWithGatewayClientConfigForCollection(@TempDir Path tempDir)
          throws Exception {
        // Arrange
        File file = createDefaultClientPropertiesFile(tempDir, "client.props");
        String[] args =
            new String[] {
              "--properties=" + file.getAbsolutePath(), "--collection-id=col123", "--use-gateway"
            };
        LedgerValidation command = parseArgs(args);
        ClientServiceFactory factory = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);
        LedgerValidationResult result = mock(LedgerValidationResult.class);

        when(result.getCode()).thenReturn(StatusCode.OK);
        when(result.getLedgerProof()).thenReturn(Optional.empty());
        when(result.getAuditorProof()).thenReturn(Optional.empty());
        when(factory.create(any(GatewayClientConfig.class), anyBoolean())).thenReturn(serviceMock);
        when(serviceMock.validateCollection(anyString(), anyInt(), anyInt())).thenReturn(result);

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
      @DisplayName("returns 1 as exit code for object validation")
      void returns1AsExitCodeForObjectValidation() throws UnsupportedEncodingException {
        // Arrange
        String[] args = new String[] {"--properties=PROPERTIES_FILE", "--object-id=obj123"};
        LedgerValidation command = parseArgs(args);
        ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);

        doThrow(new ClientException("Failed to validate object", StatusCode.RUNTIME_ERROR))
            .when(serviceMock)
            .validateObject(anyString(), anyInt(), anyInt());

        // Act
        int exitCode = command.call(factoryMock, serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(1);
        verify(factoryMock).close();
        assertThat(outputStreamCaptor.toString(UTF_8.name())).contains("Failed to validate object");
      }

      @Test
      @DisplayName("returns 1 as exit code for collection validation")
      void returns1AsExitCodeForCollectionValidation() throws UnsupportedEncodingException {
        // Arrange
        String[] args = new String[] {"--properties=PROPERTIES_FILE", "--collection-id=col123"};
        LedgerValidation command = parseArgs(args);
        ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);

        doThrow(new ClientException("Failed to validate collection", StatusCode.RUNTIME_ERROR))
            .when(serviceMock)
            .validateCollection(anyString(), anyInt(), anyInt());

        // Act
        int exitCode = command.call(factoryMock, serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(1);
        verify(factoryMock).close();
        assertThat(outputStreamCaptor.toString(UTF_8.name()))
            .contains("Failed to validate collection");
      }
    }
  }

  private LedgerValidation parseArgs(String[] args) {
    return CommandLineTestUtils.parseArgs(commandLine, LedgerValidation.class, args);
  }
}
