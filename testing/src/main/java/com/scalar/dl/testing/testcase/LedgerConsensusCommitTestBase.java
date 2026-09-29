package com.scalar.dl.testing.testcase;

import static com.scalar.dl.testing.contract.Constants.AMOUNT_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.contract.Constants.ASSETS_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.contract.Constants.ASSET_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.contract.Constants.CREATE_CONTRACT_ID1;
import static com.scalar.dl.testing.contract.Constants.PAYMENT_CONTRACT_ID1;
import static com.scalar.dl.testing.schema.SchemaConstants.ASSET_AGE_COLUMN_NAME;
import static com.scalar.dl.testing.schema.SchemaConstants.ASSET_ID_COLUMN_NAME;
import static com.scalar.dl.testing.schema.SchemaConstants.ASSET_TABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.TransactionState;
import com.scalar.db.io.Key;
import com.scalar.db.transaction.consensuscommit.Attribute;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.ledger.service.StatusCode;
import java.util.UUID;
import javax.json.Json;
import javax.json.JsonObject;
import org.junit.jupiter.api.Test;

/**
 * Base class for Ledger integration tests with Consensus Commit transaction mode
 * (tx_state_management=false).
 *
 * <p>In this mode, duplicate nonces can be committed but will be detected during validation.
 *
 * <p>Subclasses should override {@link #getAuthenticationMethod()}.
 */
public abstract class LedgerConsensusCommitTestBase extends LedgerOnlyIntegrationTestBase {

  /**
   * The margin to make a prepared record look like it was left by a transaction that is no longer
   * running. It must be longer than the transaction lifetime of ScalarDB (15 seconds), after which
   * a prepared record whose transaction has no Coordinator state becomes recoverable.
   */
  private static final long EXPIRED_TRANSACTION_MARGIN_MILLIS = 60000;

  @Test
  void executeContract_UncommittedAssetRecordLeftAndConflicted_ShouldRecoverTheRecord()
      throws Exception {
    // Arrange: create an asset, which is committed with age 0.
    JsonObject argument =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .add(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .build();
    clientServiceA.executeContract(CREATE_CONTRACT_ID1, argument);

    // Leave an asset record with age 1 in the uncommitted (PREPARED) state as if a transaction had
    // crashed after preparing it. The transaction ID is not in the Coordinator table and
    // tx_prepared_at is old enough, so the record is recoverable. The record has no before image,
    // so recovering it deletes it. Note that nothing reads this record in the normal path since
    // the latest age in the asset metadata is still 0.
    Put uncommitted =
        Put.newBuilder()
            .namespace(getPhysicalNamespace())
            .table(ASSET_TABLE)
            .partitionKey(Key.ofText(ASSET_ID_COLUMN_NAME, SOME_ASSET_ID_1))
            .clusteringKey(Key.ofInt(ASSET_AGE_COLUMN_NAME, 1))
            .textValue(Attribute.ID, UUID.randomUUID().toString())
            .intValue(Attribute.STATE, TransactionState.PREPARED.get())
            .intValue(Attribute.VERSION, 1)
            .bigIntValue(
                Attribute.PREPARED_AT,
                System.currentTimeMillis() - EXPIRED_TRANSACTION_MARGIN_MILLIS)
            .build();
    storage.put(uncommitted);

    // Act: the next execution reads the committed record with age 0 and tries to write age 1,
    // which conflicts with the uncommitted record.
    assertThatThrownBy(() -> clientServiceA.executeContract(CREATE_CONTRACT_ID1, argument))
        .isInstanceOfSatisfying(
            ClientException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(StatusCode.CONFLICT));

    // Assert: the Ledger recovered the uncommitted record while handling the conflict. ScalarDB
    // rolls back only the records that the failed transaction itself prepared, so the record is
    // left as it is unless the Ledger recovers it.
    Get get =
        Get.newBuilder()
            .namespace(getPhysicalNamespace())
            .table(ASSET_TABLE)
            .partitionKey(Key.ofText(ASSET_ID_COLUMN_NAME, SOME_ASSET_ID_1))
            .clusteringKey(Key.ofInt(ASSET_AGE_COLUMN_NAME, 1))
            .build();
    assertThat(storage.get(get)).isNotPresent();
  }

  @Test
  void validateLedger_AssetContainsDuplicateNonce_ShouldReturnInvalidNonce() {
    // Arrange: Create assets with the same nonce (duplicate nonce)
    // In Consensus Commit mode, this will succeed but validation will fail
    createAssetsWithDuplicateNonce();

    // Act
    LedgerValidationResult resultA = getClientServiceA().validateLedger(SOME_ASSET_ID_1);
    LedgerValidationResult resultB = getClientServiceA().validateLedger(SOME_ASSET_ID_2);

    // Assert
    // Asset A has duplicate nonce (used in both create and payment1)
    assertThat(resultA.getCode()).isEqualTo(StatusCode.INVALID_NONCE);
    // Asset B does not have the duplicate nonce issue in its chain
    assertThat(resultB.getCode()).isEqualTo(StatusCode.OK);
  }

  /**
   * Creates assets with duplicate nonce. The nonce used for creating asset A is reused for the
   * first payment, causing a duplicate nonce in asset A's chain.
   */
  private void createAssetsWithDuplicateNonce() {
    // Generate a nonce that will be reused
    String duplicateNonce = java.util.UUID.randomUUID().toString();

    // Create asset A with the nonce
    JsonObject createArg1 =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1)
            .add(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .build();
    getClientServiceA().executeContract(duplicateNonce, CREATE_CONTRACT_ID1, createArg1);

    // Create asset B with a different nonce
    JsonObject createArg2 =
        Json.createObjectBuilder()
            .add(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_2)
            .add(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1)
            .build();
    getClientServiceA().executeContract(CREATE_CONTRACT_ID1, createArg2);

    // Perform payment with the SAME nonce as asset A creation (duplicate nonce)
    JsonObject paymentArg1 =
        Json.createObjectBuilder()
            .add(
                ASSETS_ATTRIBUTE_NAME,
                Json.createArrayBuilder().add(SOME_ASSET_ID_1).add(SOME_ASSET_ID_2))
            .add(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_2)
            .build();
    getClientServiceA().executeContract(duplicateNonce, PAYMENT_CONTRACT_ID1, paymentArg1);

    // Perform another payment with a unique nonce
    JsonObject paymentArg2 =
        Json.createObjectBuilder()
            .add(
                ASSETS_ATTRIBUTE_NAME,
                Json.createArrayBuilder().add(SOME_ASSET_ID_1).add(SOME_ASSET_ID_2))
            .add(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_3)
            .build();
    getClientServiceA().executeContract(PAYMENT_CONTRACT_ID1, paymentArg2);
  }
}
