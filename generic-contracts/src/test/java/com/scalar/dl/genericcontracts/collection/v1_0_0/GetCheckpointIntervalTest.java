package com.scalar.dl.genericcontracts.collection.v1_0_0;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.dl.genericcontracts.collection.Constants;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Ledger;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class GetCheckpointIntervalTest {

  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String SOME_COLLECTION_ID = "foo";
  private static final int SOME_CHECKPOINT_INTERVAL = 20;

  private final GetCheckpointInterval getCheckpointInterval = new GetCheckpointInterval();

  @Mock private Ledger<JsonNode> ledger;

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this).close();
  }

  @Test
  public void invoke_CorrectPropertiesGiven_ShouldReturnSpecifiedInterval() {
    // Arrange
    JsonNode argument = mapper.createObjectNode().put(Constants.COLLECTION_ID, SOME_COLLECTION_ID);
    JsonNode properties =
        mapper
            .createObjectNode()
            .put(Constants.COLLECTION_CHECKPOINT_INTERVAL, SOME_CHECKPOINT_INTERVAL);

    // Act
    JsonNode actual = getCheckpointInterval.invoke(ledger, argument, properties);

    // Assert
    assertThat(actual).isEqualTo(properties);
  }

  @Test
  public void invoke_CorrectPropertiesNotGiven_ShouldReturnDefaultInterval() {
    // Arrange
    JsonNode argument = mapper.createObjectNode().put(Constants.COLLECTION_ID, SOME_COLLECTION_ID);
    JsonNode expected =
        mapper
            .createObjectNode()
            .put(
                Constants.COLLECTION_CHECKPOINT_INTERVAL,
                Constants.DEFAULT_COLLECTION_CHECKPOINT_INTERVAL);

    // Act
    JsonNode actual = getCheckpointInterval.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void invoke_InvalidCheckpointPropertiesGiven_ShouldReturn() {
    // Arrange
    JsonNode properties1 =
        mapper.createObjectNode().put(Constants.COLLECTION_CHECKPOINT_INTERVAL, "20");
    JsonNode properties2 =
        mapper.createObjectNode().put(Constants.COLLECTION_CHECKPOINT_INTERVAL, -1);

    // Act Assert
    validateContractPropertiesAndAssert(ledger, properties1);
    validateContractPropertiesAndAssert(ledger, properties2);
  }

  private void validateContractPropertiesAndAssert(Ledger<JsonNode> ledger, JsonNode properties) {
    JsonNode argument = mapper.createObjectNode().put(Constants.COLLECTION_ID, SOME_COLLECTION_ID);
    Assertions.assertThatThrownBy(() -> getCheckpointInterval.invoke(ledger, argument, properties))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_CONTRACT_PROPERTIES_FORMAT);
  }
}
