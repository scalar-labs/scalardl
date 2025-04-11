package com.scalar.dl.genericcontracts.table.v1_0_0;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Ledger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class SelectTest {
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String SOME_TABLE_NAME = "Person";
  private static final String SOME_PRIMARY_KEY_COLUMN = "GovId";
  private static final String SOME_PRIMARY_KEY_VALUE = "001";
  private static final String SOME_INDEX_KEY_COLUMN = "lastName";
  private static final String SOME_INDEX_KEY_COLUMN_VALUE = "Doe";
  private static final String SOME_COLUMN_STRING = "firstName";
  private static final String SOME_COLUMN_STRING_VALUE = "John";
  private static final String SOME_NON_EXISTING_FIELD = "field";
  private static final String SOME_INVALID_FIELD = "field";
  private static final String SOME_INVALID_VALUE = "value";
  private static final String SOME_INVALID_OPERATOR = "op";

  @Spy private Select select = new Select();
  @Mock private Ledger<JsonNode> ledger;

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this).close();
  }

  private JsonNode createQueryArguments(ArrayNode conditions) {
    return mapper
        .createObjectNode()
        .put(Constants.QUERY_TABLE, SOME_TABLE_NAME)
        .set(Constants.QUERY_CONDITIONS, conditions);
  }

  @Test
  public void invoke_CorrectArgumentsGiven_ShouldReturnRecords() {
    // Arrange
    ObjectNode argument = mapper.createObjectNode();
    argument.put(Constants.QUERY_TABLE, SOME_TABLE_NAME);
    argument.set(
        Constants.QUERY_CONDITIONS,
        mapper
            .createArrayNode()
            .add(
                mapper
                    .createObjectNode()
                    .put(Constants.CONDITION_COLUMN, SOME_INDEX_KEY_COLUMN)
                    .put(Constants.CONDITION_VALUE, SOME_INDEX_KEY_COLUMN_VALUE)
                    .put(Constants.CONDITION_OPERATOR, Constants.OPERATOR_EQ)));
    argument.set(
        Constants.QUERY_PROJECTIONS,
        mapper
            .createArrayNode()
            .add(SOME_INDEX_KEY_COLUMN)
            .add(SOME_COLUMN_STRING)
            .add(SOME_NON_EXISTING_FIELD));
    JsonNode records =
        mapper
            .createArrayNode()
            .add(
                mapper
                    .createObjectNode()
                    .put(SOME_PRIMARY_KEY_COLUMN, SOME_PRIMARY_KEY_VALUE)
                    .put(SOME_INDEX_KEY_COLUMN, SOME_INDEX_KEY_COLUMN_VALUE)
                    .put(SOME_COLUMN_STRING, SOME_COLUMN_STRING_VALUE));
    JsonNode expected =
        mapper
            .createObjectNode()
            .put(SOME_INDEX_KEY_COLUMN, SOME_INDEX_KEY_COLUMN_VALUE)
            .put(SOME_COLUMN_STRING, SOME_COLUMN_STRING_VALUE);
    doReturn(records).when(select).invokeSubContract(Constants.CONTRACT_SCAN, ledger, argument);

    // Act
    JsonNode actual = select.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isNotNull();
    assertThat(actual.isArray()).isTrue();
    assertThat(actual.size()).isEqualTo(1);
    assertThat(actual.get(0)).isEqualTo(expected);
  }

  @Test
  public void invoke_InvalidArgumentsGiven_ShouldThrowException() {
    // Arrange
    JsonNode argument1 = mapper.createObjectNode().put(Constants.QUERY_TABLE, SOME_TABLE_NAME);
    JsonNode argument2 =
        mapper
            .createObjectNode()
            .put(SOME_INVALID_FIELD, SOME_INVALID_VALUE)
            .put(Constants.QUERY_CONDITIONS, SOME_TABLE_NAME);
    JsonNode argument3 =
        mapper
            .createObjectNode()
            .put(Constants.QUERY_TABLE, 0)
            .put(Constants.QUERY_CONDITIONS, SOME_TABLE_NAME);
    JsonNode argument4 =
        mapper
            .createObjectNode()
            .put(Constants.QUERY_TABLE, SOME_TABLE_NAME)
            .put(SOME_INVALID_FIELD, SOME_INVALID_VALUE);
    JsonNode argument5 =
        mapper
            .createObjectNode()
            .put(Constants.QUERY_TABLE, SOME_TABLE_NAME)
            .put(Constants.QUERY_CONDITIONS, SOME_INVALID_VALUE);
    JsonNode argument6 =
        mapper
            .createObjectNode()
            .put(Constants.QUERY_TABLE, SOME_TABLE_NAME)
            .put(Constants.QUERY_PROJECTIONS, SOME_INVALID_VALUE)
            .set(
                Constants.QUERY_CONDITIONS,
                mapper
                    .createArrayNode()
                    .add(
                        mapper
                            .createObjectNode()
                            .put(Constants.CONDITION_COLUMN, SOME_COLUMN_STRING)
                            .put(Constants.CONDITION_VALUE, "aaa")
                            .put(Constants.CONDITION_OPERATOR, Constants.OPERATOR_EQ)));

    // Act Assert
    assertThatThrownBy(() -> select.invoke(ledger, argument1, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_QUERY_FORMAT);
    assertThatThrownBy(() -> select.invoke(ledger, argument2, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_QUERY_FORMAT);
    assertThatThrownBy(() -> select.invoke(ledger, argument3, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_QUERY_FORMAT);
    assertThatThrownBy(() -> select.invoke(ledger, argument4, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_QUERY_FORMAT);
    assertThatThrownBy(() -> select.invoke(ledger, argument5, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_QUERY_FORMAT);
    assertThatThrownBy(() -> select.invoke(ledger, argument6, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_QUERY_FORMAT);
    verify(ledger, never()).get(anyString());
  }

  @Test
  public void invoke_InvalidArgumentsWithInvalidConditions_ShouldThrowException() {
    // Arrange
    ArrayNode conditions1 = mapper.createArrayNode().add(0);
    ArrayNode conditions2 = mapper.createArrayNode().add(mapper.createObjectNode());
    ArrayNode conditions3 =
        mapper.createArrayNode().add(mapper.createObjectNode().put(Constants.CONDITION_COLUMN, 0));
    ArrayNode conditions4 =
        mapper
            .createArrayNode()
            .add(mapper.createObjectNode().put(Constants.CONDITION_COLUMN, SOME_COLUMN_STRING));
    ArrayNode conditions5 =
        mapper
            .createArrayNode()
            .add(
                mapper
                    .createObjectNode()
                    .put(Constants.CONDITION_COLUMN, SOME_COLUMN_STRING)
                    .put(Constants.CONDITION_OPERATOR, 0));
    ArrayNode conditions6 =
        mapper
            .createArrayNode()
            .add(
                mapper
                    .createObjectNode()
                    .put(Constants.CONDITION_COLUMN, SOME_COLUMN_STRING)
                    .put(Constants.CONDITION_OPERATOR, SOME_INVALID_OPERATOR));
    ArrayNode conditions7 =
        mapper
            .createArrayNode()
            .add(
                mapper
                    .createObjectNode()
                    .put(Constants.CONDITION_COLUMN, SOME_COLUMN_STRING)
                    .put(Constants.CONDITION_OPERATOR, Constants.OPERATOR_IS_NULL))
            .add(
                mapper
                    .createObjectNode()
                    .put(Constants.CONDITION_COLUMN, SOME_COLUMN_STRING)
                    .put(Constants.CONDITION_VALUE, SOME_COLUMN_STRING_VALUE)
                    .put(Constants.CONDITION_OPERATOR, Constants.OPERATOR_IS_NOT_NULL));
    ArrayNode conditions8 =
        mapper
            .createArrayNode()
            .add(
                mapper
                    .createObjectNode()
                    .put(Constants.CONDITION_COLUMN, SOME_COLUMN_STRING)
                    .put(Constants.CONDITION_VALUE, SOME_COLUMN_STRING_VALUE)
                    .put(SOME_INVALID_FIELD, SOME_INVALID_VALUE)
                    .put(Constants.CONDITION_OPERATOR, Constants.OPERATOR_EQ));
    ArrayNode conditions9 =
        mapper
            .createArrayNode()
            .add(
                mapper
                    .createObjectNode()
                    .put(Constants.CONDITION_COLUMN, SOME_COLUMN_STRING)
                    .put(SOME_INVALID_FIELD, SOME_INVALID_VALUE)
                    .put(Constants.CONDITION_OPERATOR, Constants.OPERATOR_EQ));
    ArrayNode conditions10 =
        mapper
            .createArrayNode()
            .add(
                mapper
                    .createObjectNode()
                    .put(Constants.CONDITION_COLUMN, SOME_COLUMN_STRING)
                    .put(Constants.CONDITION_OPERATOR, Constants.OPERATOR_EQ)
                    .set(Constants.CONDITION_VALUE, null));
    ArrayNode conditions11 =
        mapper
            .createArrayNode()
            .add(
                mapper
                    .createObjectNode()
                    .put(Constants.CONDITION_COLUMN, SOME_COLUMN_STRING)
                    .put(Constants.CONDITION_VALUE, true)
                    .put(Constants.CONDITION_OPERATOR, Constants.OPERATOR_GTE));
    ArrayNode conditions12 =
        mapper
            .createArrayNode()
            .add(
                mapper
                    .createObjectNode()
                    .put(Constants.CONDITION_COLUMN, SOME_COLUMN_STRING)
                    .put(Constants.CONDITION_OPERATOR, Constants.OPERATOR_NE)
                    .set(Constants.CONDITION_VALUE, mapper.createObjectNode()));
    ArrayNode conditions13 =
        mapper
            .createArrayNode()
            .add(
                mapper
                    .createObjectNode()
                    .put(Constants.CONDITION_COLUMN, SOME_COLUMN_STRING)
                    .put(Constants.CONDITION_OPERATOR, Constants.OPERATOR_NE)
                    .set(Constants.CONDITION_VALUE, mapper.createArrayNode()));

    // Act Assert
    assertThatThrownBy(() -> select.invoke(ledger, createQueryArguments(conditions1), null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_CONDITION_FORMAT + conditions1.get(0));
    assertThatThrownBy(() -> select.invoke(ledger, createQueryArguments(conditions2), null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_CONDITION_FORMAT + conditions2.get(0));
    assertThatThrownBy(() -> select.invoke(ledger, createQueryArguments(conditions3), null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_CONDITION_FORMAT + conditions3.get(0));
    assertThatThrownBy(() -> select.invoke(ledger, createQueryArguments(conditions4), null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_CONDITION_FORMAT + conditions4.get(0));
    assertThatThrownBy(() -> select.invoke(ledger, createQueryArguments(conditions5), null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_CONDITION_FORMAT + conditions5.get(0));
    assertThatThrownBy(() -> select.invoke(ledger, createQueryArguments(conditions6), null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_OPERATOR + conditions6.get(0));
    assertThatThrownBy(() -> select.invoke(ledger, createQueryArguments(conditions7), null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_CONDITION_FORMAT + conditions7.get(1));
    assertThatThrownBy(() -> select.invoke(ledger, createQueryArguments(conditions8), null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_CONDITION_FORMAT + conditions8.get(0));
    assertThatThrownBy(() -> select.invoke(ledger, createQueryArguments(conditions9), null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_CONDITION_FORMAT + conditions9.get(0));
    assertThatThrownBy(() -> select.invoke(ledger, createQueryArguments(conditions10), null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_CONDITION_FORMAT + conditions10.get(0));
    assertThatThrownBy(() -> select.invoke(ledger, createQueryArguments(conditions11), null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_OPERATOR + conditions11.get(0));
    assertThatThrownBy(() -> select.invoke(ledger, createQueryArguments(conditions12), null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_CONDITION_FORMAT + conditions12.get(0));
    assertThatThrownBy(() -> select.invoke(ledger, createQueryArguments(conditions13), null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_CONDITION_FORMAT + conditions13.get(0));
    verify(ledger, never()).get(anyString());
  }

  @Test
  public void invoke_InvalidProjectionsGiven_ShouldThrowException() {
    ObjectNode argument = mapper.createObjectNode().put(Constants.QUERY_TABLE, SOME_TABLE_NAME);
    argument.set(
        Constants.QUERY_CONDITIONS,
        mapper
            .createArrayNode()
            .add(
                mapper
                    .createObjectNode()
                    .put(Constants.CONDITION_COLUMN, SOME_COLUMN_STRING)
                    .put(Constants.CONDITION_VALUE, "aaa")
                    .put(Constants.CONDITION_OPERATOR, Constants.OPERATOR_EQ)));
    argument.set(Constants.QUERY_PROJECTIONS, mapper.createArrayNode().add(0));

    // Act Assert
    assertThatThrownBy(() -> select.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_PROJECTION_FORMAT + "0");
  }
}
