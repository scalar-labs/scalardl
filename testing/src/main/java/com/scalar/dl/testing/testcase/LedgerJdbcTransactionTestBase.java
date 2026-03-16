package com.scalar.dl.testing.testcase;

import static com.scalar.dl.testing.contract.Constants.AMOUNT_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.contract.Constants.ASSETS_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.contract.Constants.ASSET_ATTRIBUTE_NAME;
import static com.scalar.dl.testing.contract.Constants.CREATE_CONTRACT_ID3;
import static com.scalar.dl.testing.contract.Constants.PAYMENT_CONTRACT_ID3;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.testing.config.TransactionMode;
import org.junit.jupiter.api.Test;

/**
 * Base class for Ledger integration tests with JDBC transaction mode (tx_state_management=true).
 *
 * <p>In this mode, duplicate nonces are rejected at commit time with a conflict error.
 *
 * <p>Subclasses should override {@link #getAuthenticationMethod()}.
 */
public abstract class LedgerJdbcTransactionTestBase extends LedgerOnlyIntegrationTestBase {

  private static final ObjectMapper mapper = new ObjectMapper();

  @Override
  protected TransactionMode getTransactionMode() {
    return TransactionMode.JDBC;
  }

  @Test
  void executeContract_ContractWithDuplicateNonce_ShouldThrowClientExceptionWithConflict() {
    // Arrange: Generate a nonce that will be reused
    String duplicateNonce = java.util.UUID.randomUUID().toString();

    // Create asset A with the nonce (using Jackson-based contract)
    ObjectNode createArg1 = mapper.createObjectNode();
    createArg1.put(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_1);
    createArg1.put(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1);
    getClientServiceA().executeContract(duplicateNonce, CREATE_CONTRACT_ID3, createArg1);

    // Create asset B with a different nonce
    ObjectNode createArg2 = mapper.createObjectNode();
    createArg2.put(ASSET_ATTRIBUTE_NAME, SOME_ASSET_ID_2);
    createArg2.put(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_1);
    getClientServiceA().executeContract(CREATE_CONTRACT_ID3, createArg2);

    // Act & Assert: Attempt to perform payment with the SAME nonce (duplicate)
    // In JDBC transaction mode, this should fail with CONFLICT
    ObjectNode paymentArg = mapper.createObjectNode();
    paymentArg.putArray(ASSETS_ATTRIBUTE_NAME).add(SOME_ASSET_ID_1).add(SOME_ASSET_ID_2);
    paymentArg.put(AMOUNT_ATTRIBUTE_NAME, SOME_AMOUNT_2);

    assertThatThrownBy(
            () ->
                getClientServiceA()
                    .executeContract(duplicateNonce, PAYMENT_CONTRACT_ID3, paymentArg))
        .isInstanceOfSatisfying(
            ClientException.class,
            e -> {
              // CONFLICT status is expected when tx_state_management is enabled
              assertThat(e.getStatusCode()).isEqualTo(StatusCode.CONFLICT);
            });
  }
}
