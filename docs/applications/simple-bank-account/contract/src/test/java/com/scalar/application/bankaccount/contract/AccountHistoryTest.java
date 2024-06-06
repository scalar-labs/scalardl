package com.scalar.application.bankaccount.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import com.scalar.dl.ledger.contract.Contract;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.database.Ledger;
import java.util.Optional;
import java.util.UUID;
import javax.json.Json;
import javax.json.JsonObject;
import org.junit.Before;
import org.junit.Test;
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
  private final Contract contract = new AccountHistory();
  @Mock private Ledger ledger;

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
  public void invoke_ArgumentWithId_ShouldCallScanCorrectly() {
    // Arrange
    ArgumentCaptor<AssetFilter> filter = ArgumentCaptor.forClass(AssetFilter.class);
    JsonObject argument = Json.createObjectBuilder().add(ID_KEY, ID).build();

    // Act
    contract.invoke(ledger, argument, Optional.empty());

    // Assert
    verify(ledger).scan(filter.capture());
    assertThat(filter.getValue().getId()).isEqualTo(ID);
  }

  @Test
  public void invoke_ArgumentWithStartVersion_ShouldCallScanCorrectly() {
    // Arrange
    ArgumentCaptor<AssetFilter> filter = ArgumentCaptor.forClass(AssetFilter.class);
    JsonObject argument = Json.createObjectBuilder().add(ID_KEY, ID).add(START_KEY, 3).build();

    // Act
    contract.invoke(ledger, argument, Optional.empty());

    // Assert
    verify(ledger).scan(filter.capture());
    assertThat(filter.getValue().getId()).isEqualTo(ID);
    assertThat(filter.getValue().getStartVersion().get()).isEqualTo(3);
    assertThat(filter.getValue().isStartInclusive()).isEqualTo(true);
  }

  @Test
  public void invoke_ArgumentWithEndVersion_ShouldCallScanCorrectly() {
    // Arrange
    ArgumentCaptor<AssetFilter> filter = ArgumentCaptor.forClass(AssetFilter.class);
    JsonObject argument = Json.createObjectBuilder().add(ID_KEY, ID).add(END_KEY, END).build();

    // Act
    contract.invoke(ledger, argument, Optional.empty());

    // Assert
    verify(ledger).scan(filter.capture());
    assertThat(filter.getValue().getId()).isEqualTo(ID);
    assertThat(filter.getValue().getEndVersion().get()).isEqualTo(END);
    assertThat(filter.getValue().isEndInclusive()).isEqualTo(false);
  }

  @Test
  public void invoke_ArgumentWithLimit_ShouldCallScanCorrectly() {
    // Arrange
    ArgumentCaptor<AssetFilter> filter = ArgumentCaptor.forClass(AssetFilter.class);
    JsonObject argument = Json.createObjectBuilder().add(ID_KEY, ID).add(LIMIT_KEY, LIMIT).build();

    // Act
    contract.invoke(ledger, argument, Optional.empty());

    // Assert
    verify(ledger).scan(filter.capture());
    assertThat(filter.getValue().getId()).isEqualTo(ID);
    assertThat(filter.getValue().getLimit()).isEqualTo(LIMIT);
  }

  @Test
  public void invoke_ArgumentWithAscVersionOrderSpecified_ShouldCallScanCorrectly() {
    // Arrange
    ArgumentCaptor<AssetFilter> filter = ArgumentCaptor.forClass(AssetFilter.class);
    JsonObject argument = Json.createObjectBuilder().add(ID_KEY, ID).add(ORDER_KEY, ASC).build();

    // Act
    contract.invoke(ledger, argument, Optional.empty());

    // Assert
    verify(ledger).scan(filter.capture());
    assertThat(filter.getValue().getId()).isEqualTo(ID);
    assertThat(filter.getValue().getVersionOrder().get()).isEqualTo(AssetFilter.VersionOrder.ASC);
  }

  @Test
  public void invoke_ArgumentWithEverythingSpecified_ShouldCallScanCorrectly() {
    // Arrange
    ArgumentCaptor<AssetFilter> filter = ArgumentCaptor.forClass(AssetFilter.class);
    JsonObject argument =
        Json.createObjectBuilder()
            .add(ID_KEY, ID)
            .add(START_KEY, START)
            .add(END_KEY, END)
            .add(LIMIT_KEY, LIMIT)
            .add(ORDER_KEY, ASC)
            .build();

    // Act
    contract.invoke(ledger, argument, Optional.empty());

    // Assert
    verify(ledger).scan(filter.capture());
    assertThat(filter.getValue().getId()).isEqualTo(ID);
    assertThat(filter.getValue().getStartVersion().get()).isEqualTo(START);
    assertThat(filter.getValue().isStartInclusive()).isEqualTo(true);
    assertThat(filter.getValue().getEndVersion().get()).isEqualTo(END);
    assertThat(filter.getValue().isEndInclusive()).isEqualTo(false);
    assertThat(filter.getValue().getLimit()).isEqualTo(LIMIT);
    assertThat(filter.getValue().getVersionOrder().get()).isEqualTo(AssetFilter.VersionOrder.ASC);
  }
}
