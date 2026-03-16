package com.scalar.dl.testing.testcase;

import static com.scalar.dl.testing.contract.Constants.AMOUNT_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.contract.Constants.ASSETS_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.contract.Constants.CREATE_CONTRACT_ID1;
import static com.scalar.dl.testing.contract.Constants.PAYMENT_CONTRACT_ID1;
import static com.scalar.dl.testing.schema.SchemaConstants.ASSET_AGE_COLUMN_NAME;
import static com.scalar.dl.testing.schema.SchemaConstants.ASSET_ARGUMENT_COLUMN_NAME;
import static com.scalar.dl.testing.schema.SchemaConstants.ASSET_ID_COLUMN_NAME;
import static com.scalar.dl.testing.schema.SchemaConstants.ASSET_INPUT_COLUMN_NAME;
import static com.scalar.dl.testing.schema.SchemaConstants.ASSET_OUTPUT_COLUMN_NAME;
import static com.scalar.dl.testing.schema.SchemaConstants.ASSET_PREV_HASH_COLUMN_NAME;
import static com.scalar.dl.testing.schema.SchemaConstants.ASSET_TABLE;
import static com.scalar.dl.testing.schema.SchemaConstants.SCALAR_NAMESPACE;
import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.db.api.Delete;
import com.scalar.db.api.Put;
import com.scalar.db.io.Key;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.util.Argument;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import javax.json.Json;
import javax.json.JsonObject;
import org.junit.jupiter.api.Test;

/**
 * Base class for Ledger-only integration tests.
 *
 * <p>This class contains tests that are specific to Ledger-only configurations, particularly tamper
 * detection tests using validateLedger. These tests verify that the Ledger can detect various types
 * of data tampering (record removal, hash tampering, output tampering, etc.).
 *
 * <p>These tests are NOT applicable to Auditor configurations because Auditor uses Byzantine Fault
 * Detection which has different validation semantics.
 */
public abstract class LedgerOnlyIntegrationTestBase extends LedgerIntegrationTestBase {

  // ============ String-based Contract validateLedger Test (Ledger-only) ============

  /**
   * Tests validateLedger with assets created using StringBasedContract.
   *
   * <p>This test is placed in LedgerOnlyIntegrationTestBase because Auditor configurations require
   * a String-based ValidateLedger contract to be registered for validating String-based assets. The
   * default ValidateLedger contract ({@code
   * com.scalar.dl.client.validation.contract.xxx.ValidateLedger}) is a JacksonBasedContract that
   * expects asset data in JSON format, but StringBasedContract stores data in plain string format
   * (e.g., "balance,1000").
   */
  @Test
  void validateLedger_AssetsCreatedWithStringContractAndNothingTampered_ShouldReturnOk() {
    // Arrange: Create two assets and perform two payments using String API
    createAssetsWithPaymentsString();

    // Act
    LedgerValidationResult resultA = clientServiceA.validateLedger(SOME_ASSET_ID_1);
    LedgerValidationResult resultB = clientServiceA.validateLedger(SOME_ASSET_ID_2);

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.OK);
    assertThat(resultB.getCode()).isEqualTo(StatusCode.OK);
  }

  // ============ Tamper Detection Test Cases (Ledger-only) ============

  @Test
  void validateLedger_MiddleRecordRemoved_ShouldReturnInconsistentStates() throws Exception {
    // Arrange: Create assets and then maliciously remove a middle record
    createAssetsWithPaymentsJsonp(CREATE_CONTRACT_ID1, PAYMENT_CONTRACT_ID1);

    // Remove the middle record (age=1) of SOME_ASSET_ID_1
    Delete delete =
        Delete.newBuilder()
            .namespace(SCALAR_NAMESPACE)
            .table(ASSET_TABLE)
            .partitionKey(Key.ofText(ASSET_ID_COLUMN_NAME, SOME_ASSET_ID_1))
            .clusteringKey(Key.ofInt(ASSET_AGE_COLUMN_NAME, 1))
            .build();
    storage.delete(delete);

    // Act
    LedgerValidationResult resultA = clientServiceA.validateLedger(SOME_ASSET_ID_1);
    LedgerValidationResult resultB = clientServiceA.validateLedger(SOME_ASSET_ID_2);

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.INCONSISTENT_STATES);
    assertThat(resultB.getCode()).isEqualTo(StatusCode.INCONSISTENT_STATES);
  }

  @Test
  void validateLedger_PrevHashTampered_ShouldReturnInvalidPrevHash() throws Exception {
    // Arrange: Create assets and then tamper with prev_hash
    createAssetsWithPaymentsJsonp(CREATE_CONTRACT_ID1, PAYMENT_CONTRACT_ID1);

    // Tamper with prev_hash of the latest record (age=2) of SOME_ASSET_ID_1
    Put put =
        Put.newBuilder()
            .namespace(SCALAR_NAMESPACE)
            .table(ASSET_TABLE)
            .partitionKey(Key.ofText(ASSET_ID_COLUMN_NAME, SOME_ASSET_ID_1))
            .clusteringKey(Key.ofInt(ASSET_AGE_COLUMN_NAME, 2))
            .blobValue(ASSET_PREV_HASH_COLUMN_NAME, "tampered".getBytes(StandardCharsets.UTF_8))
            .build();
    storage.put(put);

    // Act
    LedgerValidationResult resultA = clientServiceA.validateLedger(SOME_ASSET_ID_1);
    LedgerValidationResult resultB = clientServiceA.validateLedger(SOME_ASSET_ID_2);

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.INVALID_PREV_HASH);
    assertThat(resultB.getCode()).isEqualTo(StatusCode.OK);
  }

  @Test
  void validateLedger_OutputTampered_ShouldReturnInvalidOutput() throws Exception {
    // Arrange: Create assets and then tamper with output
    createAssetsWithPaymentsJsonp(CREATE_CONTRACT_ID1, PAYMENT_CONTRACT_ID1);

    // Tamper with output of the latest record (age=2) of SOME_ASSET_ID_1
    // Original balance should be 890 (1000 - 100 - 10), tampering to 7000
    Put put =
        Put.newBuilder()
            .namespace(SCALAR_NAMESPACE)
            .table(ASSET_TABLE)
            .partitionKey(Key.ofText(ASSET_ID_COLUMN_NAME, SOME_ASSET_ID_1))
            .clusteringKey(Key.ofInt(ASSET_AGE_COLUMN_NAME, 2))
            .textValue(ASSET_OUTPUT_COLUMN_NAME, "{\"balance\":7000}")
            .build();
    storage.put(put);

    // Act
    LedgerValidationResult resultA = clientServiceA.validateLedger(SOME_ASSET_ID_1);
    LedgerValidationResult resultB = clientServiceA.validateLedger(SOME_ASSET_ID_2);

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.INVALID_OUTPUT);
    assertThat(resultB.getCode()).isEqualTo(StatusCode.OK);
  }

  @Test
  void validateLedger_InputTampered_ShouldReturnInvalidOutput() throws Exception {
    // Arrange: Create assets and then tamper with input
    createAssetsWithPaymentsJsonp(CREATE_CONTRACT_ID1, PAYMENT_CONTRACT_ID1);

    // Tamper with input of the latest record (age=2) of SOME_ASSET_ID_1
    // Changing age values in input to invalid values
    Put put =
        Put.newBuilder()
            .namespace(SCALAR_NAMESPACE)
            .table(ASSET_TABLE)
            .partitionKey(Key.ofText(ASSET_ID_COLUMN_NAME, SOME_ASSET_ID_1))
            .clusteringKey(Key.ofInt(ASSET_AGE_COLUMN_NAME, 2))
            .textValue(ASSET_INPUT_COLUMN_NAME, "{\"A\":{\"age\":2},\"B\":{\"age\":1}}")
            .build();
    storage.put(put);

    // Act
    LedgerValidationResult resultA = clientServiceA.validateLedger(SOME_ASSET_ID_1);
    LedgerValidationResult resultB = clientServiceA.validateLedger(SOME_ASSET_ID_2);

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.INVALID_OUTPUT);
    assertThat(resultB.getCode()).isEqualTo(StatusCode.OK);
  }

  @Test
  void validateLedger_ContractArgumentTampered_ShouldReturnInvalidContract() throws Exception {
    // Arrange: Create assets and then tamper with contract argument
    createAssetsWithPaymentsJsonp(CREATE_CONTRACT_ID1, PAYMENT_CONTRACT_ID1);

    // Tamper with argument of the latest record (age=2) of SOME_ASSET_ID_1
    // Change the amount to 0 instead of the original value
    JsonObject tamperedArgument =
        Json.createObjectBuilder()
            .add(
                ASSETS_ATTRIBUTE_NAME,
                Json.createArrayBuilder().add(SOME_ASSET_ID_1).add(SOME_ASSET_ID_2))
            .add(AMOUNT_ATTRIBUTE_NAME, 0)
            .add(Argument.NONCE_KEY_NAME, UUID.randomUUID().toString())
            .build();
    Put put =
        Put.newBuilder()
            .namespace(SCALAR_NAMESPACE)
            .table(ASSET_TABLE)
            .partitionKey(Key.ofText(ASSET_ID_COLUMN_NAME, SOME_ASSET_ID_1))
            .clusteringKey(Key.ofInt(ASSET_AGE_COLUMN_NAME, 2))
            .textValue(ASSET_ARGUMENT_COLUMN_NAME, tamperedArgument.toString())
            .build();
    storage.put(put);

    // Act
    LedgerValidationResult resultA = clientServiceA.validateLedger(SOME_ASSET_ID_1);
    LedgerValidationResult resultB = clientServiceA.validateLedger(SOME_ASSET_ID_2);

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.INVALID_CONTRACT);
    assertThat(resultB.getCode()).isEqualTo(StatusCode.OK);
  }

  @Test
  void
      validateLedger_OutputTamperedAndPartiallyValidatedIncludingTampered_ShouldReturnInvalidOutput()
          throws Exception {
    // Arrange: Create assets and then tamper with output
    createAssetsWithPaymentsJsonp(CREATE_CONTRACT_ID1, PAYMENT_CONTRACT_ID1);

    // Tamper with output of the latest record (age=2) of SOME_ASSET_ID_1
    // Original balance should be 890 (1000 - 100 - 10), tampering to 7000
    Put put =
        Put.newBuilder()
            .namespace(SCALAR_NAMESPACE)
            .table(ASSET_TABLE)
            .partitionKey(Key.ofText(ASSET_ID_COLUMN_NAME, SOME_ASSET_ID_1))
            .clusteringKey(Key.ofInt(ASSET_AGE_COLUMN_NAME, 2))
            .textValue(ASSET_OUTPUT_COLUMN_NAME, "{\"balance\":7000}")
            .build();
    storage.put(put);

    // Act: Validate with age range that includes the tampered record (age 1-2)
    LedgerValidationResult resultA = clientServiceA.validateLedger(SOME_ASSET_ID_1, 1, 2);

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.INVALID_OUTPUT);
  }

  @Test
  void validateLedger_OutputTamperedAndPartiallyValidatedNotIncludingTampered_ShouldReturnOk()
      throws Exception {
    // Arrange: Create assets and then tamper with output
    createAssetsWithPaymentsJsonp(CREATE_CONTRACT_ID1, PAYMENT_CONTRACT_ID1);

    // Tamper with output of the latest record (age=2) of SOME_ASSET_ID_1
    // Original balance should be 890 (1000 - 100 - 10), tampering to 7000
    Put put =
        Put.newBuilder()
            .namespace(SCALAR_NAMESPACE)
            .table(ASSET_TABLE)
            .partitionKey(Key.ofText(ASSET_ID_COLUMN_NAME, SOME_ASSET_ID_1))
            .clusteringKey(Key.ofInt(ASSET_AGE_COLUMN_NAME, 2))
            .textValue(ASSET_OUTPUT_COLUMN_NAME, "{\"balance\":7000}")
            .build();
    storage.put(put);

    // Act: Validate with age range that does NOT include the tampered record (age 0-1)
    // age 2 is tampered but it is not included in the validation range
    LedgerValidationResult resultA = clientServiceA.validateLedger(SOME_ASSET_ID_1, 0, 1);

    // Assert
    assertThat(resultA.getCode()).isEqualTo(StatusCode.OK);
  }
}
