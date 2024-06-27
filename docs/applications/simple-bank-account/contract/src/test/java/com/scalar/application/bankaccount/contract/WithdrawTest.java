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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class WithdrawTest {
  private final String AMOUNT_KEY = "amount";
  private final String BALANCE_KEY = "balance";
  private final String ID = UUID.randomUUID().toString();
  private final String ID_KEY = "id";
  private final String STATUS_KEY = "status";
  private final JacksonBasedContract contract = new Withdraw();
  @Mock private Ledger<JsonNode> ledger;
  @Mock private Asset<JsonNode> asset;
  private AutoCloseable closeable;

  @BeforeEach
  public void setUp() {
    closeable = MockitoAnnotations.openMocks(this);
    when(asset.data()).thenReturn(new ObjectMapper().createObjectNode().put(BALANCE_KEY, 1));
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
  public void invoke_IdKeyNotSuppliedInArgument_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument = new ObjectMapper().createObjectNode().put(AMOUNT_KEY, 1);

    // Act-assert
    assertThatThrownBy(() -> contract.invoke(ledger, argument, null))
        .isInstanceOf(ContractContextException.class)
        .hasMessageStartingWith("a required key is missing:");
  }

  @Test
  public void invoke_AmountKeyNotSuppliedInArgument_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument = new ObjectMapper().createObjectNode().put(ID_KEY, 1);

    // Act-assert
    assertThatThrownBy(() -> contract.invoke(ledger, argument, null))
        .isInstanceOf(ContractContextException.class)
        .hasMessageStartingWith("a required key is missing:");
  }

  @Test
  public void invoke_AccountExists_ShouldDecreaseBalanceByAppropriateAmount() {
    // Arrange
    JsonNode argument = new ObjectMapper().createObjectNode().put(ID_KEY, ID).put(AMOUNT_KEY, 1);
    when(ledger.get(ID)).thenReturn(Optional.of(asset));

    // Act
    JsonNode response = contract.invoke(ledger, argument, null);

    // Arrange
    assertThat(response.get(STATUS_KEY).asText()).isEqualTo("succeeded");
    assertThat(response.get("old_balance").asInt()).isEqualTo(1);
    assertThat(response.get("new_balance").asInt()).isEqualTo(0);
  }

  @Test
  public void invoke_AccountDoesNotExist_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument = new ObjectMapper().createObjectNode().put(ID_KEY, ID).put(AMOUNT_KEY, 1);
    when(ledger.get(ID)).thenReturn(Optional.empty());

    // Act-assert
    assertThatThrownBy(() -> contract.invoke(ledger, argument, null))
        .isInstanceOf(ContractContextException.class)
        .hasMessage("account does not exist");
  }

  @Test
  public void invoke_DepositAmountIsNegative_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument = new ObjectMapper().createObjectNode().put(ID_KEY, ID).put(AMOUNT_KEY, -1);
    when(ledger.get(ID)).thenReturn(Optional.of(asset));

    // Act-assert
    assertThatThrownBy(() -> contract.invoke(ledger, argument, null))
        .isInstanceOf(ContractContextException.class)
        .hasMessage("amount is negative");
  }

  @Test
  public void invoke_WithdrawMoreThanCurrentBalance_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument = new ObjectMapper().createObjectNode().put(ID_KEY, ID).put(AMOUNT_KEY, 2);
    when(ledger.get(ID)).thenReturn(Optional.of(asset));

    // Act-assert
    assertThatThrownBy(() -> contract.invoke(ledger, argument, null))
        .isInstanceOf(ContractContextException.class)
        .hasMessage("insufficient funds");
  }
}
