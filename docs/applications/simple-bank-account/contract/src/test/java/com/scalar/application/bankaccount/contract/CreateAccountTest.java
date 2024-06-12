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

public class CreateAccountTest {
  private final String ID = UUID.randomUUID().toString();
  private final String ID_KEY = "id";
  private final JacksonBasedContract contract = new CreateAccount();
  @Mock private Ledger<JsonNode> ledger;
  @Mock private Asset<JsonNode> asset;
  private AutoCloseable closeable;

  @BeforeEach
  public void setUp() {
    closeable = MockitoAnnotations.openMocks(this);
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
  public void invoke_AccountDoesNotExist_ShouldReturnSucceeded() {
    // Arrange
    JsonNode argument = new ObjectMapper().createObjectNode().put(ID_KEY, ID);
    when(ledger.get(ID)).thenReturn(Optional.empty());

    // Act
    JsonNode response = contract.invoke(ledger, argument, null);

    // Assert
    assertThat(response.get("status").asText()).isEqualTo("succeeded");
  }

  @Test
  public void invoke_AccountExists_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument = new ObjectMapper().createObjectNode().put(ID_KEY, ID);
    when(ledger.get(ID)).thenReturn(Optional.of(asset));

    // Act-assert
    assertThatThrownBy(() -> contract.invoke(ledger, argument, null))
        .isInstanceOf(ContractContextException.class)
        .hasMessage("account already exists");
  }
}
