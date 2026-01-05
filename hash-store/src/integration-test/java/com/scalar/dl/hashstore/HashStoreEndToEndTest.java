package com.scalar.dl.hashstore;

import static com.scalar.dl.genericcontracts.collection.Constants.COLLECTION_AGE;
import static com.scalar.dl.genericcontracts.collection.Constants.COLLECTION_EVENTS;
import static com.scalar.dl.genericcontracts.collection.Constants.COLLECTION_ID;
import static com.scalar.dl.genericcontracts.collection.Constants.COLLECTION_ID_PREFIX;
import static com.scalar.dl.genericcontracts.collection.Constants.DEFAULT_COLLECTION_CHECKPOINT_INTERVAL;
import static com.scalar.dl.genericcontracts.collection.Constants.OBJECT_ALREADY_EXISTS_IN_COLLECTION;
import static com.scalar.dl.genericcontracts.collection.Constants.OBJECT_IDS;
import static com.scalar.dl.genericcontracts.collection.Constants.OBJECT_NOT_FOUND_IN_COLLECTION;
import static com.scalar.dl.genericcontracts.collection.Constants.OPERATION_ADD;
import static com.scalar.dl.genericcontracts.collection.Constants.OPERATION_CREATE;
import static com.scalar.dl.genericcontracts.collection.Constants.OPERATION_REMOVE;
import static com.scalar.dl.genericcontracts.collection.Constants.OPERATION_TYPE;
import static com.scalar.dl.genericcontracts.object.Constants.DETAILS;
import static com.scalar.dl.genericcontracts.object.Constants.DETAILS_CORRECT_STATUS;
import static com.scalar.dl.genericcontracts.object.Constants.DETAILS_FAULTY_VERSIONS_EXIST;
import static com.scalar.dl.genericcontracts.object.Constants.DETAILS_NUMBER_OF_VERSIONS_MISMATCH;
import static com.scalar.dl.genericcontracts.object.Constants.FAULTY_VERSIONS;
import static com.scalar.dl.genericcontracts.object.Constants.HASH_VALUE;
import static com.scalar.dl.genericcontracts.object.Constants.METADATA;
import static com.scalar.dl.genericcontracts.object.Constants.OBJECT_ID;
import static com.scalar.dl.genericcontracts.object.Constants.OBJECT_ID_PREFIX;
import static com.scalar.dl.genericcontracts.object.Constants.STATUS;
import static com.scalar.dl.genericcontracts.object.Constants.STATUS_CORRECT;
import static com.scalar.dl.genericcontracts.object.Constants.STATUS_FAULTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.db.api.Scan.Ordering;
import com.scalar.db.api.Scanner;
import com.scalar.db.api.TransactionState;
import com.scalar.db.common.CoreError;
import com.scalar.db.exception.storage.ExecutionException;
import com.scalar.db.io.Key;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.hashstore.client.model.Version;
import com.scalar.dl.hashstore.client.service.HashStoreClientService;
import com.scalar.dl.hashstore.client.service.HashStoreClientServiceFactory;
import com.scalar.dl.ledger.LedgerEndToEndTestBase;
import com.scalar.dl.ledger.error.LedgerError;
import com.scalar.dl.ledger.model.ExecutionResult;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.util.JacksonSerDe;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class HashStoreEndToEndTest extends LedgerEndToEndTestBase {
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final JacksonSerDe jacksonSerDe = new JacksonSerDe(mapper);

  private static final String COORDINATOR_NAMESPACE = "coordinator";
  private static final String COORDINATOR_STATE_TABLE = "state";
  private static final String METADATA_TX_STATE = "tx_state";

  private static final String SOME_ENTITY = "entity";
  private static final String SOME_OBJECT_ID = "foo";
  private static final String SOME_HASH_VALUE_0 = "hash_value_0";
  private static final String SOME_HASH_VALUE_1 = "hash_value_1";
  private static final String SOME_HASH_VALUE_2 = "hash_value_2";
  private static final String SOME_VERSION_ID_0 = "v0";
  private static final String SOME_VERSION_ID_1 = "v1";
  private static final String SOME_VERSION_ID_2 = "v2";
  private static final ObjectNode SOME_METADATA_0 = mapper.createObjectNode().put("x", 0);
  private static final ObjectNode SOME_METADATA_1 = mapper.createObjectNode().put("x", 1);
  private static final ObjectNode SOME_METADATA_2 = mapper.createObjectNode().put("x", 2);
  private static final String SOME_COLUMN_NAME_1 = "object_id";
  private static final String SOME_COLUMN_NAME_2 = "version";
  private static final String SOME_COLUMN_NAME_3 = "status";
  private static final String SOME_COLUMN_NAME_4 = "registered_at";
  private static final Instant SOME_TIMESTAMPTZ_VALUE =
      LocalDateTime.of(2021, 2, 3, 5, 45).atZone(ZoneId.systemDefault()).toInstant();
  private static final String SOME_COLLECTION_ID = "set";
  private static final ImmutableList<String> SOME_DEFAULT_OBJECT_IDS =
      ImmutableList.of("object1", "object2", "object3", "object4");
  private static final ImmutableList<String> SOME_ADD_OBJECT_IDS_LIST =
      ImmutableList.of("object5", "object6");
  private static final ImmutableList<String> SOME_REMOVE_OBJECT_IDS_LIST =
      ImmutableList.of("object1", "object4");

  private static final HashStoreClientServiceFactory clientServiceFactory =
      new HashStoreClientServiceFactory();
  private HashStoreClientService clientService;

  @Override
  @BeforeAll
  public void setUpBeforeClass() throws Exception {
    super.setUpBeforeClass();
    clientService = clientServiceFactory.create(createClientConfig(SOME_ENTITY));
  }

  private void prepareObject() {
    clientService.putObject(SOME_OBJECT_ID, SOME_HASH_VALUE_0, SOME_METADATA_0);
    clientService.putObject(SOME_OBJECT_ID, SOME_HASH_VALUE_1, SOME_METADATA_1);
    clientService.putObject(SOME_OBJECT_ID, SOME_HASH_VALUE_2, SOME_METADATA_2);
  }

  private void prepareCollection() {
    clientService.createCollection(SOME_COLLECTION_ID, SOME_DEFAULT_OBJECT_IDS);
  }

  private Set<String> toSetFrom(JsonNode node) {
    ArrayNode array = (ArrayNode) node;
    return StreamSupport.stream(array.spliterator(), false)
        .map(JsonNode::asText)
        .collect(Collectors.toSet());
  }

  private Put createPutToMutable(String objectId, String version, int status) {
    return Put.newBuilder()
        .namespace(getFunctionNamespace())
        .table(getFunctionTable())
        .partitionKey(Key.ofText(SOME_COLUMN_NAME_1, objectId))
        .clusteringKey(Key.ofText(SOME_COLUMN_NAME_2, version))
        .intValue(SOME_COLUMN_NAME_3, status)
        .timestampTZValue(SOME_COLUMN_NAME_4, SOME_TIMESTAMPTZ_VALUE)
        .build();
  }

  @Test
  public void getObject_UpdatedObjectGiven_ShouldReturnLatestHashValueAndMetadata() {
    // Arrange
    prepareObject();
    JsonNode expected =
        mapper
            .createObjectNode()
            .put(OBJECT_ID, SOME_OBJECT_ID)
            .put(HASH_VALUE, SOME_HASH_VALUE_2)
            .set(METADATA, SOME_METADATA_2);

    // Act
    ExecutionResult actual = clientService.getObject(SOME_OBJECT_ID);

    // Assert
    assertThat(actual.getResult()).isPresent();
    assertThat(actual.getResult().get()).isEqualTo(jacksonSerDe.serialize(expected));
  }

  @Test
  public void validateObject_CorrectHashValuesGivenWithAllOption_ShouldReturnCorrectState() {
    // Arrange
    prepareObject();
    ImmutableList<Version> versions =
        ImmutableList.of(
            Version.of(SOME_VERSION_ID_2, SOME_HASH_VALUE_2, SOME_METADATA_2),
            Version.of(SOME_VERSION_ID_1, SOME_HASH_VALUE_1, SOME_METADATA_1),
            Version.of(SOME_VERSION_ID_0, SOME_HASH_VALUE_0, SOME_METADATA_0));
    JsonNode expected =
        mapper
            .createObjectNode()
            .put(STATUS, STATUS_CORRECT)
            .put(DETAILS, DETAILS_CORRECT_STATUS)
            .set(FAULTY_VERSIONS, mapper.createArrayNode());

    // Act
    ExecutionResult actual = clientService.compareAllObjectVersions(SOME_OBJECT_ID, versions);

    // Assert
    assertThat(actual.getResult()).isPresent();
    assertThat(actual.getResult().get()).isEqualTo(jacksonSerDe.serialize(expected));
  }

  @Test
  public void
      validateObject_CorrectHashValuesGivenPartiallyWithoutAllOption_ShouldReturnCorrectState() {
    // Arrange
    prepareObject();
    ImmutableList<Version> versions =
        ImmutableList.of(
            Version.of(SOME_VERSION_ID_2, SOME_HASH_VALUE_2, SOME_METADATA_2),
            Version.of(SOME_VERSION_ID_1, SOME_HASH_VALUE_1, SOME_METADATA_1));
    JsonNode expected =
        mapper
            .createObjectNode()
            .put(STATUS, STATUS_CORRECT)
            .put(DETAILS, DETAILS_CORRECT_STATUS)
            .set(FAULTY_VERSIONS, mapper.createArrayNode());

    // Act
    ExecutionResult actual = clientService.compareObjectVersions(SOME_OBJECT_ID, versions);

    // Assert
    assertThat(actual.getResult()).isPresent();
    assertThat(actual.getResult().get()).isEqualTo(jacksonSerDe.serialize(expected));
  }

  @Test
  public void validateObject_IncorrectHashValuesGiven_ShouldReturnFaultyState() {
    // Arrange
    prepareObject();
    ImmutableList<Version> versions =
        ImmutableList.of(
            Version.of(SOME_VERSION_ID_2, "faulty"),
            Version.of(SOME_VERSION_ID_1, SOME_HASH_VALUE_1),
            Version.of(SOME_VERSION_ID_0, "faulty"));
    JsonNode expected =
        mapper
            .createObjectNode()
            .put(STATUS, STATUS_FAULTY)
            .put(DETAILS, DETAILS_FAULTY_VERSIONS_EXIST)
            .set(
                FAULTY_VERSIONS,
                mapper.createArrayNode().add(SOME_VERSION_ID_2).add(SOME_VERSION_ID_0));

    // Act
    ExecutionResult actual = clientService.compareObjectVersions(SOME_OBJECT_ID, versions);

    // Assert
    assertThat(actual.getResult()).isPresent();
    assertThat(actual.getResult().get()).isEqualTo(jacksonSerDe.serialize(expected));
  }

  @Test
  public void validateObject_IncorrectMetadataGiven_ShouldReturnFaultyState() {
    // Arrange
    prepareObject();
    ImmutableList<Version> versions =
        ImmutableList.of(
            Version.of(SOME_VERSION_ID_2, SOME_HASH_VALUE_2, SOME_METADATA_2),
            Version.of(SOME_VERSION_ID_1, SOME_HASH_VALUE_1, SOME_METADATA_0),
            Version.of(SOME_VERSION_ID_0, SOME_HASH_VALUE_0));
    JsonNode expected =
        mapper
            .createObjectNode()
            .put(STATUS, STATUS_FAULTY)
            .put(DETAILS, DETAILS_FAULTY_VERSIONS_EXIST)
            .set(FAULTY_VERSIONS, mapper.createArrayNode().add(SOME_VERSION_ID_1));

    // Act
    ExecutionResult actual = clientService.compareObjectVersions(SOME_OBJECT_ID, versions);

    // Assert
    assertThat(actual.getResult()).isPresent();
    assertThat(actual.getResult().get()).isEqualTo(jacksonSerDe.serialize(expected));
  }

  @Test
  public void validateObject_IncorrectNumberOfVersionsGiven_ShouldReturnFaultyState() {
    // Arrange
    prepareObject();
    ImmutableList<Version> versions =
        ImmutableList.of(
            Version.of(SOME_VERSION_ID_2, SOME_HASH_VALUE_2),
            Version.of(SOME_VERSION_ID_1, SOME_HASH_VALUE_1));
    JsonNode expected =
        mapper
            .createObjectNode()
            .put(STATUS, STATUS_FAULTY)
            .put(DETAILS, DETAILS_NUMBER_OF_VERSIONS_MISMATCH)
            .set(FAULTY_VERSIONS, mapper.createArrayNode());

    // Act
    ExecutionResult actual = clientService.compareAllObjectVersions(SOME_OBJECT_ID, versions);

    // Assert
    assertThat(actual.getResult()).isPresent();
    assertThat(actual.getResult().get()).isEqualTo(jacksonSerDe.serialize(expected));
  }

  @Test
  public void putObject_FunctionArgumentsGiven_ShouldPutRecordToFunctionTable()
      throws ExecutionException {
    // Arrange
    Put put0 = createPutToMutable(SOME_OBJECT_ID, SOME_VERSION_ID_0, 0);
    Put put1 = createPutToMutable(SOME_OBJECT_ID, SOME_VERSION_ID_1, 1);
    Scan scan =
        Scan.newBuilder()
            .namespace(getFunctionNamespace())
            .table(getFunctionTable())
            .partitionKey(Key.ofText(SOME_COLUMN_NAME_1, SOME_OBJECT_ID))
            .ordering(Ordering.asc(SOME_COLUMN_NAME_2))
            .build();

    // Act
    clientService.putObject(SOME_OBJECT_ID, SOME_HASH_VALUE_0, SOME_METADATA_0, put0);
    clientService.putObject(SOME_OBJECT_ID, SOME_HASH_VALUE_1, SOME_METADATA_1, put1);

    // Assert
    try (Scanner scanner = getStorage().scan(scan)) {
      List<Result> results = scanner.all();
      assertThat(results).hasSize(2);
      assertThat(results.get(0).getText(SOME_COLUMN_NAME_1)).isEqualTo(SOME_OBJECT_ID);
      assertThat(results.get(0).getText(SOME_COLUMN_NAME_2)).isEqualTo(SOME_VERSION_ID_0);
      assertThat(results.get(0).getInt(SOME_COLUMN_NAME_3)).isEqualTo(0);
      assertThat(results.get(0).getTimestampTZ(SOME_COLUMN_NAME_4)).isEqualTo(SOME_TIMESTAMPTZ_VALUE);
      assertThat(results.get(1).getText(SOME_COLUMN_NAME_1)).isEqualTo(SOME_OBJECT_ID);
      assertThat(results.get(1).getText(SOME_COLUMN_NAME_2)).isEqualTo(SOME_VERSION_ID_1);
      assertThat(results.get(1).getInt(SOME_COLUMN_NAME_3)).isEqualTo(1);
      assertThat(results.get(1).getTimestampTZ(SOME_COLUMN_NAME_4)).isEqualTo(SOME_TIMESTAMPTZ_VALUE);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void
      putObject_FunctionArgumentsGivenButSpecifiedTableNotExist_ShouldThrowClientException() {
    // Arrange
    Put put =
        Put.newBuilder()
            .namespace(getFunctionNamespace())
            .table("foo")
            .partitionKey(Key.ofText(SOME_COLUMN_NAME_1, SOME_OBJECT_ID))
            .build();

    // Act Assert
    assertThatThrownBy(
            () -> clientService.putObject(SOME_OBJECT_ID, SOME_HASH_VALUE_0, SOME_METADATA_0, put))
        .isExactlyInstanceOf(ClientException.class)
        .hasMessageContaining(LedgerError.OPERATION_FAILED_DUE_TO_ILLEGAL_ARGUMENT.getId())
        .hasMessageContaining(CoreError.TABLE_NOT_FOUND.getId())
        .extracting("code")
        .isEqualTo(StatusCode.INVALID_FUNCTION);
  }

  @Test
  public void
      putObject_FunctionArgumentsGivenAndUncommittedRecordExists_ShouldThrowClientException()
          throws ExecutionException {
    // Arrange
    Put put0 = createPutToMutable(SOME_OBJECT_ID, SOME_VERSION_ID_0, 0);
    Put put1 = createPutToMutable(SOME_OBJECT_ID, SOME_VERSION_ID_0, 1);
    Put uncommittedPut =
        Put.newBuilder()
            .namespace(getFunctionNamespace())
            .table(getFunctionTable())
            .partitionKey(Key.ofText(SOME_COLUMN_NAME_1, SOME_OBJECT_ID))
            .clusteringKey(Key.ofText(SOME_COLUMN_NAME_2, SOME_VERSION_ID_0))
            .intValue(METADATA_TX_STATE, TransactionState.PREPARED.get())
            .build();
    clientService.putObject(SOME_OBJECT_ID, SOME_HASH_VALUE_0, SOME_METADATA_0, put0);
    // Make the transaction prepared state and non-expired
    getStorage().put(uncommittedPut);
    getStorageAdmin().truncateTable(COORDINATOR_NAMESPACE, COORDINATOR_STATE_TABLE);

    // Act Assert
    assertThatThrownBy(
            () -> clientService.putObject(SOME_OBJECT_ID, SOME_HASH_VALUE_1, SOME_METADATA_1, put1))
        .isExactlyInstanceOf(ClientException.class)
        .hasMessageContaining(LedgerError.OPERATION_FAILED_DUE_TO_CONFLICT.getId())
        .hasMessageContaining(CoreError.CONSENSUS_COMMIT_READ_UNCOMMITTED_RECORD.getId())
        .extracting("code")
        .isEqualTo(StatusCode.CONFLICT);
  }

  @Test
  public void putObject_SameIdCollectionGivenBefore_ShouldPutObjectWithoutEffectForCollection() {
    // Arrange
    prepareCollection();
    Set<String> expectedSet = new HashSet<>(SOME_DEFAULT_OBJECT_IDS);

    // Act
    clientService.putObject(SOME_COLLECTION_ID, SOME_HASH_VALUE_0, SOME_METADATA_0);

    // Assert
    ExecutionResult object = clientService.getObject(SOME_COLLECTION_ID);
    assertThat(object.getResult()).isPresent();
    JsonNode objectJson = jacksonSerDe.deserialize(object.getResult().get());
    assertThat(objectJson.get(OBJECT_ID).textValue()).isEqualTo(SOME_COLLECTION_ID);
    assertThat(objectJson.get(HASH_VALUE).textValue()).isEqualTo(SOME_HASH_VALUE_0);
    assertThat(objectJson.get(METADATA)).isEqualTo(SOME_METADATA_0);
    ExecutionResult collection = clientService.getCollection(SOME_COLLECTION_ID);
    assertThat(collection.getResult()).isPresent();
    JsonNode collectionJson = jacksonSerDe.deserialize(collection.getResult().get());
    assertThat(toSetFrom(collectionJson.get(OBJECT_IDS))).isEqualTo(expectedSet);
  }

  @Test
  public void addToCollection_ThenGetCollectionGiven_ShouldReturnCorrectSet() {
    // Arrange
    prepareCollection();
    Set<String> expectedSet = new HashSet<>(SOME_DEFAULT_OBJECT_IDS);
    expectedSet.addAll(SOME_ADD_OBJECT_IDS_LIST);

    // Act
    clientService.addToCollection(SOME_COLLECTION_ID, SOME_ADD_OBJECT_IDS_LIST);
    ExecutionResult actual = clientService.getCollection(SOME_COLLECTION_ID);

    // Assert
    assertThat(actual.getResult()).isPresent();
    JsonNode actualJson = jacksonSerDe.deserialize(actual.getResult().get());
    assertThat(toSetFrom(actualJson.get(OBJECT_IDS))).isEqualTo(expectedSet);
  }

  @Test
  public void addToCollection_ExistingObjectIdGivenWithoutForceOption_ShouldThrowClientException() {
    // Arrange
    prepareCollection();
    clientService.addToCollection(SOME_COLLECTION_ID, ImmutableList.of(SOME_OBJECT_ID));

    // Act Assert
    assertThatThrownBy(
            () ->
                clientService.addToCollection(SOME_COLLECTION_ID, ImmutableList.of(SOME_OBJECT_ID)))
        .isExactlyInstanceOf(ClientException.class)
        .hasMessage(OBJECT_ALREADY_EXISTS_IN_COLLECTION);
  }

  @Test
  public void removeFromCollection_ThenGetCollectionGiven_ShouldReturnCorrectSet() {
    // Arrange
    prepareCollection();
    Set<String> expectedSet = new HashSet<>(SOME_DEFAULT_OBJECT_IDS);
    SOME_REMOVE_OBJECT_IDS_LIST.forEach(expectedSet::remove);

    // Act
    clientService.removeFromCollection(SOME_COLLECTION_ID, SOME_REMOVE_OBJECT_IDS_LIST);
    ExecutionResult actual = clientService.getCollection(SOME_COLLECTION_ID);

    // Assert
    assertThat(actual.getResult()).isPresent();
    JsonNode actualJson = jacksonSerDe.deserialize(actual.getResult().get());
    assertThat(toSetFrom(actualJson.get(OBJECT_IDS))).isEqualTo(expectedSet);
  }

  @Test
  public void
      removeFromCollection_NotExistingObjectIdGivenWithoutForceOption_ShouldThrowClientException() {
    // Arrange
    prepareCollection();

    // Act Assert
    assertThatThrownBy(
            () ->
                clientService.removeFromCollection(
                    SOME_COLLECTION_ID, ImmutableList.of(SOME_OBJECT_ID)))
        .isExactlyInstanceOf(ClientException.class)
        .hasMessage(OBJECT_NOT_FOUND_IN_COLLECTION);
  }

  @Test
  public void getCollection_CollectionAfterCheckpointAgeGiven_ShouldReturnCorrectSet() {
    // Arrange
    prepareCollection();
    IntStream.range(0, DEFAULT_COLLECTION_CHECKPOINT_INTERVAL)
        .forEach(
            i ->
                clientService.addToCollection(
                    SOME_COLLECTION_ID, ImmutableList.of(String.valueOf(i))));
    IntStream.range(0, DEFAULT_COLLECTION_CHECKPOINT_INTERVAL)
        .forEach(
            i ->
                clientService.removeFromCollection(
                    SOME_COLLECTION_ID, ImmutableList.of(String.valueOf(i))));
    Set<String> expectedSet = new HashSet<>(SOME_DEFAULT_OBJECT_IDS);

    // Act
    ExecutionResult actual = clientService.getCollection(SOME_COLLECTION_ID);

    // Assert
    assertThat(actual.getResult()).isPresent();
    JsonNode actualJson = jacksonSerDe.deserialize(actual.getResult().get());
    assertThat(toSetFrom(actualJson.get(OBJECT_IDS))).isEqualTo(expectedSet);
  }

  @Test
  public void getCollection_EmptySetsAddedAndRemovedBefore_ShouldReturnInitialSet() {
    // Arrange
    prepareCollection();
    IntStream.range(0, DEFAULT_COLLECTION_CHECKPOINT_INTERVAL)
        .forEach(i -> clientService.addToCollection(SOME_COLLECTION_ID, ImmutableList.of()));
    IntStream.range(0, DEFAULT_COLLECTION_CHECKPOINT_INTERVAL)
        .forEach(i -> clientService.removeFromCollection(SOME_COLLECTION_ID, ImmutableList.of()));
    Set<String> expectedSet = new HashSet<>(SOME_DEFAULT_OBJECT_IDS);

    // Act
    ExecutionResult actual = clientService.getCollection(SOME_COLLECTION_ID);

    // Assert
    assertThat(actual.getResult()).isPresent();
    JsonNode actualJson = jacksonSerDe.deserialize(actual.getResult().get());
    assertThat(toSetFrom(actualJson.get(OBJECT_IDS))).isEqualTo(expectedSet);
  }

  @Test
  public void getCollectionHistory_AddAndRemoveOperationsGivenBefore_ShouldReturnCorrectHistory() {
    // Arrange
    prepareCollection();
    clientService.addToCollection(SOME_COLLECTION_ID, SOME_ADD_OBJECT_IDS_LIST);
    clientService.removeFromCollection(SOME_COLLECTION_ID, SOME_REMOVE_OBJECT_IDS_LIST);
    Set<String> expectedSet0 = new HashSet<>(SOME_DEFAULT_OBJECT_IDS);
    Set<String> expectedSet1 = new HashSet<>(SOME_ADD_OBJECT_IDS_LIST);
    Set<String> expectedSet2 = new HashSet<>(SOME_REMOVE_OBJECT_IDS_LIST);

    // Act
    ExecutionResult result = clientService.getCollectionHistory(SOME_COLLECTION_ID);

    // Assert
    assertThat(result.getResult()).isPresent();
    JsonNode actual = jacksonSerDe.deserialize(result.getResult().get());
    assertThat(actual.get(COLLECTION_ID).textValue()).isEqualTo(SOME_COLLECTION_ID);
    ArrayNode actualEvents = (ArrayNode) actual.get(COLLECTION_EVENTS);
    assertThat(actualEvents.size()).isEqualTo(3);
    assertThat(actualEvents.get(0).get(OPERATION_TYPE).textValue()).isEqualTo(OPERATION_REMOVE);
    assertThat(toSetFrom(actualEvents.get(0).get(OBJECT_IDS))).isEqualTo(expectedSet2);
    assertThat(actualEvents.get(0).get(COLLECTION_AGE).intValue()).isEqualTo(2);
    assertThat(actualEvents.get(1).get(OPERATION_TYPE).textValue()).isEqualTo(OPERATION_ADD);
    assertThat(toSetFrom(actualEvents.get(1).get(OBJECT_IDS))).isEqualTo(expectedSet1);
    assertThat(actualEvents.get(1).get(COLLECTION_AGE).intValue()).isEqualTo(1);
    assertThat(actualEvents.get(2).get(OPERATION_TYPE).textValue()).isEqualTo(OPERATION_CREATE);
    assertThat(toSetFrom(actualEvents.get(2).get(OBJECT_IDS))).isEqualTo(expectedSet0);
    assertThat(actualEvents.get(2).get(COLLECTION_AGE).intValue()).isEqualTo(0);
  }

  @Test
  public void
      getCollectionHistory_AddAndRemoveOperationsGivenBeforeAndLimitsGiven_ShouldReturnLimitedHistory() {
    // Arrange
    prepareCollection();
    clientService.addToCollection(SOME_COLLECTION_ID, SOME_ADD_OBJECT_IDS_LIST);
    clientService.removeFromCollection(SOME_COLLECTION_ID, SOME_REMOVE_OBJECT_IDS_LIST);
    Set<String> expectedSet = new HashSet<>(SOME_REMOVE_OBJECT_IDS_LIST);

    // Act
    ExecutionResult result = clientService.getCollectionHistory(SOME_COLLECTION_ID, 1);

    // Assert
    assertThat(result.getResult()).isPresent();
    JsonNode actual = jacksonSerDe.deserialize(result.getResult().get());
    assertThat(actual.get(COLLECTION_ID).textValue()).isEqualTo(SOME_COLLECTION_ID);
    ArrayNode actualEvents = (ArrayNode) actual.get(COLLECTION_EVENTS);
    assertThat(actualEvents.size()).isEqualTo(1);
    assertThat(actualEvents.get(0).get(OPERATION_TYPE).textValue()).isEqualTo(OPERATION_REMOVE);
    assertThat(toSetFrom(actualEvents.get(0).get(OBJECT_IDS))).isEqualTo(expectedSet);
    assertThat(actualEvents.get(0).get(COLLECTION_AGE).intValue()).isEqualTo(2);
  }

  @Test
  public void
      createCollection_SameIdObjectGivenBefore_ShouldCreateCollectionWithoutEffectForObject() {
    // Arrange
    prepareObject();
    Set<String> expectedSet = new HashSet<>(SOME_DEFAULT_OBJECT_IDS);

    // Act
    clientService.createCollection(SOME_OBJECT_ID, SOME_DEFAULT_OBJECT_IDS);

    // Assert
    ExecutionResult collection = clientService.getCollection(SOME_OBJECT_ID);
    assertThat(collection.getResult()).isPresent();
    JsonNode collectionJson = jacksonSerDe.deserialize(collection.getResult().get());
    assertThat(toSetFrom(collectionJson.get(OBJECT_IDS))).isEqualTo(expectedSet);
    ExecutionResult object = clientService.getObject(SOME_OBJECT_ID);
    assertThat(object.getResult()).isPresent();
    JsonNode objectJson = jacksonSerDe.deserialize(object.getResult().get());
    assertThat(objectJson.get(OBJECT_ID).textValue()).isEqualTo(SOME_OBJECT_ID);
    assertThat(objectJson.get(HASH_VALUE).textValue()).isEqualTo(SOME_HASH_VALUE_2);
    assertThat(objectJson.get(METADATA)).isEqualTo(SOME_METADATA_2);
  }

  @Test
  public void validateObject_ObjectGiven_ShouldReturnCorrectResult() {
    // Arrange
    prepareObject();

    // Act
    LedgerValidationResult actual = clientService.validateObject(SOME_OBJECT_ID);

    // Assert
    assertThat(actual.getCode()).isEqualTo(StatusCode.OK);
    assertThat(actual.getLedgerProof().isPresent()).isTrue();
    assertThat(actual.getLedgerProof().get().getId()).isEqualTo(OBJECT_ID_PREFIX + SOME_OBJECT_ID);
    assertThat(actual.getLedgerProof().get().getAge()).isEqualTo(2);
    assertThat(actual.getAuditorProof().isPresent()).isFalse();
  }

  @Test
  public void validateCollection_CollectionGiven_ShouldReturnCorrectResult() {
    // Arrange
    prepareCollection();

    // Act
    LedgerValidationResult actual = clientService.validateCollection(SOME_COLLECTION_ID);

    // Assert
    assertThat(actual.getCode()).isEqualTo(StatusCode.OK);
    assertThat(actual.getLedgerProof().isPresent()).isTrue();
    assertThat(actual.getLedgerProof().get().getId())
        .isEqualTo(COLLECTION_ID_PREFIX + SOME_COLLECTION_ID);
    assertThat(actual.getLedgerProof().get().getAge()).isEqualTo(0);
    assertThat(actual.getAuditorProof().isPresent()).isFalse();
  }
}
