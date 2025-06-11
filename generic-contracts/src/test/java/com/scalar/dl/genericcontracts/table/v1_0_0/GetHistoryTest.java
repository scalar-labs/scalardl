package com.scalar.dl.genericcontracts.table.v1_0_0;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.ImmutableList;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.AssetFilter.AgeOrder;
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

public class GetHistoryTest {

  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String SOME_TABLE_NAME = "tbl1";
  private static final String SOME_TABLE_KEY = "key";
  private static final String SOME_RECORD_KEY_VALUE = "key1";
  private static final String SOME_COLUMN = "column";
  private static final String SOME_COLUMN_VALUE_1 = "value1";
  private static final String SOME_COLUMN_VALUE_2 = "value2";
  private static final String SOME_UNKNOWN_TABLE_NAME = "unknown_table";
  private static final String SOME_INVALID_TABLE_NAME = "invalid-table-name";
  private static final String SOME_INVALID_COLUMN_NAME = "column-name";
  private static final String SOME_INVALID_FIELD = "filed";
  private static final String SOME_INVALID_VALUE = "value";
  private static final String SOME_KEY_TYPE_STRING = "string";
  private static final int SOME_LIMIT = 2;
  private static final JsonNode SOME_TABLE = createTable(SOME_TABLE_NAME);

  @Spy private final GetHistory getHistory = new GetHistory();
  @Mock private Ledger<JsonNode> ledger;

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this).close();
  }

  private static JsonNode createTable(String tableName) {
    return mapper
        .createObjectNode()
        .put(Constants.TABLE_NAME, tableName)
        .put(Constants.TABLE_KEY, SOME_TABLE_KEY)
        .put(Constants.TABLE_KEY_TYPE, SOME_KEY_TYPE_STRING);
  }

  private ArrayNode createArrayNode(JsonNode... jsonNodes) {
    ArrayNode result = mapper.createArrayNode();
    Arrays.stream(jsonNodes).forEach(result::add);
    return result;
  }

  private static JsonNode createCondition(String column, String value, String operator) {
    return createCondition(column, TextNode.valueOf(value), operator);
  }

  private static JsonNode createCondition(String column, JsonNode value, String operator) {
    return mapper
        .createObjectNode()
        .put(Constants.CONDITION_COLUMN, column)
        .put(Constants.CONDITION_OPERATOR, operator)
        .set(Constants.CONDITION_VALUE, value);
  }

  private String prepareTableAssetId(String tableName) {
    String assetId = GetAssetId.getAssetIdForTable(tableName);
    doReturn(assetId)
        .when(getHistory)
        .getAssetId(ledger, Constants.PREFIX_TABLE, TextNode.valueOf(tableName));
    return assetId;
  }

  private String prepareRecordAssetId(String tableName, String key, JsonNode value) {
    String assetId = GetAssetId.getAssetIdForRecord(tableName, key, value);
    doReturn(assetId)
        .when(getHistory)
        .getAssetId(
            ledger,
            Constants.PREFIX_RECORD,
            TextNode.valueOf(tableName),
            TextNode.valueOf(key),
            value);
    return assetId;
  }

  @Test
  public void invoke_CorrectArgumentsGiven_ShouldShowSingleTable() {
    // Arrange
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.QUERY_TABLE, SOME_TABLE_NAME)
            .put(Constants.QUERY_LIMIT, SOME_LIMIT)
            .set(
                Constants.QUERY_CONDITIONS,
                createArrayNode(
                    createCondition(SOME_TABLE_KEY, SOME_RECORD_KEY_VALUE, Constants.OPERATOR_EQ)));
    String recordAssetId =
        prepareRecordAssetId(
            SOME_TABLE_NAME, SOME_TABLE_KEY, TextNode.valueOf(SOME_RECORD_KEY_VALUE));
    AssetFilter filter = new AssetFilter(recordAssetId);
    filter.withAgeOrder(AgeOrder.DESC);
    filter.withLimit(SOME_LIMIT);
    JsonNode record1 =
        mapper
            .createObjectNode()
            .put(SOME_TABLE_KEY, SOME_RECORD_KEY_VALUE)
            .put(SOME_COLUMN, SOME_COLUMN_VALUE_1);
    JsonNode record2 =
        mapper
            .createObjectNode()
            .put(SOME_TABLE_KEY, SOME_RECORD_KEY_VALUE)
            .put(SOME_COLUMN, SOME_COLUMN_VALUE_2);
    Asset<JsonNode> assetRecord1 = (Asset<JsonNode>) mock(Asset.class);
    Asset<JsonNode> assetRecord2 = (Asset<JsonNode>) mock(Asset.class);
    when(assetRecord1.data()).thenReturn(record1);
    when(assetRecord2.data()).thenReturn(record2);
    when(assetRecord1.age()).thenReturn(0);
    when(assetRecord2.age()).thenReturn(1);
    when(ledger.scan(filter)).thenReturn(ImmutableList.of(assetRecord2, assetRecord1));
    String tableAssetId = prepareTableAssetId(SOME_TABLE_NAME);
    Asset<JsonNode> tableAsset = (Asset<JsonNode>) mock(Asset.class);
    when(tableAsset.data()).thenReturn(SOME_TABLE);
    when(ledger.get(tableAssetId)).thenReturn(Optional.of(tableAsset));
    JsonNode expected =
        mapper
            .createArrayNode()
            .add(
                mapper
                    .createObjectNode()
                    .put(Constants.HISTORY_ASSET_AGE, 1)
                    .set(Constants.RECORD_VALUES, record2))
            .add(
                mapper
                    .createObjectNode()
                    .put(Constants.HISTORY_ASSET_AGE, 0)
                    .set(Constants.RECORD_VALUES, record1));

    // Act
    JsonNode actual = getHistory.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isEqualTo(expected);
    verify(ledger).get(tableAssetId);
    verify(ledger).scan(filter);
  }

  @Test
  public void invoke_InvalidArgumentsGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument1 = mapper.createObjectNode().put(Constants.QUERY_TABLE, SOME_TABLE_NAME);
    JsonNode argument2 =
        mapper
            .createObjectNode()
            .put(Constants.QUERY_TABLE, SOME_TABLE_NAME)
            .put(Constants.QUERY_CONDITIONS, SOME_RECORD_KEY_VALUE)
            .put(Constants.QUERY_LIMIT, SOME_INVALID_VALUE)
            .put(SOME_INVALID_FIELD, SOME_INVALID_VALUE);
    JsonNode argument3 =
        mapper
            .createObjectNode()
            .put(Constants.QUERY_LIMIT, 0)
            .set(
                Constants.QUERY_CONDITIONS,
                createArrayNode(
                    createCondition(SOME_TABLE_KEY, SOME_RECORD_KEY_VALUE, Constants.OPERATOR_EQ)));
    JsonNode argument4 =
        mapper
            .createObjectNode()
            .put(Constants.QUERY_TABLE, 0)
            .set(
                Constants.QUERY_CONDITIONS,
                createArrayNode(
                    createCondition(SOME_TABLE_KEY, SOME_RECORD_KEY_VALUE, Constants.OPERATOR_EQ)));
    JsonNode argument5 =
        mapper
            .createObjectNode()
            .put(Constants.QUERY_TABLE, SOME_TABLE_NAME)
            .put(Constants.QUERY_LIMIT, 10);

    // Act Assert
    assertThatThrownBy(() -> getHistory.invoke(ledger, argument1, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_CONTRACT_ARGUMENTS);
    assertThatThrownBy(() -> getHistory.invoke(ledger, argument2, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_CONTRACT_ARGUMENTS);
    assertThatThrownBy(() -> getHistory.invoke(ledger, argument3, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_CONTRACT_ARGUMENTS);
    assertThatThrownBy(() -> getHistory.invoke(ledger, argument4, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_CONTRACT_ARGUMENTS);
    assertThatThrownBy(() -> getHistory.invoke(ledger, argument5, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_CONTRACT_ARGUMENTS);
    verify(ledger, never()).get(any());
    verify(ledger, never()).scan(any());
  }

  @Test
  public void invoke_InvalidConditionsGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument1 = // Non-array conditions
        mapper
            .createObjectNode()
            .put(Constants.QUERY_TABLE, SOME_TABLE_NAME)
            .put(Constants.QUERY_CONDITIONS, SOME_RECORD_KEY_VALUE);
    JsonNode argument2 = // Too many conditions
        mapper
            .createObjectNode()
            .put(Constants.QUERY_TABLE, SOME_TABLE_NAME)
            .set(
                Constants.QUERY_CONDITIONS,
                createArrayNode(
                    createCondition(SOME_TABLE_KEY, SOME_RECORD_KEY_VALUE, Constants.OPERATOR_EQ),
                    createCondition(SOME_TABLE_KEY, SOME_INVALID_VALUE, Constants.OPERATOR_EQ)));
    JsonNode argument3 = // Invalid condition format
        mapper
            .createObjectNode()
            .put(Constants.QUERY_TABLE, SOME_TABLE_NAME)
            .set(
                Constants.QUERY_CONDITIONS,
                createArrayNode(
                    mapper.createObjectNode().put(Constants.CONDITION_COLUMN, SOME_COLUMN)));
    JsonNode argument4 = // Non-equality condition
        mapper
            .createObjectNode()
            .put(Constants.QUERY_TABLE, SOME_TABLE_NAME)
            .set(
                Constants.QUERY_CONDITIONS,
                createArrayNode(
                    createCondition(SOME_TABLE_KEY, SOME_INVALID_VALUE, Constants.OPERATOR_NE)));
    JsonNode argument5 = // Unknown table reference in condition column
        mapper
            .createObjectNode()
            .put(Constants.QUERY_TABLE, SOME_COLUMN)
            .set(
                Constants.QUERY_CONDITIONS,
                createArrayNode(
                    createCondition(
                        SOME_UNKNOWN_TABLE_NAME + Constants.COLUMN_SEPARATOR + SOME_TABLE_KEY,
                        SOME_INVALID_VALUE,
                        Constants.OPERATOR_EQ)));
    JsonNode argument6 = // Invalid column name format
        mapper
            .createObjectNode()
            .put(Constants.QUERY_TABLE, SOME_COLUMN)
            .set(
                Constants.QUERY_CONDITIONS,
                createArrayNode(
                    createCondition(
                        SOME_INVALID_COLUMN_NAME, SOME_RECORD_KEY_VALUE, Constants.OPERATOR_EQ)));

    // Act Assert
    assertThatThrownBy(() -> getHistory.invoke(ledger, argument1, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_HISTORY_QUERY_CONDITION);
    assertThatThrownBy(() -> getHistory.invoke(ledger, argument2, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_HISTORY_QUERY_CONDITION);
    assertThatThrownBy(() -> getHistory.invoke(ledger, argument3, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_HISTORY_QUERY_CONDITION);
    assertThatThrownBy(() -> getHistory.invoke(ledger, argument4, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_HISTORY_QUERY_CONDITION);
    assertThatThrownBy(() -> getHistory.invoke(ledger, argument5, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.UNKNOWN_TABLE + SOME_UNKNOWN_TABLE_NAME);
    assertThatThrownBy(() -> getHistory.invoke(ledger, argument6, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_COLUMN_FORMAT + SOME_INVALID_COLUMN_NAME);
    verify(ledger, never()).get(any());
    verify(ledger, never()).scan(any());
  }

  @Test
  public void invoke_InvalidKeyTypeGiven_ShouldThrowContractContextException() {
    // Arrange
    IntNode key = IntNode.valueOf(1);
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.QUERY_TABLE, SOME_TABLE_NAME)
            .set(
                Constants.QUERY_CONDITIONS,
                createArrayNode(createCondition(SOME_TABLE_KEY, key, Constants.OPERATOR_EQ)));
    String tableAssetId = prepareTableAssetId(SOME_TABLE_NAME);
    Asset<JsonNode> tableAsset = (Asset<JsonNode>) mock(Asset.class);
    when(tableAsset.data()).thenReturn(SOME_TABLE);
    when(ledger.get(tableAssetId)).thenReturn(Optional.of(tableAsset));

    // Act Assert
    assertThatThrownBy(() -> getHistory.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_KEY_TYPE + key.getNodeType().name());
    verify(ledger).get(tableAssetId);
    verify(ledger, never()).scan(any());
  }

  @Test
  public void invoke_InvalidLimitGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument1 =
        mapper
            .createObjectNode()
            .put(Constants.QUERY_TABLE, SOME_TABLE_NAME)
            .put(Constants.QUERY_LIMIT, 1.23)
            .set(
                Constants.QUERY_CONDITIONS,
                createArrayNode(
                    createCondition(SOME_TABLE_KEY, SOME_RECORD_KEY_VALUE, Constants.OPERATOR_EQ)));
    JsonNode argument2 =
        mapper
            .createObjectNode()
            .put(Constants.QUERY_TABLE, SOME_TABLE_NAME)
            .put(Constants.QUERY_LIMIT, -1)
            .set(
                Constants.QUERY_CONDITIONS,
                createArrayNode(
                    createCondition(SOME_TABLE_KEY, SOME_RECORD_KEY_VALUE, Constants.OPERATOR_EQ)));
    String tableAssetId = prepareTableAssetId(SOME_TABLE_NAME);
    Asset<JsonNode> tableAsset = (Asset<JsonNode>) mock(Asset.class);
    when(tableAsset.data()).thenReturn(SOME_TABLE);
    when(ledger.get(tableAssetId)).thenReturn(Optional.of(tableAsset));
    prepareRecordAssetId(SOME_TABLE_NAME, SOME_TABLE_KEY, TextNode.valueOf(SOME_RECORD_KEY_VALUE));

    // Act Assert
    assertThatThrownBy(() -> getHistory.invoke(ledger, argument1, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_CONTRACT_ARGUMENTS);
    assertThatThrownBy(() -> getHistory.invoke(ledger, argument2, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_CONTRACT_ARGUMENTS);
    verify(ledger, times(2)).get(tableAssetId);
    verify(ledger, never()).scan(any());
  }

  @Test
  public void invoke_NonExistingTableGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.QUERY_TABLE, SOME_TABLE_NAME)
            .set(
                Constants.QUERY_CONDITIONS,
                createArrayNode(
                    createCondition(SOME_TABLE_KEY, SOME_RECORD_KEY_VALUE, Constants.OPERATOR_EQ)));
    String tableAssetId = prepareTableAssetId(SOME_TABLE_NAME);
    when(ledger.get(tableAssetId)).thenReturn(Optional.empty());

    // Act Assert
    assertThatThrownBy(() -> getHistory.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.TABLE_NOT_EXIST + SOME_TABLE_NAME);
    verify(ledger).get(tableAssetId);
  }

  @Test
  public void invoke_UnsupportedTableNameGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.QUERY_TABLE, SOME_INVALID_TABLE_NAME)
            .set(
                Constants.QUERY_CONDITIONS,
                createArrayNode(
                    createCondition(SOME_TABLE_KEY, SOME_RECORD_KEY_VALUE, Constants.OPERATOR_EQ)));

    // Act Assert
    assertThatThrownBy(() -> getHistory.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_OBJECT_NAME + SOME_INVALID_TABLE_NAME);
  }
}
