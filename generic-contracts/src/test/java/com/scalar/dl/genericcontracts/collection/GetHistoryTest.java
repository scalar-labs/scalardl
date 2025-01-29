package com.scalar.dl.genericcontracts.collection;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.AssetFilter.AgeOrder;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class GetHistoryTest {

  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String SOME_COLLECTION_ID = "set";
  private static final String SOME_COLLECTION_ID_WITH_PREFIX =
      Constants.COLLECTION_ID_PREFIX + SOME_COLLECTION_ID;
  private static final ArrayNode SOME_OBJECT_IDS_0 = mapper.createArrayNode().add("foo").add("bar");
  private static final ArrayNode SOME_OBJECT_IDS_1 = mapper.createArrayNode().add("baz");
  private static final ArrayNode SOME_OBJECT_IDS_2 = mapper.createArrayNode().add("foo");
  private static final JsonNode SOME_LIMIT_OPTION =
      mapper.createObjectNode().put(Constants.OPTION_LIMIT, 1);

  private final GetHistory getHistory = new GetHistory();

  @Mock private Ledger<JsonNode> ledger;

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this).close();
  }

  @Test
  public void invoke_CorrectArgumentsGiven_ShouldGetCollectionHistory() {
    // Arrange
    JsonNode argument = mapper.createObjectNode().put(Constants.COLLECTION_ID, SOME_COLLECTION_ID);
    Asset<JsonNode> asset0 = (Asset<JsonNode>) mock(Asset.class);
    Asset<JsonNode> asset1 = (Asset<JsonNode>) mock(Asset.class);
    Asset<JsonNode> asset2 = (Asset<JsonNode>) mock(Asset.class);
    JsonNode data0 =
        mapper
            .createObjectNode()
            .put(Constants.COLLECTION_ID, SOME_COLLECTION_ID)
            .put(Constants.OPERATION_TYPE, Constants.OPERATION_CREATE)
            .set(Constants.OBJECT_IDS, SOME_OBJECT_IDS_0);
    JsonNode data1 =
        mapper
            .createObjectNode()
            .put(Constants.COLLECTION_ID, SOME_COLLECTION_ID)
            .put(Constants.OPERATION_TYPE, Constants.OPERATION_ADD)
            .set(Constants.OBJECT_IDS, SOME_OBJECT_IDS_1);
    JsonNode data2 =
        mapper
            .createObjectNode()
            .put(Constants.COLLECTION_ID, SOME_COLLECTION_ID)
            .put(Constants.OPERATION_TYPE, Constants.OPERATION_REMOVE)
            .set(Constants.OBJECT_IDS, SOME_OBJECT_IDS_2);
    AssetFilter filter =
        new AssetFilter(SOME_COLLECTION_ID_WITH_PREFIX).withAgeOrder(AgeOrder.DESC);
    when(asset0.data()).thenReturn(data0);
    when(asset1.data()).thenReturn(data1);
    when(asset2.data()).thenReturn(data2);
    when(asset0.age()).thenReturn(0);
    when(asset1.age()).thenReturn(1);
    when(asset2.age()).thenReturn(2);
    when(ledger.scan(filter)).thenReturn(ImmutableList.of(asset0, asset1, asset2));
    ObjectNode expected =
        mapper
            .createObjectNode()
            .put(Constants.COLLECTION_ID, SOME_COLLECTION_ID)
            .set(
                Constants.COLLECTION_EVENTS,
                mapper
                    .createArrayNode()
                    .add(
                        mapper
                            .createObjectNode()
                            .put(Constants.OPERATION_TYPE, Constants.OPERATION_CREATE)
                            .put(Constants.COLLECTION_AGE, 0)
                            .set(Constants.OBJECT_IDS, SOME_OBJECT_IDS_0))
                    .add(
                        mapper
                            .createObjectNode()
                            .put(Constants.OPERATION_TYPE, Constants.OPERATION_ADD)
                            .put(Constants.COLLECTION_AGE, 1)
                            .set(Constants.OBJECT_IDS, SOME_OBJECT_IDS_1))
                    .add(
                        mapper
                            .createObjectNode()
                            .put(Constants.OPERATION_TYPE, Constants.OPERATION_REMOVE)
                            .put(Constants.COLLECTION_AGE, 2)
                            .set(Constants.OBJECT_IDS, SOME_OBJECT_IDS_2)));

    // Act
    JsonNode actual = getHistory.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isEqualTo(expected);
    verify(ledger).scan(filter);
  }

  @Test
  public void invoke_CorrectArgumentsWithLimitGiven_ShouldGetCollectionHistory() {
    // Arrange
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.COLLECTION_ID, SOME_COLLECTION_ID)
            .set(Constants.OPTIONS, SOME_LIMIT_OPTION);
    Asset<JsonNode> asset2 = (Asset<JsonNode>) mock(Asset.class);
    JsonNode data2 =
        mapper
            .createObjectNode()
            .put(Constants.COLLECTION_ID, SOME_COLLECTION_ID)
            .put(Constants.OPERATION_TYPE, Constants.OPERATION_REMOVE)
            .set(Constants.OBJECT_IDS, SOME_OBJECT_IDS_2);
    AssetFilter filter =
        new AssetFilter(SOME_COLLECTION_ID_WITH_PREFIX).withLimit(1).withAgeOrder(AgeOrder.DESC);
    when(asset2.data()).thenReturn(data2);
    when(asset2.age()).thenReturn(2);
    when(ledger.scan(filter)).thenReturn(ImmutableList.of(asset2));
    ObjectNode expected =
        mapper
            .createObjectNode()
            .put(Constants.COLLECTION_ID, SOME_COLLECTION_ID)
            .set(
                Constants.COLLECTION_EVENTS,
                mapper
                    .createArrayNode()
                    .add(
                        mapper
                            .createObjectNode()
                            .put(Constants.OPERATION_TYPE, Constants.OPERATION_REMOVE)
                            .put(Constants.COLLECTION_AGE, 2)
                            .set(Constants.OBJECT_IDS, SOME_OBJECT_IDS_2)));

    // Act
    JsonNode actual = getHistory.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isEqualTo(expected);
    verify(ledger).scan(filter);
  }

  @Test
  public void invoke_ArgumentsWithInvalidCollectionIdGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument = mapper.createObjectNode().put(Constants.COLLECTION_ID, 1);

    // Act Assert
    assertThatThrownBy(() -> getHistory.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.COLLECTION_ID_IS_MISSING_OR_INVALID);
    verify(ledger, never()).get(any());
    verify(ledger, never()).scan(any());
  }

  @Test
  public void invoke_ArgumentsWithInvalidLimitGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument1 =
        mapper
            .createObjectNode()
            .put(Constants.COLLECTION_ID, SOME_COLLECTION_ID)
            .set(Constants.OPTIONS, mapper.createObjectNode().put(Constants.OPTION_LIMIT, "1"));
    JsonNode argument2 =
        mapper
            .createObjectNode()
            .put(Constants.COLLECTION_ID, SOME_COLLECTION_ID)
            .set(Constants.OPTIONS, mapper.createObjectNode().put(Constants.OPTION_LIMIT, -1));

    // Act Assert
    validateLimitValueAndAssert(ledger, argument1);
    validateLimitValueAndAssert(ledger, argument2);
    verify(ledger, never()).scan(any());
  }

  private void validateLimitValueAndAssert(Ledger<JsonNode> ledger, JsonNode argument) {
    Assertions.assertThatThrownBy(() -> getHistory.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_OPTIONS_FORMAT);
  }
}
