package com.scalar.application.bankaccount.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.scalar.dl.ledger.asset.Asset;
import com.scalar.dl.ledger.contract.Contract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.database.Ledger;
import java.util.Optional;
import java.util.UUID;
import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DepositTest {
  private final String AMOUNT_KEY = "amount";
  private final String BALANCE_KEY = "balance";
  private final String ID = UUID.randomUUID().toString();
  private final String ID_KEY = "id";
  private final String STATUS_KEY = "status";
  private final Contract contract = new Deposit();
  @Mock private Ledger ledger;
  @Mock private Asset asset;
  private AutoCloseable closeable;

  @BeforeEach
  public void setUp() {
    closeable = MockitoAnnotations.openMocks(this);
    when(asset.data()).thenReturn(Json.createObjectBuilder().add(BALANCE_KEY, 1).build());
  }

  @AfterEach
  public void tearDown() throws Exception {
    closeable.close();
  }

  @Test
  public void invoke_EmptyArgument_ShouldThrowContractContextException() {
    // Arrange
    JsonObject argument = Json.createObjectBuilder().build();

    // Act-assert
    assertThatThrownBy(() -> contract.invoke(ledger, argument, Optional.empty()))
        .isInstanceOf(ContractContextException.class)
        .hasMessageStartingWith("a required key is missing:");
  }

  @Test
  public void invoke_IdKeyNotSuppliedInArgument_ShouldThrowContractContextException() {
    // Arrange
    JsonObject argument = Json.createObjectBuilder().add(AMOUNT_KEY, 1).build();

    // Act-assert
    assertThatThrownBy(() -> contract.invoke(ledger, argument, Optional.empty()))
        .isInstanceOf(ContractContextException.class)
        .hasMessageStartingWith("a required key is missing:");
  }

  @Test
  public void invoke_AmountKeyNotSuppliedInArgument_ShouldThrowContractContextException() {
    // Arrange
    JsonObject argument = Json.createObjectBuilder().add(ID_KEY, 1).build();

    // Act-assert
    assertThatThrownBy(() -> contract.invoke(ledger, argument, Optional.empty()))
        .isInstanceOf(ContractContextException.class)
        .hasMessageStartingWith("a required key is missing:");
  }

  @Test
  public void invoke_AccountExists_ShouldIncreaseBalanceByAppropriateAmount() {
    // Arrange
    JsonObject argument = Json.createObjectBuilder().add(ID_KEY, ID).add(AMOUNT_KEY, 1).build();
    when(ledger.get(ID)).thenReturn(Optional.of(asset));

    // Act
    JsonObject response = contract.invoke(ledger, argument, Optional.empty());

    // Arrange
    assertThat(response.getString(STATUS_KEY)).isEqualTo("succeeded");
    assertThat(response.getInt("old_balance")).isEqualTo(1);
    assertThat(response.getInt("new_balance")).isEqualTo(2);
  }

  @Test
  public void invoke_AccountDoesNotExist_ShouldThrowContractContextException() {
    // Arrange
    JsonObject argument = Json.createObjectBuilder().add(ID_KEY, ID).add(AMOUNT_KEY, 1).build();
    when(ledger.get(ID)).thenReturn(Optional.empty());

    // Act-assert
    assertThatThrownBy(() -> contract.invoke(ledger, argument, Optional.empty()))
        .isInstanceOf(ContractContextException.class)
        .hasMessage("account does not exist");
  }

  @Test
  public void invoke_DepositAmountIsNegative_ShouldThrowContractContextException() {
    // Arrange
    JsonObject argument = Json.createObjectBuilder().add(ID_KEY, ID).add(AMOUNT_KEY, -1).build();
    when(ledger.get(ID)).thenReturn(Optional.of(asset));

    // Act-assert
    assertThatThrownBy(() -> contract.invoke(ledger, argument, Optional.empty()))
        .isInstanceOf(ContractContextException.class)
        .hasMessage("amount is negative");
  }
}
