package com.scalar.dl.testing.testcase;

import static com.scalar.dl.testing.contract.Constants.AMOUNT_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.contract.Constants.ASSETS_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.contract.Constants.ASSET_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.contract.Constants.CREATE_CONTRACT_ID1;
import static com.scalar.dl.testing.contract.Constants.PAYMENT_CONTRACT_ID1;
import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.ledger.service.StatusCode;
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
