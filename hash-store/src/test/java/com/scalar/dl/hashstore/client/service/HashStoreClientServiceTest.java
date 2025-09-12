package com.scalar.dl.hashstore.client.service;

import static com.scalar.dl.genericcontracts.collection.Constants.COLLECTION_ID;
import static com.scalar.dl.genericcontracts.collection.Constants.COLLECTION_ID_PREFIX;
import static com.scalar.dl.genericcontracts.collection.Constants.CONTRACT_ADD;
import static com.scalar.dl.genericcontracts.collection.Constants.CONTRACT_CREATE;
import static com.scalar.dl.genericcontracts.collection.Constants.CONTRACT_GET_CHECKPOINT_INTERVAL;
import static com.scalar.dl.genericcontracts.collection.Constants.CONTRACT_GET_HISTORY;
import static com.scalar.dl.genericcontracts.collection.Constants.CONTRACT_REMOVE;
import static com.scalar.dl.genericcontracts.collection.Constants.OBJECT_IDS;
import static com.scalar.dl.genericcontracts.collection.Constants.OPTION_FORCE;
import static com.scalar.dl.genericcontracts.collection.Constants.OPTION_LIMIT;
import static com.scalar.dl.genericcontracts.object.Constants.CLUSTERING_KEY;
import static com.scalar.dl.genericcontracts.object.Constants.COLUMNS;
import static com.scalar.dl.genericcontracts.object.Constants.COLUMN_NAME;
import static com.scalar.dl.genericcontracts.object.Constants.CONTRACT_PUT;
import static com.scalar.dl.genericcontracts.object.Constants.CONTRACT_VALIDATE;
import static com.scalar.dl.genericcontracts.object.Constants.DATA_TYPE;
import static com.scalar.dl.genericcontracts.object.Constants.DATE_FORMATTER;
import static com.scalar.dl.genericcontracts.object.Constants.FUNCTION_PUT;
import static com.scalar.dl.genericcontracts.object.Constants.HASH_VALUE;
import static com.scalar.dl.genericcontracts.object.Constants.METADATA;
import static com.scalar.dl.genericcontracts.object.Constants.NAMESPACE;
import static com.scalar.dl.genericcontracts.object.Constants.OBJECT_ID;
import static com.scalar.dl.genericcontracts.object.Constants.OBJECT_ID_PREFIX;
import static com.scalar.dl.genericcontracts.object.Constants.OPTION_ALL;
import static com.scalar.dl.genericcontracts.object.Constants.OPTION_VERBOSE;
import static com.scalar.dl.genericcontracts.object.Constants.PARTITION_KEY;
import static com.scalar.dl.genericcontracts.object.Constants.TABLE;
import static com.scalar.dl.genericcontracts.object.Constants.VALUE;
import static com.scalar.dl.genericcontracts.object.Constants.VERSIONS;
import static com.scalar.dl.genericcontracts.object.Constants.VERSION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.scalar.db.api.Put;
import com.scalar.db.io.Key;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.hashstore.client.error.HashStoreClientError;
import com.scalar.dl.hashstore.client.model.Version;
import com.scalar.dl.ledger.config.AuthenticationMethod;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.scalar.dl.ledger.model.ExecutionResult;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.ledger.service.StatusCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import javax.json.Json;
import javax.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class HashStoreClientServiceTest {
  private static final String ANY_OBJECT_ID = "object1";
  private static final String ANY_HASH = "hash1";
  private static final String ANY_COLLECTION_ID = "collection1";
  private static final String ANY_VERSION_ID = "v1";
  private static final String ANY_NAMESPACE = "namespace1";
  private static final String ANY_TABLE = "table1";
  private static final int ANY_START_AGE = 0;
  private static final int ANY_END_AGE = 5;
  private static final int ANY_LIMIT = 10;
  private static final String CONTRACT_OBJECT_GET =
      com.scalar.dl.genericcontracts.object.Constants.CONTRACT_GET;
  private static final String CONTRACT_COLLECTION_GET =
      com.scalar.dl.genericcontracts.collection.Constants.CONTRACT_GET;
  private static final String OBJECT_OPTIONS =
      com.scalar.dl.genericcontracts.object.Constants.OPTIONS;
  private static final String COLLECTION_OPTIONS =
      com.scalar.dl.genericcontracts.collection.Constants.OPTIONS;

  @Mock private com.scalar.dl.client.service.ClientService clientService;
  @Mock private ClientConfig config;
  @Mock private ContractExecutionResult contractExecutionResult;
  @Mock private LedgerValidationResult ledgerValidationResult;

  private HashStoreClientService service;
  private final ObjectMapper mapper = new ObjectMapper();

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    service = new HashStoreClientService(clientService, config);
  }

  @Test
  public void registerIdentity_DigitalSignature_ShouldCallRegisterCertificateAndContracts() {
    // Arrange
    when(config.getAuthenticationMethod()).thenReturn(AuthenticationMethod.DIGITAL_SIGNATURE);

    // Act
    service.registerIdentity();

    // Assert
    verify(clientService).registerCertificate();
    verify(clientService, never()).registerSecret();
    // Verify contracts are registered
    verify(clientService)
        .registerContract(
            eq(CONTRACT_OBJECT_GET), anyString(), any(byte[].class), eq((String) null));
    verify(clientService)
        .registerContract(eq(CONTRACT_PUT), anyString(), any(byte[].class), eq((String) null));
    verify(clientService)
        .registerContract(eq(CONTRACT_VALIDATE), anyString(), any(byte[].class), eq((String) null));
    verify(clientService)
        .registerContract(eq(CONTRACT_ADD), anyString(), any(byte[].class), eq((String) null));
    verify(clientService)
        .registerContract(eq(CONTRACT_CREATE), anyString(), any(byte[].class), eq((String) null));
    verify(clientService)
        .registerContract(
            eq(CONTRACT_COLLECTION_GET), anyString(), any(byte[].class), eq((String) null));
    verify(clientService)
        .registerContract(
            eq(CONTRACT_GET_CHECKPOINT_INTERVAL),
            anyString(),
            any(byte[].class),
            eq((String) null));
    verify(clientService)
        .registerContract(
            eq(CONTRACT_GET_HISTORY), anyString(), any(byte[].class), eq((String) null));
    verify(clientService)
        .registerContract(eq(CONTRACT_REMOVE), anyString(), any(byte[].class), eq((String) null));
    // Verify function is registered
    verify(clientService).registerFunction(eq(FUNCTION_PUT), anyString(), any(byte[].class));
  }

  @Test
  public void registerIdentity_HmacSignature_ShouldCallRegisterSecretAndContracts() {
    // Arrange
    when(config.getAuthenticationMethod()).thenReturn(AuthenticationMethod.HMAC);

    // Act
    service.registerIdentity();

    // Assert
    verify(clientService, never()).registerCertificate();
    verify(clientService).registerSecret();
    // Verify contracts are registered
    verify(clientService, times(9))
        .registerContract(anyString(), anyString(), any(byte[].class), eq((String) null));
    // Verify function is registered
    verify(clientService).registerFunction(eq(FUNCTION_PUT), anyString(), any(byte[].class));
  }

  @Test
  public void registerIdentity_CertificateAlreadyRegistered_ShouldContinueWithContracts() {
    // Arrange
    when(config.getAuthenticationMethod()).thenReturn(AuthenticationMethod.DIGITAL_SIGNATURE);
    ClientException exception =
        new ClientException("Already registered", StatusCode.CERTIFICATE_ALREADY_REGISTERED);
    doThrow(exception).when(clientService).registerCertificate();

    // Act
    service.registerIdentity();

    // Assert
    verify(clientService).registerCertificate();
    // Should still register contracts
    verify(clientService, times(9))
        .registerContract(anyString(), anyString(), any(byte[].class), eq((String) null));
    verify(clientService).registerFunction(eq(FUNCTION_PUT), anyString(), any(byte[].class));
  }

  @Test
  public void registerIdentity_SecretAlreadyRegistered_ShouldContinueWithContracts() {
    // Arrange
    when(config.getAuthenticationMethod()).thenReturn(AuthenticationMethod.HMAC);
    ClientException exception =
        new ClientException("Already registered", StatusCode.SECRET_ALREADY_REGISTERED);
    doThrow(exception).when(clientService).registerSecret();

    // Act
    service.registerIdentity();

    // Assert
    verify(clientService).registerSecret();
    // Should still register contracts
    verify(clientService, times(9))
        .registerContract(anyString(), anyString(), any(byte[].class), eq((String) null));
    verify(clientService).registerFunction(eq(FUNCTION_PUT), anyString(), any(byte[].class));
  }

  @Test
  public void registerIdentity_ContractAlreadyRegistered_ShouldContinueWithOtherContracts() {
    // Arrange
    when(config.getAuthenticationMethod()).thenReturn(AuthenticationMethod.DIGITAL_SIGNATURE);
    ClientException exception =
        new ClientException("Already registered", StatusCode.CONTRACT_ALREADY_REGISTERED);
    doThrow(exception)
        .when(clientService)
        .registerContract(eq(CONTRACT_CREATE), anyString(), any(byte[].class), eq((String) null));

    // Act
    service.registerIdentity();

    // Assert
    verify(clientService).registerCertificate();
    // Should still register all contracts
    verify(clientService, times(9))
        .registerContract(anyString(), anyString(), any(byte[].class), eq((String) null));
    verify(clientService).registerFunction(eq(FUNCTION_PUT), anyString(), any(byte[].class));
  }

  @Test
  public void getObject_ShouldCallExecuteContractWithCorrectArguments() {
    // Arrange
    when(clientService.executeContract(anyString(), any(JsonNode.class)))
        .thenReturn(contractExecutionResult);

    // Act
    ExecutionResult result = service.getObject(ANY_OBJECT_ID);

    // Assert
    ArgumentCaptor<JsonNode> argumentCaptor = ArgumentCaptor.forClass(JsonNode.class);
    verify(clientService).executeContract(eq(CONTRACT_OBJECT_GET), argumentCaptor.capture());

    JsonNode capturedArgument = argumentCaptor.getValue();
    assertThat(capturedArgument.get(OBJECT_ID).asText()).isEqualTo(ANY_OBJECT_ID);
    assertThat(result).isNotNull();
  }

  @Test
  public void putObject_WithHashOnly_ShouldCallExecuteContractWithCorrectArguments() {
    // Arrange
    when(clientService.executeContract(anyString(), any(JsonNode.class)))
        .thenReturn(contractExecutionResult);

    // Act
    ExecutionResult result = service.putObject(ANY_OBJECT_ID, ANY_HASH);

    // Assert
    ArgumentCaptor<JsonNode> argumentCaptor = ArgumentCaptor.forClass(JsonNode.class);
    verify(clientService).executeContract(eq(CONTRACT_PUT), argumentCaptor.capture());

    JsonNode capturedArgument = argumentCaptor.getValue();
    assertThat(capturedArgument.get(OBJECT_ID).asText()).isEqualTo(ANY_OBJECT_ID);
    assertThat(capturedArgument.get(HASH_VALUE).asText()).isEqualTo(ANY_HASH);
    assertThat(capturedArgument.has(METADATA)).isFalse();
    assertThat(result).isNotNull();
  }

  @Test
  public void putObject_WithJsonObjectMetadata_ShouldCallExecuteContractWithCorrectArguments() {
    // Arrange
    JsonObject metadata = Json.createObjectBuilder().add("key", "value").build();
    when(clientService.executeContract(anyString(), any(JsonNode.class)))
        .thenReturn(contractExecutionResult);

    // Act
    ExecutionResult result = service.putObject(ANY_OBJECT_ID, ANY_HASH, metadata);

    // Assert
    ArgumentCaptor<JsonNode> argumentCaptor = ArgumentCaptor.forClass(JsonNode.class);
    verify(clientService).executeContract(eq(CONTRACT_PUT), argumentCaptor.capture());

    JsonNode capturedArgument = argumentCaptor.getValue();
    assertThat(capturedArgument.get(OBJECT_ID).asText()).isEqualTo(ANY_OBJECT_ID);
    assertThat(capturedArgument.get(HASH_VALUE).asText()).isEqualTo(ANY_HASH);
    assertThat(capturedArgument.has(METADATA)).isTrue();
    assertThat(capturedArgument.get(METADATA).get("key").asText()).isEqualTo("value");
    assertThat(result).isNotNull();
  }

  @Test
  public void putObject_WithJsonNodeMetadata_ShouldCallExecuteContractWithCorrectArguments() {
    // Arrange
    ObjectNode metadata = mapper.createObjectNode().put("key", "value");
    when(clientService.executeContract(anyString(), any(JsonNode.class)))
        .thenReturn(contractExecutionResult);

    // Act
    ExecutionResult result = service.putObject(ANY_OBJECT_ID, ANY_HASH, metadata);

    // Assert
    ArgumentCaptor<JsonNode> argumentCaptor = ArgumentCaptor.forClass(JsonNode.class);
    verify(clientService).executeContract(eq(CONTRACT_PUT), argumentCaptor.capture());

    JsonNode capturedArgument = argumentCaptor.getValue();
    assertThat(capturedArgument.get(OBJECT_ID).asText()).isEqualTo(ANY_OBJECT_ID);
    assertThat(capturedArgument.get(HASH_VALUE).asText()).isEqualTo(ANY_HASH);
    assertThat(capturedArgument.has(METADATA)).isTrue();
    assertThat(capturedArgument.get(METADATA).get("key").asText()).isEqualTo("value");
    assertThat(result).isNotNull();
  }

  @Test
  public void putObject_WithStringMetadata_ShouldCallExecuteContractWithCorrectArguments() {
    // Arrange
    String metadata = "{\"key\":\"value\"}";
    when(clientService.executeContract(anyString(), any(JsonNode.class)))
        .thenReturn(contractExecutionResult);

    // Act
    ExecutionResult result = service.putObject(ANY_OBJECT_ID, ANY_HASH, metadata);

    // Assert
    ArgumentCaptor<JsonNode> argumentCaptor = ArgumentCaptor.forClass(JsonNode.class);
    verify(clientService).executeContract(eq(CONTRACT_PUT), argumentCaptor.capture());

    JsonNode capturedArgument = argumentCaptor.getValue();
    assertThat(capturedArgument.get(OBJECT_ID).asText()).isEqualTo(ANY_OBJECT_ID);
    assertThat(capturedArgument.get(HASH_VALUE).asText()).isEqualTo(ANY_HASH);
    assertThat(capturedArgument.has(METADATA)).isTrue();
    assertThat(capturedArgument.get(METADATA).get("key").asText()).isEqualTo("value");
    assertThat(result).isNotNull();
  }

  @Test
  public void putObject_WithPut_ShouldCallExecuteContractWithCorrectArguments() {
    // Arrange
    Put put =
        Put.newBuilder()
            .namespace(ANY_NAMESPACE)
            .table(ANY_TABLE)
            .partitionKey(Key.ofText("pk1", "value1"))
            .clusteringKey(Key.ofText("ck1", "value2"))
            .textValue("col_text", "text_value")
            .intValue("col_int", 123)
            .doubleValue("col_double", 456.789)
            .timestampValue("col_timestamp", LocalDateTime.of(2021, 10, 15, 12, 30, 45))
            .build();

    when(clientService.executeContract(
            anyString(), any(JsonNode.class), anyString(), any(JsonNode.class)))
        .thenReturn(contractExecutionResult);

    // Act
    ExecutionResult result = service.putObject(ANY_OBJECT_ID, ANY_HASH, put);

    // Assert
    ArgumentCaptor<JsonNode> contractArgCaptor = ArgumentCaptor.forClass(JsonNode.class);
    ArgumentCaptor<JsonNode> functionArgCaptor = ArgumentCaptor.forClass(JsonNode.class);
    verify(clientService)
        .executeContract(
            eq(CONTRACT_PUT),
            contractArgCaptor.capture(),
            eq(FUNCTION_PUT),
            functionArgCaptor.capture());

    // Check contract arguments
    JsonNode contractArg = contractArgCaptor.getValue();
    assertThat(contractArg.get(OBJECT_ID).asText()).isEqualTo(ANY_OBJECT_ID);
    assertThat(contractArg.get(HASH_VALUE).asText()).isEqualTo(ANY_HASH);

    // Check function arguments
    JsonNode functionArg = functionArgCaptor.getValue();
    assertThat(functionArg.get(NAMESPACE).asText()).isEqualTo(ANY_NAMESPACE);
    assertThat(functionArg.get(TABLE).asText()).isEqualTo(ANY_TABLE);

    // Check partition key
    ArrayNode partitionKey = (ArrayNode) functionArg.get(PARTITION_KEY);
    assertThat(partitionKey.size()).isEqualTo(1);
    assertThat(partitionKey.get(0).get(COLUMN_NAME).asText()).isEqualTo("pk1");
    assertThat(partitionKey.get(0).get(DATA_TYPE).asText()).isEqualTo("TEXT");
    assertThat(partitionKey.get(0).get(VALUE).asText()).isEqualTo("value1");

    // Check clustering key
    ArrayNode clusteringKey = (ArrayNode) functionArg.get(CLUSTERING_KEY);
    assertThat(clusteringKey.size()).isEqualTo(1);
    assertThat(clusteringKey.get(0).get(COLUMN_NAME).asText()).isEqualTo("ck1");
    assertThat(clusteringKey.get(0).get(DATA_TYPE).asText()).isEqualTo("TEXT");
    assertThat(clusteringKey.get(0).get(VALUE).asText()).isEqualTo("value2");

    // Check columns
    ArrayNode columns = (ArrayNode) functionArg.get(COLUMNS);
    assertThat(columns.size()).isEqualTo(4);

    // Verify different data types in columns
    boolean hasText = false, hasInt = false, hasDouble = false, hasTimestamp = false;
    for (JsonNode column : columns) {
      String columnName = column.get(COLUMN_NAME).asText();
      String dataType = column.get(DATA_TYPE).asText();
      if ("col_text".equals(columnName)) {
        assertThat(dataType).isEqualTo("TEXT");
        assertThat(column.get(VALUE).asText()).isEqualTo("text_value");
        hasText = true;
      } else if ("col_int".equals(columnName)) {
        assertThat(dataType).isEqualTo("INT");
        assertThat(column.get(VALUE).asInt()).isEqualTo(123);
        hasInt = true;
      } else if ("col_double".equals(columnName)) {
        assertThat(dataType).isEqualTo("DOUBLE");
        assertThat(column.get(VALUE).asDouble()).isEqualTo(456.789);
        hasDouble = true;
      } else if ("col_timestamp".equals(columnName)) {
        assertThat(dataType).isEqualTo("TIMESTAMP");
        assertThat(column.get(VALUE).asText()).isEqualTo("2021-10-15 12:30:45");
        hasTimestamp = true;
      }
    }
    assertThat(hasText && hasInt && hasDouble && hasTimestamp).isTrue();

    assertThat(result).isNotNull();
  }

  @Test
  public void putObject_WithPutAndDateTypes_ShouldHandleDateCorrectly() {
    // Arrange
    LocalDate date = LocalDate.of(2021, 10, 15);
    Put put =
        Put.newBuilder()
            .namespace(ANY_NAMESPACE)
            .table(ANY_TABLE)
            .partitionKey(Key.ofText("pk1", "value1"))
            .dateValue("col_date", date)
            .build();

    when(clientService.executeContract(
            anyString(), any(JsonNode.class), anyString(), any(JsonNode.class)))
        .thenReturn(contractExecutionResult);

    // Act
    ExecutionResult result = service.putObject(ANY_OBJECT_ID, ANY_HASH, put);

    // Assert
    ArgumentCaptor<JsonNode> functionArgCaptor = ArgumentCaptor.forClass(JsonNode.class);
    verify(clientService)
        .executeContract(
            eq(CONTRACT_PUT), any(JsonNode.class), eq(FUNCTION_PUT), functionArgCaptor.capture());

    JsonNode functionArg = functionArgCaptor.getValue();
    ArrayNode columns = (ArrayNode) functionArg.get(COLUMNS);
    assertThat(columns.size()).isEqualTo(1);
    assertThat(columns.get(0).get(COLUMN_NAME).asText()).isEqualTo("col_date");
    assertThat(columns.get(0).get(DATA_TYPE).asText()).isEqualTo("DATE");
    assertThat(columns.get(0).get(VALUE).asText()).isEqualTo(date.format(DATE_FORMATTER));

    assertThat(result).isNotNull();
  }

  @Test
  public void putObject_WithPutAndMetadata_ShouldCallExecuteContractWithCorrectArguments() {
    // Arrange
    JsonObject metadata = Json.createObjectBuilder().add("key", "value").build();
    Put put =
        Put.newBuilder()
            .namespace(ANY_NAMESPACE)
            .table(ANY_TABLE)
            .partitionKey(Key.ofText("pk1", "value1"))
            .build();

    when(clientService.executeContract(
            anyString(), any(JsonNode.class), anyString(), any(JsonNode.class)))
        .thenReturn(contractExecutionResult);

    // Act
    ExecutionResult result = service.putObject(ANY_OBJECT_ID, ANY_HASH, metadata, put);

    // Assert
    ArgumentCaptor<JsonNode> contractArgCaptor = ArgumentCaptor.forClass(JsonNode.class);
    ArgumentCaptor<JsonNode> functionArgCaptor = ArgumentCaptor.forClass(JsonNode.class);
    verify(clientService)
        .executeContract(
            eq(CONTRACT_PUT),
            contractArgCaptor.capture(),
            eq(FUNCTION_PUT),
            functionArgCaptor.capture());

    // Check contract arguments include metadata
    JsonNode contractArg = contractArgCaptor.getValue();
    assertThat(contractArg.get(OBJECT_ID).asText()).isEqualTo(ANY_OBJECT_ID);
    assertThat(contractArg.get(HASH_VALUE).asText()).isEqualTo(ANY_HASH);
    assertThat(contractArg.has(METADATA)).isTrue();
    assertThat(contractArg.get(METADATA).get("key").asText()).isEqualTo("value");

    assertThat(result).isNotNull();
  }

  @Test
  public void putObject_WithJsonNodeMetadataAndPut_ShouldCallExecuteContractWithCorrectArguments() {
    // Arrange
    ObjectNode metadata = mapper.createObjectNode().put("key", "value");
    Put put =
        Put.newBuilder()
            .namespace(ANY_NAMESPACE)
            .table(ANY_TABLE)
            .partitionKey(Key.ofText("pk1", "value1"))
            .build();

    when(clientService.executeContract(
            anyString(), any(JsonNode.class), anyString(), any(JsonNode.class)))
        .thenReturn(contractExecutionResult);

    // Act
    ExecutionResult result = service.putObject(ANY_OBJECT_ID, ANY_HASH, metadata, put);

    // Assert
    ArgumentCaptor<JsonNode> contractArgCaptor = ArgumentCaptor.forClass(JsonNode.class);
    ArgumentCaptor<JsonNode> functionArgCaptor = ArgumentCaptor.forClass(JsonNode.class);
    verify(clientService)
        .executeContract(
            eq(CONTRACT_PUT),
            contractArgCaptor.capture(),
            eq(FUNCTION_PUT),
            functionArgCaptor.capture());

    // Check contract arguments include metadata
    JsonNode contractArg = contractArgCaptor.getValue();
    assertThat(contractArg.get(OBJECT_ID).asText()).isEqualTo(ANY_OBJECT_ID);
    assertThat(contractArg.get(HASH_VALUE).asText()).isEqualTo(ANY_HASH);
    assertThat(contractArg.has(METADATA)).isTrue();
    assertThat(contractArg.get(METADATA).get("key").asText()).isEqualTo("value");

    assertThat(result).isNotNull();
  }

  @Test
  public void putObject_WithStringMetadataAndPut_ShouldCallExecuteContractWithCorrectArguments() {
    // Arrange
    String metadata = "{\"key\":\"value\"}";
    Put put =
        Put.newBuilder()
            .namespace(ANY_NAMESPACE)
            .table(ANY_TABLE)
            .partitionKey(Key.ofText("pk1", "value1"))
            .build();

    when(clientService.executeContract(
            anyString(), any(JsonNode.class), anyString(), any(JsonNode.class)))
        .thenReturn(contractExecutionResult);

    // Act
    ExecutionResult result = service.putObject(ANY_OBJECT_ID, ANY_HASH, metadata, put);

    // Assert
    ArgumentCaptor<JsonNode> contractArgCaptor = ArgumentCaptor.forClass(JsonNode.class);
    ArgumentCaptor<JsonNode> functionArgCaptor = ArgumentCaptor.forClass(JsonNode.class);
    verify(clientService)
        .executeContract(
            eq(CONTRACT_PUT),
            contractArgCaptor.capture(),
            eq(FUNCTION_PUT),
            functionArgCaptor.capture());

    // Check contract arguments include metadata
    JsonNode contractArg = contractArgCaptor.getValue();
    assertThat(contractArg.get(OBJECT_ID).asText()).isEqualTo(ANY_OBJECT_ID);
    assertThat(contractArg.get(HASH_VALUE).asText()).isEqualTo(ANY_HASH);
    assertThat(contractArg.has(METADATA)).isTrue();
    assertThat(contractArg.get(METADATA).get("key").asText()).isEqualTo("value");

    assertThat(result).isNotNull();
  }

  @Test
  public void putObject_WithPutWithoutNamespaceOrTable_ShouldThrowIllegalArgumentException() {
    // Arrange
    Put putWithoutNamespace =
        Put.newBuilder().table(ANY_TABLE).partitionKey(Key.ofText("pk1", "value1")).build();

    // Act
    Throwable thrown =
        catchThrowable(() -> service.putObject(ANY_OBJECT_ID, ANY_HASH, putWithoutNamespace));

    // Assert
    assertThat(thrown).isExactlyInstanceOf(IllegalArgumentException.class);
    assertThat(thrown.getMessage())
        .contains(HashStoreClientError.PUT_MUST_HAVE_NAMESPACE_AND_TABLE.getId());
  }

  @Test
  public void compareObjectVersions_ShouldCallExecuteContractWithCorrectArguments() {
    // Arrange
    List<Version> versions =
        ImmutableList.of(Version.of(ANY_VERSION_ID, ANY_HASH), Version.of("v2", "hash2"));
    when(clientService.executeContract(anyString(), any(JsonNode.class)))
        .thenReturn(contractExecutionResult);

    // Act
    ExecutionResult result = service.compareObjectVersions(ANY_OBJECT_ID, versions);

    // Assert
    ArgumentCaptor<JsonNode> argumentCaptor = ArgumentCaptor.forClass(JsonNode.class);
    verify(clientService).executeContract(eq(CONTRACT_VALIDATE), argumentCaptor.capture());

    JsonNode capturedArgument = argumentCaptor.getValue();
    assertThat(capturedArgument.get(OBJECT_ID).asText()).isEqualTo(ANY_OBJECT_ID);
    ArrayNode versionsArray = (ArrayNode) capturedArgument.get(VERSIONS);
    assertThat(versionsArray.size()).isEqualTo(2);
    assertThat(versionsArray.get(0).get(VERSION_ID).asText()).isEqualTo(ANY_VERSION_ID);
    assertThat(versionsArray.get(0).get(HASH_VALUE).asText()).isEqualTo(ANY_HASH);
    assertThat(versionsArray.get(1).get(VERSION_ID).asText()).isEqualTo("v2");
    assertThat(versionsArray.get(1).get(HASH_VALUE).asText()).isEqualTo("hash2");
    assertThat(result).isNotNull();
  }

  @Test
  public void
      compareObjectVersions_WithVerboseOption_ShouldCallExecuteContractWithCorrectArguments() {
    // Arrange
    List<Version> versions = ImmutableList.of(Version.of(ANY_VERSION_ID, ANY_HASH));
    when(clientService.executeContract(anyString(), any(JsonNode.class)))
        .thenReturn(contractExecutionResult);

    // Act
    ExecutionResult result = service.compareObjectVersions(ANY_OBJECT_ID, versions, true);

    // Assert
    ArgumentCaptor<JsonNode> argumentCaptor = ArgumentCaptor.forClass(JsonNode.class);
    verify(clientService).executeContract(eq(CONTRACT_VALIDATE), argumentCaptor.capture());

    JsonNode capturedArgument = argumentCaptor.getValue();
    assertThat(capturedArgument.get(OBJECT_ID).asText()).isEqualTo(ANY_OBJECT_ID);
    assertThat(capturedArgument.has(OBJECT_OPTIONS)).isTrue();
    assertThat(capturedArgument.get(OBJECT_OPTIONS).get(OPTION_VERBOSE).asBoolean()).isTrue();
    assertThat(result).isNotNull();
  }

  @Test
  public void compareAllObjectVersions_ShouldCallExecuteContractWithCorrectArguments() {
    // Arrange
    List<Version> versions = ImmutableList.of(Version.of(ANY_VERSION_ID, ANY_HASH));
    when(clientService.executeContract(anyString(), any(JsonNode.class)))
        .thenReturn(contractExecutionResult);

    // Act
    ExecutionResult result = service.compareAllObjectVersions(ANY_OBJECT_ID, versions);

    // Assert
    ArgumentCaptor<JsonNode> argumentCaptor = ArgumentCaptor.forClass(JsonNode.class);
    verify(clientService).executeContract(eq(CONTRACT_VALIDATE), argumentCaptor.capture());

    JsonNode capturedArgument = argumentCaptor.getValue();
    assertThat(capturedArgument.get(OBJECT_ID).asText()).isEqualTo(ANY_OBJECT_ID);
    assertThat(capturedArgument.has(OBJECT_OPTIONS)).isTrue();
    assertThat(capturedArgument.get(OBJECT_OPTIONS).get(OPTION_ALL).asBoolean()).isTrue();
    assertThat(result).isNotNull();
  }

  @Test
  public void
      compareAllObjectVersions_WithVerboseOption_ShouldCallExecuteContractWithCorrectArguments() {
    // Arrange
    List<Version> versions = ImmutableList.of(Version.of(ANY_VERSION_ID, ANY_HASH));
    when(clientService.executeContract(anyString(), any(JsonNode.class)))
        .thenReturn(contractExecutionResult);

    // Act
    ExecutionResult result = service.compareAllObjectVersions(ANY_OBJECT_ID, versions, true);

    // Assert
    ArgumentCaptor<JsonNode> argumentCaptor = ArgumentCaptor.forClass(JsonNode.class);
    verify(clientService).executeContract(eq(CONTRACT_VALIDATE), argumentCaptor.capture());

    JsonNode capturedArgument = argumentCaptor.getValue();
    assertThat(capturedArgument.get(OBJECT_ID).asText()).isEqualTo(ANY_OBJECT_ID);
    assertThat(capturedArgument.has(OBJECT_OPTIONS)).isTrue();
    assertThat(capturedArgument.get(OBJECT_OPTIONS).get(OPTION_ALL).asBoolean()).isTrue();
    assertThat(capturedArgument.get(OBJECT_OPTIONS).get(OPTION_VERBOSE).asBoolean()).isTrue();
    assertThat(result).isNotNull();
  }

  @Test
  public void createCollection_ShouldCallExecuteContractWithCorrectArguments() {
    // Arrange
    List<String> objectIds = ImmutableList.of("obj1", "obj2", "obj3");
    when(clientService.executeContract(anyString(), any(JsonNode.class)))
        .thenReturn(contractExecutionResult);

    // Act
    ExecutionResult result = service.createCollection(ANY_COLLECTION_ID, objectIds);

    // Assert
    ArgumentCaptor<JsonNode> argumentCaptor = ArgumentCaptor.forClass(JsonNode.class);
    verify(clientService).executeContract(eq(CONTRACT_CREATE), argumentCaptor.capture());

    JsonNode capturedArgument = argumentCaptor.getValue();
    assertThat(capturedArgument.get(COLLECTION_ID).asText()).isEqualTo(ANY_COLLECTION_ID);
    ArrayNode objectIdsArray = (ArrayNode) capturedArgument.get(OBJECT_IDS);
    assertThat(objectIdsArray.size()).isEqualTo(3);
    assertThat(objectIdsArray.get(0).asText()).isEqualTo("obj1");
    assertThat(objectIdsArray.get(1).asText()).isEqualTo("obj2");
    assertThat(objectIdsArray.get(2).asText()).isEqualTo("obj3");
    assertThat(result).isNotNull();
  }

  @Test
  public void getCollection_ShouldCallExecuteContractWithCorrectArguments() {
    // Arrange
    when(clientService.executeContract(anyString(), any(JsonNode.class)))
        .thenReturn(contractExecutionResult);

    // Act
    ExecutionResult result = service.getCollection(ANY_COLLECTION_ID);

    // Assert
    ArgumentCaptor<JsonNode> argumentCaptor = ArgumentCaptor.forClass(JsonNode.class);
    verify(clientService).executeContract(eq(CONTRACT_COLLECTION_GET), argumentCaptor.capture());

    JsonNode capturedArgument = argumentCaptor.getValue();
    assertThat(capturedArgument.get(COLLECTION_ID).asText()).isEqualTo(ANY_COLLECTION_ID);
    assertThat(result).isNotNull();
  }

  @Test
  public void addToCollection_ShouldCallExecuteContractWithCorrectArguments() {
    // Arrange
    List<String> objectIds = ImmutableList.of("obj1", "obj2");
    when(clientService.executeContract(anyString(), any(JsonNode.class)))
        .thenReturn(contractExecutionResult);

    // Act
    ExecutionResult result = service.addToCollection(ANY_COLLECTION_ID, objectIds);

    // Assert
    ArgumentCaptor<JsonNode> argumentCaptor = ArgumentCaptor.forClass(JsonNode.class);
    verify(clientService).executeContract(eq(CONTRACT_ADD), argumentCaptor.capture());

    JsonNode capturedArgument = argumentCaptor.getValue();
    assertThat(capturedArgument.get(COLLECTION_ID).asText()).isEqualTo(ANY_COLLECTION_ID);
    ArrayNode objectIdsArray = (ArrayNode) capturedArgument.get(OBJECT_IDS);
    assertThat(objectIdsArray.size()).isEqualTo(2);
    assertThat(objectIdsArray.get(0).asText()).isEqualTo("obj1");
    assertThat(objectIdsArray.get(1).asText()).isEqualTo("obj2");
    assertThat(result).isNotNull();
  }

  @Test
  public void addToCollection_WithForceOption_ShouldCallExecuteContractWithCorrectArguments() {
    // Arrange
    List<String> objectIds = ImmutableList.of("obj1");
    when(clientService.executeContract(anyString(), any(JsonNode.class)))
        .thenReturn(contractExecutionResult);

    // Act
    ExecutionResult result = service.addToCollection(ANY_COLLECTION_ID, objectIds, true);

    // Assert
    ArgumentCaptor<JsonNode> argumentCaptor = ArgumentCaptor.forClass(JsonNode.class);
    verify(clientService).executeContract(eq(CONTRACT_ADD), argumentCaptor.capture());

    JsonNode capturedArgument = argumentCaptor.getValue();
    assertThat(capturedArgument.get(COLLECTION_ID).asText()).isEqualTo(ANY_COLLECTION_ID);
    assertThat(capturedArgument.has(COLLECTION_OPTIONS)).isTrue();
    assertThat(capturedArgument.get(COLLECTION_OPTIONS).get(OPTION_FORCE).asBoolean()).isTrue();
    assertThat(result).isNotNull();
  }

  @Test
  public void removeFromCollection_ShouldCallExecuteContractWithCorrectArguments() {
    // Arrange
    List<String> objectIds = ImmutableList.of("obj1", "obj2");
    when(clientService.executeContract(anyString(), any(JsonNode.class)))
        .thenReturn(contractExecutionResult);

    // Act
    ExecutionResult result = service.removeFromCollection(ANY_COLLECTION_ID, objectIds);

    // Assert
    ArgumentCaptor<JsonNode> argumentCaptor = ArgumentCaptor.forClass(JsonNode.class);
    verify(clientService).executeContract(eq(CONTRACT_REMOVE), argumentCaptor.capture());

    JsonNode capturedArgument = argumentCaptor.getValue();
    assertThat(capturedArgument.get(COLLECTION_ID).asText()).isEqualTo(ANY_COLLECTION_ID);
    ArrayNode objectIdsArray = (ArrayNode) capturedArgument.get(OBJECT_IDS);
    assertThat(objectIdsArray.size()).isEqualTo(2);
    assertThat(objectIdsArray.get(0).asText()).isEqualTo("obj1");
    assertThat(objectIdsArray.get(1).asText()).isEqualTo("obj2");
    assertThat(result).isNotNull();
  }

  @Test
  public void removeFromCollection_WithForceOption_ShouldCallExecuteContractWithCorrectArguments() {
    // Arrange
    List<String> objectIds = ImmutableList.of("obj1");
    when(clientService.executeContract(anyString(), any(JsonNode.class)))
        .thenReturn(contractExecutionResult);

    // Act
    ExecutionResult result = service.removeFromCollection(ANY_COLLECTION_ID, objectIds, true);

    // Assert
    ArgumentCaptor<JsonNode> argumentCaptor = ArgumentCaptor.forClass(JsonNode.class);
    verify(clientService).executeContract(eq(CONTRACT_REMOVE), argumentCaptor.capture());

    JsonNode capturedArgument = argumentCaptor.getValue();
    assertThat(capturedArgument.get(COLLECTION_ID).asText()).isEqualTo(ANY_COLLECTION_ID);
    assertThat(capturedArgument.has(COLLECTION_OPTIONS)).isTrue();
    assertThat(capturedArgument.get(COLLECTION_OPTIONS).get(OPTION_FORCE).asBoolean()).isTrue();
    assertThat(result).isNotNull();
  }

  @Test
  public void getCollectionHistory_ShouldCallExecuteContractWithCorrectArguments() {
    // Arrange
    when(clientService.executeContract(anyString(), any(JsonNode.class)))
        .thenReturn(contractExecutionResult);

    // Act
    ExecutionResult result = service.getCollectionHistory(ANY_COLLECTION_ID);

    // Assert
    ArgumentCaptor<JsonNode> argumentCaptor = ArgumentCaptor.forClass(JsonNode.class);
    verify(clientService).executeContract(eq(CONTRACT_GET_HISTORY), argumentCaptor.capture());

    JsonNode capturedArgument = argumentCaptor.getValue();
    assertThat(capturedArgument.get(COLLECTION_ID).asText()).isEqualTo(ANY_COLLECTION_ID);
    assertThat(result).isNotNull();
  }

  @Test
  public void getCollectionHistory_WithLimit_ShouldCallExecuteContractWithCorrectArguments() {
    // Arrange
    when(clientService.executeContract(anyString(), any(JsonNode.class)))
        .thenReturn(contractExecutionResult);

    // Act
    ExecutionResult result = service.getCollectionHistory(ANY_COLLECTION_ID, ANY_LIMIT);

    // Assert
    ArgumentCaptor<JsonNode> argumentCaptor = ArgumentCaptor.forClass(JsonNode.class);
    verify(clientService).executeContract(eq(CONTRACT_GET_HISTORY), argumentCaptor.capture());

    JsonNode capturedArgument = argumentCaptor.getValue();
    assertThat(capturedArgument.get(COLLECTION_ID).asText()).isEqualTo(ANY_COLLECTION_ID);
    assertThat(capturedArgument.has(COLLECTION_OPTIONS)).isTrue();
    assertThat(capturedArgument.get(COLLECTION_OPTIONS).get(OPTION_LIMIT).asInt())
        .isEqualTo(ANY_LIMIT);
    assertThat(result).isNotNull();
  }

  @Test
  public void validateObject_ShouldCallValidateLedgerWithCorrectAssetId() {
    // Arrange
    when(clientService.validateLedger(anyString())).thenReturn(ledgerValidationResult);

    // Act
    LedgerValidationResult result = service.validateObject(ANY_OBJECT_ID);

    // Assert
    verify(clientService).validateLedger(OBJECT_ID_PREFIX + ANY_OBJECT_ID);
    assertThat(result).isEqualTo(ledgerValidationResult);
  }

  @Test
  public void validateObject_WithAge_ShouldCallValidateLedgerWithCorrectAssetIdAndAge() {
    // Arrange
    when(clientService.validateLedger(anyString(), eq(ANY_START_AGE), eq(ANY_END_AGE)))
        .thenReturn(ledgerValidationResult);

    // Act
    LedgerValidationResult result =
        service.validateObject(ANY_OBJECT_ID, ANY_START_AGE, ANY_END_AGE);

    // Assert
    verify(clientService)
        .validateLedger(OBJECT_ID_PREFIX + ANY_OBJECT_ID, ANY_START_AGE, ANY_END_AGE);
    assertThat(result).isEqualTo(ledgerValidationResult);
  }

  @Test
  public void validateCollection_ShouldCallValidateLedgerWithCorrectAssetId() {
    // Arrange
    when(clientService.validateLedger(anyString())).thenReturn(ledgerValidationResult);

    // Act
    LedgerValidationResult result = service.validateCollection(ANY_COLLECTION_ID);

    // Assert
    verify(clientService).validateLedger(COLLECTION_ID_PREFIX + ANY_COLLECTION_ID);
    assertThat(result).isEqualTo(ledgerValidationResult);
  }

  @Test
  public void validateCollection_WithAge_ShouldCallValidateLedgerWithCorrectAssetIdAndAge() {
    // Arrange
    when(clientService.validateLedger(anyString(), eq(ANY_START_AGE), eq(ANY_END_AGE)))
        .thenReturn(ledgerValidationResult);

    // Act
    LedgerValidationResult result =
        service.validateCollection(ANY_COLLECTION_ID, ANY_START_AGE, ANY_END_AGE);

    // Assert
    verify(clientService)
        .validateLedger(COLLECTION_ID_PREFIX + ANY_COLLECTION_ID, ANY_START_AGE, ANY_END_AGE);
    assertThat(result).isEqualTo(ledgerValidationResult);
  }
}
