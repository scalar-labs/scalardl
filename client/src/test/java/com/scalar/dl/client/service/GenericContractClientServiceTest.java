package com.scalar.dl.client.service;

import static com.scalar.dl.genericcontracts.collection.Constants.COLLECTION_ID_PREFIX;
import static com.scalar.dl.genericcontracts.object.Constants.OBJECT_ID_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.scalar.dl.genericcontracts.AssetType;
import java.nio.charset.StandardCharsets;
import javax.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class GenericContractClientServiceTest {
  private static final String ANY_ID = "id";
  private static final String ANY_NAME = "name";
  private static final String ANY_FILE_PATH = "path/to/file";
  private static final int ANY_START_AGE = 0;
  private static final int ANY_END_AGE = 5;
  private static final JsonObject ANY_JSON_OBJECT = mock(JsonObject.class);
  private static final JsonNode ANY_JSON_NODE = mock(JsonNode.class);
  private static final String ANY_JSON_STRING = "json";
  private static final byte[] ANY_BYTES = "bytes".getBytes(StandardCharsets.UTF_8);
  @Mock private ClientService clientService;
  private GenericContractClientService service;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    service = new GenericContractClientService(clientService);
  }

  @Test
  public void registerCertificate_ShouldCallClientServiceRegisterCertificate() {
    // Arrange Act
    service.registerCertificate();
    service.registerCertificate(ANY_BYTES);

    // Assert
    verify(clientService).registerCertificate();
    verify(clientService).registerCertificate(ANY_BYTES);
  }

  @Test
  public void registerSecret_ShouldCallClientServiceRegisterSecret() {
    // Arrange Act
    service.registerSecret();
    service.registerSecret(ANY_BYTES);

    // Assert
    verify(clientService).registerSecret();
    verify(clientService).registerSecret(ANY_BYTES);
  }

  @Test
  public void registerFunction_ShouldCallClientServiceRegisterFunction() {
    // Arrange Act
    service.registerFunction(ANY_ID, ANY_NAME, ANY_BYTES);
    service.registerFunction(ANY_ID, ANY_NAME, ANY_FILE_PATH);
    service.registerFunction(ANY_BYTES);

    // Assert
    verify(clientService).registerFunction(ANY_ID, ANY_NAME, ANY_BYTES);
    verify(clientService).registerFunction(ANY_ID, ANY_NAME, ANY_FILE_PATH);
    verify(clientService).registerFunction(ANY_BYTES);
  }

  @Test
  public void registerContract_ShouldCallClientServiceRegisterContract() {
    // Arrange Act
    service.registerContract(ANY_ID, ANY_NAME, ANY_BYTES);
    service.registerContract(ANY_ID, ANY_NAME, ANY_FILE_PATH);
    service.registerContract(ANY_ID, ANY_NAME, ANY_BYTES, ANY_JSON_OBJECT);
    service.registerContract(ANY_ID, ANY_NAME, ANY_FILE_PATH, ANY_JSON_OBJECT);
    service.registerContract(ANY_ID, ANY_NAME, ANY_BYTES, ANY_JSON_NODE);
    service.registerContract(ANY_ID, ANY_NAME, ANY_FILE_PATH, ANY_JSON_NODE);
    service.registerContract(ANY_ID, ANY_NAME, ANY_BYTES, ANY_JSON_STRING);
    service.registerContract(ANY_ID, ANY_NAME, ANY_FILE_PATH, ANY_JSON_STRING);
    service.registerContract(ANY_BYTES);

    // Assert
    verify(clientService).registerContract(ANY_ID, ANY_NAME, ANY_BYTES);
    verify(clientService).registerContract(ANY_ID, ANY_NAME, ANY_FILE_PATH);
    verify(clientService).registerContract(ANY_ID, ANY_NAME, ANY_BYTES, ANY_JSON_OBJECT);
    verify(clientService).registerContract(ANY_ID, ANY_NAME, ANY_FILE_PATH, ANY_JSON_OBJECT);
    verify(clientService).registerContract(ANY_ID, ANY_NAME, ANY_BYTES, ANY_JSON_NODE);
    verify(clientService).registerContract(ANY_ID, ANY_NAME, ANY_FILE_PATH, ANY_JSON_NODE);
    verify(clientService).registerContract(ANY_ID, ANY_NAME, ANY_BYTES, ANY_JSON_STRING);
    verify(clientService).registerContract(ANY_ID, ANY_NAME, ANY_FILE_PATH, ANY_JSON_STRING);
    verify(clientService).registerContract(ANY_BYTES);
  }

  @Test
  public void listContracts_ShouldCallClientServiceListContracts() {
    // Arrange Act
    service.listContracts(ANY_ID);
    service.listContracts(ANY_BYTES);

    // Assert
    verify(clientService).listContracts(ANY_ID);
    verify(clientService).listContracts(ANY_BYTES);
  }

  @Test
  public void executeContract_ShouldCallClientServiceExecuteContract() {
    // Arrange Act
    service.executeContract(ANY_ID, ANY_JSON_OBJECT);
    service.executeContract(ANY_ID, ANY_JSON_OBJECT, ANY_ID, ANY_JSON_OBJECT);
    service.executeContract(ANY_ID, ANY_JSON_NODE);
    service.executeContract(ANY_ID, ANY_JSON_NODE, ANY_ID, ANY_JSON_NODE);
    service.executeContract(ANY_ID, ANY_JSON_STRING);
    service.executeContract(ANY_ID, ANY_JSON_STRING, ANY_ID, ANY_JSON_STRING);
    service.executeContract(ANY_BYTES);

    // Assert
    verify(clientService).executeContract(ANY_ID, ANY_JSON_OBJECT);
    verify(clientService).executeContract(ANY_ID, ANY_JSON_OBJECT, ANY_ID, ANY_JSON_OBJECT);
    verify(clientService).executeContract(ANY_ID, ANY_JSON_NODE);
    verify(clientService).executeContract(ANY_ID, ANY_JSON_NODE, ANY_ID, ANY_JSON_NODE);
    verify(clientService).executeContract(ANY_ID, ANY_JSON_STRING);
    verify(clientService).executeContract(ANY_ID, ANY_JSON_STRING, ANY_ID, ANY_JSON_STRING);
    verify(clientService).executeContract(ANY_BYTES);
  }

  @Test
  public void
      validateLedger_ObjectTypeGiven_ShouldCallClientServiceValidateLedgerWithObjectPrefix() {
    // Arrange Act
    service.validateLedger(AssetType.OBJECT, ImmutableList.of(ANY_ID));
    service.validateLedger(AssetType.OBJECT, ImmutableList.of(ANY_ID), ANY_START_AGE, ANY_END_AGE);

    // Assert
    verify(clientService).validateLedger(OBJECT_ID_PREFIX + ANY_ID);
    verify(clientService).validateLedger(OBJECT_ID_PREFIX + ANY_ID, ANY_START_AGE, ANY_END_AGE);
  }

  @Test
  public void
      validateLedger_CollectionTypeGiven_ShouldCallClientServiceValidateLedgerWithCollectionPrefix() {
    // Arrange Act
    service.validateLedger(AssetType.COLLECTION, ImmutableList.of(ANY_ID));
    service.validateLedger(
        AssetType.COLLECTION, ImmutableList.of(ANY_ID), ANY_START_AGE, ANY_END_AGE);

    // Assert
    verify(clientService).validateLedger(COLLECTION_ID_PREFIX + ANY_ID);
    verify(clientService).validateLedger(COLLECTION_ID_PREFIX + ANY_ID, ANY_START_AGE, ANY_END_AGE);
  }

  @Test
  public void
      validateLedger_SerializedBinaryGiven_ShouldCallClientServiceValidateLedgerWithBinaryAsIs() {
    // Arrange Act
    service.validateLedger(ANY_BYTES);

    // Assert
    verify(clientService).validateLedger(ANY_BYTES);
  }

  @Test
  public void
      validateLedger_ObjectTypeWithInvalidNumberOfKeysGiven_ShouldThrowIllegalArgumentException() {
    // Arrange Act
    Throwable thrown =
        catchThrowable(
            () -> service.validateLedger(AssetType.OBJECT, ImmutableList.of(ANY_ID, ANY_NAME)));

    // Assert
    assertThat(thrown).isExactlyInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void
      validateLedger_CollectionTypeWithInvalidNumberOfKeysGiven_ShouldThrowIllegalArgumentException() {
    // Arrange Act
    Throwable thrown =
        catchThrowable(() -> service.validateLedger(AssetType.COLLECTION, ImmutableList.of()));

    // Assert
    assertThat(thrown).isExactlyInstanceOf(IllegalArgumentException.class);
  }
}
