package com.scalar.dl.client.contract;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.scalar.dl.client.Constants;
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

public class ValidateObjectTest {

  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String SOME_OBJECT_ID = "foo";
  private static final String SOME_VERSION_ID_0 = "v3";
  private static final String SOME_VERSION_ID_1 = "v4";
  private static final String SOME_VERSION_ID_2 = "v5";
  private static final String SOME_HASH_VALUE_0 = "bar3";
  private static final String SOME_HASH_VALUE_1 = "bar4";
  private static final String SOME_HASH_VALUE_2 = "bar5";

  private final ValidateObject validateObject = new ValidateObject();

  @Mock private Ledger<JsonNode> ledger;

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this).close();
  }

  @Test
  public void invoke_CorrectArgumentsGiven_ShouldReturnStatusNormal() {
    // Arrange
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .set(
                Constants.HASH_VALUES,
                mapper
                    .createArrayNode()
                    .add(mapper.createObjectNode().put(SOME_VERSION_ID_2, SOME_HASH_VALUE_2))
                    .add(mapper.createObjectNode().put(SOME_VERSION_ID_1, SOME_HASH_VALUE_1))
                    .add(mapper.createObjectNode().put(SOME_VERSION_ID_0, SOME_HASH_VALUE_0)));
    Asset<JsonNode> asset0 = (Asset<JsonNode>) mock(Asset.class);
    Asset<JsonNode> asset1 = (Asset<JsonNode>) mock(Asset.class);
    Asset<JsonNode> asset2 = (Asset<JsonNode>) mock(Asset.class);
    JsonNode data0 =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .put(Constants.HASH_VALUE, SOME_HASH_VALUE_0);
    JsonNode data1 =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .put(Constants.HASH_VALUE, SOME_HASH_VALUE_1);
    JsonNode data2 =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .put(Constants.HASH_VALUE, SOME_HASH_VALUE_2);
    AssetFilter filter = new AssetFilter(SOME_OBJECT_ID).withLimit(3).withAgeOrder(AgeOrder.DESC);
    when(asset0.data()).thenReturn(data0);
    when(asset1.data()).thenReturn(data1);
    when(asset2.data()).thenReturn(data2);
    when(ledger.scan(any(AssetFilter.class))).thenReturn(ImmutableList.of(asset2, asset1, asset0));

    // Act
    JsonNode actual = validateObject.invoke(ledger, argument, null);

    // Assert
    assertThat(actual.get(Constants.STATUS).asText()).isEqualTo(Constants.STATUS_NORMAL);
    assertThat(actual.get(Constants.FAULTY_VERSIONS).isArray()).isTrue();
    assertThat(actual.get(Constants.FAULTY_VERSIONS).size()).isEqualTo(0);
    verify(ledger).scan(filter);
  }

  @Test
  public void invoke_TamperedArgumentsGiven_ShouldReturnStatusFaulty() {
    // Arrange
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .set(
                Constants.HASH_VALUES,
                mapper
                    .createArrayNode()
                    .add(mapper.createObjectNode().put(SOME_VERSION_ID_2, "tampered"))
                    .add(mapper.createObjectNode().put(SOME_VERSION_ID_1, SOME_HASH_VALUE_1))
                    .add(mapper.createObjectNode().put(SOME_VERSION_ID_0, "tampered")));
    Asset<JsonNode> asset0 = (Asset<JsonNode>) mock(Asset.class);
    Asset<JsonNode> asset1 = (Asset<JsonNode>) mock(Asset.class);
    Asset<JsonNode> asset2 = (Asset<JsonNode>) mock(Asset.class);
    JsonNode data0 =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .put(Constants.HASH_VALUE, SOME_HASH_VALUE_0);
    JsonNode data1 =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .put(Constants.HASH_VALUE, SOME_HASH_VALUE_1);
    JsonNode data2 =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .put(Constants.HASH_VALUE, SOME_HASH_VALUE_2);
    AssetFilter filter = new AssetFilter(SOME_OBJECT_ID).withLimit(3).withAgeOrder(AgeOrder.DESC);
    when(asset0.data()).thenReturn(data0);
    when(asset1.data()).thenReturn(data1);
    when(asset2.data()).thenReturn(data2);
    when(ledger.scan(any(AssetFilter.class))).thenReturn(ImmutableList.of(asset2, asset1, asset0));

    // Act
    JsonNode actual = validateObject.invoke(ledger, argument, null);

    // Assert
    assertThat(actual.get(Constants.STATUS).asText()).isEqualTo(Constants.STATUS_FAULTY);
    assertThat(actual.get(Constants.FAULTY_VERSIONS).isArray()).isTrue();
    assertThat(actual.get(Constants.FAULTY_VERSIONS).size()).isEqualTo(2);
    assertThat(actual.get(Constants.FAULTY_VERSIONS).get(0).asText()).isEqualTo(SOME_VERSION_ID_2);
    assertThat(actual.get(Constants.FAULTY_VERSIONS).get(1).asText()).isEqualTo(SOME_VERSION_ID_0);
    verify(ledger).scan(filter);
  }

  @Test
  public void invoke_ArgumentsWithoutObjectIdGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument = mapper.createObjectNode();

    // Act Assert
    assertThatThrownBy(() -> validateObject.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.OBJECT_ID_IS_MISSING_OR_INVALID);
    verify(ledger, never()).scan(any());
  }

  @Test
  public void invoke_ArgumentsWithInvalidObjectIdTypeGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument = mapper.createObjectNode().put(Constants.OBJECT_ID, 1);

    // Act Assert
    Assertions.assertThatThrownBy(() -> validateObject.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.OBJECT_ID_IS_MISSING_OR_INVALID);
    verify(ledger, never()).scan(any());
  }

  @Test
  public void invoke_ArgumentsWithoutHashValuesGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument = mapper.createObjectNode().put(Constants.OBJECT_ID, SOME_OBJECT_ID);

    // Act Assert
    assertThatThrownBy(() -> validateObject.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.HASH_VALUES_ARE_MISSING);
    verify(ledger, never()).scan(any());
  }

  @Test
  public void invoke_ArgumentsWithInvalidHashValuesGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument1 =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .put(Constants.HASH_VALUES, "");
    JsonNode argument2 =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .set(
                Constants.HASH_VALUES,
                mapper.createArrayNode().add("bar5").add("bar4").add("bar3"));
    JsonNode argument3 =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .set(
                Constants.HASH_VALUES,
                mapper
                    .createArrayNode()
                    .add(
                        mapper
                            .createObjectNode()
                            .put("v5", "bar5")
                            .put("v4", "bar4")
                            .put("v3", "bar3")));
    JsonNode argument4 =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .set(
                Constants.HASH_VALUES,
                mapper
                    .createArrayNode()
                    .add(mapper.createObjectNode().put("v5", 5))
                    .add(mapper.createObjectNode().put("v4", 4))
                    .add(mapper.createObjectNode().put("v3", 3)));
    JsonNode argument5 =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .set(Constants.HASH_VALUES, mapper.createArrayNode());

    // Act Assert
    validateHashValuesAndAssert(ledger, argument1);
    validateHashValuesAndAssert(ledger, argument2);
    validateHashValuesAndAssert(ledger, argument3);
    validateHashValuesAndAssert(ledger, argument4);
    validateHashValuesAndAssert(ledger, argument5);
    verify(ledger, never()).scan(any());
  }

  private void validateHashValuesAndAssert(Ledger<JsonNode> ledger, JsonNode argument) {
    Assertions.assertThatThrownBy(() -> validateObject.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_HASH_VALUES_FORMAT);
  }
}
