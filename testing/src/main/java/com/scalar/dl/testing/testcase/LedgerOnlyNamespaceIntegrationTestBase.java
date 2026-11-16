package com.scalar.dl.testing.testcase;

import static com.scalar.dl.testing.contract.Constants.AMOUNT_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.contract.Constants.ASSETS_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.contract.Constants.ASSET_ID_SEPARATOR;
import static com.scalar.dl.testing.schema.SchemaConstants.ASSET_AGE_COLUMN_NAME;
import static com.scalar.dl.testing.schema.SchemaConstants.ASSET_ARGUMENT_COLUMN_NAME;
import static com.scalar.dl.testing.schema.SchemaConstants.ASSET_ID_COLUMN_NAME;
import static com.scalar.dl.testing.schema.SchemaConstants.ASSET_INPUT_COLUMN_NAME;
import static com.scalar.dl.testing.schema.SchemaConstants.ASSET_OUTPUT_COLUMN_NAME;
import static com.scalar.dl.testing.schema.SchemaConstants.ASSET_PREV_HASH_COLUMN_NAME;
import static com.scalar.dl.testing.schema.SchemaConstants.ASSET_TABLE;
import static com.scalar.dl.testing.schema.SchemaConstants.CONTRACT_CLASS_TABLE;
import static com.scalar.dl.testing.schema.SchemaConstants.CONTRACT_TABLE;
import static com.scalar.dl.testing.schema.SchemaConstants.NAMESPACE_DEFAULT_PARTITION_ID;
import static com.scalar.dl.testing.schema.SchemaConstants.NAMESPACE_IMPORTED_COLUMN_NAME;
import static com.scalar.dl.testing.schema.SchemaConstants.NAMESPACE_NAME_COLUMN_NAME;
import static com.scalar.dl.testing.schema.SchemaConstants.NAMESPACE_PARTITION_ID_COLUMN_NAME;
import static com.scalar.dl.testing.schema.SchemaConstants.NAMESPACE_TABLE;
import static com.scalar.dl.testing.schema.SchemaConstants.SCALAR_NAMESPACE;
import static com.scalar.dl.testing.schema.SchemaConstants.resolveNamespace;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scalar.db.api.Delete;
import com.scalar.db.api.DistributedStorage;
import com.scalar.db.api.Get;
import com.scalar.db.api.GetBuilder;
import com.scalar.db.api.Put;
import com.scalar.db.api.PutBuilder;
import com.scalar.db.api.Result;
import com.scalar.db.io.DataType;
import com.scalar.db.io.Key;
import com.scalar.db.service.StorageFactory;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.client.util.Common;
import com.scalar.dl.ledger.contract.ContractEntry;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.util.Argument;
import com.scalar.dl.testing.contract.Noop;
import com.scalar.dl.testing.util.TestCertificates;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.json.Json;
import javax.json.JsonObject;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Base class for Ledger-only Namespace integration tests.
 *
 * <p>This class contains tamper detection tests that are specific to Ledger-only configurations.
 * These tests verify that the Ledger can detect various types of data tampering (record removal,
 * hash tampering, output tampering, etc.) for namespace-aware assets.
 *
 * <p>These tests are NOT applicable to Auditor configurations because Auditor uses Byzantine Fault
 * Detection which has different validation semantics.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class LedgerOnlyNamespaceIntegrationTestBase
    extends LedgerNamespaceIntegrationTestBase {

  // ============ Namespace Management Tests (Ledger-only) ============

  /**
   * Runs last because it changes the schema of the namespace table. Ledger caches the table
   * metadata, so a test that creates an imported namespace could be rejected for a while if it ran
   * after the imported column is added back.
   */
  @Test
  @Order(Integer.MAX_VALUE)
  void createNamespace_NamespaceTableWithoutImportedColumnGiven_ShouldCreateOnlyRegularNamespace()
      throws Exception {
    // Arrange: make the namespace table the same as the one created with an older schema
    storageAdmin.dropColumnFromTable(
        SCALAR_NAMESPACE, NAMESPACE_TABLE, NAMESPACE_IMPORTED_COLUMN_NAME);

    try {
      // Act & Assert: creating an imported namespace is rejected without creating anything
      assertThatThrownBy(() -> clientServiceA.createNamespace(NAMESPACE_3, true))
          .isInstanceOf(ClientException.class)
          .satisfies(
              e ->
                  assertThat(((ClientException) e).getStatusCode())
                      .isEqualTo(StatusCode.INVALID_ARGUMENT));
      assertThat(namespaceEntryExists(NAMESPACE_3)).isFalse();
      assertThat(storageAdmin.namespaceExists(resolveNamespace(SCALAR_NAMESPACE, NAMESPACE_3)))
          .isFalse();

      // Act & Assert: creating a regular namespace still works
      clientServiceA.createNamespace(NAMESPACE_3);
      assertThat(namespaceEntryExists(NAMESPACE_3)).isTrue();
      clientServiceA.dropNamespace(NAMESPACE_3);
    } finally {
      storageAdmin.addNewColumnToTable(
          SCALAR_NAMESPACE, NAMESPACE_TABLE, NAMESPACE_IMPORTED_COLUMN_NAME, DataType.BOOLEAN);
    }
  }

  private boolean namespaceEntryExists(String namespace) throws Exception {
    // Use a new storage instance because the shared one caches the table metadata that has the
    // imported column, with which reading a record fails after the column is dropped.
    DistributedStorage freshStorage =
        StorageFactory.create(cluster.getStorageConfig().getPropertiesForHost()).getStorage();
    try {
      Get get =
          Get.newBuilder()
              .namespace(SCALAR_NAMESPACE)
              .table(NAMESPACE_TABLE)
              .partitionKey(
                  Key.ofInt(NAMESPACE_PARTITION_ID_COLUMN_NAME, NAMESPACE_DEFAULT_PARTITION_ID))
              .clusteringKey(Key.ofText(NAMESPACE_NAME_COLUMN_NAME, namespace))
              .build();
      return freshStorage.get(get).isPresent();
    } finally {
      freshStorage.close();
    }
  }

  // ============ Migrated Contract Tests (Ledger-only) ============

  /**
   * A contract registered in the default namespace and then copied to another namespace as it is
   * has a signature created for the default namespace. Ledger validates such a contract as the one
   * for the default namespace only when the Auditor is enabled, so it is rejected in the
   * Ledger-only configuration even in a namespace created for import.
   */
  @Test
  void
      executeContract_ContractMigratedToImportedNamespaceGiven_ShouldThrowClientExceptionWithInvalidContract()
          throws Exception {
    // Arrange
    String migratedContractId = "Migrated-" + UUID.randomUUID();
    String registeredContractId = "Registered-" + UUID.randomUUID();
    clientServiceA.registerContract(
        migratedContractId, Noop.class.getName(), Common.getClassBytes(Noop.class));
    clientServiceA.createNamespace(NAMESPACE_3, true);

    try {
      // The namespace is marked as imported, so the rejection below is due to the configuration
      assertThat(getNamespaceEntry(SCALAR_NAMESPACE, NAMESPACE_3))
          .hasValueSatisfying(
              entry -> assertThat(entry.getBoolean(NAMESPACE_IMPORTED_COLUMN_NAME)).isTrue());

      // The credential is migrated along with the contracts, so the same credential is used.
      ClientService namespaceClientServiceA = createClientServiceAFor(NAMESPACE_3);
      String physicalNamespace = resolveNamespace(SCALAR_NAMESPACE, NAMESPACE_3);
      Result contract =
          copyRecord(
              physicalNamespace,
              CONTRACT_TABLE,
              Key.ofText(ContractEntry.ENTITY_ID, TestCertificates.ENTITY_ID_A),
              Key.of(
                  ContractEntry.KEY_VERSION,
                  TestCertificates.KEY_VERSION,
                  ContractEntry.ID,
                  migratedContractId));
      copyRecord(
          physicalNamespace,
          CONTRACT_CLASS_TABLE,
          Key.ofText(ContractEntry.BINARY_NAME, contract.getText(ContractEntry.BINARY_NAME)),
          null);

      // Act & Assert
      assertThatThrownBy(
              () ->
                  namespaceClientServiceA.executeContract(
                      migratedContractId, mapper.createObjectNode()))
          .isInstanceOfSatisfying(
              ClientException.class,
              e -> assertThat(e.getStatusCode()).isEqualTo(StatusCode.INVALID_CONTRACT));

      // Control case: the same contract registered in the namespace can be executed
      namespaceClientServiceA.registerContract(
          registeredContractId, Noop.class.getName(), Common.getClassBytes(Noop.class));
      ContractExecutionResult result =
          namespaceClientServiceA.executeContract(registeredContractId, mapper.createObjectNode());
      assertThat(result.getContractResult()).isNotPresent();
    } finally {
      clientServiceA.dropNamespace(NAMESPACE_3);
    }
  }

  /** Copies a record in the default namespace to the specified namespace as it is. */
  private Result copyRecord(
      String destinationNamespace, String table, Key partitionKey, @Nullable Key clusteringKey)
      throws Exception {
    GetBuilder.BuildableGet get =
        Get.newBuilder().namespace(SCALAR_NAMESPACE).table(table).partitionKey(partitionKey);
    PutBuilder.Buildable put =
        Put.newBuilder().namespace(destinationNamespace).table(table).partitionKey(partitionKey);
    Set<String> keyColumnNames = new HashSet<>();
    partitionKey.getColumns().forEach(c -> keyColumnNames.add(c.getName()));
    if (clusteringKey != null) {
      get.clusteringKey(clusteringKey);
      put.clusteringKey(clusteringKey);
      clusteringKey.getColumns().forEach(c -> keyColumnNames.add(c.getName()));
    }
    Result record =
        storage
            .get(get.build())
            .orElseThrow(() -> new AssertionError("The record to copy does not exist"));
    record.getColumns().values().stream()
        .filter(c -> !keyColumnNames.contains(c.getName()))
        .forEach(put::value);
    storage.put(put.build());
    return record;
  }

  // ============ Tamper Detection Tests (Ledger-only) ============

  @Test
  void validateLedger_MiddleRecordRemovedWithNamespace_ShouldReturnInconsistentStates()
      throws Exception {
    // Arrange: Create assets and then maliciously remove a middle record
    createAssetsWithPaymentsInNamespaces();

    // Remove the middle record (age=1) of SOME_ASSET_ID_1 in NAMESPACE_1
    Delete delete =
        Delete.newBuilder()
            .namespace(resolveNamespace(SCALAR_NAMESPACE, NAMESPACE_1))
            .table(ASSET_TABLE)
            .partitionKey(Key.ofText(ASSET_ID_COLUMN_NAME, SOME_ASSET_ID_1))
            .clusteringKey(Key.ofInt(ASSET_AGE_COLUMN_NAME, 1))
            .build();
    storage.delete(delete);

    // Act
    LedgerValidationResult resultA = clientServiceA.validateLedger(NAMESPACE_1, SOME_ASSET_ID_1);
    LedgerValidationResult resultB = clientServiceA.validateLedger(NAMESPACE_2, SOME_ASSET_ID_2);

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.INCONSISTENT_STATES);
    assertThat(resultB.getCode()).isEqualTo(StatusCode.INCONSISTENT_STATES);
  }

  @Test
  void validateLedger_PrevHashTamperedWithNamespace_ShouldReturnInvalidPrevHash() throws Exception {
    // Arrange: Create assets and then tamper with prev_hash
    createAssetsWithPaymentsInNamespaces();

    // Tamper with prev_hash of the latest record (age=2) of SOME_ASSET_ID_1 in NAMESPACE_1
    Put put =
        Put.newBuilder()
            .namespace(resolveNamespace(SCALAR_NAMESPACE, NAMESPACE_1))
            .table(ASSET_TABLE)
            .partitionKey(Key.ofText(ASSET_ID_COLUMN_NAME, SOME_ASSET_ID_1))
            .clusteringKey(Key.ofInt(ASSET_AGE_COLUMN_NAME, 2))
            .blobValue(ASSET_PREV_HASH_COLUMN_NAME, "tampered".getBytes(StandardCharsets.UTF_8))
            .build();
    storage.put(put);

    // Act
    LedgerValidationResult resultA = clientServiceA.validateLedger(NAMESPACE_1, SOME_ASSET_ID_1);
    LedgerValidationResult resultB = clientServiceA.validateLedger(NAMESPACE_2, SOME_ASSET_ID_2);

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.INVALID_PREV_HASH);
    assertThat(resultB.getCode()).isEqualTo(StatusCode.OK);
  }

  @Test
  void validateLedger_OutputTamperedWithNamespace_ShouldReturnInvalidOutput() throws Exception {
    // Arrange: Create assets and then tamper with output
    createAssetsWithPaymentsInNamespaces();

    // Tamper with output of the latest record (age=2) of SOME_ASSET_ID_1 in NAMESPACE_1
    // Original balance should be 700 (1000 - 100 - 200), tampering to 7000
    Put put =
        Put.newBuilder()
            .namespace(resolveNamespace(SCALAR_NAMESPACE, NAMESPACE_1))
            .table(ASSET_TABLE)
            .partitionKey(Key.ofText(ASSET_ID_COLUMN_NAME, SOME_ASSET_ID_1))
            .clusteringKey(Key.ofInt(ASSET_AGE_COLUMN_NAME, 2))
            .textValue(ASSET_OUTPUT_COLUMN_NAME, "{\"balance\":7000}")
            .build();
    storage.put(put);

    // Act
    LedgerValidationResult resultA = clientServiceA.validateLedger(NAMESPACE_1, SOME_ASSET_ID_1);
    LedgerValidationResult resultB = clientServiceA.validateLedger(NAMESPACE_2, SOME_ASSET_ID_2);

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.INVALID_OUTPUT);
    assertThat(resultB.getCode()).isEqualTo(StatusCode.OK);
  }

  @Test
  void validateLedger_InputTamperedWithNamespace_ShouldReturnInvalidOutput() throws Exception {
    // Arrange: Create assets and then tamper with input
    createAssetsWithPaymentsInNamespaces();

    // Tamper with input of the latest record (age=2) of SOME_ASSET_ID_1 in NAMESPACE_1
    // Use V2 format with namespace but tamper the age values (correct age is 1, not 2)
    String tamperedInput =
        "{\""
            + NAMESPACE_1
            + "\":{\""
            + SOME_ASSET_ID_1
            + "\":{\"age\":2}},"
            + "\""
            + NAMESPACE_2
            + "\":{\""
            + SOME_ASSET_ID_2
            + "\":{\"age\":1}},"
            + "\"_version\":2}";
    Put put =
        Put.newBuilder()
            .namespace(resolveNamespace(SCALAR_NAMESPACE, NAMESPACE_1))
            .table(ASSET_TABLE)
            .partitionKey(Key.ofText(ASSET_ID_COLUMN_NAME, SOME_ASSET_ID_1))
            .clusteringKey(Key.ofInt(ASSET_AGE_COLUMN_NAME, 2))
            .textValue(ASSET_INPUT_COLUMN_NAME, tamperedInput)
            .build();
    storage.put(put);

    // Act
    LedgerValidationResult resultA = clientServiceA.validateLedger(NAMESPACE_1, SOME_ASSET_ID_1);
    LedgerValidationResult resultB = clientServiceA.validateLedger(NAMESPACE_2, SOME_ASSET_ID_2);

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.INVALID_OUTPUT);
    assertThat(resultB.getCode()).isEqualTo(StatusCode.OK);
  }

  @Test
  void validateLedger_ContractArgumentTamperedWithNamespace_ShouldReturnInvalidContract()
      throws Exception {
    // Arrange: Create assets and then tamper with contract argument
    createAssetsWithPaymentsInNamespaces();

    // Tamper with argument of the latest record (age=2) of SOME_ASSET_ID_1 in NAMESPACE_1
    // Change the amount to 0 instead of the original value
    JsonObject tamperedArgument =
        Json.createObjectBuilder()
            .add(
                ASSETS_ATTRIBUTE_NAME,
                Json.createArrayBuilder()
                    .add(NAMESPACE_1 + ASSET_ID_SEPARATOR + SOME_ASSET_ID_1)
                    .add(NAMESPACE_2 + ASSET_ID_SEPARATOR + SOME_ASSET_ID_2))
            .add(AMOUNT_ATTRIBUTE_NAME, 0)
            .add(Argument.NONCE_KEY_NAME, UUID.randomUUID().toString())
            .build();
    Put put =
        Put.newBuilder()
            .namespace(resolveNamespace(SCALAR_NAMESPACE, NAMESPACE_1))
            .table(ASSET_TABLE)
            .partitionKey(Key.ofText(ASSET_ID_COLUMN_NAME, SOME_ASSET_ID_1))
            .clusteringKey(Key.ofInt(ASSET_AGE_COLUMN_NAME, 2))
            .textValue(ASSET_ARGUMENT_COLUMN_NAME, tamperedArgument.toString())
            .build();
    storage.put(put);

    // Act
    LedgerValidationResult resultA = clientServiceA.validateLedger(NAMESPACE_1, SOME_ASSET_ID_1);
    LedgerValidationResult resultB = clientServiceA.validateLedger(NAMESPACE_2, SOME_ASSET_ID_2);

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.INVALID_CONTRACT);
    assertThat(resultB.getCode()).isEqualTo(StatusCode.OK);
  }

  @Test
  void validateLedger_PartialValidationWithNamespace_ShouldValidateCorrectRange() throws Exception {
    // Arrange: Create assets and then tamper with output
    createAssetsWithPaymentsInNamespaces();

    // Tamper with output of the latest record (age=2) of SOME_ASSET_ID_1 in NAMESPACE_1
    Put put =
        Put.newBuilder()
            .namespace(resolveNamespace(SCALAR_NAMESPACE, NAMESPACE_1))
            .table(ASSET_TABLE)
            .partitionKey(Key.ofText(ASSET_ID_COLUMN_NAME, SOME_ASSET_ID_1))
            .clusteringKey(Key.ofInt(ASSET_AGE_COLUMN_NAME, 2))
            .textValue(ASSET_OUTPUT_COLUMN_NAME, "{\"balance\":7000}")
            .build();
    storage.put(put);

    // Act: Validate with age range that does NOT include the tampered record (age 0-1)
    LedgerValidationResult resultNotIncluding =
        clientServiceA.validateLedger(NAMESPACE_1, SOME_ASSET_ID_1, 0, 1);

    // Act: Validate with age range that includes the tampered record (age 1-2)
    LedgerValidationResult resultIncluding =
        clientServiceA.validateLedger(NAMESPACE_1, SOME_ASSET_ID_1, 1, 2);

    // Assert
    assertThat(resultNotIncluding.getCode()).isEqualTo(StatusCode.OK);
    assertThat(resultIncluding.getCode()).isEqualTo(StatusCode.INVALID_OUTPUT);
  }
}
