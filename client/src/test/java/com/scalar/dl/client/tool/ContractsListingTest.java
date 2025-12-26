package com.scalar.dl.client.tool;

import static com.scalar.dl.client.tool.CommandLineTestUtils.createDefaultClientPropertiesFile;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.GatewayClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.client.service.ClientServiceFactory;
import com.scalar.dl.ledger.service.StatusCode;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Path;
import javax.json.Json;
import javax.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;
import picocli.CommandLine.Option;

public class ContractsListingTest {
  private CommandLine commandLine;
  private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

  @BeforeEach
  void setup() throws Exception {
    commandLine = new CommandLine(new ContractsListing());

    // To verify the output to stdout, e.g., System.out.println(...).
    System.setOut(new PrintStream(outputStreamCaptor, true, UTF_8.name()));
  }

  @Nested
  @DisplayName("#call()")
  class call {
    @Test
    @DisplayName("returns 0 as exit code")
    void returns0AsExitCode() throws Exception {
      // Arrange
      String[] args =
          new String[] {
            // Set the required options.
            "--properties=PROPERTIES_FILE",
            // Set the optional options.
            "--contract-id=CONTRACT_ID",
          };
      ContractsListing command = parseArgs(args);
      // Mock service that returns ContractExecutionResult.
      ClientService serviceMock = mock(ClientService.class);
      JsonObject jsonObject =
          Json.createObjectBuilder()
              .add("contract-a", Json.createObjectBuilder())
              .add("contract-b", Json.createObjectBuilder())
              .build();
      when(serviceMock.listContracts(eq("CONTRACT_ID"))).thenReturn(jsonObject);

      // Act
      int exitCode = command.execute(serviceMock);

      // Assert
      assertThat(exitCode).isEqualTo(0);

      String stdout = outputStreamCaptor.toString(UTF_8.name()).trim();
      assertThat(stdout).contains("\"contract-a\" : { }");
      assertThat(stdout).contains("\"contract-b\" : { }");
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
              // Set the optional options.
              "--contract-id=CONTRACT_ID",
              // Enable Gateway.
              "--use-gateway"
            };
        ContractsListing command = parseArgs(args);
        ClientServiceFactory factory = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);
        doReturn(serviceMock).when(factory).create(any(GatewayClientConfig.class), eq(false));
        JsonObject jsonObject = Json.createObjectBuilder().build();
        when(serviceMock.listContracts(eq("CONTRACT_ID"))).thenReturn(jsonObject);

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
              // Set the optional options.
              "--contract-id=CONTRACT_ID",
              // Gateway is disabled by default.
            };
        ContractsListing command = parseArgs(args);
        ClientServiceFactory factory = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);
        doReturn(serviceMock).when(factory).create(any(ClientConfig.class), eq(false));
        JsonObject jsonObject = Json.createObjectBuilder().build();
        when(serviceMock.listContracts(eq("CONTRACT_ID"))).thenReturn(jsonObject);

        // Act
        command.call(factory);

        // Verify
        verify(factory).create(any(ClientConfig.class), eq(false));
        verify(factory, never()).create(any(GatewayClientConfig.class), eq(false));
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
              "--properties=" + file.getAbsolutePath(),
              // Set the optional options.
              "--contract-id=CONTRACT_ID",
            };
        ContractsListing command = parseArgs(args);
        // Mock service that throws an exception.
        ClientServiceFactory factoryMock = mock(ClientServiceFactory.class);
        ClientService serviceMock = mock(ClientService.class);
        when(factoryMock.create(any(ClientConfig.class), eq(false))).thenReturn(serviceMock);
        when(serviceMock.listContracts(eq("CONTRACT_ID")))
            .thenThrow(new ClientException("", StatusCode.RUNTIME_ERROR));

        // Act
        int exitCode = command.call(factoryMock);

        // Assert
        assertThat(exitCode).isEqualTo(1);
        verify(factoryMock).close();
      }
    }
  }

  @Nested
  @DisplayName("@Option annotation")
  class OptionAnnotation {
    @Nested
    @DisplayName("--contract-id")
    class contractId {
      @Test
      @DisplayName("member values are properly set")
      void memberValuesAreProperlySet() throws Exception {
        Option option = getOption("contractId");

        assertThat(option.required()).isFalse();
        assertThat(option.paramLabel()).isEqualTo("CONTRACT_ID");
        assertThat(option.names()).isEqualTo(new String[] {"--contract-id"});
      }
    }
  }

  private ContractsListing parseArgs(String[] args) {
    return CommandLineTestUtils.parseArgs(commandLine, ContractsListing.class, args);
  }

  private Option getOption(String fieldName) throws Exception {
    return CommandLineTestUtils.getOption(ContractsListing.class, fieldName);
  }
}
