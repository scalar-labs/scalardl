package com.scalar.dl.genericcontracts.table.v1_0_0;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class SelectTest {
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String SOME_TABLE_NAME = "Person";
  private static final String SOME_TABLE_NAME_1 = "table1";
  private static final String SOME_TABLE_NAME_2 = "table2";
  private static final String SOME_TABLE_NAME_3 = "table3";
  private static final String SOME_TABLE_PREFIX_1 = "table1.";
  private static final String SOME_TABLE_PREFIX_2 = "table2.";
  private static final String SOME_TABLE_PREFIX_3 = "table3.";
  private static final String SOME_PRIMARY_KEY_COLUMN = "GovId";
  private static final String SOME_PRIMARY_KEY_VALUE = "001";
  private static final String SOME_INDEX_KEY_COLUMN = "lastName";
  private static final String SOME_INDEX_KEY_COLUMN_VALUE = "Doe";
  private static final String SOME_KEY_TYPE_STRING = "string";
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

  private ObjectNode createQueryArguments(JsonNode conditions) {
    return createQueryArguments(SOME_TABLE_NAME, conditions);
  }

  private ObjectNode createQueryArguments(String tableName, JsonNode conditions) {
    return mapper
        .createObjectNode()
        .put(Constants.QUERY_TABLE, tableName)
        .set(Constants.QUERY_CONDITIONS, conditions);
  }

  private ObjectNode createScanArguments(String tableName, JsonNode conditions) {
    ObjectNode scan = mapper.createObjectNode();
    scan.put(Constants.QUERY_TABLE, tableName);
    scan.set(Constants.QUERY_CONDITIONS, conditions);
    scan.set(
        Constants.SCAN_OPTIONS,
        mapper.createObjectNode().put(Constants.SCAN_OPTIONS_TABLE_REFERENCE, tableName));
    return scan;
  }

  private ArrayNode createArrayNode(JsonNode... jsonNodes) {
    ArrayNode result = mapper.createArrayNode();
    Arrays.stream(jsonNodes).forEach(result::add);
    return result;
  }

  private static JsonNode createCondition(String column, String value, String operator) {
    return mapper
        .createObjectNode()
        .put(Constants.CONDITION_COLUMN, column)
        .put(Constants.CONDITION_VALUE, value)
        .put(Constants.CONDITION_OPERATOR, operator);
  }

  private ArrayNode createConditions(String column, String value) {
    return mapper.createArrayNode().add(createCondition(column, value, Constants.OPERATOR_EQ));
  }

  private static ObjectNode createIndexNode(String key, String type) {
    return mapper
        .createObjectNode()
        .put(Constants.INDEX_KEY, key)
        .put(Constants.INDEX_KEY_TYPE, type);
  }

  private static ObjectNode createTable(String tableName) {
    return mapper
        .createObjectNode()
        .put(Constants.TABLE_NAME, tableName)
        .put(Constants.TABLE_KEY, SOME_PRIMARY_KEY_COLUMN)
        .put(Constants.TABLE_KEY_TYPE, SOME_KEY_TYPE_STRING)
        .set(
            Constants.TABLE_INDEXES,
            mapper
                .createArrayNode()
                .add(createIndexNode(SOME_INDEX_KEY_COLUMN, SOME_KEY_TYPE_STRING)));
  }

  private static ObjectNode createRecord(
      String prefix, String primaryKey, String indexKey, String stringValue) {
    return mapper
        .createObjectNode()
        .put(prefix + SOME_PRIMARY_KEY_COLUMN, primaryKey)
        .put(prefix + SOME_INDEX_KEY_COLUMN, indexKey)
        .put(prefix + SOME_COLUMN_STRING, stringValue);
  }

  private Asset<JsonNode> createAsset(JsonNode data) {
    Asset<JsonNode> asset = (Asset<JsonNode>) mock(Asset.class);
    when(asset.data()).thenReturn(data);
    return asset;
  }

  private JsonNode createQueryArguments(ArrayNode conditions) {
    return mapper
        .createObjectNode()
        .put(Constants.QUERY_TABLE, SOME_TABLE_NAME)
        .set(Constants.QUERY_CONDITIONS, conditions);
  }

  @Test
  public void invoke_CorrectArgumentsWithoutJoinsGiven_ShouldReturnRecords() {
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
            .add(SOME_TABLE_NAME + Constants.COLUMN_SEPARATOR + SOME_COLUMN_STRING)
            .add(SOME_NON_EXISTING_FIELD));
    ObjectNode scan = argument.deepCopy();
    scan.remove(Constants.QUERY_PROJECTIONS);
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
    doReturn(records).when(select).invokeSubContract(Constants.CONTRACT_SCAN, ledger, scan);

    // Act
    JsonNode actual = select.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isNotNull();
    assertThat(actual.isArray()).isTrue();
    assertThat(actual.size()).isEqualTo(1);
    assertThat(actual.get(0)).isEqualTo(expected);
  }

  @Test
  public void invoke_CorrectArgumentsWithoutJoinsAndProjectionsGiven_ShouldReturnRecords() {
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
    ObjectNode scan = argument.deepCopy();
    scan.remove(Constants.QUERY_PROJECTIONS);
    JsonNode records =
        mapper
            .createArrayNode()
            .add(
                mapper
                    .createObjectNode()
                    .put(SOME_PRIMARY_KEY_COLUMN, SOME_PRIMARY_KEY_VALUE)
                    .put(SOME_INDEX_KEY_COLUMN, SOME_INDEX_KEY_COLUMN_VALUE)
                    .put(SOME_COLUMN_STRING, SOME_COLUMN_STRING_VALUE));
    doReturn(records).when(select).invokeSubContract(Constants.CONTRACT_SCAN, ledger, scan);

    // Act
    JsonNode actual = select.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isNotNull();
    assertThat(actual.isArray()).isTrue();
    assertThat(actual.size()).isEqualTo(1);
    assertThat(actual.get(0)).isEqualTo(records.get(0));
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
            .put(Constants.QUERY_TABLE, SOME_TABLE_NAME)
            .put(SOME_INVALID_FIELD, SOME_INVALID_VALUE);

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
    verify(ledger, never()).get(anyString());
  }

  @Test
  public void invoke_InvalidTableArgumentsGiven_ShouldThrowException() {
    // Arrange
    ObjectNode baseArgument =
        mapper
            .createObjectNode()
            .set(Constants.QUERY_CONDITIONS, createConditions(SOME_INDEX_KEY_COLUMN, "val"));
    JsonNode empty = mapper.createObjectNode();
    JsonNode noName =
        mapper.createObjectNode().put(Constants.ALIAS_AS, "").put(SOME_INVALID_FIELD, "");
    JsonNode nonTextName =
        mapper.createObjectNode().put(Constants.ALIAS_AS, "").put(Constants.ALIAS_NAME, 0);
    JsonNode noAlias =
        mapper.createObjectNode().put(Constants.ALIAS_NAME, "").put(SOME_INVALID_FIELD, "");
    JsonNode nonTextAlias =
        mapper.createObjectNode().put(Constants.ALIAS_NAME, "").put(Constants.ALIAS_AS, 0);
    JsonNode argument1 = baseArgument.deepCopy().set(Constants.QUERY_TABLE, empty);
    JsonNode argument2 = baseArgument.deepCopy().set(Constants.QUERY_TABLE, noName);
    JsonNode argument3 = baseArgument.deepCopy().set(Constants.QUERY_TABLE, nonTextName);
    JsonNode argument4 = baseArgument.deepCopy().set(Constants.QUERY_TABLE, noAlias);
    JsonNode argument5 = baseArgument.deepCopy().set(Constants.QUERY_TABLE, nonTextAlias);

    // Act Assert
    assertThatThrownBy(() -> select.invoke(ledger, argument1, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_QUERY_TABLE_FORMAT + empty);
    assertThatThrownBy(() -> select.invoke(ledger, argument2, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_QUERY_TABLE_FORMAT + noName);
    assertThatThrownBy(() -> select.invoke(ledger, argument3, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_QUERY_TABLE_FORMAT + nonTextName);
    assertThatThrownBy(() -> select.invoke(ledger, argument4, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_QUERY_TABLE_FORMAT + noAlias);
    assertThatThrownBy(() -> select.invoke(ledger, argument5, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_QUERY_TABLE_FORMAT + nonTextAlias);
    verify(ledger, never()).get(anyString());
  }

  @Test
  public void invoke_InvalidJoinsGiven_ShouldThrowException() {
    // Arrange
    ObjectNode baseArgument =
        mapper
            .createObjectNode()
            .put(Constants.QUERY_TABLE, SOME_TABLE_NAME)
            .set(Constants.QUERY_CONDITIONS, createConditions(SOME_INDEX_KEY_COLUMN, "val"));
    JsonNode noRequiredFields =
        createArrayNode(mapper.createObjectNode().put(SOME_INVALID_FIELD, ""));
    JsonNode noTable =
        createArrayNode(
            mapper
                .createObjectNode()
                .put(SOME_INVALID_FIELD, "")
                .put(Constants.JOIN_LEFT_KEY, SOME_TABLE_NAME + ".column")
                .put(Constants.JOIN_RIGHT_KEY, "table.column"));
    JsonNode noLeftKey =
        createArrayNode(
            mapper
                .createObjectNode()
                .put(Constants.JOIN_TABLE, "table")
                .put(SOME_INVALID_FIELD, "")
                .put(Constants.JOIN_RIGHT_KEY, "table.column"));
    JsonNode noRightKey =
        createArrayNode(
            mapper
                .createObjectNode()
                .put(Constants.JOIN_TABLE, "table")
                .put(SOME_INVALID_FIELD, "")
                .put(Constants.JOIN_LEFT_KEY, SOME_TABLE_NAME + ".column"));
    JsonNode invalidLeftKeyType =
        createArrayNode(
            mapper
                .createObjectNode()
                .put(Constants.JOIN_TABLE, "table")
                .put(Constants.JOIN_LEFT_KEY, 0)
                .put(Constants.JOIN_RIGHT_KEY, "table.column"));
    JsonNode invalidRightKeyType =
        createArrayNode(
            mapper
                .createObjectNode()
                .put(Constants.JOIN_TABLE, "table")
                .put(Constants.JOIN_LEFT_KEY, SOME_TABLE_NAME + ".column")
                .put(Constants.JOIN_RIGHT_KEY, 0));
    JsonNode invalidJoinKeyFormat =
        createArrayNode(
            mapper
                .createObjectNode()
                .put(Constants.JOIN_TABLE, "table")
                .put(Constants.JOIN_LEFT_KEY, "")
                .put(Constants.JOIN_RIGHT_KEY, ""));
    JsonNode unknownTableForLeftKey =
        createArrayNode(
            mapper
                .createObjectNode()
                .put(Constants.JOIN_TABLE, "table")
                .put(Constants.JOIN_LEFT_KEY, "unknown.column")
                .put(Constants.JOIN_RIGHT_KEY, "table.column"));
    JsonNode unknownTableForRightKey =
        createArrayNode(
            mapper
                .createObjectNode()
                .put(Constants.JOIN_TABLE, "table")
                .put(Constants.JOIN_LEFT_KEY, SOME_TABLE_NAME + ".column")
                .put(Constants.JOIN_RIGHT_KEY, "unknown.column"));
    JsonNode ambiguousTable =
        createArrayNode(
            mapper
                .createObjectNode()
                .put(Constants.JOIN_TABLE, "table")
                .put(Constants.JOIN_LEFT_KEY, SOME_TABLE_NAME + ".column")
                .put(Constants.JOIN_RIGHT_KEY, "table.column"),
            mapper
                .createObjectNode()
                .put(Constants.JOIN_TABLE, "table")
                .put(Constants.JOIN_LEFT_KEY, SOME_TABLE_NAME + ".column")
                .put(Constants.JOIN_RIGHT_KEY, "table.column"));
    JsonNode argument1 = baseArgument.deepCopy().put(Constants.QUERY_JOINS, "non-array");
    JsonNode argument2 = baseArgument.deepCopy().set(Constants.QUERY_JOINS, noRequiredFields);
    JsonNode argument3 = baseArgument.deepCopy().set(Constants.QUERY_JOINS, noTable);
    JsonNode argument4 = baseArgument.deepCopy().set(Constants.QUERY_JOINS, noLeftKey);
    JsonNode argument5 = baseArgument.deepCopy().set(Constants.QUERY_JOINS, noRightKey);
    JsonNode argument6 = baseArgument.deepCopy().set(Constants.QUERY_JOINS, invalidLeftKeyType);
    JsonNode argument7 = baseArgument.deepCopy().set(Constants.QUERY_JOINS, invalidRightKeyType);
    JsonNode argument8 = baseArgument.deepCopy().set(Constants.QUERY_JOINS, invalidJoinKeyFormat);
    JsonNode argument9 = baseArgument.deepCopy().set(Constants.QUERY_JOINS, unknownTableForLeftKey);
    JsonNode argument10 =
        baseArgument.deepCopy().set(Constants.QUERY_JOINS, unknownTableForRightKey);
    JsonNode argument11 = baseArgument.deepCopy().set(Constants.QUERY_JOINS, ambiguousTable);

    // Act Assert
    assertThatThrownBy(() -> select.invoke(ledger, argument1, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_QUERY_FORMAT);
    assertThatThrownBy(() -> select.invoke(ledger, argument2, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_JOIN_FORMAT + noRequiredFields.get(0));
    assertThatThrownBy(() -> select.invoke(ledger, argument3, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_JOIN_FORMAT + noTable.get(0));
    assertThatThrownBy(() -> select.invoke(ledger, argument4, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_JOIN_FORMAT + noLeftKey.get(0));
    assertThatThrownBy(() -> select.invoke(ledger, argument5, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_JOIN_FORMAT + noRightKey.get(0));
    assertThatThrownBy(() -> select.invoke(ledger, argument6, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_JOIN_FORMAT + invalidLeftKeyType.get(0));
    assertThatThrownBy(() -> select.invoke(ledger, argument7, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_JOIN_FORMAT + invalidRightKeyType.get(0));
    assertThatThrownBy(() -> select.invoke(ledger, argument8, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_JOIN_FORMAT + invalidJoinKeyFormat.get(0));
    assertThatThrownBy(() -> select.invoke(ledger, argument9, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_JOIN_FORMAT + unknownTableForLeftKey.get(0));
    assertThatThrownBy(() -> select.invoke(ledger, argument10, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_JOIN_FORMAT + unknownTableForRightKey.get(0));
    assertThatThrownBy(() -> select.invoke(ledger, argument11, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.TABLE_AMBIGUOUS + "table");
    verify(ledger, never()).get(anyString());
  }

  @Test
  public void invoke_InvalidConditionsGiven_ShouldThrowException() {
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
    assertThatThrownBy(() -> select.invoke(ledger, createQueryArguments(IntNode.valueOf(0)), null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_QUERY_FORMAT);
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
  public void invoke_InvalidConditionColumnsGiven_ShouldThrowException() {
    // Arrange
    ObjectNode baseScanArgument = mapper.createObjectNode().put(Constants.QUERY_TABLE, "table1");
    ObjectNode baseJoinArgument =
        baseScanArgument
            .deepCopy()
            .set(
                Constants.QUERY_JOINS,
                createArrayNode(
                    mapper
                        .createObjectNode()
                        .put(Constants.JOIN_TABLE, "table2")
                        .put(Constants.JOIN_LEFT_KEY, "table1.column")
                        .put(Constants.JOIN_RIGHT_KEY, "table2.column")));
    JsonNode noTableReference1 = createConditions("invalid-column-name", "val");
    JsonNode noTableReference2 = createConditions("column", "val");
    JsonNode withTableReference = createConditions("unknown_table.column", "val");
    JsonNode argument1 =
        baseScanArgument.deepCopy().set(Constants.QUERY_CONDITIONS, noTableReference1);
    JsonNode argument2 =
        baseJoinArgument.deepCopy().set(Constants.QUERY_CONDITIONS, noTableReference2);
    JsonNode argument3 =
        baseJoinArgument.deepCopy().set(Constants.QUERY_CONDITIONS, withTableReference);

    // Act Assert
    assertThatThrownBy(() -> select.invoke(ledger, argument1, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_COLUMN_FORMAT + "invalid-column-name");
    assertThatThrownBy(() -> select.invoke(ledger, argument2, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_COLUMN_FORMAT + "column");
    assertThatThrownBy(() -> select.invoke(ledger, argument3, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.UNKNOWN_TABLE + "unknown_table");
    verify(ledger, never()).get(anyString());
  }

  @Test
  public void invoke_InvalidProjectionsGiven_ShouldThrowException() {
    // Arrange
    ObjectNode baseScanArgument =
        mapper
            .createObjectNode()
            .put(Constants.QUERY_TABLE, "table1")
            .set(Constants.QUERY_CONDITIONS, createConditions("table1.column", "val"));
    ObjectNode baseJoinArgument =
        baseScanArgument
            .deepCopy()
            .set(
                Constants.QUERY_JOINS,
                createArrayNode(
                    mapper
                        .createObjectNode()
                        .put(Constants.JOIN_TABLE, "table2")
                        .put(Constants.JOIN_LEFT_KEY, "table1.column")
                        .put(Constants.JOIN_RIGHT_KEY, "table2.column")));
    JsonNode argument1 = baseScanArgument.deepCopy().put(Constants.QUERY_PROJECTIONS, 0);
    JsonNode argument2 =
        baseScanArgument
            .deepCopy()
            .set(Constants.QUERY_PROJECTIONS, createArrayNode(IntNode.valueOf(0)));
    JsonNode argument3 =
        baseScanArgument
            .deepCopy()
            .set(
                Constants.QUERY_PROJECTIONS,
                createArrayNode(TextNode.valueOf("invalid-column-name")));
    JsonNode argument4 =
        baseJoinArgument
            .deepCopy()
            .set(Constants.QUERY_PROJECTIONS, createArrayNode(TextNode.valueOf("column")));
    JsonNode argument5 =
        baseJoinArgument
            .deepCopy()
            .set(
                Constants.QUERY_PROJECTIONS,
                createArrayNode(TextNode.valueOf("unknown_table.column")));

    // Act Assert
    assertThatThrownBy(() -> select.invoke(ledger, argument1, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_QUERY_FORMAT);
    assertThatThrownBy(() -> select.invoke(ledger, argument2, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_PROJECTION_FORMAT + "0");
    assertThatThrownBy(() -> select.invoke(ledger, argument3, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_COLUMN_FORMAT + "invalid-column-name");
    assertThatThrownBy(() -> select.invoke(ledger, argument4, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_COLUMN_FORMAT + "column");
    assertThatThrownBy(() -> select.invoke(ledger, argument5, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.UNKNOWN_TABLE + "unknown_table");
    verify(ledger, never()).get(anyString());
  }

  @Test
  public void invoke_CorrectArgumentsWithJoinsGiven_ShouldReturnRecords() {
    // Arrange
    ObjectNode argument = mapper.createObjectNode();
    argument.put(Constants.QUERY_TABLE, SOME_TABLE_NAME_1);
    argument.set(
        Constants.QUERY_JOINS,
        mapper
            .createArrayNode()
            .add(
                mapper
                    .createObjectNode()
                    .put(Constants.JOIN_TABLE, SOME_TABLE_NAME_2)
                    .put(Constants.JOIN_LEFT_KEY, SOME_TABLE_PREFIX_1 + SOME_COLUMN_STRING)
                    .put(Constants.JOIN_RIGHT_KEY, SOME_TABLE_PREFIX_2 + SOME_PRIMARY_KEY_COLUMN))
            .add(
                mapper
                    .createObjectNode()
                    .put(Constants.JOIN_TABLE, SOME_TABLE_NAME_3)
                    .put(Constants.JOIN_LEFT_KEY, SOME_TABLE_PREFIX_2 + SOME_COLUMN_STRING)
                    .put(Constants.JOIN_RIGHT_KEY, SOME_TABLE_PREFIX_3 + SOME_INDEX_KEY_COLUMN)));
    argument.set(
        Constants.QUERY_CONDITIONS,
        mapper
            .createArrayNode()
            .add(
                mapper
                    .createObjectNode()
                    .put(Constants.CONDITION_COLUMN, SOME_TABLE_PREFIX_1 + SOME_INDEX_KEY_COLUMN)
                    .put(Constants.CONDITION_VALUE, "0")
                    .put(Constants.CONDITION_OPERATOR, Constants.OPERATOR_EQ)));
    argument.set(
        Constants.QUERY_PROJECTIONS,
        mapper
            .createArrayNode()
            .add(SOME_TABLE_PREFIX_1 + SOME_PRIMARY_KEY_COLUMN)
            .add(SOME_TABLE_PREFIX_3 + SOME_COLUMN_STRING));
    Asset<JsonNode> table1 = createAsset(createTable(SOME_TABLE_NAME_1));
    Asset<JsonNode> table2 = createAsset(createTable(SOME_TABLE_NAME_2));
    Asset<JsonNode> table3 = createAsset(createTable(SOME_TABLE_NAME_3));
    JsonNode records1 =
        mapper
            .createArrayNode()
            .add(createRecord(SOME_TABLE_PREFIX_1, "1", "0", "3"))
            .add(createRecord(SOME_TABLE_PREFIX_1, "2", "0", "4"));
    JsonNode records2 =
        mapper.createArrayNode().add(createRecord(SOME_TABLE_PREFIX_2, "3", "1", "1"));
    JsonNode records3 =
        mapper.createArrayNode().add(createRecord(SOME_TABLE_PREFIX_2, "4", "1", "2"));
    JsonNode records4 = mapper.createArrayNode();
    JsonNode records5 =
        mapper
            .createArrayNode()
            .add(createRecord(SOME_TABLE_PREFIX_3, "5", "2", "3"))
            .add(createRecord(SOME_TABLE_PREFIX_3, "6", "2", "3"));
    JsonNode expected =
        mapper
            .createArrayNode()
            .add(
                mapper
                    .createObjectNode()
                    .put(SOME_TABLE_PREFIX_1 + SOME_PRIMARY_KEY_COLUMN, "2")
                    .put(SOME_TABLE_PREFIX_3 + SOME_COLUMN_STRING, "3"))
            .add(
                mapper
                    .createObjectNode()
                    .put(SOME_TABLE_PREFIX_1 + SOME_PRIMARY_KEY_COLUMN, "2")
                    .put(SOME_TABLE_PREFIX_3 + SOME_COLUMN_STRING, "3"));
    JsonNode expectedScan1 =
        createScanArguments(SOME_TABLE_NAME_1, createConditions(SOME_INDEX_KEY_COLUMN, "0"));
    JsonNode expectedScan2 =
        createScanArguments(SOME_TABLE_NAME_2, createConditions(SOME_PRIMARY_KEY_COLUMN, "3"));
    JsonNode expectedScan3 =
        createScanArguments(SOME_TABLE_NAME_2, createConditions(SOME_PRIMARY_KEY_COLUMN, "4"));
    JsonNode expectedScan4 =
        createScanArguments(SOME_TABLE_NAME_3, createConditions(SOME_INDEX_KEY_COLUMN, "1"));
    JsonNode expectedScan5 =
        createScanArguments(SOME_TABLE_NAME_3, createConditions(SOME_INDEX_KEY_COLUMN, "2"));
    when(ledger.get(Constants.PREFIX_TABLE + SOME_TABLE_NAME_1)).thenReturn(Optional.of(table1));
    when(ledger.get(Constants.PREFIX_TABLE + SOME_TABLE_NAME_2)).thenReturn(Optional.of(table2));
    when(ledger.get(Constants.PREFIX_TABLE + SOME_TABLE_NAME_3)).thenReturn(Optional.of(table3));
    doReturn(records1)
        .doReturn(records2)
        .doReturn(records3)
        .doReturn(records4)
        .doReturn(records5)
        .when(select)
        .invokeSubContract(eq(Constants.CONTRACT_SCAN), eq(ledger), any(JsonNode.class));

    // Act
    JsonNode actual = select.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isNotNull();
    assertThat(actual).isEqualTo(expected);
    verify(select, times(5))
        .invokeSubContract(eq(Constants.CONTRACT_SCAN), eq(ledger), any(JsonNode.class));
    verify(select).invokeSubContract(Constants.CONTRACT_SCAN, ledger, expectedScan1);
    verify(select).invokeSubContract(Constants.CONTRACT_SCAN, ledger, expectedScan2);
    verify(select).invokeSubContract(Constants.CONTRACT_SCAN, ledger, expectedScan3);
    verify(select).invokeSubContract(Constants.CONTRACT_SCAN, ledger, expectedScan4);
    verify(select).invokeSubContract(Constants.CONTRACT_SCAN, ledger, expectedScan5);
  }
}
