package com.scalar.dl.client.tool;

import static com.scalar.dl.client.tool.CommandLineTestUtils.createDefaultClientPropertiesFile;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableList;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.GatewayClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientServiceFactory;
import com.scalar.dl.client.service.GenericContractClientService;
import com.scalar.dl.genericcontracts.AssetType;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.ledger.service.StatusCode;
import java.io.File;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import picocli.CommandLine;

public class GenericContractLedgerValidationTest {
  @Mock private ClientServiceFactory factory;
  @Mock private GenericContractClientService clientService;
  private CommandLine commandLine;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    commandLine = new CommandLine(new GenericContractLedgerValidation());
  }

  @Nested
  @DisplayName("#call()")
  class call {
    @Nested
    @DisplayName("with --object-id")
    class withObjectId {
      @Test
      @DisplayName("without ages")
      void withoutAges() {
        // Arrange
        String[] args =
            new String[] {
              "--properties=PROPERTIES_FILE", "--object-id=OBJECT_ID",
            };
        ImmutableList<String> keys = ImmutableList.of("OBJECT_ID");
        GenericContractLedgerValidation command = parseArgs(args);
        LedgerValidationResult result = new LedgerValidationResult(StatusCode.OK, null, null);
        when(clientService.validateLedger(AssetType.OBJECT, keys, 0, Integer.MAX_VALUE))
            .thenReturn(result);

        // Act
        int exitCode = command.call(factory, clientService);

        // Assert
        assertThat(exitCode).isEqualTo(0);
        verify(clientService).validateLedger(AssetType.OBJECT, keys, 0, Integer.MAX_VALUE);
      }

      @Test
      @DisplayName("with ages")
      void withAges() {
        // Arrange
        String[] args =
            new String[] {
              "--properties=PROPERTIES_FILE",
              "--object-id=OBJECT_ID",
              "--start-age=10",
              "--end-age=20",
            };
        ImmutableList<String> keys = ImmutableList.of("OBJECT_ID");
        GenericContractLedgerValidation command = parseArgs(args);
        LedgerValidationResult result = new LedgerValidationResult(StatusCode.OK, null, null);
        when(clientService.validateLedger(AssetType.OBJECT, keys, 10, 20)).thenReturn(result);

        // Act
        int exitCode = command.call(factory, clientService);

        // Assert
        assertThat(exitCode).isEqualTo(0);
        verify(clientService).validateLedger(AssetType.OBJECT, keys, 10, 20);
      }
    }

    @Nested
    @DisplayName("with --collection-id")
    class withCollectionId {
      @Test
      @DisplayName("without ages")
      void withoutAges() {
        // Arrange
        String[] args =
            new String[] {
              "--properties=PROPERTIES_FILE", "--collection-id=COLLECTION_ID",
            };
        ImmutableList<String> keys = ImmutableList.of("COLLECTION_ID");
        GenericContractLedgerValidation command = parseArgs(args);
        LedgerValidationResult result = new LedgerValidationResult(StatusCode.OK, null, null);
        when(clientService.validateLedger(AssetType.COLLECTION, keys, 0, Integer.MAX_VALUE))
            .thenReturn(result);

        // Act
        int exitCode = command.call(factory, clientService);

        // Assert
        assertThat(exitCode).isEqualTo(0);
        verify(clientService).validateLedger(AssetType.COLLECTION, keys, 0, Integer.MAX_VALUE);
      }

      @Test
      @DisplayName("with ages")
      void withAges() {
        // Arrange
        String[] args =
            new String[] {
              "--properties=PROPERTIES_FILE",
              "--collection-id=COLLECTION_ID",
              "--start-age=10",
              "--end-age=20",
            };
        ImmutableList<String> keys = ImmutableList.of("COLLECTION_ID");
        GenericContractLedgerValidation command = parseArgs(args);
        LedgerValidationResult result = new LedgerValidationResult(StatusCode.OK, null, null);
        when(clientService.validateLedger(AssetType.COLLECTION, keys, 10, 20)).thenReturn(result);

        // Act
        int exitCode = command.call(factory, clientService);

        // Assert
        assertThat(exitCode).isEqualTo(0);
        verify(clientService).validateLedger(AssetType.COLLECTION, keys, 10, 20);
      }
    }

    @Nested
    @DisplayName("with gateway option")
    class withGatewayOption {
      @Test
      @DisplayName("create ClientService with GatewayClientConfig")
      public void createClientServiceWithGatewayClientConfig(@TempDir Path tempDir)
          throws Exception {
        // Arrange
        File file = createDefaultClientPropertiesFile(tempDir, "client.props");
        String propertiesOption = String.format("--properties=%s", file.getAbsolutePath());
        String[] args =
            new String[] {
              propertiesOption,
              "--use-gateway",
              "--object-id=OBJECT_ID",
              "--start-age=0",
              "--end-age=10",
            };

        ImmutableList<String> keys = ImmutableList.of("OBJECT_ID");
        GenericContractLedgerValidation command = parseArgs(args);
        LedgerValidationResult result = new LedgerValidationResult(StatusCode.OK, null, null);
        when(clientService.validateLedger(AssetType.OBJECT, keys, 0, 10)).thenReturn(result);
        when(factory.createForGenericContract(any(GatewayClientConfig.class)))
            .thenReturn(clientService);

        // Act
        command.call(factory);

        // Verify
        verify(factory).createForGenericContract(any(GatewayClientConfig.class));
        verify(factory, never()).createForGenericContract(any(ClientConfig.class));
      }
    }

    @Nested
    @DisplayName("without gateway option")
    class withoutGatewayOption {
      @Test
      @DisplayName("create ClientService with ClientConfig")
      public void createClientServiceWithClientConfig(@TempDir Path tempDir) throws Exception {
        // Arrange
        File file = createDefaultClientPropertiesFile(tempDir, "client.props");
        String propertiesOption = String.format("--properties=%s", file.getAbsolutePath());
        String[] args =
            new String[] {
              propertiesOption, "--object-id=OBJECT_ID", "--start-age=0", "--end-age=10",
            };

        ImmutableList<String> keys = ImmutableList.of("OBJECT_ID");
        GenericContractLedgerValidation command = parseArgs(args);
        LedgerValidationResult result = new LedgerValidationResult(StatusCode.OK, null, null);
        when(clientService.validateLedger(AssetType.OBJECT, keys, 0, 10)).thenReturn(result);
        when(factory.createForGenericContract(any(ClientConfig.class))).thenReturn(clientService);

        // Act
        command.call(factory);

        // Verify
        verify(factory).createForGenericContract(any(ClientConfig.class));
        verify(factory, never()).createForGenericContract(any(GatewayClientConfig.class));
      }
    }

    @Nested
    @DisplayName("where ClientService throws exceptions")
    class whereClientServiceThrowsExceptions {
      @Test
      @DisplayName("with IllegalArgumentException")
      void throwsIllegalArgumentExceptionThenReturns1() {
        // Arrange
        String[] args =
            new String[] {
              "--properties=PROPERTIES_FILE",
              "--object-id=OBJECT_ID",
              "--start-age=10",
              "--end-age=0",
            };

        ImmutableList<String> keys = ImmutableList.of("OBJECT_ID");
        GenericContractLedgerValidation command = parseArgs(args);
        doThrow(IllegalArgumentException.class)
            .when(clientService)
            .validateLedger(AssetType.OBJECT, keys, 10, 0);

        // Act
        int exitCode = command.call(factory, clientService);

        // Assert
        assertThat(exitCode).isEqualTo(1);
        verify(clientService).validateLedger(AssetType.OBJECT, keys, 10, 0);
      }

      @Test
      @DisplayName("with ClientException")
      void throwsClientExceptionThenReturns1() {
        // Arrange
        String[] args =
            new String[] {
              "--properties=PROPERTIES_FILE",
              "--object-id=OBJECT_ID",
              "--start-age=0",
              "--end-age=10",
            };

        ImmutableList<String> keys = ImmutableList.of("OBJECT_ID");
        GenericContractLedgerValidation command = parseArgs(args);

        doThrow(new ClientException("", StatusCode.RUNTIME_ERROR))
            .when(clientService)
            .validateLedger(AssetType.OBJECT, keys, 0, 10);

        // Act
        int exitCode = command.call(factory, clientService);

        // Assert
        assertThat(exitCode).isEqualTo(1);
        verify(clientService).validateLedger(AssetType.OBJECT, keys, 0, 10);
      }
    }
  }

  private GenericContractLedgerValidation parseArgs(String[] args) {
    return CommandLineTestUtils.parseArgs(commandLine, GenericContractLedgerValidation.class, args);
  }
}
