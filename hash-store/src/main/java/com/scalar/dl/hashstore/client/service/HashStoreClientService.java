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
import static com.scalar.dl.genericcontracts.object.Constants.TIMESTAMPTZ_FORMATTER;
import static com.scalar.dl.genericcontracts.object.Constants.TIMESTAMP_FORMATTER;
import static com.scalar.dl.genericcontracts.object.Constants.TIME_FORMATTER;
import static com.scalar.dl.genericcontracts.object.Constants.VALUE;
import static com.scalar.dl.genericcontracts.object.Constants.VERSIONS;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.scalar.db.api.Put;
import com.scalar.db.io.Column;
import com.scalar.db.io.DataType;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.client.util.Common;
import com.scalar.dl.genericcontracts.collection.v1_0_0.Add;
import com.scalar.dl.genericcontracts.collection.v1_0_0.Create;
import com.scalar.dl.genericcontracts.collection.v1_0_0.GetCheckpointInterval;
import com.scalar.dl.genericcontracts.collection.v1_0_0.GetHistory;
import com.scalar.dl.genericcontracts.collection.v1_0_0.Remove;
import com.scalar.dl.genericcontracts.object.v1_0_0.PutToMutableDatabase;
import com.scalar.dl.genericcontracts.object.v1_0_0.Validate;
import com.scalar.dl.hashstore.client.error.HashStoreClientError;
import com.scalar.dl.hashstore.client.model.Version;
import com.scalar.dl.hashstore.client.util.HashStoreClientUtils;
import com.scalar.dl.ledger.config.AuthenticationMethod;
import com.scalar.dl.ledger.model.ExecutionResult;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.ledger.service.StatusCode;
import java.util.List;
import java.util.Map;
import javax.annotation.concurrent.Immutable;
import javax.json.JsonObject;

/**
 * A thread-safe client for the hash store. The client interacts with Ledger and Auditor components
 * to register identities, manage objects and collections, and validate them.
 *
 * <h3>Usage Examples</h3>
 *
 * Here is a simple example to demonstrate how to use {@code HashStoreClientService}. {@code
 * HashStoreClientService} should always be created with {@link HashStoreClientServiceFactory},
 * which reuses internal instances as much as possible for better performance and less resource
 * usage. When you create {@code HashStoreClientService}, the client certificate or secret key and
 * the necessary contracts for using the hash store are automatically registered by default based on
 * the configuration in {@code ClientConfig}.
 *
 * <pre>{@code
 * HashStoreClientServiceFactory factory = new HashStoreClientServiceFactory(); // the factory should be reused
 *
 * HashStoreClientService service = factory.create(new ClientConfig(new File(properties));
 * try {
 *   service.putObject(objectId, hash);
 * } catch (ClientException e) {
 *   System.err.println(e.getStatusCode());
 *   System.err.println(e.getMessage());
 * }
 *
 * factory.close();
 * }</pre>
 */
@Immutable
public class HashStoreClientService {
  private static final String CONTRACT_OBJECT_GET =
      com.scalar.dl.genericcontracts.object.Constants.CONTRACT_GET;
  private static final String CONTRACT_COLLECTION_GET =
      com.scalar.dl.genericcontracts.collection.Constants.CONTRACT_GET;
  private static final String OBJECT_OPTIONS =
      com.scalar.dl.genericcontracts.object.Constants.OPTIONS;
  private static final String COLLECTION_OPTIONS =
      com.scalar.dl.genericcontracts.collection.Constants.OPTIONS;
  private static final ImmutableMap<String, Class<?>> CONTRACTS =
      ImmutableMap.<String, Class<?>>builder()
          .put(CONTRACT_OBJECT_GET, com.scalar.dl.genericcontracts.object.v1_0_0.Get.class)
          .put(CONTRACT_PUT, com.scalar.dl.genericcontracts.object.v1_0_0.Put.class)
          .put(CONTRACT_VALIDATE, Validate.class)
          .put(CONTRACT_ADD, Add.class)
          .put(CONTRACT_CREATE, Create.class)
          .put(CONTRACT_COLLECTION_GET, com.scalar.dl.genericcontracts.collection.v1_0_0.Get.class)
          .put(CONTRACT_GET_CHECKPOINT_INTERVAL, GetCheckpointInterval.class)
          .put(CONTRACT_GET_HISTORY, GetHistory.class)
          .put(CONTRACT_REMOVE, Remove.class)
          .build();
  private static final ImmutableMap<String, Class<?>> FUNCTIONS =
      ImmutableMap.<String, Class<?>>builder()
          .put(FUNCTION_PUT, PutToMutableDatabase.class)
          .build();

  private final ClientService clientService;
  private final ClientConfig config;

  /**
   * Constructs a {@code HashStoreClientService} for hash store with the specified primitive {@link
   * ClientService}.
   *
   * @param clientService a client service
   */
  public HashStoreClientService(ClientService clientService, ClientConfig config) {
    this.clientService = clientService;
    this.config = config;
  }

  /**
   * Registers the identity (certificate or secret key) and necessary contracts and functions for
   * the hash store client, based on {@code ClientConfig}. The authentication method (digital
   * signature or HMAC) is determined by the configuration. If the identity or contract is already
   * registered, it is simply skipped without throwing an exception.
   *
   * <p>This method is primarily for internal use, and users don't need to call it because the
   * identity and contracts are automatically registered when creating {@code
   * HashStoreClientService}. Breaking changes can and will be introduced to this method. Users
   * should not depend on it.
   *
   * @throws ClientException if a request fails for some reason
   */
  public void registerIdentity() {
    try {
      if (config.getAuthenticationMethod().equals(AuthenticationMethod.DIGITAL_SIGNATURE)) {
        clientService.registerCertificate();
      } else {
        clientService.registerSecret();
      }
    } catch (ClientException e) {
      if (!e.getStatusCode().equals(StatusCode.CERTIFICATE_ALREADY_REGISTERED)
          && !e.getStatusCode().equals(StatusCode.SECRET_ALREADY_REGISTERED)) {
        throw e;
      }
    }

    registerContracts();
    registerFunctions();
  }

  private void registerContracts() {
    for (Map.Entry<String, Class<?>> entry : CONTRACTS.entrySet()) {
      String contractId = entry.getKey();
      Class<?> clazz = entry.getValue();
      String contractBinaryName = clazz.getName();
      byte[] bytes = Common.getClassBytes(clazz);
      try {
        clientService.registerContract(contractId, contractBinaryName, bytes, (String) null);
      } catch (ClientException e) {
        if (!e.getStatusCode().equals(StatusCode.CONTRACT_ALREADY_REGISTERED)) {
          throw e;
        }
      }
    }
  }

  private void registerFunctions() {
    for (Map.Entry<String, Class<?>> entry : FUNCTIONS.entrySet()) {
      String functionId = entry.getKey();
      Class<?> clazz = entry.getValue();
      String contractBinaryName = clazz.getName();
      byte[] bytes = Common.getClassBytes(clazz);
      clientService.registerFunction(functionId, contractBinaryName, bytes);
    }
  }

  /**
   * Retrieves the latest version of the specified object from the hash store.
   *
   * @param objectId an object ID
   * @return {@link ExecutionResult} containing the object's hash value and metadata
   * @throws ClientException if a request fails for some reason
   */
  public ExecutionResult getObject(String objectId) {
    JsonNode arguments = HashStoreClientUtils.createObjectNode().put(OBJECT_ID, objectId);
    return new ExecutionResult(clientService.executeContract(CONTRACT_OBJECT_GET, arguments));
  }

  /**
   * Stores a new version of an object in the hash store with the specified object ID and hash
   * value.
   *
   * @param objectId an object ID
   * @param hash a hash value representing the content of the object
   * @return {@link ExecutionResult}
   * @throws ClientException if a request fails for some reason
   */
  public ExecutionResult putObject(String objectId, String hash) {
    JsonNode arguments = buildPutObjectArguments(objectId, hash);
    return new ExecutionResult(clientService.executeContract(CONTRACT_PUT, arguments));
  }

  /**
   * Stores a new version of an object in the hash store with the specified object ID, hash value,
   * and metadata.
   *
   * @param objectId an object ID
   * @param hash a hash value representing the content of the object
   * @param metadata metadata associated with the object as {@link JsonObject}
   * @return {@link ExecutionResult}
   * @throws ClientException if a request fails for some reason
   */
  public ExecutionResult putObject(String objectId, String hash, JsonObject metadata) {
    JsonNode arguments = buildPutObjectArguments(objectId, hash, metadata);
    return new ExecutionResult(clientService.executeContract(CONTRACT_PUT, arguments));
  }

  /**
   * Stores a new version of an object in the hash store with the specified object ID, hash value,
   * and metadata.
   *
   * @param objectId an object ID
   * @param hash a hash value representing the content of the object
   * @param metadata metadata associated with the object as {@link JsonNode}
   * @return {@link ExecutionResult}
   * @throws ClientException if a request fails for some reason
   */
  public ExecutionResult putObject(String objectId, String hash, JsonNode metadata) {
    JsonNode arguments = buildPutObjectArguments(objectId, hash, metadata);
    return new ExecutionResult(clientService.executeContract(CONTRACT_PUT, arguments));
  }

  /**
   * Stores a new version of an object in the hash store with the specified object ID, hash value,
   * and metadata.
   *
   * @param objectId an object ID
   * @param hash a hash value representing the content of the object
   * @param metadata metadata associated with the object as JSON string
   * @return {@link ExecutionResult}
   * @throws ClientException if a request fails for some reason
   */
  public ExecutionResult putObject(String objectId, String hash, String metadata) {
    JsonNode arguments = buildPutObjectArguments(objectId, hash, metadata);
    return new ExecutionResult(clientService.executeContract(CONTRACT_PUT, arguments));
  }

  /**
   * Stores a new version of an object in the hash store and also performs a put operation to a
   * mutable database. This method allows atomic updates to both the hash store and a mutable
   * database.
   *
   * @param objectId an object ID
   * @param hash a hash value representing the content of the object
   * @param putToMutable a {@link Put} operation to be executed on the mutable database
   * @return {@link ExecutionResult}
   * @throws ClientException if a request fails for some reason
   * @throws IllegalArgumentException if the Put operation lacks namespace or table information
   */
  public ExecutionResult putObject(String objectId, String hash, Put putToMutable) {
    JsonNode contractArguments = buildPutObjectArguments(objectId, hash);
    JsonNode functionArguments = buildPutToMutableDatabaseArguments(putToMutable);
    return new ExecutionResult(
        clientService.executeContract(
            CONTRACT_PUT, contractArguments, FUNCTION_PUT, functionArguments));
  }

  /**
   * Stores a new version of an object in the hash store with metadata and also performs a put
   * operation to a mutable database. This method allows atomic updates to both the hash store and a
   * mutable database.
   *
   * @param objectId an object ID
   * @param hash a hash value representing the content of the object
   * @param metadata metadata associated with the object as {@link JsonObject}
   * @param putToMutable a {@link Put} operation to be executed on the mutable database
   * @return {@link ExecutionResult}
   * @throws ClientException if a request fails for some reason
   * @throws IllegalArgumentException if the Put operation lacks namespace or table information
   */
  public ExecutionResult putObject(
      String objectId, String hash, JsonObject metadata, Put putToMutable) {
    JsonNode contractArguments = buildPutObjectArguments(objectId, hash, metadata);
    JsonNode functionArguments = buildPutToMutableDatabaseArguments(putToMutable);
    return new ExecutionResult(
        clientService.executeContract(
            CONTRACT_PUT, contractArguments, FUNCTION_PUT, functionArguments));
  }

  /**
   * Stores a new version of an object in the hash store with metadata and also performs a put
   * operation to a mutable database. This method allows atomic updates to both the hash store and a
   * mutable database.
   *
   * @param objectId an object ID
   * @param hash a hash value representing the content of the object
   * @param metadata metadata associated with the object as {@link JsonNode}
   * @param putToMutable a {@link Put} operation to be executed on the mutable database
   * @return {@link ExecutionResult}
   * @throws ClientException if a request fails for some reason
   * @throws IllegalArgumentException if the Put operation lacks namespace or table information
   */
  public ExecutionResult putObject(
      String objectId, String hash, JsonNode metadata, Put putToMutable) {
    JsonNode contractArguments = buildPutObjectArguments(objectId, hash, metadata);
    JsonNode functionArguments = buildPutToMutableDatabaseArguments(putToMutable);
    return new ExecutionResult(
        clientService.executeContract(
            CONTRACT_PUT, contractArguments, FUNCTION_PUT, functionArguments));
  }

  /**
   * Stores a new version of an object in the hash store with metadata and also performs a put
   * operation to a mutable database. This method allows atomic updates to both the hash store and a
   * mutable database.
   *
   * @param objectId an object ID
   * @param hash a hash value representing the content of the object
   * @param metadata metadata associated with the object as JSON string
   * @param putToMutable a {@link Put} operation to be executed on the mutable database
   * @return {@link ExecutionResult}
   * @throws ClientException if a request fails for some reason
   * @throws IllegalArgumentException if the Put operation lacks namespace or table information
   */
  public ExecutionResult putObject(
      String objectId, String hash, String metadata, Put putToMutable) {
    JsonNode contractArguments = buildPutObjectArguments(objectId, hash, metadata);
    JsonNode functionArguments = buildPutToMutableDatabaseArguments(putToMutable);
    return new ExecutionResult(
        clientService.executeContract(
            CONTRACT_PUT, contractArguments, FUNCTION_PUT, functionArguments));
  }

  /**
   * Stores a new version of an object in the hash store using JsonNode arguments. Expected format:
   * {"objectId": "...", "hash": "...", "metadata": {...}}
   *
   * @param arguments JsonNode containing objectId, hash, and optional metadata
   * @return {@link ExecutionResult}
   * @throws ClientException if a request fails for some reason
   */
  public ExecutionResult putObject(JsonNode arguments) {
    return new ExecutionResult(clientService.executeContract(CONTRACT_PUT, arguments));
  }

  /**
   * Stores a new version of an object in the hash store and also performs a put operation to a
   * mutable database using JsonNode arguments.
   *
   * @param arguments JsonNode containing objectId, hash, and optional metadata
   * @param putToMutable JsonNode containing the Put operation data for the mutable database
   * @return {@link ExecutionResult}
   * @throws ClientException if a request fails for some reason
   */
  public ExecutionResult putObject(JsonNode arguments, JsonNode putToMutable) {
    return new ExecutionResult(
        clientService.executeContract(CONTRACT_PUT, arguments, FUNCTION_PUT, putToMutable));
  }

  private JsonNode buildPutObjectArguments(String objectId, String hash, JsonObject metadata) {
    return HashStoreClientUtils.createObjectNode()
        .put(OBJECT_ID, objectId)
        .put(HASH_VALUE, hash)
        .set(METADATA, HashStoreClientUtils.convertToJsonNode(metadata));
  }

  private ObjectNode buildPutObjectArguments(String objectId, String hash, JsonNode metadata) {
    return buildPutObjectArguments(objectId, hash).set(METADATA, metadata);
  }

  private ObjectNode buildPutObjectArguments(String objectId, String hash, String metadata) {
    return buildPutObjectArguments(objectId, hash)
        .set(METADATA, HashStoreClientUtils.convertToJsonNode(metadata));
  }

  private ObjectNode buildPutObjectArguments(String objectId, String hash) {
    return HashStoreClientUtils.createObjectNode().put(OBJECT_ID, objectId).put(HASH_VALUE, hash);
  }

  private JsonNode buildPutToMutableDatabaseArguments(Put put) {
    if (!put.forNamespace().isPresent() || !put.forTable().isPresent()) {
      throw new IllegalArgumentException(
          HashStoreClientError.PUT_MUST_HAVE_NAMESPACE_AND_TABLE.buildMessage());
    }

    ObjectNode arguments =
        HashStoreClientUtils.createObjectNode()
            .put(NAMESPACE, put.forNamespace().get())
            .put(TABLE, put.forTable().get());

    ArrayNode partitionKey = HashStoreClientUtils.createArrayNode();
    put.getPartitionKey()
        .getColumns()
        .forEach(
            column -> {
              partitionKey.add(buildColumn(column));
            });
    arguments.set(PARTITION_KEY, partitionKey);

    if (put.getClusteringKey().isPresent()) {
      ArrayNode clusteringKey = HashStoreClientUtils.createArrayNode();
      put.getClusteringKey()
          .get()
          .getColumns()
          .forEach(
              column -> {
                clusteringKey.add(buildColumn(column));
              });
      arguments.set(CLUSTERING_KEY, clusteringKey);
    }

    // Add columns if present
    ArrayNode columns = HashStoreClientUtils.createArrayNode();
    put.getColumns().forEach((key, column) -> columns.add(buildColumn(column)));
    arguments.set(COLUMNS, columns);

    return arguments;
  }

  private ObjectNode buildColumn(Column<?> column) {
    ObjectNode columnJson =
        HashStoreClientUtils.createObjectNode()
            .put(COLUMN_NAME, column.getName())
            .put(DATA_TYPE, column.getDataType().name());
    DataType type = column.getDataType();
    switch (type) {
      case BOOLEAN:
        return columnJson.put(VALUE, column.getBooleanValue());
      case INT:
        return columnJson.put(VALUE, column.getIntValue());
      case BIGINT:
        return columnJson.put(VALUE, column.getBigIntValue());
      case FLOAT:
        return columnJson.put(VALUE, column.getFloatValue());
      case DOUBLE:
        return columnJson.put(VALUE, column.getDoubleValue());
      case TEXT:
        return columnJson.put(VALUE, column.getTextValue());
      case BLOB:
        return columnJson.put(VALUE, column.getBlobValueAsBytes());
      case DATE:
        String date =
            column.getDateValue() == null ? null : column.getDateValue().format(DATE_FORMATTER);
        return columnJson.put(VALUE, date);
      case TIME:
        String time =
            column.getTimeValue() == null ? null : column.getTimeValue().format(TIME_FORMATTER);
        return columnJson.put(VALUE, time);
      case TIMESTAMP:
        String timestamp =
            column.getTimestampValue() == null
                ? null
                : column.getTimestampValue().format(TIMESTAMP_FORMATTER);
        return columnJson.put(VALUE, timestamp);
      case TIMESTAMPTZ:
        String timestampTz =
            column.getTimestampTZValue() == null
                ? null
                : TIMESTAMPTZ_FORMATTER.format(column.getTimestampTZValue());
        return columnJson.put(VALUE, timestampTz);
      default:
        throw new IllegalArgumentException(
            HashStoreClientError.UNSUPPORTED_DATA_TYPE_FOR_MUTABLE_PUT.buildMessage(type.name()));
    }
  }

  /**
   * Compares specific versions of an object to detect tampering. This method validates whether the
   * provided hash values match the stored versions in the ledger.
   *
   * @param objectId an object ID
   * @param versions a list of {@link Version} objects containing version ID and hash pairs to
   *     compare
   * @return {@link ExecutionResult} containing validation results
   * @throws ClientException if a request fails for some reason
   */
  public ExecutionResult compareObjectVersions(String objectId, List<Version> versions) {
    JsonNode arguments = buildCompareObjectVersionsArguments(objectId, versions);
    return new ExecutionResult(clientService.executeContract(CONTRACT_VALIDATE, arguments));
  }

  /**
   * Compares object versions to detect tampering using JsonNode arguments.
   *
   * <p>Options can include:
   *
   * <ul>
   *   <li>"verbose": true - shows detailed validation information
   *   <li>"all": true - compares all versions including stored versions in the ledger
   * </ul>
   *
   * <p>Example: {"objectId": "obj1", "versions": [{"versionId": "v1", "hash": "hash1"}], "options":
   * {"all": true, "verbose": true}}
   *
   * @param arguments JsonNode containing objectId, versions array, and options
   * @return {@link ExecutionResult} containing validation results
   * @throws ClientException if a request fails for some reason
   */
  public ExecutionResult compareObjectVersions(JsonNode arguments) {
    return new ExecutionResult(clientService.executeContract(CONTRACT_VALIDATE, arguments));
  }

  /**
   * Compares specific versions of an object to detect tampering with verbose output option.
   *
   * @param objectId an object ID
   * @param versions a list of {@link Version} objects containing version ID and hash pairs to
   *     compare
   * @param verbose if true, provides detailed validation information
   * @return {@link ExecutionResult} containing validation results
   * @throws ClientException if a request fails for some reason
   */
  public ExecutionResult compareObjectVersions(
      String objectId, List<Version> versions, boolean verbose) {
    JsonNode options = HashStoreClientUtils.createObjectNode().put(OPTION_VERBOSE, verbose);
    JsonNode arguments =
        buildCompareObjectVersionsArguments(objectId, versions).set(OBJECT_OPTIONS, options);
    return new ExecutionResult(clientService.executeContract(CONTRACT_VALIDATE, arguments));
  }

  /**
   * Compares all versions of an object, including both specified and stored versions in the ledger,
   * to detect tampering.
   *
   * @param objectId an object ID
   * @param versions a list of {@link Version} objects containing version ID and hash pairs to
   *     compare
   * @return {@link ExecutionResult} containing validation results for all versions
   * @throws ClientException if a request fails for some reason
   */
  public ExecutionResult compareAllObjectVersions(String objectId, List<Version> versions) {
    JsonNode options = HashStoreClientUtils.createObjectNode().put(OPTION_ALL, true);
    JsonNode arguments =
        buildCompareObjectVersionsArguments(objectId, versions).set(OBJECT_OPTIONS, options);
    return new ExecutionResult(clientService.executeContract(CONTRACT_VALIDATE, arguments));
  }

  /**
   * Compares all versions of an object, including both specified and stored versions in the ledger,
   * to detect tampering with verbose output option.
   *
   * @param objectId an object ID
   * @param versions a list of {@link Version} objects containing version ID and hash pairs to
   *     compare
   * @param verbose if true, provides detailed validation information
   * @return {@link ExecutionResult} containing validation results for all versions
   * @throws ClientException if a request fails for some reason
   */
  public ExecutionResult compareAllObjectVersions(
      String objectId, List<Version> versions, boolean verbose) {
    JsonNode options =
        HashStoreClientUtils.createObjectNode().put(OPTION_ALL, true).put(OPTION_VERBOSE, verbose);
    JsonNode arguments =
        buildCompareObjectVersionsArguments(objectId, versions).set(OBJECT_OPTIONS, options);
    return new ExecutionResult(clientService.executeContract(CONTRACT_VALIDATE, arguments));
  }

  private ObjectNode buildCompareObjectVersionsArguments(String objectId, List<Version> versions) {
    ArrayNode arrayNode = HashStoreClientUtils.createArrayNode();
    versions.forEach(version -> arrayNode.add(version.toObjectNode()));
    return HashStoreClientUtils.createObjectNode()
        .put(OBJECT_ID, objectId)
        .set(VERSIONS, arrayNode);
  }

  /**
   * Creates a new collection with the specified collection ID and initial set of object IDs.
   * Collections allow grouping related objects together for batch operations and validation.
   *
   * @param collectionId a collection ID
   * @param objectIds a list of object IDs to include in the collection
   * @return {@link ExecutionResult}
   * @throws ClientException if a request fails for some reason
   */
  public ExecutionResult createCollection(String collectionId, List<String> objectIds) {
    JsonNode arguments = buildCollection(collectionId, objectIds);
    return new ExecutionResult(clientService.executeContract(CONTRACT_CREATE, arguments));
  }

  /**
   * Retrieves the current state of the specified collection, including all object IDs currently in
   * the collection.
   *
   * @param collectionId a collection ID
   * @return {@link ExecutionResult} containing the collection's object IDs
   * @throws ClientException if a request fails for some reason
   */
  public ExecutionResult getCollection(String collectionId) {
    JsonNode arguments = HashStoreClientUtils.createObjectNode().put(COLLECTION_ID, collectionId);
    return new ExecutionResult(clientService.executeContract(CONTRACT_COLLECTION_GET, arguments));
  }

  /**
   * Adds the specified object IDs to an existing collection. If any object ID is already in the
   * collection, the operation will throw an exception.
   *
   * @param collectionId a collection ID
   * @param objectIds a list of object IDs to add to the collection
   * @return {@link ExecutionResult}
   * @throws ClientException if a request fails for some reason
   */
  public ExecutionResult addToCollection(String collectionId, List<String> objectIds) {
    JsonNode arguments = buildCollection(collectionId, objectIds);
    return new ExecutionResult(clientService.executeContract(CONTRACT_ADD, arguments));
  }

  /**
   * Adds the specified object IDs to an existing collection with force option.
   *
   * @param collectionId a collection ID
   * @param objectIds a list of object IDs to add to the collection
   * @param force if true, skips validation for duplicate object IDs that are already in the
   *     collection
   * @return {@link ExecutionResult}
   * @throws ClientException if a request fails for some reason
   */
  public ExecutionResult addToCollection(
      String collectionId, List<String> objectIds, boolean force) {
    JsonNode arguments =
        buildCollection(collectionId, objectIds).set(COLLECTION_OPTIONS, buildForceOption(force));
    return new ExecutionResult(clientService.executeContract(CONTRACT_ADD, arguments));
  }

  /**
   * Removes the specified object IDs from an existing collection. If any object ID is not in the
   * collection, the operation will throw an exception.
   *
   * @param collectionId a collection ID
   * @param objectIds a list of object IDs to remove from the collection
   * @return {@link ExecutionResult}
   * @throws ClientException if a request fails for some reason
   */
  public ExecutionResult removeFromCollection(String collectionId, List<String> objectIds) {
    JsonNode arguments = buildCollection(collectionId, objectIds);
    return new ExecutionResult(clientService.executeContract(CONTRACT_REMOVE, arguments));
  }

  /**
   * Removes the specified object IDs from an existing collection with force option.
   *
   * @param collectionId a collection ID
   * @param objectIds a list of object IDs to remove from the collection
   * @param force if true, skips validation for object IDs that are not in the collection
   * @return {@link ExecutionResult}
   * @throws ClientException if a request fails for some reason
   */
  public ExecutionResult removeFromCollection(
      String collectionId, List<String> objectIds, boolean force) {
    JsonNode arguments =
        buildCollection(collectionId, objectIds).set(COLLECTION_OPTIONS, buildForceOption(force));
    return new ExecutionResult(clientService.executeContract(CONTRACT_REMOVE, arguments));
  }

  /**
   * Retrieves the history of changes made to the specified collection, showing all add and remove
   * operations in chronological order.
   *
   * @param collectionId a collection ID
   * @return {@link ExecutionResult} containing the collection's change history
   * @throws ClientException if a request fails for some reason
   */
  public ExecutionResult getCollectionHistory(String collectionId) {
    JsonNode arguments = HashStoreClientUtils.createObjectNode().put(COLLECTION_ID, collectionId);
    return new ExecutionResult(clientService.executeContract(CONTRACT_GET_HISTORY, arguments));
  }

  /**
   * Retrieves the history of changes made to the specified collection with a limit on the number of
   * entries returned.
   *
   * @param collectionId a collection ID
   * @param limit maximum number of recent history entries to return (starting from the most recent)
   * @return {@link ExecutionResult} containing the collection's change history
   * @throws ClientException if a request fails for some reason
   */
  public ExecutionResult getCollectionHistory(String collectionId, int limit) {
    JsonNode arguments =
        HashStoreClientUtils.createObjectNode()
            .put(COLLECTION_ID, collectionId)
            .set(COLLECTION_OPTIONS, buildLimitOption(limit));
    return new ExecutionResult(clientService.executeContract(CONTRACT_GET_HISTORY, arguments));
  }

  private ObjectNode buildCollection(String collectionId, List<String> objectIds) {
    ArrayNode objectIdArray = HashStoreClientUtils.createArrayNode();
    objectIds.forEach(objectIdArray::add);
    return HashStoreClientUtils.createObjectNode()
        .put(COLLECTION_ID, collectionId)
        .set(OBJECT_IDS, objectIdArray);
  }

  private ObjectNode buildForceOption(boolean force) {
    return HashStoreClientUtils.createObjectNode().put(OPTION_FORCE, force);
  }

  private ObjectNode buildLimitOption(int limit) {
    return HashStoreClientUtils.createObjectNode().put(OPTION_LIMIT, limit);
  }

  /**
   * Validates the specified object in the ledger.
   *
   * @param objectId an object ID
   * @return {@link LedgerValidationResult}
   * @throws ClientException if a request fails for some reason
   */
  public LedgerValidationResult validateObject(String objectId) {
    return clientService.validateLedger(buildObjectAssetId(objectId));
  }

  /**
   * Validates the specified object in the ledger.
   *
   * @param objectId an object ID
   * @param startAge an age to be validated from (inclusive)
   * @param endAge an age to be validated to (inclusive)
   * @return {@link LedgerValidationResult}
   * @throws ClientException if a request fails for some reason
   */
  public LedgerValidationResult validateObject(String objectId, int startAge, int endAge) {
    return clientService.validateLedger(buildObjectAssetId(objectId), startAge, endAge);
  }

  /**
   * Validates the specified collection in the ledger.
   *
   * @param collectionId a collection ID
   * @return {@link LedgerValidationResult}
   * @throws ClientException if a request fails for some reason
   */
  public LedgerValidationResult validateCollection(String collectionId) {
    return clientService.validateLedger(buildCollectionAssetId(collectionId));
  }

  /**
   * Validates the specified collection in the ledger.
   *
   * @param collectionId a collection ID
   * @param startAge an age to be validated from (inclusive)
   * @param endAge an age to be validated to (inclusive)
   * @return {@link LedgerValidationResult}
   * @throws ClientException if a request fails for some reason
   */
  public LedgerValidationResult validateCollection(String collectionId, int startAge, int endAge) {
    return clientService.validateLedger(buildCollectionAssetId(collectionId), startAge, endAge);
  }

  private String buildObjectAssetId(String objectId) {
    return OBJECT_ID_PREFIX + objectId;
  }

  private String buildCollectionAssetId(String collectionId) {
    return COLLECTION_ID_PREFIX + collectionId;
  }
}
