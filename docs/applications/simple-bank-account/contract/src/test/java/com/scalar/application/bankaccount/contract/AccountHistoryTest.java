package com.scalar.application.bankaccount.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AccountHistoryTest {
  private final String ASC = "asc";
  private final int END = 9;
  private final String END_KEY = "end";
  private final String ID = UUID.randomUUID().toString();
  private final String ID_KEY = "id";
  private final int LIMIT = 9;
  private final String LIMIT_KEY = "limit";
  private final String ORDER_KEY = "order";
  private final int START = 3;
  private final String START_KEY = "start";
  private final JacksonBasedContract contract = new AccountHistory();
  @Mock private Ledger<JsonNode> ledger;
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
  public void invoke_ArgumentWithId_ShouldCallScanCorrectly() {
    // Arrange
    ArgumentCaptor<AssetFilter> filter = ArgumentCaptor.forClass(AssetFilter.class);
    JsonNode argument = new ObjectMapper().createObjectNode().put(ID_KEY, ID);

    // Act
    contract.invoke(ledger, argument, null);

    // Assert
    verify(ledger).scan(filter.capture());
    assertThat(filter.getValue().getId()).isEqualTo(ID);
  }

  @Test
  public void invoke_ArgumentWithStartVersion_ShouldCallScanCorrectly() {
    // Arrange
    ArgumentCaptor<AssetFilter> filter = ArgumentCaptor.forClass(AssetFilter.class);
    JsonNode argument = new ObjectMapper().createObjectNode().put(ID_KEY, ID).put(START_KEY, 3);

    // Act
    contract.invoke(ledger, argument, null);

    // Assert
    verify(ledger).scan(filter.capture());
    assertThat(filter.getValue().getId()).isEqualTo(ID);
    assertThat(filter.getValue().getStartAge().get()).isEqualTo(3);
    assertThat(filter.getValue().isStartInclusive()).isEqualTo(true);
  }

  @Test
  public void invoke_ArgumentWithEndVersion_ShouldCallScanCorrectly() {
    // Arrange
    ArgumentCaptor<AssetFilter> filter = ArgumentCaptor.forClass(AssetFilter.class);
    JsonNode argument = new ObjectMapper().createObjectNode().put(ID_KEY, ID).put(END_KEY, END);

    // Act
    contract.invoke(ledger, argument, null);

    // Assert
    verify(ledger).scan(filter.capture());
    assertThat(filter.getValue().getId()).isEqualTo(ID);
    assertThat(filter.getValue().getEndAge().get()).isEqualTo(END);
    assertThat(filter.getValue().isEndInclusive()).isEqualTo(false);
  }

  @Test
  public void invoke_ArgumentWithLimit_ShouldCallScanCorrectly() {
    // Arrange
    ArgumentCaptor<AssetFilter> filter = ArgumentCaptor.forClass(AssetFilter.class);
    JsonNode argument = new ObjectMapper().createObjectNode().put(ID_KEY, ID).put(LIMIT_KEY, LIMIT);

    // Act
    contract.invoke(ledger, argument, null);

    // Assert
    verify(ledger).scan(filter.capture());
    assertThat(filter.getValue().getId()).isEqualTo(ID);
    assertThat(filter.getValue().getLimit()).isEqualTo(LIMIT);
  }

  @Test
  public void invoke_ArgumentWithAscAgeOrderSpecified_ShouldCallScanCorrectly() {
    // Arrange
    ArgumentCaptor<AssetFilter> filter = ArgumentCaptor.forClass(AssetFilter.class);
    JsonNode argument = new ObjectMapper().createObjectNode().put(ID_KEY, ID).put(ORDER_KEY, ASC);

    // Act
    contract.invoke(ledger, argument, null);

    // Assert
    verify(ledger).scan(filter.capture());
    assertThat(filter.getValue().getId()).isEqualTo(ID);
    assertThat(filter.getValue().getAgeOrder().get()).isEqualTo(AssetFilter.AgeOrder.ASC);
  }

  @Test
  public void invoke_ArgumentWithEverythingSpecified_ShouldCallScanCorrectly() {
    // Arrange
    ArgumentCaptor<AssetFilter> filter = ArgumentCaptor.forClass(AssetFilter.class);
    JsonNode argument =
        new ObjectMapper()
            .createObjectNode()
            .put(ID_KEY, ID)
            .put(START_KEY, START)
            .put(END_KEY, END)
            .put(LIMIT_KEY, LIMIT)
            .put(ORDER_KEY, ASC);

    // Act
    contract.invoke(ledger, argument, null);

    // Assert
    verify(ledger).scan(filter.capture());
    assertThat(filter.getValue().getId()).isEqualTo(ID);
    assertThat(filter.getValue().getStartAge().get()).isEqualTo(START);
    assertThat(filter.getValue().isStartInclusive()).isEqualTo(true);
    assertThat(filter.getValue().getEndAge().get()).isEqualTo(END);
    assertThat(filter.getValue().isEndInclusive()).isEqualTo(false);
    assertThat(filter.getValue().getLimit()).isEqualTo(LIMIT);
    assertThat(filter.getValue().getAgeOrder().get()).isEqualTo(AssetFilter.AgeOrder.ASC);
  }
}
