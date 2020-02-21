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
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CreateAccountTest {
  private final String ID = UUID.randomUUID().toString();
  private final String ID_KEY = "id";
  private final Contract contract = new CreateAccount();
  @Mock private Ledger ledger;
  @Mock private Asset asset;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
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
  public void invoke_AccountDoesNotExist_ShouldReturnSucceeded() {
    // Arrange
    JsonObject argument = Json.createObjectBuilder().add(ID_KEY, ID).build();
    when(ledger.get(ID)).thenReturn(Optional.empty());

    // Act
    JsonObject response = contract.invoke(ledger, argument, Optional.empty());

    // Assert
    assertThat(response.getString("status")).isEqualTo("succeeded");
  }

  @Test
  public void invoke_AccountExists_ShouldThrowContractContextException() {
    // Arrange
    JsonObject argument = Json.createObjectBuilder().add(ID_KEY, ID).build();
    when(ledger.get(ID)).thenReturn(Optional.of(asset));

    // Act-assert
    assertThatThrownBy(() -> contract.invoke(ledger, argument, Optional.empty()))
        .isInstanceOf(ContractContextException.class)
        .hasMessage("account already exists");
  }
}
