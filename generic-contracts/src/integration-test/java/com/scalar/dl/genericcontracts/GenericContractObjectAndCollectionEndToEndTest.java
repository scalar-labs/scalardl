package com.scalar.dl.genericcontracts;

import static com.scalar.dl.genericcontracts.collection.Constants.COLLECTION_AGE;
import static com.scalar.dl.genericcontracts.collection.Constants.COLLECTION_CHECKPOINT_INTERVAL;
import static com.scalar.dl.genericcontracts.collection.Constants.COLLECTION_EVENTS;
import static com.scalar.dl.genericcontracts.collection.Constants.COLLECTION_ID;
import static com.scalar.dl.genericcontracts.collection.Constants.COLLECTION_ID_PREFIX;
import static com.scalar.dl.genericcontracts.collection.Constants.COLLECTION_SNAPSHOT;
import static com.scalar.dl.genericcontracts.collection.Constants.DEFAULT_COLLECTION_CHECKPOINT_INTERVAL;
import static com.scalar.dl.genericcontracts.collection.Constants.INVALID_CHECKPOINT;
import static com.scalar.dl.genericcontracts.collection.Constants.OBJECT_ALREADY_EXISTS_IN_COLLECTION;
import static com.scalar.dl.genericcontracts.collection.Constants.OBJECT_IDS;
import static com.scalar.dl.genericcontracts.collection.Constants.OBJECT_NOT_FOUND_IN_COLLECTION;
import static com.scalar.dl.genericcontracts.collection.Constants.OPERATION_ADD;
import static com.scalar.dl.genericcontracts.collection.Constants.OPERATION_CREATE;
import static com.scalar.dl.genericcontracts.collection.Constants.OPERATION_REMOVE;
import static com.scalar.dl.genericcontracts.collection.Constants.OPERATION_TYPE;
import static com.scalar.dl.genericcontracts.collection.Constants.OPTION_LIMIT;
import static com.scalar.dl.genericcontracts.object.Constants.CLUSTERING_KEY;
import static com.scalar.dl.genericcontracts.object.Constants.COLUMNS;
import static com.scalar.dl.genericcontracts.object.Constants.COLUMN_NAME;
import static com.scalar.dl.genericcontracts.object.Constants.DATA_TYPE;
import static com.scalar.dl.genericcontracts.object.Constants.DETAILS;
import static com.scalar.dl.genericcontracts.object.Constants.DETAILS_CORRECT_STATUS;
import static com.scalar.dl.genericcontracts.object.Constants.DETAILS_FAULTY_VERSIONS_EXIST;
import static com.scalar.dl.genericcontracts.object.Constants.DETAILS_NUMBER_OF_VERSIONS_MISMATCH;
import static com.scalar.dl.genericcontracts.object.Constants.FAULTY_VERSIONS;
import static com.scalar.dl.genericcontracts.object.Constants.HASH_VALUE;
import static com.scalar.dl.genericcontracts.object.Constants.INVALID_METADATA_FORMAT;
import static com.scalar.dl.genericcontracts.object.Constants.INVALID_VERSIONS_FORMAT;
import static com.scalar.dl.genericcontracts.object.Constants.METADATA;
import static com.scalar.dl.genericcontracts.object.Constants.NAMESPACE;
import static com.scalar.dl.genericcontracts.object.Constants.OBJECT_ID;
import static com.scalar.dl.genericcontracts.object.Constants.OBJECT_ID_PREFIX;
import static com.scalar.dl.genericcontracts.object.Constants.OPTIONS;
import static com.scalar.dl.genericcontracts.object.Constants.OPTION_ALL;
import static com.scalar.dl.genericcontracts.object.Constants.PARTITION_KEY;
import static com.scalar.dl.genericcontracts.object.Constants.STATUS;
import static com.scalar.dl.genericcontracts.object.Constants.STATUS_CORRECT;
import static com.scalar.dl.genericcontracts.object.Constants.STATUS_FAULTY;
import static com.scalar.dl.genericcontracts.object.Constants.TABLE;
import static com.scalar.dl.genericcontracts.object.Constants.VALUE;
import static com.scalar.dl.genericcontracts.object.Constants.VERSIONS;
import static com.scalar.dl.genericcontracts.object.Constants.VERSION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.scalar.db.api.Get;
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
import com.scalar.dl.client.service.GenericContractClientService;
import com.scalar.dl.genericcontracts.object.Constants;
import com.scalar.dl.ledger.error.LedgerError;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.util.JacksonSerDe;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;

public class GenericContractObjectAndCollectionEndToEndTest
    extends GenericContractEndToEndTestBase {
  private static final String COORDINATOR_NAMESPACE = "coordinator";
  private static final String COORDINATOR_STATE_TABLE = "state";
  private static final String METADATA_TX_STATE = "tx_state";

  private static final String ASSET_ID = "id";
  private static final String ASSET_AGE = "age";
  private static final String ASSET_OUTPUT = "output";
  private static final String DATA_TYPE_INT = "INT";
  private static final String DATA_TYPE_TIMESTAMP = "TIMESTAMP";
  private static final String DATA_TYPE_TEXT = "TEXT";

  private static final String PACKAGE_OBJECT = "object";
  private static final String PACKAGE_COLLECTION = "collection";
  private static final String NAME_OBJECT_GET = "Get";
  private static final String NAME_OBJECT_PUT = "Put";
  private static final String NAME_OBJECT_PUT_TO_MUTABLE = "PutToMutableDatabase";
  private static final String NAME_OBJECT_VALIDATE = "Validate";
  private static final String NAME_COLLECTION_CREATE = "Create";
  private static final String NAME_COLLECTION_ADD = "Add";
  private static final String NAME_COLLECTION_REMOVE = "Remove";
  private static final String NAME_COLLECTION_GET = "Get";
  private static final String NAME_COLLECTION_GET_HISTORY = "GetHistory";
  private static final String NAME_COLLECTION_GET_CHECKPOINT_INTERVAL = "GetCheckpointInterval";
  private static final String ID_OBJECT_GET = getObjectContractId(NAME_OBJECT_GET);
  private static final String ID_OBJECT_PUT = getObjectContractId(NAME_OBJECT_PUT);
  private static final String ID_OBJECT_VALIDATE = getObjectContractId(NAME_OBJECT_VALIDATE);
  private static final String ID_OBJECT_PUT_MUTABLE =
      getFunctionId(PACKAGE_OBJECT, NAME_OBJECT_PUT_TO_MUTABLE);
  private static final String ID_COLLECTION_CREATE =
      getCollectionContractId(NAME_COLLECTION_CREATE);
  private static final String ID_COLLECTION_ADD_OBJECTS =
      getCollectionContractId(NAME_COLLECTION_ADD);
  private static final String ID_COLLECTION_DEL_OBJECTS =
      getCollectionContractId(NAME_COLLECTION_REMOVE);
  private static final String ID_COLLECTION_GET = getCollectionContractId(NAME_COLLECTION_GET);
  private static final String ID_COLLECTION_GET_HISTORY =
      getCollectionContractId(NAME_COLLECTION_GET_HISTORY);
  private static final String ID_COLLECTION_GET_CHECKPOINT_INTERVAL =
      getCollectionContractId(NAME_COLLECTION_GET_CHECKPOINT_INTERVAL);

  private static final ObjectMapper mapper = new ObjectMapper();
  private static final JacksonSerDe jacksonSerDe = new JacksonSerDe(mapper);
  private static final String SOME_OBJECT_ID = "foo";
  private static final String SOME_HASH_VALUE_0 = "hash_value_0";
  private static final String SOME_HASH_VALUE_1 = "hash_value_1";
  private static final String SOME_HASH_VALUE_2 = "hash_value_2";
  private static final String SOME_VERSION_ID_0 = "v0";
  private static final String SOME_VERSION_ID_1 = "v1";
  private static final String SOME_VERSION_ID_2 = "v2";
  private static final JsonNode SOME_METADATA_0 = mapper.createObjectNode().put("x", 0);
  private static final JsonNode SOME_METADATA_1 = mapper.createObjectNode().put("x", 1);
  private static final JsonNode SOME_METADATA_2 = mapper.createObjectNode().put("x", 2);
  private static final String SOME_COLUMN_NAME_1 = "object_id";
  private static final String SOME_COLUMN_NAME_2 = "version";
  private static final String SOME_COLUMN_NAME_3 = "status";
  private static final String SOME_COLUMN_NAME_4 = "registered_at";
  private static final String SOME_TIMESTAMP_TEXT = "2021-02-03 05:45:00";
  private static final LocalDateTime SOME_TIMESTAMP_VALUE = LocalDateTime.of(2021, 2, 3, 5, 45);
  private static final String SOME_COLLECTION_ID = "set";
  private static final ArrayNode SOME_DEFAULT_OBJECT_IDS =
      mapper.createArrayNode().add("object1").add("object2").add("object3").add("object4");
  private static final ArrayNode SOME_ADD_OBJECT_IDS_ARRAY =
      mapper.createArrayNode().add("object5").add("object6");
  private static final ArrayNode SOME_REMOVE_OBJECT_IDS_ARRAY =
      mapper.createArrayNode().add("object1").add("object4");
  private static final JsonNode SOME_LIMIT_OPTION = mapper.createObjectNode().put(OPTION_LIMIT, 1);
  private static final int SOME_CHECKPOINT_INTERVAL = 2;

  private static String getObjectContractId(String contractName) {
    return getContractId(PACKAGE_OBJECT, contractName);
  }

  private static String getCollectionContractId(String contractName) {
    return getContractId(PACKAGE_COLLECTION, contractName);
  }

  private static String getObjectContractBinaryName(String contractName) {
    return getContractBinaryName(PACKAGE_OBJECT, contractName);
  }

  private static String getCollectionContractBinaryName(String contractName) {
    return getContractBinaryName(PACKAGE_COLLECTION, contractName);
  }

  @Override
  Map<String, String> getContractsMap() {
    return ImmutableMap.<String, String>builder()
        .put(ID_OBJECT_GET, getObjectContractBinaryName(NAME_OBJECT_GET))
        .put(ID_OBJECT_PUT, getObjectContractBinaryName(NAME_OBJECT_PUT))
        .put(ID_OBJECT_VALIDATE, getObjectContractBinaryName(NAME_OBJECT_VALIDATE))
        .put(ID_COLLECTION_CREATE, getCollectionContractBinaryName(NAME_COLLECTION_CREATE))
        .put(ID_COLLECTION_ADD_OBJECTS, getCollectionContractBinaryName(NAME_COLLECTION_ADD))
        .put(ID_COLLECTION_DEL_OBJECTS, getCollectionContractBinaryName(NAME_COLLECTION_REMOVE))
        .put(ID_COLLECTION_GET, getCollectionContractBinaryName(NAME_COLLECTION_GET))
        .put(
            ID_COLLECTION_GET_HISTORY, getCollectionContractBinaryName(NAME_COLLECTION_GET_HISTORY))
        .put(
            ID_COLLECTION_GET_CHECKPOINT_INTERVAL,
            getCollectionContractBinaryName(NAME_COLLECTION_GET_CHECKPOINT_INTERVAL))
        .build();
  }

  @Override
  Map<String, String> getFunctionsMap() {
    return ImmutableMap.of(
        getFunctionId(PACKAGE_OBJECT, NAME_OBJECT_PUT_TO_MUTABLE),
        getFunctionBinaryName(PACKAGE_OBJECT, NAME_OBJECT_PUT_TO_MUTABLE));
  }

  @Override
  protected Map<String, JsonNode> getAnotherContractPropertiesMap() {
    return ImmutableMap.of(
        ID_COLLECTION_GET_CHECKPOINT_INTERVAL,
        mapper.createObjectNode().put(COLLECTION_CHECKPOINT_INTERVAL, SOME_CHECKPOINT_INTERVAL));
  }

  private void prepareObject() {
    JsonNode objectV0 =
        mapper
            .createObjectNode()
            .put(OBJECT_ID, SOME_OBJECT_ID)
            .put(HASH_VALUE, SOME_HASH_VALUE_0)
            .set(METADATA, SOME_METADATA_0);
    JsonNode objectV1 =
        mapper
            .createObjectNode()
            .put(OBJECT_ID, SOME_OBJECT_ID)
            .put(HASH_VALUE, SOME_HASH_VALUE_1)
            .set(METADATA, SOME_METADATA_1);
    JsonNode objectV2 =
        mapper
            .createObjectNode()
            .put(OBJECT_ID, SOME_OBJECT_ID)
            .put(HASH_VALUE, SOME_HASH_VALUE_2)
            .set(METADATA, SOME_METADATA_2);
    clientService.executeContract(ID_OBJECT_PUT, objectV0);
    clientService.executeContract(ID_OBJECT_PUT, objectV1);
    clientService.executeContract(ID_OBJECT_PUT, objectV2);
  }

  private void prepareCollection(GenericContractClientService clientService) {
    JsonNode arguments =
        mapper
            .createObjectNode()
            .put(COLLECTION_ID, SOME_COLLECTION_ID)
            .set(OBJECT_IDS, SOME_DEFAULT_OBJECT_IDS);
    clientService.executeContract(ID_COLLECTION_CREATE, arguments);
  }

  private void prepareCollection() {
    prepareCollection(clientService);
  }

  private JsonNode createColumn(String name, int value) {
    return mapper
        .createObjectNode()
        .put(COLUMN_NAME, name)
        .put(VALUE, value)
        .put(DATA_TYPE, DATA_TYPE_INT);
  }

  private JsonNode createColumn(String name, String value) {
    return mapper
        .createObjectNode()
        .put(COLUMN_NAME, name)
        .put(VALUE, value)
        .put(DATA_TYPE, DATA_TYPE_TEXT);
  }

  private JsonNode createTimestampColumn(String name, String value) {
    return mapper
        .createObjectNode()
        .put(COLUMN_NAME, name)
        .put(VALUE, value)
        .put(DATA_TYPE, DATA_TYPE_TIMESTAMP);
  }

  private JsonNode createFunctionArguments(
      String objectId, String version, int status, long registeredAt) {
    ArrayNode partitionKey =
        mapper.createArrayNode().add(createColumn(SOME_COLUMN_NAME_1, objectId));
    ArrayNode clusteringKey =
        mapper.createArrayNode().add(createColumn(SOME_COLUMN_NAME_2, version));
    ArrayNode columns =
        mapper
            .createArrayNode()
            .add(createColumn(SOME_COLUMN_NAME_3, status))
            .add(createTimestampColumn(SOME_COLUMN_NAME_4, SOME_TIMESTAMP_TEXT));

    ObjectNode arguments = mapper.createObjectNode();
    arguments.put(NAMESPACE, getFunctionNamespace());
    arguments.put(TABLE, getFunctionTable());
    arguments.set(PARTITION_KEY, partitionKey);
    arguments.set(CLUSTERING_KEY, clusteringKey);
    arguments.set(COLUMNS, columns);

    return arguments;
  }

  private void addObjectsToCollection(
      GenericContractClientService clientService, String collectionId, ArrayNode objectIds) {
    JsonNode arguments =
        mapper.createObjectNode().put(COLLECTION_ID, collectionId).set(OBJECT_IDS, objectIds);
    clientService.executeContract(ID_COLLECTION_ADD_OBJECTS, arguments);
  }

  private void addObjectsToCollection(String collectionId, ArrayNode objectIds) {
    addObjectsToCollection(clientService, collectionId, objectIds);
  }

  private void addObjectsToCollection(String collectionId, Set<String> objectIds) {
    ArrayNode array = mapper.createArrayNode();
    objectIds.forEach(array::add);
    addObjectsToCollection(collectionId, array);
  }

  private void removeObjectsFromCollection(
      GenericContractClientService clientService, String collectionId, ArrayNode objectIds) {
    JsonNode arguments =
        mapper.createObjectNode().put(COLLECTION_ID, collectionId).set(OBJECT_IDS, objectIds);
    clientService.executeContract(ID_COLLECTION_DEL_OBJECTS, arguments);
  }

  private void removeObjectsFromCollection(String collectionId, ArrayNode objectIds) {
    removeObjectsFromCollection(clientService, collectionId, objectIds);
  }

  private void removeObjectsFromCollection(String collectionId, Set<String> objectIds) {
    ArrayNode array = mapper.createArrayNode();
    objectIds.forEach(array::add);
    removeObjectsFromCollection(collectionId, array);
  }

  private Set<String> toSetFrom(ArrayNode array) {
    return StreamSupport.stream(array.spliterator(), false)
        .map(JsonNode::asText)
        .collect(Collectors.toSet());
  }

  private Set<String> toSetFrom(JsonNode node) {
    return toSetFrom((ArrayNode) node);
  }

  private JsonNode createVersion(String versionId, String hashValue, JsonNode metadata) {
    ObjectNode version =
        mapper.createObjectNode().put(VERSION_ID, versionId).put(HASH_VALUE, hashValue);
    if (metadata != null) {
      version.set(METADATA, metadata);
    }
    return version;
  }

  private JsonNode createVersion(String versionId, String hashValue) {
    return createVersion(versionId, hashValue, null);
  }

  @Test
  public void getObject_UpdatedObjectGiven_ShouldReturnLatestHashValueAndMetadata() {
    // Arrange
    prepareObject();
    JsonNode getContractArguments = mapper.createObjectNode().put(OBJECT_ID, SOME_OBJECT_ID);
    JsonNode expected =
        mapper
            .createObjectNode()
            .put(OBJECT_ID, SOME_OBJECT_ID)
            .put(HASH_VALUE, SOME_HASH_VALUE_2)
            .set(METADATA, SOME_METADATA_2);

    // Act
    ContractExecutionResult actual =
        clientService.executeContract(ID_OBJECT_GET, getContractArguments);

    // Assert
    assertThat(actual.getContractResult()).isPresent();
    assertThat(actual.getContractResult().get()).isEqualTo(jacksonSerDe.serialize(expected));
  }

  @Test
  public void validateObject_CorrectHashValuesGivenWithAllOption_ShouldReturnCorrectState() {
    // Arrange
    prepareObject();
    ObjectNode validateContractArguments = mapper.createObjectNode();
    validateContractArguments.put(OBJECT_ID, SOME_OBJECT_ID);
    validateContractArguments.set(
        VERSIONS,
        mapper
            .createArrayNode()
            .add(createVersion(SOME_VERSION_ID_2, SOME_HASH_VALUE_2, SOME_METADATA_2))
            .add(createVersion(SOME_VERSION_ID_1, SOME_HASH_VALUE_1, SOME_METADATA_1))
            .add(createVersion(SOME_VERSION_ID_0, SOME_HASH_VALUE_0, SOME_METADATA_0)));
    validateContractArguments.set(OPTIONS, mapper.createObjectNode().put(OPTION_ALL, true));
    JsonNode expected =
        mapper
            .createObjectNode()
            .put(STATUS, STATUS_CORRECT)
            .put(DETAILS, DETAILS_CORRECT_STATUS)
            .set(FAULTY_VERSIONS, mapper.createArrayNode());

    // Act
    ContractExecutionResult actual =
        clientService.executeContract(ID_OBJECT_VALIDATE, validateContractArguments);

    // Assert
    assertThat(actual.getContractResult()).isPresent();
    assertThat(actual.getContractResult().get()).isEqualTo(jacksonSerDe.serialize(expected));
  }

  @Test
  public void
      validateObject_CorrectHashValuesGivenPartiallyWithoutAllOption_ShouldReturnCorrectState() {
    // Arrange
    prepareObject();
    JsonNode validateContractArguments =
        mapper
            .createObjectNode()
            .put(OBJECT_ID, SOME_OBJECT_ID)
            .set(
                VERSIONS,
                mapper
                    .createArrayNode()
                    .add(createVersion(SOME_VERSION_ID_2, SOME_HASH_VALUE_2, SOME_METADATA_2))
                    .add(createVersion(SOME_VERSION_ID_1, SOME_HASH_VALUE_1, SOME_METADATA_1)));
    JsonNode expected =
        mapper
            .createObjectNode()
            .put(STATUS, STATUS_CORRECT)
            .put(DETAILS, DETAILS_CORRECT_STATUS)
            .set(FAULTY_VERSIONS, mapper.createArrayNode());

    // Act
    ContractExecutionResult actual =
        clientService.executeContract(ID_OBJECT_VALIDATE, validateContractArguments);

    // Assert
    assertThat(actual.getContractResult()).isPresent();
    assertThat(actual.getContractResult().get()).isEqualTo(jacksonSerDe.serialize(expected));
  }

  @Test
  public void validateObject_IncorrectHashValuesGiven_ShouldReturnFaultyState() {
    // Arrange
    prepareObject();
    JsonNode validateContractArguments =
        mapper
            .createObjectNode()
            .put(OBJECT_ID, SOME_OBJECT_ID)
            .set(
                VERSIONS,
                mapper
                    .createArrayNode()
                    .add(createVersion(SOME_VERSION_ID_2, "faulty"))
                    .add(createVersion(SOME_VERSION_ID_1, SOME_HASH_VALUE_1))
                    .add(createVersion(SOME_VERSION_ID_0, "faulty")));
    JsonNode expected =
        mapper
            .createObjectNode()
            .put(STATUS, STATUS_FAULTY)
            .put(DETAILS, DETAILS_FAULTY_VERSIONS_EXIST)
            .set(
                FAULTY_VERSIONS,
                mapper.createArrayNode().add(SOME_VERSION_ID_2).add(SOME_VERSION_ID_0));

    // Act
    ContractExecutionResult actual =
        clientService.executeContract(ID_OBJECT_VALIDATE, validateContractArguments);

    // Assert
    assertThat(actual.getContractResult()).isPresent();
    assertThat(actual.getContractResult().get()).isEqualTo(jacksonSerDe.serialize(expected));
  }

  @Test
  public void validateObject_IncorrectMetadataGiven_ShouldReturnFaultyState() {
    // Arrange
    prepareObject();
    JsonNode validateContractArguments =
        mapper
            .createObjectNode()
            .put(OBJECT_ID, SOME_OBJECT_ID)
            .set(
                VERSIONS,
                mapper
                    .createArrayNode()
                    .add(createVersion(SOME_VERSION_ID_2, SOME_HASH_VALUE_2, SOME_METADATA_2))
                    .add(createVersion(SOME_VERSION_ID_1, SOME_HASH_VALUE_1, SOME_METADATA_0))
                    .add(createVersion(SOME_VERSION_ID_0, SOME_HASH_VALUE_0)));
    JsonNode expected =
        mapper
            .createObjectNode()
            .put(STATUS, STATUS_FAULTY)
            .put(DETAILS, DETAILS_FAULTY_VERSIONS_EXIST)
            .set(FAULTY_VERSIONS, mapper.createArrayNode().add(SOME_VERSION_ID_1));

    // Act
    ContractExecutionResult actual =
        clientService.executeContract(ID_OBJECT_VALIDATE, validateContractArguments);

    // Assert
    assertThat(actual.getContractResult()).isPresent();
    assertThat(actual.getContractResult().get()).isEqualTo(jacksonSerDe.serialize(expected));
  }

  @Test
  public void validateObject_IncorrectNumberOfVersionsGiven_ShouldReturnFaultyState() {
    // Arrange
    prepareObject();
    ObjectNode validateContractArguments = mapper.createObjectNode();
    validateContractArguments.put(OBJECT_ID, SOME_OBJECT_ID);
    validateContractArguments.set(
        VERSIONS,
        mapper
            .createArrayNode()
            .add(createVersion(SOME_VERSION_ID_2, SOME_HASH_VALUE_2))
            .add(createVersion(SOME_VERSION_ID_1, SOME_HASH_VALUE_1)));
    validateContractArguments.set(OPTIONS, mapper.createObjectNode().put(OPTION_ALL, true));
    JsonNode expected =
        mapper
            .createObjectNode()
            .put(STATUS, STATUS_FAULTY)
            .put(DETAILS, DETAILS_NUMBER_OF_VERSIONS_MISMATCH)
            .set(FAULTY_VERSIONS, mapper.createArrayNode());

    // Act
    ContractExecutionResult actual =
        clientService.executeContract(ID_OBJECT_VALIDATE, validateContractArguments);

    // Assert
    assertThat(actual.getContractResult()).isPresent();
    assertThat(actual.getContractResult().get()).isEqualTo(jacksonSerDe.serialize(expected));
  }

  @Test
  public void validateObject_HashValuesWithoutVersionIdsGiven_ShouldThrowClientException() {
    // Arrange
    prepareObject();
    JsonNode validateContractArguments =
        mapper
            .createObjectNode()
            .put(OBJECT_ID, SOME_OBJECT_ID)
            .set(
                VERSIONS,
                mapper
                    .createArrayNode()
                    .add(SOME_HASH_VALUE_2)
                    .add(SOME_HASH_VALUE_1)
                    .add(SOME_HASH_VALUE_0));

    // Act Assert
    assertThatThrownBy(
            () -> clientService.executeContract(ID_OBJECT_VALIDATE, validateContractArguments))
        .isExactlyInstanceOf(ClientException.class)
        .hasMessage(INVALID_VERSIONS_FORMAT);
  }

  @Test
  public void putObject_FunctionArgumentsGiven_ShouldPutRecordToFunctionTable()
      throws ExecutionException {
    // Arrange
    JsonNode contractArguments0 =
        mapper
            .createObjectNode()
            .put(OBJECT_ID, SOME_OBJECT_ID)
            .put(HASH_VALUE, SOME_HASH_VALUE_0)
            .set(METADATA, SOME_METADATA_0);
    JsonNode contractArguments1 =
        mapper
            .createObjectNode()
            .put(OBJECT_ID, SOME_OBJECT_ID)
            .put(HASH_VALUE, SOME_HASH_VALUE_1)
            .set(METADATA, SOME_METADATA_1);
    JsonNode functionArguments0 = createFunctionArguments(SOME_OBJECT_ID, SOME_VERSION_ID_0, 0, 1L);
    JsonNode functionArguments1 =
        createFunctionArguments(SOME_OBJECT_ID, SOME_VERSION_ID_1, 1, 1234567890123L);
    Scan scan =
        Scan.newBuilder()
            .namespace(getFunctionNamespace())
            .table(getFunctionTable())
            .partitionKey(Key.ofText(SOME_COLUMN_NAME_1, SOME_OBJECT_ID))
            .ordering(Ordering.asc(SOME_COLUMN_NAME_2))
            .build();

    // Act
    clientService.executeContract(
        ID_OBJECT_PUT, contractArguments0, ID_OBJECT_PUT_MUTABLE, functionArguments0);
    clientService.executeContract(
        ID_OBJECT_PUT, contractArguments1, ID_OBJECT_PUT_MUTABLE, functionArguments1);

    // Assert
    try (Scanner scanner = storage.scan(scan)) {
      List<Result> results = scanner.all();
      assertThat(results).hasSize(2);
      assertThat(results.get(0).getText(SOME_COLUMN_NAME_1)).isEqualTo(SOME_OBJECT_ID);
      assertThat(results.get(0).getText(SOME_COLUMN_NAME_2)).isEqualTo(SOME_VERSION_ID_0);
      assertThat(results.get(0).getInt(SOME_COLUMN_NAME_3)).isEqualTo(0);
      assertThat(results.get(0).getTimestamp(SOME_COLUMN_NAME_4)).isEqualTo(SOME_TIMESTAMP_VALUE);
      assertThat(results.get(1).getText(SOME_COLUMN_NAME_1)).isEqualTo(SOME_OBJECT_ID);
      assertThat(results.get(1).getText(SOME_COLUMN_NAME_2)).isEqualTo(SOME_VERSION_ID_1);
      assertThat(results.get(1).getInt(SOME_COLUMN_NAME_3)).isEqualTo(1);
      assertThat(results.get(1).getTimestamp(SOME_COLUMN_NAME_4)).isEqualTo(SOME_TIMESTAMP_VALUE);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void
      putObject_FunctionArgumentsGivenButSpecifiedTableNotExist_ShouldThrowClientExceptionWithInvalidFunctionException() {
    // Arrange
    JsonNode contractArguments =
        mapper
            .createObjectNode()
            .put(OBJECT_ID, SOME_OBJECT_ID)
            .put(HASH_VALUE, SOME_HASH_VALUE_0)
            .set(METADATA, SOME_METADATA_0);
    JsonNode functionArguments =
        mapper
            .createObjectNode()
            .put(NAMESPACE, getFunctionNamespace())
            .put(TABLE, "foo")
            .set(
                PARTITION_KEY,
                mapper.createArrayNode().add(createColumn(SOME_COLUMN_NAME_1, SOME_OBJECT_ID)));

    // Act Assert
    assertThatThrownBy(
            () ->
                clientService.executeContract(
                    ID_OBJECT_PUT, contractArguments, ID_OBJECT_PUT_MUTABLE, functionArguments))
        .isExactlyInstanceOf(ClientException.class)
        .hasMessage(
            LedgerError.OPERATION_FAILED_DUE_TO_ILLEGAL_ARGUMENT.buildMessage(
                CoreError.TABLE_NOT_FOUND.buildMessage(getFunctionNamespace() + ".foo")))
        .extracting("code")
        .isEqualTo(StatusCode.INVALID_FUNCTION);
  }

  @Test
  public void
      putObject_FunctionArgumentsGivenAndUncommittedRecordExists_ShouldThrowClientExceptionWithConflictException()
          throws ExecutionException {
    // Arrange
    JsonNode contractArguments0 =
        mapper
            .createObjectNode()
            .put(OBJECT_ID, SOME_OBJECT_ID)
            .put(HASH_VALUE, SOME_HASH_VALUE_0)
            .set(METADATA, SOME_METADATA_0);
    JsonNode contractArguments1 =
        mapper
            .createObjectNode()
            .put(OBJECT_ID, SOME_OBJECT_ID)
            .put(HASH_VALUE, SOME_HASH_VALUE_1)
            .set(METADATA, SOME_METADATA_1);
    JsonNode functionArguments0 = createFunctionArguments(SOME_OBJECT_ID, SOME_VERSION_ID_0, 0, 1L);
    JsonNode functionArguments1 = createFunctionArguments(SOME_OBJECT_ID, SOME_VERSION_ID_0, 1, 1L);
    Put put =
        Put.newBuilder()
            .namespace(getFunctionNamespace())
            .table(getFunctionTable())
            .partitionKey(Key.ofText(SOME_COLUMN_NAME_1, SOME_OBJECT_ID))
            .clusteringKey(Key.ofText(SOME_COLUMN_NAME_2, SOME_VERSION_ID_0))
            .intValue(METADATA_TX_STATE, TransactionState.PREPARED.get())
            .build();
    clientService.executeContract(
        ID_OBJECT_PUT, contractArguments0, ID_OBJECT_PUT_MUTABLE, functionArguments0);
    // Make the transaction prepared state and non-expired
    storage.put(put);
    storageAdmin.truncateTable(COORDINATOR_NAMESPACE, COORDINATOR_STATE_TABLE);

    // Act Assert
    assertThatThrownBy(
            () ->
                clientService.executeContract(
                    ID_OBJECT_PUT, contractArguments1, ID_OBJECT_PUT_MUTABLE, functionArguments1))
        .isExactlyInstanceOf(ClientException.class)
        .hasMessageStartingWith(
            LedgerError.OPERATION_FAILED_DUE_TO_CONFLICT.buildMessage(
                CoreError.CONSENSUS_COMMIT_READ_UNCOMMITTED_RECORD.buildMessage()))
        .extracting("code")
        .isEqualTo(StatusCode.CONFLICT);
  }

  @Test
  public void
      putObject_FunctionArgumentsWithInvalidTimeRelatedTypeFormatGiven_ShouldThrowClientException() {
    // Arrange
    JsonNode contractArguments =
        mapper
            .createObjectNode()
            .put(OBJECT_ID, SOME_OBJECT_ID)
            .put(HASH_VALUE, SOME_HASH_VALUE_0)
            .set(METADATA, SOME_METADATA_0);
    ObjectNode functionArguments =
        mapper
            .createObjectNode()
            .put(NAMESPACE, getFunctionNamespace())
            .put(TABLE, getFunctionTable());
    functionArguments.set(
        PARTITION_KEY,
        mapper.createArrayNode().add(createColumn(SOME_COLUMN_NAME_1, SOME_OBJECT_ID)));
    functionArguments.set(
        CLUSTERING_KEY,
        mapper.createArrayNode().add(createColumn(SOME_COLUMN_NAME_2, SOME_VERSION_ID_0)));
    functionArguments.set(
        COLUMNS,
        mapper
            .createArrayNode()
            .add(createColumn(SOME_COLUMN_NAME_3, 0))
            .add(createTimestampColumn(SOME_COLUMN_NAME_4, "2024-05-19")));

    // Act Assert
    assertThatThrownBy(
            () ->
                clientService.executeContract(
                    ID_OBJECT_PUT, contractArguments, ID_OBJECT_PUT_MUTABLE, functionArguments))
        .isExactlyInstanceOf(ClientException.class)
        .hasMessage(Constants.INVALID_PUT_MUTABLE_FUNCTION_ARGUMENT_FORMAT)
        .extracting("code")
        .isEqualTo(StatusCode.CONTRACT_CONTEXTUAL_ERROR);
  }

  @Test
  public void putObject_InvalidMetadataGiven_ShouldThrowClientException() {
    // Arrange
    JsonNode putArguments =
        mapper
            .createObjectNode()
            .put(OBJECT_ID, SOME_COLLECTION_ID)
            .put(HASH_VALUE, SOME_HASH_VALUE_0)
            .put(METADATA, SOME_VERSION_ID_0);

    // Act Assert
    assertThatThrownBy(() -> clientService.executeContract(ID_OBJECT_PUT, putArguments))
        .isExactlyInstanceOf(ClientException.class)
        .hasMessage(INVALID_METADATA_FORMAT);
  }

  @Test
  public void putObject_SameIdCollectionGivenBefore_ShouldPutObjectWithoutEffectForCollection() {
    // Arrange
    prepareCollection();
    JsonNode putArguments =
        mapper
            .createObjectNode()
            .put(OBJECT_ID, SOME_COLLECTION_ID)
            .put(HASH_VALUE, SOME_HASH_VALUE_0)
            .set(METADATA, SOME_METADATA_0);
    JsonNode getObjectArguments = mapper.createObjectNode().put(OBJECT_ID, SOME_COLLECTION_ID);
    JsonNode getCollectionArguments =
        mapper.createObjectNode().put(COLLECTION_ID, SOME_COLLECTION_ID);
    Set<String> expectedSet = new HashSet<>();
    SOME_DEFAULT_OBJECT_IDS.forEach(id -> expectedSet.add(id.textValue()));

    // Act
    clientService.executeContract(ID_OBJECT_PUT, putArguments);

    // Assert
    ContractExecutionResult object =
        clientService.executeContract(ID_OBJECT_GET, getObjectArguments);
    assertThat(object.getContractResult()).isPresent();
    JsonNode objectJson = jacksonSerDe.deserialize(object.getContractResult().get());
    assertThat(objectJson.get(OBJECT_ID).textValue()).isEqualTo(SOME_COLLECTION_ID);
    assertThat(objectJson.get(HASH_VALUE).textValue()).isEqualTo(SOME_HASH_VALUE_0);
    assertThat(objectJson.get(METADATA)).isEqualTo(SOME_METADATA_0);
    ContractExecutionResult collection =
        clientService.executeContract(ID_COLLECTION_GET, getCollectionArguments);
    assertThat(collection.getContractResult()).isPresent();
    JsonNode collectionJson = jacksonSerDe.deserialize(collection.getContractResult().get());
    assertThat(toSetFrom(collectionJson.get(OBJECT_IDS))).isEqualTo(expectedSet);
  }

  @Test
  public void addToCollection_ThenGetCollectionGiven_ShouldReturnCorrectSet() {
    // Arrange
    prepareCollection();
    JsonNode getArguments = mapper.createObjectNode().put(COLLECTION_ID, SOME_COLLECTION_ID);
    Set<String> expectedSet = new HashSet<>();
    SOME_DEFAULT_OBJECT_IDS.forEach(id -> expectedSet.add(id.textValue()));
    SOME_ADD_OBJECT_IDS_ARRAY.forEach(id -> expectedSet.add(id.textValue()));

    // Act
    addObjectsToCollection(SOME_COLLECTION_ID, SOME_ADD_OBJECT_IDS_ARRAY);
    ContractExecutionResult actual = clientService.executeContract(ID_COLLECTION_GET, getArguments);

    // Assert
    assertThat(actual.getContractResult()).isPresent();
    JsonNode actualJson = jacksonSerDe.deserialize(actual.getContractResult().get());
    assertThat(toSetFrom(actualJson.get(OBJECT_IDS))).isEqualTo(expectedSet);
  }

  @Test
  public void addToCollection_ExistingObjectIdGivenWithoutForceOption_ShouldThrowClientException() {
    // Arrange
    prepareCollection();
    addObjectsToCollection(SOME_COLLECTION_ID, mapper.createArrayNode().add(SOME_OBJECT_ID));
    JsonNode addArguments =
        mapper
            .createObjectNode()
            .put(COLLECTION_ID, SOME_COLLECTION_ID)
            .set(OBJECT_IDS, mapper.createArrayNode().add(SOME_OBJECT_ID));

    // Act Assert
    assertThatThrownBy(() -> clientService.executeContract(ID_COLLECTION_ADD_OBJECTS, addArguments))
        .isExactlyInstanceOf(ClientException.class)
        .hasMessage(OBJECT_ALREADY_EXISTS_IN_COLLECTION);
  }

  @Test
  public void removeFromCollection_ThenGetCollectionGiven_ShouldReturnCorrectSet() {
    // Arrange
    prepareCollection();
    JsonNode getArguments = mapper.createObjectNode().put(COLLECTION_ID, SOME_COLLECTION_ID);
    Set<String> expectedSet = new HashSet<>();
    SOME_DEFAULT_OBJECT_IDS.forEach(id -> expectedSet.add(id.textValue()));
    SOME_REMOVE_OBJECT_IDS_ARRAY.forEach(id -> expectedSet.remove(id.textValue()));

    // Act
    removeObjectsFromCollection(SOME_COLLECTION_ID, SOME_REMOVE_OBJECT_IDS_ARRAY);
    ContractExecutionResult actual = clientService.executeContract(ID_COLLECTION_GET, getArguments);

    // Assert
    assertThat(actual.getContractResult()).isPresent();
    JsonNode actualJson = jacksonSerDe.deserialize(actual.getContractResult().get());
    assertThat(toSetFrom(actualJson.get(OBJECT_IDS))).isEqualTo(expectedSet);
  }

  @Test
  public void
      removeFromCollection_NotExistingObjectIdGivenWithoutForceOption_ShouldThrowClientException() {
    // Arrange
    prepareCollection();
    JsonNode removeArguments =
        mapper
            .createObjectNode()
            .put(COLLECTION_ID, SOME_COLLECTION_ID)
            .set(OBJECT_IDS, mapper.createArrayNode().add(SOME_OBJECT_ID));

    // Act Assert
    assertThatThrownBy(
            () -> clientService.executeContract(ID_COLLECTION_DEL_OBJECTS, removeArguments))
        .isExactlyInstanceOf(ClientException.class)
        .hasMessage(OBJECT_NOT_FOUND_IN_COLLECTION);
  }

  @Test
  public void getCollection_CollectionAfterCheckpointAgeGiven_ShouldReturnCorrectSet() {
    // Arrange
    prepareCollection();
    JsonNode getArguments = mapper.createObjectNode().put(COLLECTION_ID, SOME_COLLECTION_ID);
    IntStream.range(0, DEFAULT_COLLECTION_CHECKPOINT_INTERVAL)
        .forEach(
            i -> addObjectsToCollection(SOME_COLLECTION_ID, ImmutableSet.of(String.valueOf(i))));
    IntStream.range(0, DEFAULT_COLLECTION_CHECKPOINT_INTERVAL)
        .forEach(
            i ->
                removeObjectsFromCollection(
                    SOME_COLLECTION_ID, ImmutableSet.of(String.valueOf(i))));
    Set<String> expectedSet = new HashSet<>();
    SOME_DEFAULT_OBJECT_IDS.forEach(id -> expectedSet.add(id.textValue()));

    // Act
    ContractExecutionResult actual = clientService.executeContract(ID_COLLECTION_GET, getArguments);

    // Assert
    assertThat(actual.getContractResult()).isPresent();
    JsonNode actualJson = jacksonSerDe.deserialize(actual.getContractResult().get());
    assertThat(toSetFrom(actualJson.get(OBJECT_IDS))).isEqualTo(expectedSet);
  }

  @Test
  public void getCollection_EmptySetsAddedAndRemovedBefore_ShouldReturnInitialSet() {
    // Arrange
    prepareCollection();
    IntStream.range(0, DEFAULT_COLLECTION_CHECKPOINT_INTERVAL)
        .forEach(i -> addObjectsToCollection(SOME_COLLECTION_ID, ImmutableSet.of()));
    IntStream.range(0, DEFAULT_COLLECTION_CHECKPOINT_INTERVAL)
        .forEach(i -> removeObjectsFromCollection(SOME_COLLECTION_ID, ImmutableSet.of()));
    JsonNode getArguments = mapper.createObjectNode().put(COLLECTION_ID, SOME_COLLECTION_ID);
    Set<String> expectedSet = new HashSet<>();
    SOME_DEFAULT_OBJECT_IDS.forEach(id -> expectedSet.add(id.textValue()));

    // Act
    ContractExecutionResult actual = clientService.executeContract(ID_COLLECTION_GET, getArguments);

    // Assert
    assertThat(actual.getContractResult()).isPresent();
    JsonNode actualJson = jacksonSerDe.deserialize(actual.getContractResult().get());
    assertThat(toSetFrom(actualJson.get(OBJECT_IDS))).isEqualTo(expectedSet);
  }

  @Test
  public void getCollectionHistory_AddAndRemoveOperationsGivenBefore_ShouldReturnCorrectHistory() {
    // Arrange
    prepareCollection();
    addObjectsToCollection(SOME_COLLECTION_ID, SOME_ADD_OBJECT_IDS_ARRAY);
    removeObjectsFromCollection(SOME_COLLECTION_ID, SOME_REMOVE_OBJECT_IDS_ARRAY);
    Set<String> expectedSet0 = toSetFrom(SOME_DEFAULT_OBJECT_IDS);
    Set<String> expectedSet1 = toSetFrom(SOME_ADD_OBJECT_IDS_ARRAY);
    Set<String> expectedSet2 = toSetFrom(SOME_REMOVE_OBJECT_IDS_ARRAY);

    // Act
    ContractExecutionResult result =
        clientService.executeContract(
            ID_COLLECTION_GET_HISTORY,
            mapper.createObjectNode().put(COLLECTION_ID, SOME_COLLECTION_ID));

    // Assert
    assertThat(result.getContractResult()).isPresent();
    JsonNode actual = jacksonSerDe.deserialize(result.getContractResult().get());
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
    addObjectsToCollection(SOME_COLLECTION_ID, SOME_ADD_OBJECT_IDS_ARRAY);
    removeObjectsFromCollection(SOME_COLLECTION_ID, SOME_REMOVE_OBJECT_IDS_ARRAY);
    Set<String> expectedSet2 = toSetFrom(SOME_REMOVE_OBJECT_IDS_ARRAY);
    JsonNode arguments =
        mapper
            .createObjectNode()
            .put(COLLECTION_ID, SOME_COLLECTION_ID)
            .set(OPTIONS, SOME_LIMIT_OPTION);

    // Act
    ContractExecutionResult result =
        clientService.executeContract(ID_COLLECTION_GET_HISTORY, arguments);

    // Assert
    assertThat(result.getContractResult()).isPresent();
    JsonNode actual = jacksonSerDe.deserialize(result.getContractResult().get());
    assertThat(actual.get(COLLECTION_ID).textValue()).isEqualTo(SOME_COLLECTION_ID);
    ArrayNode actualEvents = (ArrayNode) actual.get(COLLECTION_EVENTS);
    assertThat(actualEvents.size()).isEqualTo(1);
    assertThat(actualEvents.get(0).get(OPERATION_TYPE).textValue()).isEqualTo(OPERATION_REMOVE);
    assertThat(toSetFrom(actualEvents.get(0).get(OBJECT_IDS))).isEqualTo(expectedSet2);
    assertThat(actualEvents.get(0).get(COLLECTION_AGE).intValue()).isEqualTo(2);
  }

  @Test
  public void getCollection_CustomCheckpointAgeGiven_ShouldExistSnapshotInCheckpointAge()
      throws ExecutionException {
    // Arrange
    Get get =
        Get.newBuilder()
            .namespace(getScalarNamespace())
            .table(getAssetTable())
            .partitionKey(Key.ofText(ASSET_ID, COLLECTION_ID_PREFIX + SOME_COLLECTION_ID))
            .clusteringKey(Key.ofInt(ASSET_AGE, 2))
            .build();
    Set<String> expectedSnapshot = new HashSet<>();
    SOME_DEFAULT_OBJECT_IDS.forEach(id -> expectedSnapshot.add(id.textValue()));
    SOME_ADD_OBJECT_IDS_ARRAY.forEach(id -> expectedSnapshot.add(id.textValue()));

    // Act
    prepareCollection(anotherClientService);
    addObjectsToCollection(anotherClientService, SOME_COLLECTION_ID, SOME_ADD_OBJECT_IDS_ARRAY);
    removeObjectsFromCollection(
        anotherClientService, SOME_COLLECTION_ID, SOME_REMOVE_OBJECT_IDS_ARRAY);

    // Assert
    Optional<Result> latestAsset = storage.get(get);
    assertThat(latestAsset.isPresent()).isTrue();
    JsonNode latestCollection = jacksonSerDe.deserialize(latestAsset.get().getText(ASSET_OUTPUT));
    assertThat(latestCollection.get(COLLECTION_ID).textValue()).isEqualTo(SOME_COLLECTION_ID);
    assertThat(latestCollection.get(OPERATION_TYPE).textValue()).isEqualTo(OPERATION_REMOVE);
    assertThat(latestCollection.get(OBJECT_IDS)).isEqualTo(SOME_REMOVE_OBJECT_IDS_ARRAY);
    assertThat(toSetFrom(latestCollection.get(COLLECTION_SNAPSHOT))).isEqualTo(expectedSnapshot);
  }

  @Test
  public void getCollection_CheckpointMissingDueToDifferentIntervalGiven_ShouldThrowException() {
    // Arrange
    prepareCollection(); // by default entity
    addObjectsToCollection(SOME_COLLECTION_ID, SOME_ADD_OBJECT_IDS_ARRAY);
    removeObjectsFromCollection(SOME_COLLECTION_ID, SOME_REMOVE_OBJECT_IDS_ARRAY);
    JsonNode getArguments = mapper.createObjectNode().put(COLLECTION_ID, SOME_COLLECTION_ID);

    // Act Assert
    assertThatThrownBy(() -> anotherClientService.executeContract(ID_COLLECTION_GET, getArguments))
        .isExactlyInstanceOf(ClientException.class)
        .hasMessage(INVALID_CHECKPOINT);
  }

  @Test
  public void getCollection_CheckpointExistsButDifferentIntervalGiven_ShouldThrowException() {
    // Arrange
    prepareCollection(); // by default entity
    addObjectsToCollection(SOME_COLLECTION_ID, SOME_ADD_OBJECT_IDS_ARRAY);
    JsonNode getArguments = mapper.createObjectNode().put(COLLECTION_ID, SOME_COLLECTION_ID);

    // Act Assert
    assertThatThrownBy(() -> anotherClientService.executeContract(ID_COLLECTION_GET, getArguments))
        .isExactlyInstanceOf(ClientException.class)
        .hasMessage(INVALID_CHECKPOINT);
  }

  @Test
  public void addToCollection_CheckpointExistsButDifferentIntervalGiven_ShouldThrowException() {
    // Arrange
    prepareCollection(); // by default entity
    addObjectsToCollection(SOME_COLLECTION_ID, SOME_ADD_OBJECT_IDS_ARRAY);
    JsonNode addArguments =
        mapper
            .createObjectNode()
            .put(COLLECTION_ID, SOME_COLLECTION_ID)
            .set(OBJECT_IDS, mapper.createArrayNode().add("object9"));

    // Act Assert
    assertThatThrownBy(
            () -> anotherClientService.executeContract(ID_COLLECTION_ADD_OBJECTS, addArguments))
        .isExactlyInstanceOf(ClientException.class)
        .hasMessage(INVALID_CHECKPOINT);
  }

  @Test
  public void
      removeFromCollection_CheckpointExistsButDifferentIntervalGiven_ShouldThrowException() {
    // Arrange
    prepareCollection(); // by default entity
    addObjectsToCollection(SOME_COLLECTION_ID, SOME_ADD_OBJECT_IDS_ARRAY);
    JsonNode removeArguments =
        mapper
            .createObjectNode()
            .put(COLLECTION_ID, SOME_COLLECTION_ID)
            .set(OBJECT_IDS, mapper.createArrayNode().add("object1"));

    // Act Assert
    assertThatThrownBy(
            () -> anotherClientService.executeContract(ID_COLLECTION_DEL_OBJECTS, removeArguments))
        .isExactlyInstanceOf(ClientException.class)
        .hasMessage(INVALID_CHECKPOINT);
  }

  @Test
  public void
      createCollection_SameIdObjectGivenBefore_ShouldCreateCollectionWithoutEffectForObject() {
    // Arrange
    prepareObject();
    JsonNode createArguments =
        mapper
            .createObjectNode()
            .put(COLLECTION_ID, SOME_OBJECT_ID)
            .set(OBJECT_IDS, SOME_DEFAULT_OBJECT_IDS);
    JsonNode getCollectionArguments = mapper.createObjectNode().put(COLLECTION_ID, SOME_OBJECT_ID);
    JsonNode getObjectArguments = mapper.createObjectNode().put(OBJECT_ID, SOME_OBJECT_ID);
    Set<String> expectedSet = new HashSet<>();
    SOME_DEFAULT_OBJECT_IDS.forEach(id -> expectedSet.add(id.textValue()));

    // Act
    clientService.executeContract(ID_COLLECTION_CREATE, createArguments);

    // Assert
    ContractExecutionResult collection =
        clientService.executeContract(ID_COLLECTION_GET, getCollectionArguments);
    assertThat(collection.getContractResult()).isPresent();
    JsonNode collectionJson = jacksonSerDe.deserialize(collection.getContractResult().get());
    assertThat(toSetFrom(collectionJson.get(OBJECT_IDS))).isEqualTo(expectedSet);
    ContractExecutionResult object =
        clientService.executeContract(ID_OBJECT_GET, getObjectArguments);
    assertThat(object.getContractResult()).isPresent();
    JsonNode objectJson = jacksonSerDe.deserialize(object.getContractResult().get());
    assertThat(objectJson.get(OBJECT_ID).textValue()).isEqualTo(SOME_OBJECT_ID);
    assertThat(objectJson.get(HASH_VALUE).textValue()).isEqualTo(SOME_HASH_VALUE_2);
    assertThat(objectJson.get(METADATA)).isEqualTo(SOME_METADATA_2);
  }

  @Test
  public void validateLedger_ObjectGiven_ShouldReturnCorrectResult() {
    // Arrange
    prepareObject();

    // Act
    LedgerValidationResult actual =
        clientService.validateLedger(AssetType.OBJECT, ImmutableList.of(SOME_OBJECT_ID));

    // Assert
    assertThat(actual.getCode()).isEqualTo(StatusCode.OK);
    assertThat(actual.getLedgerProof().isPresent()).isTrue();
    assertThat(actual.getLedgerProof().get().getId()).isEqualTo(OBJECT_ID_PREFIX + SOME_OBJECT_ID);
    assertThat(actual.getLedgerProof().get().getAge()).isEqualTo(2);
    assertThat(actual.getAuditorProof().isPresent()).isFalse();
  }

  @Test
  public void validateLedger_CollectionGiven_ShouldReturnCorrectResult() {
    // Arrange
    prepareCollection();

    // Act
    LedgerValidationResult actual =
        clientService.validateLedger(AssetType.COLLECTION, ImmutableList.of(SOME_COLLECTION_ID));

    // Assert
    assertThat(actual.getCode()).isEqualTo(StatusCode.OK);
    assertThat(actual.getLedgerProof().isPresent()).isTrue();
    assertThat(actual.getLedgerProof().get().getId())
        .isEqualTo(COLLECTION_ID_PREFIX + SOME_COLLECTION_ID);
    assertThat(actual.getLedgerProof().get().getAge()).isEqualTo(0);
    assertThat(actual.getAuditorProof().isPresent()).isFalse();
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
