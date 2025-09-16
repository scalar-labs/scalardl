package com.scalar.dl.client.tool;

import static com.scalar.dl.client.tool.CommandLineTestUtils.createDefaultClientPropertiesFile;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.GatewayClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.client.service.ClientServiceFactory;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.ledger.service.StatusCode;
import java.io.File;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

public class LedgerValidationTest {
  private CommandLine commandLine;

  @BeforeEach
  void setup() {
    commandLine = new CommandLine(new LedgerValidation());
  }

  @Nested
  @DisplayName("#call()")
  class call {
    @Nested
    @DisplayName("where age is set in --asset-id option")
    class whereAgeIsSetInAssetIdOption {
      @Test
      @DisplayName("returns 0 as exit code")
      void returns0AsExitCode() {
        // Arrange
        String[] args =
            new String[] {
              // Set the required options.
              "--properties=PROPERTIES_FILE",
              "--asset-id=ASSET_ID_1,0,1", // startAge = 0 & endAge = 1.
              "--asset-id=ASSET_ID_2,2,3", // startAge = 2 & endAge = 3.
            };
        LedgerValidation command = parseArgs(args);
        // Mock service that returns ContractExecutionResult.
        ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);

        LedgerValidationResult result1 = new LedgerValidationResult(StatusCode.OK, null, null);
        when(serviceMock.validateLedger("ASSET_ID_1", 0, 1)).thenReturn(result1);
        LedgerValidationResult result2 = new LedgerValidationResult(StatusCode.OK, null, null);
        when(serviceMock.validateLedger("ASSET_ID_2", 2, 3)).thenReturn(result2);

        // Act
        int exitCode = command.call(factoryMock, serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(0);

        verify(serviceMock).validateLedger("ASSET_ID_1", 0, 1);
        verify(serviceMock).validateLedger("ASSET_ID_2", 2, 3);
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
                "--asset-id=ASSET_ID_1",
                // Enable Gateway.
                "--use-gateway"
              };
          LedgerValidation command = parseArgs(args);
          ClientServiceFactory factory = mock(ClientServiceFactory.class);
          ClientService serviceMock = mock(ClientService.class);

          LedgerValidationResult result = new LedgerValidationResult(StatusCode.OK, null, null);
          when(serviceMock.validateLedger("ASSET_ID_1")).thenReturn(result);

          doReturn(serviceMock).when(factory).create(any(GatewayClientConfig.class));

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
                propertiesOption, "--asset-id=ASSET_ID_1",
                // Gateway is disabled by default.
              };
          LedgerValidation command = parseArgs(args);
          ClientServiceFactory factory = mock(ClientServiceFactory.class);
          ClientService serviceMock = mock(ClientService.class);

          LedgerValidationResult result = new LedgerValidationResult(StatusCode.OK, null, null);
          when(serviceMock.validateLedger("ASSET_ID_1")).thenReturn(result);

          doReturn(serviceMock).when(factory).create(any(ClientConfig.class));

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
                "--asset-id=ASSET_ID_1,0,1", // startAge = 0 & endAge = 1.
                "--asset-id=ASSET_ID_2,2,3", // startAge = 2 & endAge = 3.
              };
          LedgerValidation command = parseArgs(args);
          // Mock service that throws an exception.
          ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
          ClientService serviceMock = mock(ClientService.class);
          doThrow(new ClientException("", StatusCode.RUNTIME_ERROR))
              .when(serviceMock)
              .validateLedger("ASSET_ID_1", 0, 1);

          // Act
          int exitCode = command.call(factoryMock, serviceMock);

          // Assert
          assertThat(exitCode).isEqualTo(1);

          verify(serviceMock).validateLedger("ASSET_ID_1", 0, 1);
          verify(serviceMock, never()).validateLedger(eq("ASSET_ID_2"), anyInt(), anyInt());
        }
      }

      @Nested
      @DisplayName("when age is not an integer")
      class whenAgeIsNotInteger {
        @Test
        @DisplayName("returns 1 as exit code")
        void returns1AsExitCode() {
          // Arrange
          String[] args =
              new String[] {
                // Set the required options.
                "--properties=PROPERTIES_FILE",
                "--asset-id=ASSET_ID_1,a,1", // Set invalid start age.
                "--asset-id=ASSET_ID_2,2,3",
              };
          LedgerValidation command = parseArgs(args);
          // Mock service that throws an exception.
          ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
          ClientService serviceMock = mock(ClientService.class);

          // Act
          int exitCode = command.call(factoryMock, serviceMock);

          // Assert
          assertThat(exitCode).isEqualTo(1);

          // Verify that validate-ledger was not called.
          verify(serviceMock, never()).validateLedger(eq("ASSET_ID_1"), anyInt(), anyInt());
          verify(serviceMock, never()).validateLedger(eq("ASSET_ID_2"), anyInt(), anyInt());
        }
      }

      @Nested
      @DisplayName("when end age is missing")
      class whenEndAgeIsMissing {
        @Test
        @DisplayName("returns 1 as exit code")
        void returns1AsExitCode() {
          // Arrange
          String[] args =
              new String[] {
                // Set the required options.
                "--properties=PROPERTIES_FILE",
                "--asset-id=ASSET_ID_1,0", // startAge = 0 & no endAge.
                "--asset-id=ASSET_ID_2,2,3", // startAge = 2 & endAge = 3.
              };
          LedgerValidation command = parseArgs(args);
          // Mock service that throws an exception.
          ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
          ClientService serviceMock = mock(ClientService.class);

          // Act
          int exitCode = command.call(factoryMock, serviceMock);

          // Assert
          assertThat(exitCode).isEqualTo(1);

          // Verify validate-ledger was not called.
          verify(serviceMock, never()).validateLedger(eq("ASSET_ID_1"), anyInt(), anyInt());
          verify(serviceMock, never()).validateLedger(eq("ASSET_ID_2"), anyInt(), anyInt());
        }
      }

      @Nested
      @DisplayName("when too many args are set")
      class whenTooManyArgsAreSet {
        @Test
        @DisplayName("returns 1 as exit code")
        void returns1AsExitCode() {
          // Arrange
          String[] args =
              new String[] {
                // Set the required options.
                "--properties=PROPERTIES_FILE",
                "--asset-id=ASSET_ID_1,0,1,2", // startAge = 0 & endAge = 1 (and more...).
                "--asset-id=ASSET_ID_2,2,3", // startAge = 2 & endAge = 3.
              };
          LedgerValidation command = parseArgs(args);
          // Mock service that throws an exception.
          ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
          ClientService serviceMock = mock(ClientService.class);

          // Act
          int exitCode = command.call(factoryMock, serviceMock);

          // Assert
          assertThat(exitCode).isEqualTo(1);

          // Verify validate-ledger was not called.
          verify(serviceMock, never()).validateLedger(eq("ASSET_ID_1"), anyInt(), anyInt());
          verify(serviceMock, never()).validateLedger(eq("ASSET_ID_2"), anyInt(), anyInt());
        }
      }
    }

    @Nested
    @DisplayName("where age is NOT set in --asset-id option")
    class whereAgeIsNotSetInAssetIdOption {
      @Test
      @DisplayName("returns 0 as exit code")
      void returns0AsExitCode() {
        // Arrange
        String[] args =
            new String[] {
              // Set the required options.
              "--properties=PROPERTIES_FILE", "--asset-id=ASSET_ID_1", "--asset-id=ASSET_ID_2",
            };
        LedgerValidation command = parseArgs(args);
        // Mock service that returns ContractExecutionResult.
        ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);

        LedgerValidationResult result1 = new LedgerValidationResult(StatusCode.OK, null, null);
        when(serviceMock.validateLedger("ASSET_ID_1")).thenReturn(result1);
        LedgerValidationResult result2 = new LedgerValidationResult(StatusCode.OK, null, null);
        when(serviceMock.validateLedger("ASSET_ID_2")).thenReturn(result2);

        // Act
        int exitCode = command.call(factoryMock, serviceMock);

        // Assert
        assertThat(exitCode).isEqualTo(0);

        verify(serviceMock).validateLedger("ASSET_ID_1");
        verify(serviceMock).validateLedger("ASSET_ID_2");
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
                "--properties=PROPERTIES_FILE", "--asset-id=ASSET_ID_1", "--asset-id=ASSET_ID_2",
              };
          LedgerValidation command = parseArgs(args);
          // Mock service that throws an exception.
          ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
          ClientService serviceMock = mock(ClientService.class);
          doThrow(new ClientException("", StatusCode.RUNTIME_ERROR))
              .when(serviceMock)
              .validateLedger("ASSET_ID_1");

          // Act
          int exitCode = command.call(factoryMock, serviceMock);

          // Assert
          assertThat(exitCode).isEqualTo(1);

          verify(serviceMock).validateLedger("ASSET_ID_1");
          verify(serviceMock, never()).validateLedger("ASSET_ID_2");
        }
      }
    }
  }

  @Nested
  @DisplayName("@Option annotation")
  class OptionAnnotation {
    @Nested
    @DisplayName("--asset-id")
    class assetId {
      @Test
      @DisplayName("member values are properly set")
      void memberValuesAreProperlySet() throws Exception {
        CommandLine.Option option = getOption("assetIds");

        assertThat(option.required()).isTrue();
        assertThat(option.paramLabel()).isEqualTo("ASSET_ID");
        assertThat(option.names()).isEqualTo(new String[] {"--asset-id"});
      }
    }
  }

  private LedgerValidation parseArgs(String[] args) {
    return CommandLineTestUtils.parseArgs(commandLine, LedgerValidation.class, args);
  }

  private CommandLine.Option getOption(String fieldName) throws Exception {
    return CommandLineTestUtils.getOption(LedgerValidation.class, fieldName);
  }
}
