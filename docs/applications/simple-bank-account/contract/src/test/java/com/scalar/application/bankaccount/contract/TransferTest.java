package com.scalar.application.bankaccount.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TransferTest {
  private final String AMOUNT_KEY = "amount";
  private final String BALANCE_KEY = "balance";
  private final String FROM = UUID.randomUUID().toString();
  private final String FROM_KEY = "from";
  private final String STATUS_KEY = "status";
  private final String TO = UUID.randomUUID().toString();
  private final String TO_KEY = "to";
  private final JacksonBasedContract contract = new Transfer();
  @Mock private Ledger<JsonNode> ledger;
  @Mock private Asset<JsonNode> fromAsset;
  @Mock private Asset<JsonNode> toAsset;
  private AutoCloseable closeable;

  @BeforeEach
  public void setUp() {
    closeable = MockitoAnnotations.openMocks(this);
    when(fromAsset.data()).thenReturn(new ObjectMapper().createObjectNode().put(BALANCE_KEY, 1));
    when(toAsset.data()).thenReturn(new ObjectMapper().createObjectNode().put(BALANCE_KEY, 0));
  }

  @AfterEach
  public void tearDown() throws Exception {
    closeable.close();
  }

  @Test
  public void invoke_EmptyArgument_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument = new ObjectMapper().createObjectNode();

    // Act-assert
    assertThatThrownBy(() -> contract.invoke(ledger, argument, null))
        .isInstanceOf(ContractContextException.class)
        .hasMessageStartingWith("a required key is missing:");
  }

  @Test
  public void invoke_FromKeyNotSuppliedInArgument_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument = new ObjectMapper().createObjectNode().put(TO_KEY, TO).put(AMOUNT_KEY, 1);

    // Act-assert
    assertThatThrownBy(() -> contract.invoke(ledger, argument, null))
        .isInstanceOf(ContractContextException.class)
        .hasMessageStartingWith("a required key is missing:");
  }

  @Test
  public void invoke_ToKeyNotSuppliedInArgument_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument =
        new ObjectMapper().createObjectNode().put(FROM_KEY, FROM).put(AMOUNT_KEY, 1);

    // Act-assert
    assertThatThrownBy(() -> contract.invoke(ledger, argument, null))
        .isInstanceOf(ContractContextException.class)
        .hasMessageStartingWith("a required key is missing:");
  }

  @Test
  public void invoke_AmountKeyNotSuppliedInArgument_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument = new ObjectMapper().createObjectNode().put(TO_KEY, TO).put(FROM_KEY, FROM);

    // Act-assert
    assertThatThrownBy(() -> contract.invoke(ledger, argument, null))
        .isInstanceOf(ContractContextException.class)
        .hasMessageStartingWith("a required key is missing:");
  }

  @Test
  public void invoke_AccountExists_ShouldTransferFundsByAppropriateAmount() {
    // Arrange
    JsonNode argument =
        new ObjectMapper()
            .createObjectNode()
            .put(FROM_KEY, FROM)
            .put(TO_KEY, TO)
            .put(AMOUNT_KEY, 1);
    when(ledger.get(FROM)).thenReturn(Optional.of(fromAsset));
    when(ledger.get(TO)).thenReturn(Optional.of(toAsset));

    // Act
    JsonNode response = contract.invoke(ledger, argument, null);

    // Arrange
    assertThat(response.get(STATUS_KEY).asText()).isEqualTo("succeeded");
    assertThat(response.get("from_old_balance").asInt()).isEqualTo(1);
    assertThat(response.get("from_new_balance").asInt()).isEqualTo(0);
    assertThat(response.get("to_old_balance").asInt()).isEqualTo(0);
    assertThat(response.get("to_new_balance").asInt()).isEqualTo(1);
  }

  @Test
  public void invoke_FromAccountDoesNotExist_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument =
        new ObjectMapper()
            .createObjectNode()
            .put(FROM_KEY, FROM)
            .put(TO_KEY, TO)
            .put(AMOUNT_KEY, 1);
    when(ledger.get(FROM)).thenReturn(Optional.empty());
    when(ledger.get(TO)).thenReturn(Optional.of(toAsset));

    // Act-assert
    assertThatThrownBy(() -> contract.invoke(ledger, argument, null))
        .isInstanceOf(ContractContextException.class)
        .hasMessage("from account does not exist");
  }

  @Test
  public void invoke_ToAccountDoesNotExist_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument =
        new ObjectMapper()
            .createObjectNode()
            .put(FROM_KEY, FROM)
            .put(TO_KEY, TO)
            .put(AMOUNT_KEY, 1);
    when(ledger.get(FROM)).thenReturn(Optional.of(fromAsset));
    when(ledger.get(TO)).thenReturn(Optional.empty());

    // Act-assert
    assertThatThrownBy(() -> contract.invoke(ledger, argument, null))
        .isInstanceOf(ContractContextException.class)
        .hasMessage("to account does not exist");
  }

  @Test
  public void invoke_DepositAmountIsNegative_ShouldThrowContractContextException() {
    // Arrange
    // Arrange
    JsonNode argument =
        new ObjectMapper()
            .createObjectNode()
            .put(FROM_KEY, FROM)
            .put(TO_KEY, TO)
            .put(AMOUNT_KEY, -1);
    when(ledger.get(FROM)).thenReturn(Optional.empty());
    when(ledger.get(TO)).thenReturn(Optional.of(toAsset));

    // Act-assert
    assertThatThrownBy(() -> contract.invoke(ledger, argument, null))
        .isInstanceOf(ContractContextException.class)
        .hasMessage("amount is negative");
  }

  @Test
  public void invoke_TransferMoreThanCurrentBalance_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument =
        new ObjectMapper()
            .createObjectNode()
            .put(FROM_KEY, FROM)
            .put(TO_KEY, TO)
            .put(AMOUNT_KEY, 2);
    when(ledger.get(FROM)).thenReturn(Optional.of(fromAsset));
    when(ledger.get(TO)).thenReturn(Optional.of(toAsset));

    // Act-assert
    assertThatThrownBy(() -> contract.invoke(ledger, argument, null))
        .isInstanceOf(ContractContextException.class)
        .hasMessage("insufficient funds");
  }
}
