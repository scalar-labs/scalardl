package com.scalar.application.bankaccount.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.scalar.ledger.asset.Asset;
import com.scalar.ledger.contract.Contract;
import com.scalar.ledger.exception.ContractContextException;
import com.scalar.ledger.ledger.Ledger;
import java.util.Optional;
import java.util.UUID;
import javax.json.Json;
import javax.json.JsonObject;
import org.junit.Before;
import org.junit.Test;
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
  private final Contract contract = new Transfer();
  @Mock private Ledger ledger;
  @Mock private Asset fromAsset;
  @Mock private Asset toAsset;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(fromAsset.data()).thenReturn(Json.createObjectBuilder().add(BALANCE_KEY, 1).build());
    when(toAsset.data()).thenReturn(Json.createObjectBuilder().add(BALANCE_KEY, 0).build());
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
  public void invoke_FromKeyNotSuppliedInArgument_ShouldThrowContractContextException() {
    // Arrange
    JsonObject argument = Json.createObjectBuilder().add(TO_KEY, TO).add(AMOUNT_KEY, 1).build();

    // Act-assert
    assertThatThrownBy(() -> contract.invoke(ledger, argument, Optional.empty()))
        .isInstanceOf(ContractContextException.class)
        .hasMessageStartingWith("a required key is missing:");
  }

  @Test
  public void invoke_ToKeyNotSuppliedInArgument_ShouldThrowContractContextException() {
    // Arrange
    JsonObject argument = Json.createObjectBuilder().add(FROM_KEY, FROM).add(AMOUNT_KEY, 1).build();

    // Act-assert
    assertThatThrownBy(() -> contract.invoke(ledger, argument, Optional.empty()))
        .isInstanceOf(ContractContextException.class)
        .hasMessageStartingWith("a required key is missing:");
  }

  @Test
  public void invoke_AmountKeyNotSuppliedInArgument_ShouldThrowContractContextException() {
    // Arrange
    JsonObject argument = Json.createObjectBuilder().add(TO_KEY, TO).add(FROM_KEY, FROM).build();

    // Act-assert
    assertThatThrownBy(() -> contract.invoke(ledger, argument, Optional.empty()))
        .isInstanceOf(ContractContextException.class)
        .hasMessageStartingWith("a required key is missing:");
  }

  @Test
  public void invoke_AccountExists_ShouldTransferFundsByAppropriateAmount() {
    // Arrange
    JsonObject argument =
        Json.createObjectBuilder().add(FROM_KEY, FROM).add(TO_KEY, TO).add(AMOUNT_KEY, 1).build();
    when(ledger.get(FROM)).thenReturn(Optional.of(fromAsset));
    when(ledger.get(TO)).thenReturn(Optional.of(toAsset));

    // Act
    JsonObject response = contract.invoke(ledger, argument, Optional.empty());

    // Arrange
    assertThat(response.getString(STATUS_KEY)).isEqualTo("succeeded");
    assertThat(response.getInt("from_old_balance")).isEqualTo(1);
    assertThat(response.getInt("from_new_balance")).isEqualTo(0);
    assertThat(response.getInt("to_old_balance")).isEqualTo(0);
    assertThat(response.getInt("to_new_balance")).isEqualTo(1);
  }

  @Test
  public void invoke_FromAccountDoesNotExist_ShouldThrowContractContextException() {
    // Arrange
    JsonObject argument =
        Json.createObjectBuilder().add(FROM_KEY, FROM).add(TO_KEY, TO).add(AMOUNT_KEY, 1).build();
    when(ledger.get(FROM)).thenReturn(Optional.empty());
    when(ledger.get(TO)).thenReturn(Optional.of(toAsset));

    // Act-assert
    assertThatThrownBy(() -> contract.invoke(ledger, argument, Optional.empty()))
        .isInstanceOf(ContractContextException.class)
        .hasMessage("from account does not exist");
  }

  @Test
  public void invoke_ToAccountDoesNotExist_ShouldThrowContractContextException() {
    // Arrange
    JsonObject argument =
        Json.createObjectBuilder().add(FROM_KEY, FROM).add(TO_KEY, TO).add(AMOUNT_KEY, 1).build();
    when(ledger.get(FROM)).thenReturn(Optional.of(fromAsset));
    when(ledger.get(TO)).thenReturn(Optional.empty());

    // Act-assert
    assertThatThrownBy(() -> contract.invoke(ledger, argument, Optional.empty()))
        .isInstanceOf(ContractContextException.class)
        .hasMessage("to account does not exist");
  }

  @Test
  public void invoke_DepositAmountIsNegative_ShouldThrowContractContextException() {
    // Arrange
    // Arrange
    JsonObject argument =
        Json.createObjectBuilder().add(FROM_KEY, FROM).add(TO_KEY, TO).add(AMOUNT_KEY, -1).build();
    when(ledger.get(FROM)).thenReturn(Optional.empty());
    when(ledger.get(TO)).thenReturn(Optional.of(toAsset));

    // Act-assert
    assertThatThrownBy(() -> contract.invoke(ledger, argument, Optional.empty()))
        .isInstanceOf(ContractContextException.class)
        .hasMessage("amount is negative");
  }

  @Test
  public void invoke_TransferMoreThanCurrentBalance_ShouldThrowContractContextException() {
    // Arrange
    JsonObject argument =
        Json.createObjectBuilder().add(FROM_KEY, FROM).add(TO_KEY, TO).add(AMOUNT_KEY, 2).build();
    when(ledger.get(FROM)).thenReturn(Optional.of(fromAsset));
    when(ledger.get(TO)).thenReturn(Optional.of(toAsset));

    // Act-assert
    assertThatThrownBy(() -> contract.invoke(ledger, argument, Optional.empty()))
        .isInstanceOf(ContractContextException.class)
        .hasMessage("insufficient funds");
  }
}
