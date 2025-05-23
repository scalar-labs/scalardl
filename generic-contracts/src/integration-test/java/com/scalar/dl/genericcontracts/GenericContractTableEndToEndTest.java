package com.scalar.dl.genericcontracts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BigIntegerNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.genericcontracts.table.v1_0_0.Constants;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.scalar.dl.ledger.util.JacksonSerDe;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GenericContractTableEndToEndTest extends GenericContractEndToEndTestBase {
  private static final String CONTRACT_NAME_CREATE = "Create";
  private static final String CONTRACT_NAME_INSERT = "Insert";
  private static final String CONTRACT_NAME_SELECT = "Select";
  private static final String CONTRACT_NAME_UPDATE = "Update";
  private static final String CONTRACT_NAME_SHOW_TABLES = "ShowTables";
  private static final String CONTRACT_NAME_GET_HISTORY = "GetHistory";
  private static final String CONTRACT_NAME_GET_ASSET_ID = "GetAssetId";
  private static final String CONTRACT_NAME_SCAN = "Scan";
  private static final String CONTRACT_ID_PREFIX = Constants.PACKAGE + "." + Constants.VERSION;
  private static final String CONTRACT_ID_CREATE = getContractId(CONTRACT_NAME_CREATE);
  private static final String CONTRACT_ID_INSERT = getContractId(CONTRACT_NAME_INSERT);
  private static final String CONTRACT_ID_SELECT = getContractId(CONTRACT_NAME_SELECT);
  private static final String CONTRACT_ID_UPDATE = getContractId(CONTRACT_NAME_UPDATE);
  private static final String CONTRACT_ID_SHOW_TABLES = getContractId(CONTRACT_NAME_SHOW_TABLES);
  private static final String CONTRACT_ID_GET_HISTORY = getContractId(CONTRACT_NAME_GET_HISTORY);
  private static final String CONTRACT_ID_SCAN = getContractId(CONTRACT_NAME_SCAN);
  private static final String CONTRACT_ID_GET_ASSET_ID = getContractId(CONTRACT_NAME_GET_ASSET_ID);

  private static final ObjectMapper mapper = new ObjectMapper();
  private static final JacksonSerDe jacksonSerDe = new JacksonSerDe(mapper);
  private static final String KEY_COLUMN = "column";
  private static final String KEY_TYPE = "type";
  private static final String KEY_VALUE = "value";
  private static final String KEY_OPERATOR = "operator";
  private static final String KEY_EXPECTED = "expected";
  private static final String COMMON_TEST_TABLE = "test_table";
  private static final String TABLE_NAME_1 = "table1";
  private static final String TABLE_NAME_2 = "table2";
  private static final String TABLE_NAME_3 = "table3";
  private static final String TABLE_ALIAS = "tbl";
  private static final String COLUMN_NAME_1 = "col1";
  private static final String COLUMN_NAME_2 = "col2";
  private static final String COLUMN_NAME_3 = "col3";
  private static final String COLUMN_NAME_4 = "col4";
  private static final String COLUMN_NAME_5 = "col5";
  private static final String COLUMN_NAME_6 = "col6";
  private static final String COLUMN_NAME_7 = "col7";
  private static final String KEY_TYPE_STRING = "string";
  private static final String KEY_TYPE_NUMBER = "number";
  private static final String KEY_TYPE_BOOLEAN = "boolean";

  private JsonNode commonTestTable;

  @BeforeEach
  @Override
  public void setUp() {
    super.setUp();
    commonTestTable =
        createTable(
            COMMON_TEST_TABLE,
            COLUMN_NAME_1,
            KEY_TYPE_STRING,
            ImmutableMap.of(COLUMN_NAME_2, KEY_TYPE_NUMBER));
  }

  private static String getContractId(String contractName) {
    return getContractId(CONTRACT_ID_PREFIX, contractName);
  }

  private static String getContractBinaryName(String contractName) {
    return getContractBinaryName(CONTRACT_ID_PREFIX, contractName);
  }

  @Override
  Map<String, String> getContractsMap() {
    return ImmutableMap.<String, String>builder()
        .put(CONTRACT_ID_CREATE, getContractBinaryName(CONTRACT_NAME_CREATE))
        .put(CONTRACT_ID_INSERT, getContractBinaryName(CONTRACT_NAME_INSERT))
        .put(CONTRACT_ID_SELECT, getContractBinaryName(CONTRACT_NAME_SELECT))
        .put(CONTRACT_ID_UPDATE, getContractBinaryName(CONTRACT_NAME_UPDATE))
        .put(CONTRACT_ID_SHOW_TABLES, getContractBinaryName(CONTRACT_NAME_SHOW_TABLES))
        .put(CONTRACT_ID_GET_HISTORY, getContractBinaryName(CONTRACT_NAME_GET_HISTORY))
        .put(CONTRACT_ID_GET_ASSET_ID, getContractBinaryName(CONTRACT_NAME_GET_ASSET_ID))
        .put(CONTRACT_ID_SCAN, getContractBinaryName(CONTRACT_NAME_SCAN))
        .build();
  }

  @Override
  Map<String, String> getFunctionsMap() {
    return ImmutableMap.of();
  }

  private ArrayNode createArrayNode(JsonNode... jsonNodes) {
    ArrayNode result = mapper.createArrayNode();
    Arrays.stream(jsonNodes).forEach(result::add);
    return result;
  }

  private JsonNode createTable(
      String tableName, String keyColumnName, String keyColumnType, Map<String, String> indexes) {
    ArrayNode indexNodes = mapper.createArrayNode();
    indexes.forEach((column, type) -> indexNodes.add(createIndexNode(column, type)));
    JsonNode table =
        mapper
            .createObjectNode()
            .put(Constants.TABLE_NAME, tableName)
            .put(Constants.TABLE_KEY, keyColumnName)
            .put(Constants.TABLE_KEY_TYPE, keyColumnType)
            .set(Constants.TABLE_INDEXES, indexNodes);
    clientService.executeContract(CONTRACT_ID_CREATE, table);
    return table;
  }

  private static ObjectNode createIndexNode(String key, String type) {
    return mapper
        .createObjectNode()
        .put(Constants.INDEX_KEY, key)
        .put(Constants.INDEX_KEY_TYPE, type);
  }

  private List<JsonNode> insertAndGetRecords() {
    JsonNode record0 =
        prepareRecord(
            ImmutableMap.of(
                COLUMN_NAME_1,
                TextNode.valueOf("1"),
                COLUMN_NAME_2,
                IntNode.valueOf(1),
                COLUMN_NAME_3,
                TextNode.valueOf("aaa"),
                COLUMN_NAME_4,
                IntNode.valueOf(1),
                COLUMN_NAME_5,
                DoubleNode.valueOf(1.23),
                COLUMN_NAME_6,
                BooleanNode.valueOf(true),
                COLUMN_NAME_7,
                NullNode.getInstance()));
    JsonNode record1 =
        prepareRecord(
            ImmutableMap.of(
                COLUMN_NAME_1,
                TextNode.valueOf("2"),
                COLUMN_NAME_2,
                IntNode.valueOf(1),
                COLUMN_NAME_3,
                TextNode.valueOf("bbb"),
                COLUMN_NAME_4,
                IntNode.valueOf(2),
                COLUMN_NAME_5,
                DoubleNode.valueOf(2.34),
                COLUMN_NAME_6,
                BooleanNode.valueOf(false),
                COLUMN_NAME_7,
                createArrayNode()));
    JsonNode record2 =
        prepareRecord(
            ImmutableMap.of(
                COLUMN_NAME_1,
                TextNode.valueOf("3"),
                COLUMN_NAME_2,
                IntNode.valueOf(1),
                COLUMN_NAME_3,
                TextNode.valueOf("ccc"),
                COLUMN_NAME_4,
                IntNode.valueOf(3),
                COLUMN_NAME_5,
                DoubleNode.valueOf(3.45),
                COLUMN_NAME_6,
                BooleanNode.valueOf(true)));
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(COMMON_TEST_TABLE, record0));
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(COMMON_TEST_TABLE, record1));
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(COMMON_TEST_TABLE, record2));
    return ImmutableList.of(record0, record1, record2);
  }

  private void insertRecord(String tableName, int primaryKey, int indexKey) {
    JsonNode record =
        prepareRecord(
            ImmutableMap.of(
                COLUMN_NAME_1,
                IntNode.valueOf(primaryKey),
                COLUMN_NAME_2,
                IntNode.valueOf(indexKey)));
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(tableName, record));
  }

  private void insertRecord(
      String tableName, int primaryKey, int indexKey, int value1, int value2) {
    JsonNode record =
        prepareRecord(
            ImmutableMap.of(
                COLUMN_NAME_1,
                IntNode.valueOf(primaryKey),
                COLUMN_NAME_2,
                IntNode.valueOf(indexKey),
                COLUMN_NAME_3,
                IntNode.valueOf(value1),
                COLUMN_NAME_4,
                IntNode.valueOf(value2)));
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(tableName, record));
  }

  private void prepareJoinTables() {
    createTable(
        TABLE_NAME_1,
        COLUMN_NAME_1,
        KEY_TYPE_NUMBER,
        ImmutableMap.of(COLUMN_NAME_2, KEY_TYPE_NUMBER));
    insertRecord(TABLE_NAME_1, 1, 20250101, 101, 300);
    insertRecord(TABLE_NAME_1, 2, 20250101, 201, 400);
    insertRecord(TABLE_NAME_1, 3, 20250101);
    insertRecord(TABLE_NAME_1, 4, 20251231, 301, 500);
    createTable(
        TABLE_NAME_2,
        COLUMN_NAME_1,
        KEY_TYPE_NUMBER,
        ImmutableMap.of(COLUMN_NAME_2, KEY_TYPE_NUMBER));
    insertRecord(TABLE_NAME_2, 101, 0, 0, 0);
    insertRecord(TABLE_NAME_2, 201, 1, 1, 1);
    createTable(
        TABLE_NAME_3,
        COLUMN_NAME_1,
        KEY_TYPE_NUMBER,
        ImmutableMap.of(COLUMN_NAME_2, KEY_TYPE_NUMBER));
    insertRecord(TABLE_NAME_3, 111, 400, 0, 0);
    insertRecord(TABLE_NAME_3, 222, 400, 1, 1);
    insertRecord(TABLE_NAME_3, 333, 300, 0, 0);
    insertRecord(TABLE_NAME_3, 444, 300, 0, 0);
  }

  private ObjectNode prepareRecord(Map<String, JsonNode> map) {
    ObjectNode record = mapper.createObjectNode();
    map.forEach(record::set);
    return record;
  }

  private JsonNode prepareInsert(String tableName, JsonNode values) {
    return mapper
        .createObjectNode()
        .put(Constants.RECORD_TABLE, tableName)
        .set(Constants.RECORD_VALUES, values);
  }

  private ObjectNode prepareSelect(
      String tableName, String keyColumnName, JsonNode value, JsonNode... conditions) {
    List<JsonNode> conditionList = new ArrayList<>();
    conditionList.add(prepareCondition(keyColumnName, value, Constants.OPERATOR_EQ));
    conditionList.addAll(Arrays.asList(conditions));
    return mapper
        .createObjectNode()
        .put(Constants.QUERY_TABLE, tableName)
        .set(Constants.QUERY_CONDITIONS, prepareArrayNode(conditionList));
  }

  private ObjectNode prepareSelect(String tableName, String keyColumnName) {
    return mapper
        .createObjectNode()
        .put(Constants.QUERY_TABLE, tableName)
        .set(
            Constants.QUERY_CONDITIONS,
            prepareArrayNode(
                ImmutableList.of(prepareCondition(keyColumnName, Constants.OPERATOR_IS_NULL))));
  }

  private ObjectNode prepareUpdate(
      String tableName,
      JsonNode updateValues,
      String keyColumnName,
      JsonNode keyColumnValue,
      JsonNode... conditions) {
    return prepareUpdate(
        tableName, updateValues, keyColumnName, keyColumnValue, Constants.OPERATOR_EQ, conditions);
  }

  private ObjectNode prepareUpdate(
      String tableName,
      JsonNode updateValues,
      String keyColumnName,
      JsonNode keyColumnValue,
      String operator,
      JsonNode... conditions) {
    List<JsonNode> conditionList = new ArrayList<>();
    if (operator.equalsIgnoreCase(Constants.OPERATOR_IS_NULL)) {
      conditionList.add(prepareCondition(keyColumnName, operator));
    } else {
      conditionList.add(prepareCondition(keyColumnName, keyColumnValue, operator));
    }
    conditionList.addAll(Arrays.asList(conditions));
    ObjectNode update = mapper.createObjectNode().put(Constants.UPDATE_TABLE, tableName);
    update.set(Constants.UPDATE_VALUES, updateValues);
    update.set(Constants.UPDATE_CONDITIONS, prepareArrayNode(conditionList));
    return update;
  }

  private ArrayNode prepareArrayNode(List<JsonNode> jsonNodes) {
    ArrayNode result = mapper.createArrayNode();
    jsonNodes.forEach(result::add);
    return result;
  }

  private JsonNode prepareTableAlias(String name, String alias) {
    return mapper.createObjectNode().put(Constants.ALIAS_NAME, name).put(Constants.ALIAS_AS, alias);
  }

  private JsonNode prepareJoin(JsonNode table, String leftColumn, String rightColumn) {
    ObjectNode join = mapper.createObjectNode();
    join.set(Constants.JOIN_TABLE, table);
    join.put(Constants.JOIN_LEFT_KEY, leftColumn);
    join.put(Constants.JOIN_RIGHT_KEY, rightColumn);
    return join;
  }

  private JsonNode prepareCondition(String column, String operator) {
    return mapper
        .createObjectNode()
        .put(Constants.CONDITION_COLUMN, column)
        .put(Constants.CONDITION_OPERATOR, operator);
  }

  private JsonNode prepareCondition(String column, JsonNode value, String operator) {
    return mapper
        .createObjectNode()
        .put(Constants.CONDITION_COLUMN, column)
        .put(Constants.CONDITION_OPERATOR, operator)
        .set(Constants.CONDITION_VALUE, value);
  }

  private void runConditionTestCasesInParallel(ImmutableTable<Integer, String, JsonNode> caseTable)
      throws ExecutionException, InterruptedException {
    List<Callable<Void>> testCallables = new ArrayList<>();
    caseTable
        .rowMap()
        .forEach(
            (id, row) -> {
              testCallables.add(
                  () -> {
                    select_AdditionalConditionsGiven_ShouldSelectSingleRecordProperly(
                        id,
                        row.get(KEY_COLUMN).asText(),
                        row.get(KEY_VALUE),
                        row.get(KEY_OPERATOR).asText(),
                        row.get(KEY_EXPECTED));
                    return null;
                  });
            });

    executeInParallel(testCallables);
  }

  private void addTestCase(
      ImmutableTable.Builder<Integer, String, JsonNode> caseBuilder,
      int caseId,
      Map<String, JsonNode> row) {
    row.forEach((column, value) -> caseBuilder.put(caseId, column, value));
  }

  private String description(String table, String column, String value) {
    return String.format("failed with table: %s, column: %s, value: %s", table, column, value);
  }

  private String description(int caseId, String column, JsonNode value, String operator) {
    return String.format(
        "failed with case: %d, column: %s, value: %s, operator: %s",
        caseId, column, value.asText(), operator);
  }

  private void assertSelectResult(JsonNode actual, List<JsonNode> expected) {
    List<JsonNode> records = new ArrayList<>();
    assertThat(actual.isArray()).isTrue();
    actual.forEach(records::add);
    assertThat(records).containsExactlyInAnyOrderElementsOf(expected);
  }

  private void assertSelectResult(JsonNode actual, List<JsonNode> expected, String description) {
    List<JsonNode> records = new ArrayList<>();
    assertThat(actual.isArray()).describedAs(description).isTrue();
    actual.forEach(records::add);
    assertThat(records).describedAs(description).containsExactlyInAnyOrderElementsOf(expected);
  }

  private void assertSelectResult(JsonNode actual, JsonNode expected, String description) {
    List<JsonNode> expectedRecords = new ArrayList<>();
    expected.forEach(expectedRecords::add);
    assertSelectResult(actual, expectedRecords, description);
  }

  @Test
  public void insert_NonExistingRecordGiven_ShouldInsertRecordProperly() {
    // Arrange
    JsonNode key = TextNode.valueOf("key");
    JsonNode value = IntNode.valueOf(1);
    JsonNode record = prepareRecord(ImmutableMap.of(COLUMN_NAME_1, key, COLUMN_NAME_2, value));
    JsonNode insert = prepareInsert(COMMON_TEST_TABLE, record);
    JsonNode select = prepareSelect(COMMON_TEST_TABLE, COLUMN_NAME_1, key);
    JsonNode expected = createArrayNode(record);

    // Act
    clientService.executeContract(CONTRACT_ID_INSERT, insert);

    // Assert
    ContractExecutionResult actual = clientService.executeContract(CONTRACT_ID_SELECT, select);
    assertThat(actual.getContractResult()).isPresent();
    assertThat(actual.getContractResult().get()).isEqualTo(jacksonSerDe.serialize(expected));
  }

  @Test
  public void insert_ExistingRecordGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode key = TextNode.valueOf("key");
    JsonNode value1 = IntNode.valueOf(1);
    JsonNode value2 = IntNode.valueOf(2);
    JsonNode record1 = prepareRecord(ImmutableMap.of(COLUMN_NAME_1, key, COLUMN_NAME_2, value1));
    JsonNode record2 = prepareRecord(ImmutableMap.of(COLUMN_NAME_1, key, COLUMN_NAME_2, value2));
    JsonNode insert1 = prepareInsert(COMMON_TEST_TABLE, record1);
    JsonNode insert2 = prepareInsert(COMMON_TEST_TABLE, record2);
    clientService.executeContract(CONTRACT_ID_INSERT, insert1);

    // Act Assert
    assertThatThrownBy(() -> clientService.executeContract(CONTRACT_ID_INSERT, insert2))
        .isExactlyInstanceOf(ClientException.class)
        .hasMessage(Constants.RECORD_ALREADY_EXISTS);
  }

  @Test
  public void insert_RecordForNonExistingTableGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode key = TextNode.valueOf("key");
    JsonNode value = IntNode.valueOf(1);
    JsonNode record = prepareRecord(ImmutableMap.of(COLUMN_NAME_1, key, COLUMN_NAME_2, value));
    JsonNode insert = prepareInsert(TABLE_NAME_1, record);

    // Act Assert
    assertThatThrownBy(() -> clientService.executeContract(CONTRACT_ID_INSERT, insert))
        .isExactlyInstanceOf(ClientException.class)
        .hasMessage(Constants.TABLE_NOT_EXIST);
  }

  @Test
  public void insert_EqualButDifferentNumericTypePrimaryKeysGiven_ShouldNotInsertRecords() {
    // Arrange
    createTable(TABLE_NAME_1, COLUMN_NAME_1, KEY_TYPE_NUMBER, ImmutableMap.of());
    JsonNode key1 = IntNode.valueOf(1);
    JsonNode record1 = prepareRecord(ImmutableMap.of(COLUMN_NAME_1, key1));
    JsonNode key2 = DoubleNode.valueOf(1.0);
    JsonNode record2 = prepareRecord(ImmutableMap.of(COLUMN_NAME_1, key2));
    JsonNode key3 = LongNode.valueOf(1L);
    JsonNode record3 = prepareRecord(ImmutableMap.of(COLUMN_NAME_1, key3));
    JsonNode key4 = DecimalNode.valueOf(new BigDecimal("1.000"));
    JsonNode record4 = prepareRecord(ImmutableMap.of(COLUMN_NAME_1, key4));
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(TABLE_NAME_1, record1));

    // Act Assert
    assertThatThrownBy(
            () ->
                clientService.executeContract(
                    CONTRACT_ID_INSERT, prepareInsert(TABLE_NAME_1, record2)))
        .isExactlyInstanceOf(ClientException.class)
        .hasMessage(Constants.RECORD_ALREADY_EXISTS);
    assertThatThrownBy(
            () ->
                clientService.executeContract(
                    CONTRACT_ID_INSERT, prepareInsert(TABLE_NAME_1, record3)))
        .isExactlyInstanceOf(ClientException.class)
        .hasMessage(Constants.RECORD_ALREADY_EXISTS);
    assertThatThrownBy(
            () ->
                clientService.executeContract(
                    CONTRACT_ID_INSERT, prepareInsert(TABLE_NAME_1, record4)))
        .isExactlyInstanceOf(ClientException.class)
        .hasMessage(Constants.RECORD_ALREADY_EXISTS);
  }

  @Test
  public void select_PrimaryKeyConditionGiven_ShouldSelectSingleRecordProperly()
      throws ExecutionException, InterruptedException {
    ImmutableTable<String, String, JsonNode> cases =
        new ImmutableTable.Builder<String, String, JsonNode>()
            .put("tbl1", KEY_TYPE, TextNode.valueOf(KEY_TYPE_STRING))
            .put("tbl1", KEY_VALUE, TextNode.valueOf("1"))
            .put("tbl2", KEY_TYPE, TextNode.valueOf(KEY_TYPE_NUMBER))
            .put("tbl2", KEY_VALUE, IntNode.valueOf(1))
            .put("tbl3", KEY_TYPE, TextNode.valueOf(KEY_TYPE_NUMBER))
            .put("tbl3", KEY_VALUE, DoubleNode.valueOf(1.2345))
            .put("tbl4", KEY_TYPE, TextNode.valueOf(KEY_TYPE_BOOLEAN))
            .put("tbl4", KEY_VALUE, BooleanNode.valueOf(false))
            .build();
    cases
        .rowMap()
        .forEach(
            (tableName, row) ->
                createTable(
                    tableName, COLUMN_NAME_1, row.get(KEY_TYPE).asText(), ImmutableMap.of()));

    List<Callable<Void>> testCallables = new ArrayList<>();
    cases
        .rowMap()
        .forEach(
            (tableName, row) -> {
              testCallables.add(
                  () -> {
                    select_PrimaryKeyConditionGiven_ShouldSelectSingleRecordProperly(
                        tableName, row.get(KEY_VALUE));
                    return null;
                  });
            });

    executeInParallel(testCallables);
  }

  private void select_PrimaryKeyConditionGiven_ShouldSelectSingleRecordProperly(
      String tableName, JsonNode value) {
    // Arrange
    JsonNode record = prepareRecord(ImmutableMap.of(COLUMN_NAME_1, value));
    JsonNode select = prepareSelect(tableName, COLUMN_NAME_1, value);
    JsonNode expected = createArrayNode(record);
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(tableName, record));

    // Act
    ContractExecutionResult actual = clientService.executeContract(CONTRACT_ID_SELECT, select);

    // Assert
    assertThat(actual.getContractResult()).isPresent();
    assertThat(actual.getContractResult().get()).isEqualTo(jacksonSerDe.serialize(expected));
  }

  @Test
  public void select_ProjectionsGiven_ShouldSelectSingleRecordProperly() {
    // Arrange
    JsonNode record1 =
        prepareRecord(
            ImmutableMap.of(
                COLUMN_NAME_1,
                TextNode.valueOf("1"),
                COLUMN_NAME_2,
                IntNode.valueOf(1),
                COLUMN_NAME_3,
                TextNode.valueOf("0"),
                COLUMN_NAME_4,
                TextNode.valueOf("aaa")));
    JsonNode record2 =
        prepareRecord(
            ImmutableMap.of(
                COLUMN_NAME_1,
                TextNode.valueOf("2"),
                COLUMN_NAME_2,
                IntNode.valueOf(1),
                COLUMN_NAME_3,
                IntNode.valueOf(0),
                COLUMN_NAME_5,
                TextNode.valueOf("aaa")));
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(COMMON_TEST_TABLE, record1));
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(COMMON_TEST_TABLE, record2));
    JsonNode select =
        prepareSelect(COMMON_TEST_TABLE, COLUMN_NAME_2, IntNode.valueOf(1))
            .set(
                Constants.QUERY_PROJECTIONS,
                createArrayNode(
                    TextNode.valueOf(
                        COMMON_TEST_TABLE + Constants.COLUMN_SEPARATOR + COLUMN_NAME_1),
                    TextNode.valueOf(COLUMN_NAME_3),
                    TextNode.valueOf(COLUMN_NAME_4),
                    TextNode.valueOf(COLUMN_NAME_5)));
    ObjectNode expectedRecord1 = record1.deepCopy();
    ObjectNode expectedRecord2 = record2.deepCopy();
    expectedRecord1.remove(COLUMN_NAME_2);
    expectedRecord2.remove(COLUMN_NAME_2);

    // Act
    ContractExecutionResult actual = clientService.executeContract(CONTRACT_ID_SELECT, select);

    // Assert
    assertThat(actual.getContractResult()).isPresent();
    assertSelectResult(
        jacksonSerDe.deserialize(actual.getContractResult().get()),
        ImmutableList.of(expectedRecord1, expectedRecord2));
  }

  @Test
  public void select_IndexKeyConditionGiven_ShouldSelectRecordsProperly()
      throws ExecutionException, InterruptedException {
    ImmutableTable<String, String, JsonNode> cases =
        new ImmutableTable.Builder<String, String, JsonNode>()
            .put("tbl1", KEY_COLUMN, TextNode.valueOf(COLUMN_NAME_2))
            .put("tbl1", KEY_TYPE, TextNode.valueOf(KEY_TYPE_STRING))
            .put("tbl1", KEY_VALUE, TextNode.valueOf("abc"))
            .put("tbl2", KEY_COLUMN, TextNode.valueOf(COLUMN_NAME_3))
            .put("tbl2", KEY_TYPE, TextNode.valueOf(KEY_TYPE_NUMBER))
            .put("tbl2", KEY_VALUE, IntNode.valueOf(-10))
            .put("tbl3", KEY_COLUMN, TextNode.valueOf(COLUMN_NAME_4))
            .put("tbl3", KEY_TYPE, TextNode.valueOf(KEY_TYPE_NUMBER))
            .put("tbl3", KEY_VALUE, DoubleNode.valueOf(1.23))
            .put("tbl4", KEY_COLUMN, TextNode.valueOf(COLUMN_NAME_5))
            .put("tbl4", KEY_TYPE, TextNode.valueOf(KEY_TYPE_BOOLEAN))
            .put("tbl4", KEY_VALUE, BooleanNode.valueOf(true))
            .build();
    cases
        .rowMap()
        .forEach(
            (tableName, row) -> {
              // prepare test table
              createTable(
                  tableName,
                  COLUMN_NAME_1,
                  KEY_TYPE_STRING,
                  ImmutableMap.of(row.get(KEY_COLUMN).asText(), row.get(KEY_TYPE).asText()));
              // prepare a record that does not match the condition is the test case
              JsonNode record =
                  mapper
                      .createObjectNode()
                      .put(COLUMN_NAME_1, "key")
                      .put(COLUMN_NAME_2, "val")
                      .put(COLUMN_NAME_3, 10)
                      .put(COLUMN_NAME_4, 1.2345)
                      .put(COLUMN_NAME_5, false);
              clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(tableName, record));
            });

    List<Callable<Void>> testCallables = new ArrayList<>();
    cases
        .rowMap()
        .forEach(
            (tableName, row) -> {
              testCallables.add(
                  () -> {
                    select_IndexKeyConditionGiven_ShouldSelectRecordsProperly(
                        tableName, row.get(KEY_COLUMN).asText(), row.get(KEY_VALUE));
                    return null;
                  });
            });

    executeInParallel(testCallables);
  }

  private void select_IndexKeyConditionGiven_ShouldSelectRecordsProperly(
      String tableName, String indexColumnName, JsonNode value) {
    // Arrange
    JsonNode key1 = TextNode.valueOf("key1");
    JsonNode key2 = TextNode.valueOf("key2");
    JsonNode record1 = prepareRecord(ImmutableMap.of(COLUMN_NAME_1, key1, indexColumnName, value));
    JsonNode record2 = prepareRecord(ImmutableMap.of(COLUMN_NAME_1, key2, indexColumnName, value));
    JsonNode select = prepareSelect(tableName, indexColumnName, value);
    ImmutableList<JsonNode> expected = ImmutableList.of(record1, record2);
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(tableName, record1));
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(tableName, record2));

    // Act
    ContractExecutionResult actual = clientService.executeContract(CONTRACT_ID_SELECT, select);

    // Assert
    assertThat(actual.getContractResult()).isPresent();
    assertSelectResult(
        jacksonSerDe.deserialize(actual.getContractResult().get()),
        expected,
        description(tableName, indexColumnName, value.asText()));
  }

  @Test
  public void select_NullIndexKeyConditionGiven_ShouldSelectRecordsProperly()
      throws ExecutionException, InterruptedException {
    createTable(
        TABLE_NAME_1,
        COLUMN_NAME_1,
        KEY_TYPE_STRING,
        ImmutableMap.of(
            COLUMN_NAME_2,
            KEY_TYPE_STRING,
            COLUMN_NAME_3,
            KEY_TYPE_NUMBER,
            COLUMN_NAME_4,
            KEY_TYPE_BOOLEAN));
    JsonNode record0 =
        prepareRecord(
            ImmutableMap.of(
                COLUMN_NAME_1,
                TextNode.valueOf("1"),
                COLUMN_NAME_2,
                TextNode.valueOf("aaa"),
                COLUMN_NAME_3,
                IntNode.valueOf(1),
                COLUMN_NAME_4,
                BooleanNode.valueOf(true)));
    JsonNode record1 =
        prepareRecord(
            ImmutableMap.of(
                COLUMN_NAME_1,
                TextNode.valueOf("2"),
                COLUMN_NAME_2,
                NullNode.getInstance(),
                COLUMN_NAME_3,
                NullNode.getInstance(),
                COLUMN_NAME_4,
                NullNode.getInstance()));
    JsonNode record2 =
        prepareRecord(
            ImmutableMap.of(
                COLUMN_NAME_1,
                TextNode.valueOf("3"),
                COLUMN_NAME_2,
                NullNode.getInstance(),
                COLUMN_NAME_3,
                NullNode.getInstance(),
                COLUMN_NAME_4,
                NullNode.getInstance()));
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(TABLE_NAME_1, record0));
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(TABLE_NAME_1, record1));
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(TABLE_NAME_1, record2));

    List<Callable<Void>> testCallables = new ArrayList<>();
    ImmutableList.of(COLUMN_NAME_2, COLUMN_NAME_3, COLUMN_NAME_4)
        .forEach(
            column ->
                testCallables.add(
                    () -> {
                      select_NullIndexKeyConditionGiven_ShouldSelectRecordsProperly(
                          column, createArrayNode(record1, record2));
                      return null;
                    }));

    executeInParallel(testCallables);
  }

  private void select_NullIndexKeyConditionGiven_ShouldSelectRecordsProperly(
      String columnName, JsonNode expected) {
    // Arrange
    JsonNode select = prepareSelect(TABLE_NAME_1, columnName);

    // Act
    ContractExecutionResult actual = clientService.executeContract(CONTRACT_ID_SELECT, select);

    // Assert
    assertThat(actual.getContractResult()).isPresent();
    assertSelectResult(
        jacksonSerDe.deserialize(actual.getContractResult().get()),
        expected,
        description(TABLE_NAME_1, columnName, NullNode.getInstance().asText()));
  }

  @Test
  public void select_AdditionalStringConditionsGiven_ShouldSelectRecordsProperly()
      throws ExecutionException, InterruptedException {
    List<JsonNode> records = insertAndGetRecords();
    ImmutableTable.Builder<Integer, String, JsonNode> caseBuilder = new ImmutableTable.Builder<>();
    addTestCase(
        caseBuilder,
        1,
        ImmutableMap.of(
            KEY_COLUMN, TextNode.valueOf(COLUMN_NAME_3),
            KEY_VALUE, TextNode.valueOf("bbb"),
            KEY_OPERATOR, TextNode.valueOf(Constants.OPERATOR_EQ),
            KEY_EXPECTED, createArrayNode(records.get(1))));
    addTestCase(
        caseBuilder,
        2,
        ImmutableMap.of(
            KEY_COLUMN, TextNode.valueOf(COLUMN_NAME_3),
            KEY_VALUE, TextNode.valueOf("bbb"),
            KEY_OPERATOR, TextNode.valueOf(Constants.OPERATOR_NE),
            KEY_EXPECTED, createArrayNode(records.get(0), records.get(2))));
    addTestCase(
        caseBuilder,
        3,
        ImmutableMap.of(
            KEY_COLUMN, TextNode.valueOf(COLUMN_NAME_3),
            KEY_VALUE, TextNode.valueOf("bbb"),
            KEY_OPERATOR, TextNode.valueOf(Constants.OPERATOR_LT),
            KEY_EXPECTED, createArrayNode(records.get(0))));
    addTestCase(
        caseBuilder,
        4,
        ImmutableMap.of(
            KEY_COLUMN, TextNode.valueOf(COLUMN_NAME_3),
            KEY_VALUE, TextNode.valueOf("bbb"),
            KEY_OPERATOR, TextNode.valueOf(Constants.OPERATOR_LTE),
            KEY_EXPECTED, createArrayNode(records.get(0), records.get(1))));
    addTestCase(
        caseBuilder,
        5,
        ImmutableMap.of(
            KEY_COLUMN, TextNode.valueOf(COLUMN_NAME_3),
            KEY_VALUE, TextNode.valueOf("bbb"),
            KEY_OPERATOR, TextNode.valueOf(Constants.OPERATOR_GT),
            KEY_EXPECTED, createArrayNode(records.get(2))));
    addTestCase(
        caseBuilder,
        6,
        ImmutableMap.of(
            KEY_COLUMN, TextNode.valueOf(COLUMN_NAME_3),
            KEY_VALUE, TextNode.valueOf("bbb"),
            KEY_OPERATOR, TextNode.valueOf(Constants.OPERATOR_GTE),
            KEY_EXPECTED, createArrayNode(records.get(1), records.get(2))));

    runConditionTestCasesInParallel(caseBuilder.build());
  }

  @Test
  public void select_AdditionalIntConditionsGiven_ShouldSelectRecordsProperly()
      throws ExecutionException, InterruptedException {
    List<JsonNode> records = insertAndGetRecords();
    ImmutableTable.Builder<Integer, String, JsonNode> caseBuilder = new ImmutableTable.Builder<>();
    addTestCase(
        caseBuilder,
        1,
        ImmutableMap.of(
            KEY_COLUMN, TextNode.valueOf(COLUMN_NAME_4),
            KEY_VALUE, IntNode.valueOf(2),
            KEY_OPERATOR, TextNode.valueOf(Constants.OPERATOR_EQ),
            KEY_EXPECTED, createArrayNode(records.get(1))));
    addTestCase(
        caseBuilder,
        2,
        ImmutableMap.of(
            KEY_COLUMN, TextNode.valueOf(COLUMN_NAME_4),
            KEY_VALUE, IntNode.valueOf(2),
            KEY_OPERATOR, TextNode.valueOf(Constants.OPERATOR_NE),
            KEY_EXPECTED, createArrayNode(records.get(0), records.get(2))));
    addTestCase(
        caseBuilder,
        3,
        ImmutableMap.of(
            KEY_COLUMN, TextNode.valueOf(COLUMN_NAME_4),
            KEY_VALUE, IntNode.valueOf(2),
            KEY_OPERATOR, TextNode.valueOf(Constants.OPERATOR_LT),
            KEY_EXPECTED, createArrayNode(records.get(0))));
    addTestCase(
        caseBuilder,
        4,
        ImmutableMap.of(
            KEY_COLUMN, TextNode.valueOf(COLUMN_NAME_4),
            KEY_VALUE, IntNode.valueOf(2),
            KEY_OPERATOR, TextNode.valueOf(Constants.OPERATOR_LTE),
            KEY_EXPECTED, createArrayNode(records.get(0), records.get(1))));
    addTestCase(
        caseBuilder,
        5,
        ImmutableMap.of(
            KEY_COLUMN, TextNode.valueOf(COLUMN_NAME_4),
            KEY_VALUE, IntNode.valueOf(2),
            KEY_OPERATOR, TextNode.valueOf(Constants.OPERATOR_GT),
            KEY_EXPECTED, createArrayNode(records.get(2))));
    addTestCase(
        caseBuilder,
        6,
        ImmutableMap.of(
            KEY_COLUMN, TextNode.valueOf(COLUMN_NAME_4),
            KEY_VALUE, IntNode.valueOf(2),
            KEY_OPERATOR, TextNode.valueOf(Constants.OPERATOR_GTE),
            KEY_EXPECTED, createArrayNode(records.get(1), records.get(2))));

    runConditionTestCasesInParallel(caseBuilder.build());
  }

  @Test
  public void select_AdditionalDoubleConditionsGiven_ShouldSelectRecordsProperly()
      throws ExecutionException, InterruptedException {
    List<JsonNode> records = insertAndGetRecords();
    ImmutableTable.Builder<Integer, String, JsonNode> caseBuilder = new ImmutableTable.Builder<>();
    addTestCase(
        caseBuilder,
        1,
        ImmutableMap.of(
            KEY_COLUMN, TextNode.valueOf(COLUMN_NAME_5),
            KEY_VALUE, DoubleNode.valueOf(2.34),
            KEY_OPERATOR, TextNode.valueOf(Constants.OPERATOR_EQ),
            KEY_EXPECTED, createArrayNode(records.get(1))));
    addTestCase(
        caseBuilder,
        2,
        ImmutableMap.of(
            KEY_COLUMN, TextNode.valueOf(COLUMN_NAME_5),
            KEY_VALUE, DoubleNode.valueOf(2.34),
            KEY_OPERATOR, TextNode.valueOf(Constants.OPERATOR_NE),
            KEY_EXPECTED, createArrayNode(records.get(0), records.get(2))));
    addTestCase(
        caseBuilder,
        3,
        ImmutableMap.of(
            KEY_COLUMN, TextNode.valueOf(COLUMN_NAME_5),
            KEY_VALUE, DoubleNode.valueOf(2.34),
            KEY_OPERATOR, TextNode.valueOf(Constants.OPERATOR_LT),
            KEY_EXPECTED, createArrayNode(records.get(0))));
    addTestCase(
        caseBuilder,
        4,
        ImmutableMap.of(
            KEY_COLUMN, TextNode.valueOf(COLUMN_NAME_5),
            KEY_VALUE, DoubleNode.valueOf(2.34),
            KEY_OPERATOR, TextNode.valueOf(Constants.OPERATOR_LTE),
            KEY_EXPECTED, createArrayNode(records.get(0), records.get(1))));
    addTestCase(
        caseBuilder,
        5,
        ImmutableMap.of(
            KEY_COLUMN, TextNode.valueOf(COLUMN_NAME_5),
            KEY_VALUE, DoubleNode.valueOf(2.34),
            KEY_OPERATOR, TextNode.valueOf(Constants.OPERATOR_GT),
            KEY_EXPECTED, createArrayNode(records.get(2))));
    addTestCase(
        caseBuilder,
        6,
        ImmutableMap.of(
            KEY_COLUMN, TextNode.valueOf(COLUMN_NAME_5),
            KEY_VALUE, DoubleNode.valueOf(2.34),
            KEY_OPERATOR, TextNode.valueOf(Constants.OPERATOR_GTE),
            KEY_EXPECTED, createArrayNode(records.get(1), records.get(2))));
    addTestCase(
        caseBuilder,
        7,
        ImmutableMap.of(
            KEY_COLUMN, TextNode.valueOf(COLUMN_NAME_5),
            KEY_VALUE, IntNode.valueOf(2),
            KEY_OPERATOR, TextNode.valueOf(Constants.OPERATOR_EQ),
            KEY_EXPECTED, createArrayNode()));
    addTestCase(
        caseBuilder,
        8,
        ImmutableMap.of(
            KEY_COLUMN, TextNode.valueOf(COLUMN_NAME_5),
            KEY_VALUE, IntNode.valueOf(2),
            KEY_OPERATOR, TextNode.valueOf(Constants.OPERATOR_NE),
            KEY_EXPECTED, createArrayNode(records.get(0), records.get(1), records.get(2))));
    addTestCase(
        caseBuilder,
        9,
        ImmutableMap.of(
            KEY_COLUMN, TextNode.valueOf(COLUMN_NAME_5),
            KEY_VALUE, IntNode.valueOf(2),
            KEY_OPERATOR, TextNode.valueOf(Constants.OPERATOR_GT),
            KEY_EXPECTED, createArrayNode(records.get(1), records.get(2))));

    runConditionTestCasesInParallel(caseBuilder.build());
  }

  @Test
  public void select_AdditionalBooleanConditionsGiven_ShouldSelectRecordsProperly()
      throws ExecutionException, InterruptedException {
    List<JsonNode> records = insertAndGetRecords();
    ImmutableTable.Builder<Integer, String, JsonNode> caseBuilder = new ImmutableTable.Builder<>();
    addTestCase(
        caseBuilder,
        1,
        ImmutableMap.of(
            KEY_COLUMN, TextNode.valueOf(COLUMN_NAME_6),
            KEY_VALUE, BooleanNode.valueOf(true),
            KEY_OPERATOR, TextNode.valueOf(Constants.OPERATOR_EQ),
            KEY_EXPECTED, createArrayNode(records.get(0), records.get(2))));
    addTestCase(
        caseBuilder,
        2,
        ImmutableMap.of(
            KEY_COLUMN, TextNode.valueOf(COLUMN_NAME_6),
            KEY_VALUE, BooleanNode.valueOf(true),
            KEY_OPERATOR, TextNode.valueOf(Constants.OPERATOR_NE),
            KEY_EXPECTED, createArrayNode(records.get(1))));

    runConditionTestCasesInParallel(caseBuilder.build());
  }

  private void select_AdditionalConditionsGiven_ShouldSelectSingleRecordProperly(
      int caseId, String columnName, JsonNode value, String operator, JsonNode expected) {
    // Arrange
    ObjectNode select =
        prepareSelect(
            COMMON_TEST_TABLE,
            COLUMN_NAME_2,
            IntNode.valueOf(1),
            prepareCondition(columnName, value, operator));

    // Act
    ContractExecutionResult actual = clientService.executeContract(CONTRACT_ID_SELECT, select);

    // Assert
    assertThat(actual.getContractResult()).isPresent();
    assertSelectResult(
        jacksonSerDe.deserialize(actual.getContractResult().get()),
        expected,
        description(caseId, columnName, value, operator));
  }

  @Test
  public void select_AdditionalNullConditionsGiven_ShouldSelectRecordsProperly()
      throws ExecutionException, InterruptedException {
    List<JsonNode> records = insertAndGetRecords();
    ImmutableTable.Builder<Integer, String, JsonNode> caseBuilder = new ImmutableTable.Builder<>();
    addTestCase(
        caseBuilder,
        1,
        ImmutableMap.of(
            KEY_COLUMN, TextNode.valueOf(COLUMN_NAME_7),
            KEY_OPERATOR, TextNode.valueOf(Constants.OPERATOR_IS_NULL),
            KEY_EXPECTED, createArrayNode(records.get(0), records.get(2))));
    addTestCase(
        caseBuilder,
        2,
        ImmutableMap.of(
            KEY_COLUMN, TextNode.valueOf(COLUMN_NAME_7),
            KEY_OPERATOR, TextNode.valueOf(Constants.OPERATOR_IS_NOT_NULL),
            KEY_EXPECTED, createArrayNode(records.get(1))));
    addTestCase(
        caseBuilder,
        3,
        ImmutableMap.of(
            KEY_COLUMN, TextNode.valueOf(COLUMN_NAME_3),
            KEY_OPERATOR, TextNode.valueOf(Constants.OPERATOR_IS_NULL),
            KEY_EXPECTED, createArrayNode()));
    addTestCase(
        caseBuilder,
        4,
        ImmutableMap.of(
            KEY_COLUMN, TextNode.valueOf(COLUMN_NAME_3),
            KEY_OPERATOR, TextNode.valueOf(Constants.OPERATOR_IS_NOT_NULL),
            KEY_EXPECTED, createArrayNode(records.get(0), records.get(1), records.get(2))));

    List<Callable<Void>> testCallables = new ArrayList<>();
    caseBuilder
        .build()
        .rowMap()
        .forEach(
            (id, row) -> {
              testCallables.add(
                  () -> {
                    select_AdditionalNullConditionsGiven_ShouldSelectSingleRecordProperly(
                        id,
                        row.get(KEY_COLUMN).asText(),
                        row.get(KEY_OPERATOR).asText(),
                        row.get(KEY_EXPECTED));
                    return null;
                  });
            });

    executeInParallel(testCallables);
  }

  private void select_AdditionalNullConditionsGiven_ShouldSelectSingleRecordProperly(
      int caseId, String columnName, String operator, JsonNode expected) {
    // Arrange
    ObjectNode select =
        prepareSelect(
            COMMON_TEST_TABLE,
            COLUMN_NAME_2,
            IntNode.valueOf(1),
            prepareCondition(columnName, operator));

    // Act
    ContractExecutionResult actual = clientService.executeContract(CONTRACT_ID_SELECT, select);

    // Assert
    assertThat(actual.getContractResult()).isPresent();
    assertSelectResult(
        jacksonSerDe.deserialize(actual.getContractResult().get()),
        expected,
        description(caseId, columnName, NullNode.getInstance(), operator));
  }

  @Test
  public void select_JoinsAndProjectionsGiven_ShouldSelectSingleRecordProperly() {
    // Arrange
    prepareJoinTables();
    String columnReference1 = TABLE_NAME_1 + Constants.COLUMN_SEPARATOR + COLUMN_NAME_1;
    String columnReference2 = TABLE_NAME_1 + Constants.COLUMN_SEPARATOR + COLUMN_NAME_2;
    String columnReference3 = TABLE_NAME_2 + Constants.COLUMN_SEPARATOR + COLUMN_NAME_1;
    String columnReference4 = TABLE_NAME_2 + Constants.COLUMN_SEPARATOR + COLUMN_NAME_2;
    String columnReference5 = TABLE_ALIAS + Constants.COLUMN_SEPARATOR + COLUMN_NAME_1;
    String columnReference6 = TABLE_ALIAS + Constants.COLUMN_SEPARATOR + COLUMN_NAME_2;
    String columnReference7 = TABLE_ALIAS + Constants.COLUMN_SEPARATOR + COLUMN_NAME_3;
    ObjectNode select =
        prepareSelect(
            TABLE_NAME_1,
            columnReference2,
            IntNode.valueOf(20250101),
            prepareCondition(columnReference7, IntNode.valueOf(0), Constants.OPERATOR_EQ));
    JsonNode joinTable2 =
        prepareJoin(
            TextNode.valueOf(TABLE_NAME_2),
            TABLE_NAME_1 + Constants.COLUMN_SEPARATOR + COLUMN_NAME_3,
            TABLE_NAME_2 + Constants.COLUMN_SEPARATOR + COLUMN_NAME_1);
    JsonNode table3 = prepareTableAlias(TABLE_NAME_3, TABLE_ALIAS);
    JsonNode joinTable3 =
        prepareJoin(
            table3,
            TABLE_NAME_1 + Constants.COLUMN_SEPARATOR + COLUMN_NAME_4,
            TABLE_ALIAS + Constants.COLUMN_SEPARATOR + COLUMN_NAME_2);
    select.set(Constants.QUERY_JOINS, createArrayNode(joinTable2, joinTable3));
    select.set(
        Constants.QUERY_PROJECTIONS,
        createArrayNode(
            TextNode.valueOf(columnReference1),
            TextNode.valueOf(columnReference3),
            TextNode.valueOf(columnReference4),
            TextNode.valueOf(columnReference5),
            TextNode.valueOf(columnReference6),
            TextNode.valueOf(columnReference7)));
    JsonNode record1 =
        prepareRecord(
            ImmutableMap.of(
                columnReference1, IntNode.valueOf(1),
                columnReference3, IntNode.valueOf(101),
                columnReference4, IntNode.valueOf(0),
                columnReference5, IntNode.valueOf(333),
                columnReference6, IntNode.valueOf(300),
                columnReference7, IntNode.valueOf(0)));
    JsonNode record2 =
        prepareRecord(
            ImmutableMap.of(
                columnReference1, IntNode.valueOf(1),
                columnReference3, IntNode.valueOf(101),
                columnReference4, IntNode.valueOf(0),
                columnReference5, IntNode.valueOf(444),
                columnReference6, IntNode.valueOf(300),
                columnReference7, IntNode.valueOf(0)));
    JsonNode record3 =
        prepareRecord(
            ImmutableMap.of(
                columnReference1, IntNode.valueOf(2),
                columnReference3, IntNode.valueOf(201),
                columnReference4, IntNode.valueOf(1),
                columnReference5, IntNode.valueOf(111),
                columnReference6, IntNode.valueOf(400),
                columnReference7, IntNode.valueOf(0)));

    // Act
    ContractExecutionResult actual = clientService.executeContract(CONTRACT_ID_SELECT, select);

    // Assert
    assertThat(actual.getContractResult()).isPresent();
    assertSelectResult(
        jacksonSerDe.deserialize(actual.getContractResult().get()),
        ImmutableList.of(record1, record2, record3));
  }

  @Test
  public void select_JoinOnDifferentKeyTypeGiven_ShouldNotReturnRecords() {
    // Arrange
    createTable(TABLE_NAME_1, COLUMN_NAME_1, KEY_TYPE_NUMBER, ImmutableMap.of());
    clientService.executeContract(
        CONTRACT_ID_INSERT,
        prepareInsert(
            TABLE_NAME_1,
            prepareRecord(
                ImmutableMap.of(
                    COLUMN_NAME_1, IntNode.valueOf(1), COLUMN_NAME_2, IntNode.valueOf(1)))));
    createTable(TABLE_NAME_2, COLUMN_NAME_1, KEY_TYPE_STRING, ImmutableMap.of());
    clientService.executeContract(
        CONTRACT_ID_INSERT,
        prepareInsert(
            TABLE_NAME_2,
            prepareRecord(
                ImmutableMap.of(
                    COLUMN_NAME_1, TextNode.valueOf("1"), COLUMN_NAME_2, IntNode.valueOf(1)))));

    String columnReference1 = TABLE_NAME_1 + Constants.COLUMN_SEPARATOR + COLUMN_NAME_1;
    String columnReference2 = TABLE_NAME_1 + Constants.COLUMN_SEPARATOR + COLUMN_NAME_2;
    String columnReference3 = TABLE_NAME_2 + Constants.COLUMN_SEPARATOR + COLUMN_NAME_1;
    ObjectNode select = prepareSelect(TABLE_NAME_1, columnReference1, IntNode.valueOf(1));
    JsonNode join = prepareJoin(TextNode.valueOf(TABLE_NAME_2), columnReference2, columnReference3);
    select.set(Constants.QUERY_JOINS, createArrayNode(join));

    // Act
    ContractExecutionResult actual = clientService.executeContract(CONTRACT_ID_SELECT, select);

    // Assert
    assertThat(actual.getContractResult()).isPresent();
    assertSelectResult(
        jacksonSerDe.deserialize(actual.getContractResult().get()), ImmutableList.of());
  }

  @Test
  public void
      select_RecordsWithJsonObjectColumnAndNumericTypeConditionGiven_ShouldNotReturnRecords() {
    // Arrange
    JsonNode record1 =
        prepareRecord(
            ImmutableMap.of(
                COLUMN_NAME_1,
                TextNode.valueOf("aaa"),
                COLUMN_NAME_2,
                IntNode.valueOf(1),
                COLUMN_NAME_3,
                mapper.createObjectNode()));
    JsonNode record2 =
        prepareRecord(
            ImmutableMap.of(
                COLUMN_NAME_1,
                TextNode.valueOf("bbb"),
                COLUMN_NAME_2,
                IntNode.valueOf(1),
                COLUMN_NAME_3,
                mapper.createArrayNode()));
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(COMMON_TEST_TABLE, record1));
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(COMMON_TEST_TABLE, record2));
    JsonNode select =
        prepareSelect(
            COMMON_TEST_TABLE,
            COLUMN_NAME_2,
            IntNode.valueOf(1),
            prepareCondition(COLUMN_NAME_3, IntNode.valueOf(10), Constants.OPERATOR_NE));

    // Act
    ContractExecutionResult actual = clientService.executeContract(CONTRACT_ID_SELECT, select);

    // Assert
    assertThat(actual.getContractResult()).isPresent();
    assertSelectResult(
        jacksonSerDe.deserialize(actual.getContractResult().get()), ImmutableList.of());
  }

  @Test
  public void select_JsonObjectTypeConditionsGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode condition1 =
        prepareCondition(COLUMN_NAME_3, mapper.createObjectNode(), Constants.OPERATOR_EQ);
    JsonNode condition2 =
        prepareCondition(COLUMN_NAME_3, mapper.createArrayNode(), Constants.OPERATOR_EQ);
    JsonNode select1 =
        prepareSelect(COMMON_TEST_TABLE, COLUMN_NAME_2, IntNode.valueOf(1), condition1);
    JsonNode select2 =
        prepareSelect(COMMON_TEST_TABLE, COLUMN_NAME_2, IntNode.valueOf(1), condition2);

    // Act Assert
    assertThatThrownBy(() -> clientService.executeContract(CONTRACT_ID_SELECT, select1))
        .isExactlyInstanceOf(ClientException.class)
        .hasMessage(Constants.INVALID_CONDITION_FORMAT + condition1);
    assertThatThrownBy(() -> clientService.executeContract(CONTRACT_ID_SELECT, select2))
        .isExactlyInstanceOf(ClientException.class)
        .hasMessage(Constants.INVALID_CONDITION_FORMAT + condition2);
  }

  @Test
  public void select_EqualButDifferentNumericTypePrimaryKeysGiven_ShouldReturnRecordProperly() {
    // Arrange
    createTable(TABLE_NAME_1, COLUMN_NAME_1, KEY_TYPE_NUMBER, ImmutableMap.of());
    JsonNode oneInt = IntNode.valueOf(1);
    JsonNode oneDouble = DoubleNode.valueOf(1.0);
    JsonNode oneLong = LongNode.valueOf(1L);
    JsonNode oneDecimal = DecimalNode.valueOf(new BigDecimal("1.000"));
    JsonNode twoInt = IntNode.valueOf(2);
    JsonNode twoDouble = DoubleNode.valueOf(2.0);
    JsonNode twoLong = LongNode.valueOf(2L);
    JsonNode twoDecimal = DecimalNode.valueOf(new BigDecimal("2.000"));
    JsonNode recordOne = prepareRecord(ImmutableMap.of(COLUMN_NAME_1, oneInt));
    JsonNode recordTwo = prepareRecord(ImmutableMap.of(COLUMN_NAME_1, twoDouble));
    JsonNode selectOneByDouble = prepareSelect(TABLE_NAME_1, COLUMN_NAME_1, oneDouble);
    JsonNode selectOneByLong = prepareSelect(TABLE_NAME_1, COLUMN_NAME_1, oneLong);
    JsonNode selectOneByDecimal = prepareSelect(TABLE_NAME_1, COLUMN_NAME_1, oneDecimal);
    JsonNode selectTwoByInt = prepareSelect(TABLE_NAME_1, COLUMN_NAME_1, twoInt);
    JsonNode selectTwoByLong = prepareSelect(TABLE_NAME_1, COLUMN_NAME_1, twoLong);
    JsonNode selectTwoByDecimal = prepareSelect(TABLE_NAME_1, COLUMN_NAME_1, twoDecimal);
    ImmutableList<JsonNode> expectedOne = ImmutableList.of(recordOne);
    ImmutableList<JsonNode> expectedTwo = ImmutableList.of(recordTwo);
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(TABLE_NAME_1, recordOne));
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(TABLE_NAME_1, recordTwo));

    // Act
    ContractExecutionResult actual1 =
        clientService.executeContract(CONTRACT_ID_SELECT, selectOneByDouble);
    ContractExecutionResult actual2 =
        clientService.executeContract(CONTRACT_ID_SELECT, selectOneByLong);
    ContractExecutionResult actual3 =
        clientService.executeContract(CONTRACT_ID_SELECT, selectOneByDecimal);
    ContractExecutionResult actual4 =
        clientService.executeContract(CONTRACT_ID_SELECT, selectTwoByInt);
    ContractExecutionResult actual5 =
        clientService.executeContract(CONTRACT_ID_SELECT, selectTwoByLong);
    ContractExecutionResult actual6 =
        clientService.executeContract(CONTRACT_ID_SELECT, selectTwoByDecimal);

    // Assert
    assertThat(actual1.getContractResult()).isPresent();
    assertThat(actual2.getContractResult()).isPresent();
    assertThat(actual3.getContractResult()).isPresent();
    assertThat(actual4.getContractResult()).isPresent();
    assertThat(actual5.getContractResult()).isPresent();
    assertThat(actual6.getContractResult()).isPresent();
    assertSelectResult(jacksonSerDe.deserialize(actual1.getContractResult().get()), expectedOne);
    assertSelectResult(jacksonSerDe.deserialize(actual2.getContractResult().get()), expectedOne);
    assertSelectResult(jacksonSerDe.deserialize(actual3.getContractResult().get()), expectedOne);
    assertSelectResult(jacksonSerDe.deserialize(actual4.getContractResult().get()), expectedTwo);
    assertSelectResult(jacksonSerDe.deserialize(actual5.getContractResult().get()), expectedTwo);
    assertSelectResult(jacksonSerDe.deserialize(actual6.getContractResult().get()), expectedTwo);
  }

  @Test
  public void select_EqualButDifferentNumericTypeIndexValuesGiven_ShouldReturnRecordsProperly() {
    // Arrange
    JsonNode key1 = TextNode.valueOf("aaa");
    JsonNode value1 = IntNode.valueOf(1);
    JsonNode record1 = prepareRecord(ImmutableMap.of(COLUMN_NAME_1, key1, COLUMN_NAME_2, value1));
    JsonNode key2 = TextNode.valueOf("bbb");
    JsonNode value2 = DoubleNode.valueOf(1.0);
    JsonNode record2 = prepareRecord(ImmutableMap.of(COLUMN_NAME_1, key2, COLUMN_NAME_2, value2));
    JsonNode key3 = TextNode.valueOf("ccc");
    JsonNode value3 = LongNode.valueOf(1L);
    JsonNode record3 = prepareRecord(ImmutableMap.of(COLUMN_NAME_1, key3, COLUMN_NAME_2, value3));
    JsonNode key4 = TextNode.valueOf("ddd");
    JsonNode value4 = DecimalNode.valueOf(new BigDecimal("1.000"));
    JsonNode record4 = prepareRecord(ImmutableMap.of(COLUMN_NAME_1, key4, COLUMN_NAME_2, value4));
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(COMMON_TEST_TABLE, record1));
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(COMMON_TEST_TABLE, record2));
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(COMMON_TEST_TABLE, record3));
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(COMMON_TEST_TABLE, record4));
    JsonNode selectByInt = prepareSelect(COMMON_TEST_TABLE, COLUMN_NAME_2, value1);
    JsonNode selectByDouble = prepareSelect(COMMON_TEST_TABLE, COLUMN_NAME_2, value2);
    JsonNode selectByLong = prepareSelect(COMMON_TEST_TABLE, COLUMN_NAME_2, value3);
    JsonNode selectByDecimal = prepareSelect(COMMON_TEST_TABLE, COLUMN_NAME_2, value4);
    ImmutableList<JsonNode> expected =
        ImmutableList.of(
            record1,
            record2,
            prepareRecord(ImmutableMap.of(COLUMN_NAME_1, key3, COLUMN_NAME_2, value1)),
            prepareRecord(ImmutableMap.of(COLUMN_NAME_1, key4, COLUMN_NAME_2, value2)));

    // Act
    ContractExecutionResult actual1 =
        clientService.executeContract(CONTRACT_ID_SELECT, selectByInt);
    ContractExecutionResult actual2 =
        clientService.executeContract(CONTRACT_ID_SELECT, selectByDouble);
    ContractExecutionResult actual3 =
        clientService.executeContract(CONTRACT_ID_SELECT, selectByLong);
    ContractExecutionResult actual4 =
        clientService.executeContract(CONTRACT_ID_SELECT, selectByDecimal);

    // Assert
    assertThat(actual1.getContractResult()).isPresent();
    assertThat(actual2.getContractResult()).isPresent();
    assertThat(actual3.getContractResult()).isPresent();
    assertThat(actual4.getContractResult()).isPresent();
    assertSelectResult(jacksonSerDe.deserialize(actual1.getContractResult().get()), expected);
    assertSelectResult(jacksonSerDe.deserialize(actual2.getContractResult().get()), expected);
    assertSelectResult(jacksonSerDe.deserialize(actual3.getContractResult().get()), expected);
    assertSelectResult(jacksonSerDe.deserialize(actual4.getContractResult().get()), expected);
  }

  @Test
  public void select_HighPrecisionIndexValuesGiven_ShouldReturnRecordsWithLosingPrecision() {
    // Arrange
    JsonNode key1 = TextNode.valueOf("aaa");
    JsonNode value1 = DoubleNode.valueOf(1.2345678901234567);
    JsonNode record1 = prepareRecord(ImmutableMap.of(COLUMN_NAME_1, key1, COLUMN_NAME_2, value1));
    JsonNode key2 = TextNode.valueOf("bbb");
    JsonNode value2 = DoubleNode.valueOf(1.2345678901234568);
    JsonNode record2 = prepareRecord(ImmutableMap.of(COLUMN_NAME_1, key2, COLUMN_NAME_2, value2));
    JsonNode key3 = TextNode.valueOf("ccc");
    JsonNode value3 = DecimalNode.valueOf(new BigDecimal("1.2345678901234567890123456789"));
    JsonNode record3 = prepareRecord(ImmutableMap.of(COLUMN_NAME_1, key3, COLUMN_NAME_2, value3));
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(COMMON_TEST_TABLE, record1));
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(COMMON_TEST_TABLE, record2));
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(COMMON_TEST_TABLE, record3));
    JsonNode selectByDouble = prepareSelect(COMMON_TEST_TABLE, COLUMN_NAME_2, value1);
    JsonNode selectByDecimal = prepareSelect(COMMON_TEST_TABLE, COLUMN_NAME_2, value3);
    ImmutableList<JsonNode> expected =
        ImmutableList.of(
            record1,
            record2,
            prepareRecord(ImmutableMap.of(COLUMN_NAME_1, key3, COLUMN_NAME_2, value1)));

    // Act
    ContractExecutionResult actual1 =
        clientService.executeContract(CONTRACT_ID_SELECT, selectByDouble);
    ContractExecutionResult actual2 =
        clientService.executeContract(CONTRACT_ID_SELECT, selectByDecimal);

    // Assert
    assertThat(actual1.getContractResult()).isPresent();
    assertThat(actual2.getContractResult()).isPresent();
    assertSelectResult(jacksonSerDe.deserialize(actual1.getContractResult().get()), expected);
    assertSelectResult(jacksonSerDe.deserialize(actual2.getContractResult().get()), expected);
  }

  @Test
  public void select_MinAndMaxLongIndexValuesGiven_ShouldReturnRecordsProperly() {
    // Arrange
    JsonNode key1 = TextNode.valueOf("aaa");
    JsonNode value1 = LongNode.valueOf(Long.MIN_VALUE);
    JsonNode record1 = prepareRecord(ImmutableMap.of(COLUMN_NAME_1, key1, COLUMN_NAME_2, value1));
    JsonNode key2 = TextNode.valueOf("bbb");
    JsonNode value2 = LongNode.valueOf(Long.MAX_VALUE);
    JsonNode record2 = prepareRecord(ImmutableMap.of(COLUMN_NAME_1, key2, COLUMN_NAME_2, value2));
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(COMMON_TEST_TABLE, record1));
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(COMMON_TEST_TABLE, record2));
    JsonNode selectByMin = prepareSelect(COMMON_TEST_TABLE, COLUMN_NAME_2, value1);
    JsonNode selectByMax = prepareSelect(COMMON_TEST_TABLE, COLUMN_NAME_2, value2);

    // Act
    ContractExecutionResult actual1 =
        clientService.executeContract(CONTRACT_ID_SELECT, selectByMin);
    ContractExecutionResult actual2 =
        clientService.executeContract(CONTRACT_ID_SELECT, selectByMax);

    // Assert
    assertThat(actual1.getContractResult()).isPresent();
    assertThat(actual2.getContractResult()).isPresent();
    assertSelectResult(
        jacksonSerDe.deserialize(actual1.getContractResult().get()), ImmutableList.of(record1));
    assertSelectResult(
        jacksonSerDe.deserialize(actual2.getContractResult().get()), ImmutableList.of(record2));
  }

  @Test
  public void select_BigIntegerIndexValuesGiven_ShouldReturnRecordsProperly() {
    // Arrange
    JsonNode key1 = TextNode.valueOf("aaa");
    JsonNode value1 =
        BigIntegerNode.valueOf(BigInteger.valueOf(Long.MIN_VALUE).multiply(BigInteger.TEN));
    JsonNode record1 = prepareRecord(ImmutableMap.of(COLUMN_NAME_1, key1, COLUMN_NAME_2, value1));
    JsonNode key2 = TextNode.valueOf("bbb");
    JsonNode value2 =
        BigIntegerNode.valueOf(BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.TEN));
    JsonNode record2 = prepareRecord(ImmutableMap.of(COLUMN_NAME_1, key2, COLUMN_NAME_2, value2));
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(COMMON_TEST_TABLE, record1));
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(COMMON_TEST_TABLE, record2));
    JsonNode selectByNegative = prepareSelect(COMMON_TEST_TABLE, COLUMN_NAME_2, value1);
    JsonNode selectByPositive = prepareSelect(COMMON_TEST_TABLE, COLUMN_NAME_2, value2);

    // Act
    ContractExecutionResult actual1 =
        clientService.executeContract(CONTRACT_ID_SELECT, selectByNegative);
    ContractExecutionResult actual2 =
        clientService.executeContract(CONTRACT_ID_SELECT, selectByPositive);

    // Assert
    assertThat(actual1.getContractResult()).isPresent();
    assertThat(actual2.getContractResult()).isPresent();
    assertSelectResult(
        jacksonSerDe.deserialize(actual1.getContractResult().get()), ImmutableList.of(record1));
    assertSelectResult(
        jacksonSerDe.deserialize(actual2.getContractResult().get()), ImmutableList.of(record2));
  }

  @Test
  public void select_ConditionWithMaxLongValuesGiven_ShouldReturnRecordsWithoutLosingPrecision() {
    // Arrange
    JsonNode indexValue = IntNode.valueOf(1);
    JsonNode key1 = TextNode.valueOf("aaa");
    JsonNode value1 = LongNode.valueOf(Long.MAX_VALUE);
    JsonNode record1 =
        prepareRecord(
            ImmutableMap.of(COLUMN_NAME_1, key1, COLUMN_NAME_2, indexValue, COLUMN_NAME_3, value1));
    JsonNode key2 = TextNode.valueOf("bbb");
    JsonNode value2 = LongNode.valueOf(Long.MAX_VALUE - 1);
    JsonNode record2 =
        prepareRecord(
            ImmutableMap.of(COLUMN_NAME_1, key2, COLUMN_NAME_2, indexValue, COLUMN_NAME_3, value2));
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(COMMON_TEST_TABLE, record1));
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(COMMON_TEST_TABLE, record2));
    JsonNode selectEq =
        prepareSelect(
            COMMON_TEST_TABLE,
            COLUMN_NAME_2,
            indexValue,
            prepareCondition(COLUMN_NAME_3, value1, Constants.OPERATOR_EQ));
    JsonNode selectNe =
        prepareSelect(
            COMMON_TEST_TABLE,
            COLUMN_NAME_2,
            indexValue,
            prepareCondition(COLUMN_NAME_3, value1, Constants.OPERATOR_NE));

    // Act
    ContractExecutionResult actual1 = clientService.executeContract(CONTRACT_ID_SELECT, selectEq);
    ContractExecutionResult actual2 = clientService.executeContract(CONTRACT_ID_SELECT, selectNe);

    // Assert
    assertThat(actual1.getContractResult()).isPresent();
    assertThat(actual2.getContractResult()).isPresent();
    assertSelectResult(
        jacksonSerDe.deserialize(actual1.getContractResult().get()), ImmutableList.of(record1));
    assertSelectResult(
        jacksonSerDe.deserialize(actual2.getContractResult().get()), ImmutableList.of(record2));
  }

  @Test
  public void
      select_ConditionWithBigIntegerValuesGiven_ShouldReturnRecordsWithoutLosingPrecision() {
    // Arrange
    JsonNode indexValue = IntNode.valueOf(1);
    JsonNode key1 = TextNode.valueOf("aaa");
    JsonNode value1 =
        BigIntegerNode.valueOf(BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.TEN));
    JsonNode record1 =
        prepareRecord(
            ImmutableMap.of(COLUMN_NAME_1, key1, COLUMN_NAME_2, indexValue, COLUMN_NAME_3, value1));
    JsonNode key2 = TextNode.valueOf("bbb");
    JsonNode value2 =
        BigIntegerNode.valueOf(
            BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.TEN).add(BigInteger.ONE));
    JsonNode record2 =
        prepareRecord(
            ImmutableMap.of(COLUMN_NAME_1, key2, COLUMN_NAME_2, indexValue, COLUMN_NAME_3, value2));
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(COMMON_TEST_TABLE, record1));
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(COMMON_TEST_TABLE, record2));
    JsonNode selectEq =
        prepareSelect(
            COMMON_TEST_TABLE,
            COLUMN_NAME_2,
            indexValue,
            prepareCondition(COLUMN_NAME_3, value1, Constants.OPERATOR_EQ));
    JsonNode selectNe =
        prepareSelect(
            COMMON_TEST_TABLE,
            COLUMN_NAME_2,
            indexValue,
            prepareCondition(COLUMN_NAME_3, value1, Constants.OPERATOR_NE));

    // Act
    ContractExecutionResult actual1 = clientService.executeContract(CONTRACT_ID_SELECT, selectEq);
    ContractExecutionResult actual2 = clientService.executeContract(CONTRACT_ID_SELECT, selectNe);

    // Assert
    assertThat(actual1.getContractResult()).isPresent();
    assertThat(actual2.getContractResult()).isPresent();
    assertSelectResult(
        jacksonSerDe.deserialize(actual1.getContractResult().get()), ImmutableList.of(record1));
    assertSelectResult(
        jacksonSerDe.deserialize(actual2.getContractResult().get()), ImmutableList.of(record2));
  }

  @Test
  public void select_NonExistingTableGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode select = prepareSelect(TABLE_NAME_1, COLUMN_NAME_1, IntNode.valueOf(1));

    // Act Assert
    assertThatThrownBy(() -> clientService.executeContract(CONTRACT_ID_SELECT, select))
        .isExactlyInstanceOf(ClientException.class)
        .hasMessage(Constants.TABLE_NOT_EXIST);
  }

  @Test
  public void select_DifferentTypeKeyConditionGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode select = prepareSelect(COMMON_TEST_TABLE, COLUMN_NAME_1, IntNode.valueOf(1));

    // Act Assert
    assertThatThrownBy(() -> clientService.executeContract(CONTRACT_ID_SELECT, select))
        .isExactlyInstanceOf(ClientException.class)
        .hasMessage(Constants.INVALID_KEY_TYPE);
  }

  @Test
  public void select_DifferentTypeIndexConditionGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode select = prepareSelect(COMMON_TEST_TABLE, COLUMN_NAME_2, TextNode.valueOf("a"));

    // Act Assert
    assertThatThrownBy(() -> clientService.executeContract(CONTRACT_ID_SELECT, select))
        .isExactlyInstanceOf(ClientException.class)
        .hasMessage(Constants.INVALID_INDEX_KEY_TYPE);
  }

  @Test
  public void select_WithoutKeyConditionsGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode select = prepareSelect(COMMON_TEST_TABLE, COLUMN_NAME_3, IntNode.valueOf(1));

    // Act Assert
    assertThatThrownBy(() -> clientService.executeContract(CONTRACT_ID_SELECT, select))
        .isExactlyInstanceOf(ClientException.class)
        .hasMessage(Constants.INVALID_KEY_SPECIFICATION);
  }

  @Test
  public void select_JoinOnNonKeyRightColumnGiven_ShouldThrowContractContextException() {
    // Arrange
    createTable(TABLE_NAME_1, COLUMN_NAME_1, KEY_TYPE_NUMBER, ImmutableMap.of());
    clientService.executeContract(
        CONTRACT_ID_INSERT,
        prepareInsert(
            TABLE_NAME_1,
            prepareRecord(
                ImmutableMap.of(
                    COLUMN_NAME_1, IntNode.valueOf(1), COLUMN_NAME_2, IntNode.valueOf(1)))));
    createTable(TABLE_NAME_2, COLUMN_NAME_1, KEY_TYPE_STRING, ImmutableMap.of());

    String columnReference1 = TABLE_NAME_1 + Constants.COLUMN_SEPARATOR + COLUMN_NAME_1;
    String columnReference2 = TABLE_NAME_1 + Constants.COLUMN_SEPARATOR + COLUMN_NAME_2;
    String columnReference3 = TABLE_NAME_2 + Constants.COLUMN_SEPARATOR + COLUMN_NAME_2;
    ObjectNode select = prepareSelect(TABLE_NAME_1, columnReference1, IntNode.valueOf(1));
    JsonNode join = prepareJoin(TextNode.valueOf(TABLE_NAME_2), columnReference2, columnReference3);
    select.set(Constants.QUERY_JOINS, createArrayNode(join));

    // Act Assert
    assertThatThrownBy(() -> clientService.executeContract(CONTRACT_ID_SELECT, select))
        .isExactlyInstanceOf(ClientException.class)
        .hasMessage(Constants.INVALID_JOIN_COLUMN + COLUMN_NAME_2);
  }

  @Test
  public void update_RegularColumnValueWithPrimaryKeyConditionGiven_ShouldUpdateRecord() {
    // Arrange
    JsonNode key = TextNode.valueOf("key");
    JsonNode value = IntNode.valueOf(1);
    JsonNode newValue = IntNode.valueOf(2);
    JsonNode record = prepareRecord(ImmutableMap.of(COLUMN_NAME_1, key, COLUMN_NAME_2, value));
    JsonNode updateValues = mapper.createObjectNode().set(COLUMN_NAME_2, newValue);
    JsonNode update = prepareUpdate(COMMON_TEST_TABLE, updateValues, COLUMN_NAME_1, key);
    JsonNode select = prepareSelect(COMMON_TEST_TABLE, COLUMN_NAME_1, key);
    JsonNode expected =
        createArrayNode(
            prepareRecord(ImmutableMap.of(COLUMN_NAME_1, key, COLUMN_NAME_2, newValue)));
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(COMMON_TEST_TABLE, record));

    // Act
    clientService.executeContract(CONTRACT_ID_UPDATE, update);

    // Assert
    ContractExecutionResult actual = clientService.executeContract(CONTRACT_ID_SELECT, select);
    assertThat(actual.getContractResult()).isPresent();
    assertThat(actual.getContractResult().get()).isEqualTo(jacksonSerDe.serialize(expected));
  }

  @Test
  public void update_RegularColumnValueWithIndexKeyConditionGiven_ShouldUpdateRecords() {
    // Arrange
    List<JsonNode> records = insertAndGetRecords();
    String newString = "abc";
    JsonNode values = mapper.createObjectNode().put(COLUMN_NAME_3, newString);
    JsonNode update = prepareUpdate(COMMON_TEST_TABLE, values, COLUMN_NAME_2, IntNode.valueOf(1));
    JsonNode select = prepareSelect(COMMON_TEST_TABLE, COLUMN_NAME_2, IntNode.valueOf(1));
    ImmutableList<JsonNode> expected =
        records.stream()
            .map(
                record -> {
                  ObjectNode newRecord = record.deepCopy();
                  newRecord.set(COLUMN_NAME_3, TextNode.valueOf(newString));
                  return newRecord;
                })
            .collect(ImmutableList.toImmutableList());

    // Act
    clientService.executeContract(CONTRACT_ID_UPDATE, update);

    // Assert
    ContractExecutionResult actual = clientService.executeContract(CONTRACT_ID_SELECT, select);
    assertThat(actual.getContractResult()).isPresent();
    assertSelectResult(jacksonSerDe.deserialize(actual.getContractResult().get()), expected);
  }

  @Test
  public void update_IndexColumnValueWithIndexKeyConditionGiven_ShouldUpdateRecords() {
    // Arrange
    List<JsonNode> records = insertAndGetRecords();
    int oldIndexValue = 1;
    int newIndexValue = 10;
    String newString = "abc";
    JsonNode values =
        mapper.createObjectNode().put(COLUMN_NAME_2, newIndexValue).put(COLUMN_NAME_3, newString);
    JsonNode update =
        prepareUpdate(COMMON_TEST_TABLE, values, COLUMN_NAME_2, IntNode.valueOf(oldIndexValue));
    JsonNode selectOld =
        prepareSelect(COMMON_TEST_TABLE, COLUMN_NAME_2, IntNode.valueOf(oldIndexValue));
    JsonNode selectNew =
        prepareSelect(COMMON_TEST_TABLE, COLUMN_NAME_2, IntNode.valueOf(newIndexValue));
    ImmutableList<JsonNode> expected =
        records.stream()
            .map(
                record -> {
                  ObjectNode newRecord = record.deepCopy();
                  newRecord.set(COLUMN_NAME_2, IntNode.valueOf(newIndexValue));
                  newRecord.set(COLUMN_NAME_3, TextNode.valueOf(newString));
                  return newRecord;
                })
            .collect(ImmutableList.toImmutableList());

    // Act
    clientService.executeContract(CONTRACT_ID_UPDATE, update);

    // Assert
    ContractExecutionResult before = clientService.executeContract(CONTRACT_ID_SELECT, selectOld);
    assertThat(before.getContractResult()).isPresent();
    assertSelectResult(
        jacksonSerDe.deserialize(before.getContractResult().get()), ImmutableList.of());
    ContractExecutionResult after = clientService.executeContract(CONTRACT_ID_SELECT, selectNew);
    assertThat(after.getContractResult()).isPresent();
    assertSelectResult(jacksonSerDe.deserialize(after.getContractResult().get()), expected);
  }

  @Test
  public void
      update_NullIndexColumnValuesWithIndexKeyConditionAndAdditionalConditionGiven_ShouldUpdateRecords() {
    // Arrange
    createTable(
        TABLE_NAME_1,
        COLUMN_NAME_1,
        KEY_TYPE_NUMBER,
        ImmutableMap.of(COLUMN_NAME_2, KEY_TYPE_STRING, COLUMN_NAME_3, KEY_TYPE_STRING));
    JsonNode record1 =
        prepareRecord(
            ImmutableMap.of(
                COLUMN_NAME_1,
                IntNode.valueOf(1),
                COLUMN_NAME_2,
                TextNode.valueOf("aaa"),
                COLUMN_NAME_3,
                NullNode.getInstance(),
                COLUMN_NAME_4,
                IntNode.valueOf(10)));
    JsonNode record2 =
        prepareRecord(
            ImmutableMap.of(
                COLUMN_NAME_1,
                IntNode.valueOf(2),
                COLUMN_NAME_2,
                TextNode.valueOf("bbb"),
                COLUMN_NAME_3,
                NullNode.getInstance(),
                COLUMN_NAME_4,
                IntNode.valueOf(20)));
    JsonNode record3 =
        prepareRecord(
            ImmutableMap.of(
                COLUMN_NAME_1,
                IntNode.valueOf(3),
                COLUMN_NAME_2,
                TextNode.valueOf("ccc"),
                COLUMN_NAME_3,
                NullNode.getInstance(),
                COLUMN_NAME_4,
                IntNode.valueOf(10)));
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(TABLE_NAME_1, record1));
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(TABLE_NAME_1, record2));
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(TABLE_NAME_1, record3));
    String newString = "abc";
    int newValue = 0;
    JsonNode values =
        mapper
            .createObjectNode()
            .put(COLUMN_NAME_3, newString)
            .put(COLUMN_NAME_4, newValue)
            .set(COLUMN_NAME_2, NullNode.getInstance());
    JsonNode update =
        prepareUpdate(
            TABLE_NAME_1,
            values,
            COLUMN_NAME_3,
            NullNode.getInstance(),
            Constants.OPERATOR_IS_NULL,
            prepareCondition(COLUMN_NAME_4, IntNode.valueOf(20), Constants.OPERATOR_LT));
    JsonNode select = prepareSelect(TABLE_NAME_1, COLUMN_NAME_3, TextNode.valueOf(newString));
    ImmutableList<JsonNode> expected =
        ImmutableList.of(record1, record3).stream()
            .map(
                record -> {
                  ObjectNode newRecord = record.deepCopy();
                  newRecord.set(COLUMN_NAME_2, NullNode.getInstance());
                  newRecord.set(COLUMN_NAME_3, TextNode.valueOf(newString));
                  newRecord.set(COLUMN_NAME_4, IntNode.valueOf(newValue));
                  return newRecord;
                })
            .collect(ImmutableList.toImmutableList());

    // Act
    clientService.executeContract(CONTRACT_ID_UPDATE, update);

    // Assert
    ContractExecutionResult actual = clientService.executeContract(CONTRACT_ID_SELECT, select);
    assertThat(actual.getContractResult()).isPresent();
    assertSelectResult(jacksonSerDe.deserialize(actual.getContractResult().get()), expected);
  }

  @Test
  public void update_SameValuesGiven_ShouldNotAddNewAssetRecord() {
    // Arrange
    JsonNode key = TextNode.valueOf("key");
    JsonNode value = IntNode.valueOf(1);
    JsonNode record = prepareRecord(ImmutableMap.of(COLUMN_NAME_1, key, COLUMN_NAME_2, value));
    JsonNode updateValues = mapper.createObjectNode().set(COLUMN_NAME_2, value);
    JsonNode update = prepareUpdate(COMMON_TEST_TABLE, updateValues, COLUMN_NAME_1, key);
    ObjectNode scan = mapper.createObjectNode();
    scan.put(Constants.QUERY_TABLE, COMMON_TEST_TABLE);
    scan.set(
        Constants.QUERY_CONDITIONS,
        createArrayNode(prepareCondition(COLUMN_NAME_1, key, Constants.OPERATOR_EQ)));
    scan.set(
        Constants.SCAN_OPTIONS,
        mapper.createObjectNode().put(Constants.SCAN_OPTIONS_INCLUDE_METADATA, true));
    ObjectNode expected = record.deepCopy();
    expected.put(Constants.SCAN_METADATA_AGE, 0);
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(COMMON_TEST_TABLE, record));

    // Act
    clientService.executeContract(CONTRACT_ID_UPDATE, update);

    // Assert
    ContractExecutionResult actual = clientService.executeContract(CONTRACT_ID_SCAN, scan);
    assertThat(actual.getContractResult()).isPresent();
    assertThat(actual.getContractResult().get())
        .isEqualTo(jacksonSerDe.serialize(createArrayNode(expected)));
  }

  @Test
  public void update_NonExistingTableGiven_ShouldThrowContractContextException() {
    // Arrange
    insertAndGetRecords();
    JsonNode values = mapper.createObjectNode().put(COLUMN_NAME_2, 10);
    JsonNode update = prepareUpdate(TABLE_NAME_1, values, COLUMN_NAME_2, IntNode.valueOf(1));

    // Act Assert
    assertThatThrownBy(() -> clientService.executeContract(CONTRACT_ID_UPDATE, update))
        .isExactlyInstanceOf(ClientException.class)
        .hasMessage(Constants.TABLE_NOT_EXIST);
  }

  @Test
  public void update_KeyConditionsNotGiven_ShouldThrowContractContextException() {
    // Arrange
    insertAndGetRecords();
    JsonNode values = mapper.createObjectNode().put(COLUMN_NAME_2, 10);
    JsonNode update =
        prepareUpdate(COMMON_TEST_TABLE, values, COLUMN_NAME_3, TextNode.valueOf("aaa"));

    // Act Assert
    assertThatThrownBy(() -> clientService.executeContract(CONTRACT_ID_UPDATE, update))
        .isExactlyInstanceOf(ClientException.class)
        .hasMessage(Constants.INVALID_KEY_SPECIFICATION);
  }

  @Test
  public void update_DifferentTypeIndexValueGiven_ShouldThrowContractContextException() {
    // Arrange
    insertAndGetRecords();
    JsonNode values = mapper.createObjectNode().put(COLUMN_NAME_2, "updated");
    JsonNode update = prepareUpdate(COMMON_TEST_TABLE, values, COLUMN_NAME_2, IntNode.valueOf(1));

    // Act Assert
    assertThatThrownBy(() -> clientService.executeContract(CONTRACT_ID_UPDATE, update))
        .isExactlyInstanceOf(ClientException.class)
        .hasMessage(Constants.INVALID_INDEX_KEY_TYPE);
  }

  @Test
  public void update_PrimaryKeyValueGiven_ShouldThrowContractContextException() {
    // Arrange
    insertAndGetRecords();
    JsonNode values =
        mapper.createObjectNode().put(COLUMN_NAME_1, "updated").put(COLUMN_NAME_2, 10);
    JsonNode update = prepareUpdate(COMMON_TEST_TABLE, values, COLUMN_NAME_2, IntNode.valueOf(1));

    // Act Assert
    assertThatThrownBy(() -> clientService.executeContract(CONTRACT_ID_UPDATE, update))
        .isExactlyInstanceOf(ClientException.class)
        .hasMessage(Constants.CANNOT_UPDATE_KEY);
  }

  @Test
  public void showTables_TableNameGiven_ShouldReturnSingleTable() {
    // Arrange
    createTable(TABLE_NAME_1, COLUMN_NAME_1, KEY_TYPE_NUMBER, ImmutableMap.of());
    createTable(TABLE_NAME_2, COLUMN_NAME_1, KEY_TYPE_STRING, ImmutableMap.of());
    JsonNode arguments = mapper.createObjectNode().put(Constants.TABLE_NAME, COMMON_TEST_TABLE);
    JsonNode expected = mapper.createArrayNode().add(commonTestTable);

    // Act
    ContractExecutionResult actual =
        clientService.executeContract(CONTRACT_ID_SHOW_TABLES, arguments);

    // Assert
    assertThat(actual.getContractResult()).isPresent();
    assertThat(actual.getContractResult().get()).isEqualTo(jacksonSerDe.serialize(expected));
  }

  @Test
  public void showTables_TableNameNotGiven_ShouldReturnSingleTable() {
    // Arrange
    JsonNode table1 = createTable(TABLE_NAME_1, COLUMN_NAME_1, KEY_TYPE_NUMBER, ImmutableMap.of());
    JsonNode table2 = createTable(TABLE_NAME_2, COLUMN_NAME_1, KEY_TYPE_STRING, ImmutableMap.of());
    JsonNode arguments = mapper.createObjectNode();
    JsonNode expected = mapper.createArrayNode().add(commonTestTable).add(table1).add(table2);

    // Act
    ContractExecutionResult actual =
        clientService.executeContract(CONTRACT_ID_SHOW_TABLES, arguments);

    // Assert
    assertThat(actual.getContractResult()).isPresent();
    assertThat(actual.getContractResult().get()).isEqualTo(jacksonSerDe.serialize(expected));
  }

  @Test
  public void showTables_NonExistingTableNameGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode arguments = mapper.createObjectNode().put(Constants.TABLE_NAME, TABLE_NAME_1);

    // Act Assert
    assertThatThrownBy(() -> clientService.executeContract(CONTRACT_ID_SHOW_TABLES, arguments))
        .isExactlyInstanceOf(ClientException.class)
        .hasMessage(Constants.TABLE_NOT_EXIST);
  }

  @Test
  public void getHistory_WithoutLimitAndWithLimitGiven_ShouldReturnRecordAgesCorrectly() {
    // Arrange
    String keyString = "key1";
    JsonNode argumentsWithoutLimit =
        mapper
            .createObjectNode()
            .put(Constants.RECORD_TABLE, COMMON_TEST_TABLE)
            .put(Constants.RECORD_KEY, keyString);
    ObjectNode argumentsWithLimit = argumentsWithoutLimit.deepCopy();
    argumentsWithLimit.put(Constants.HISTORY_LIMIT, 2);
    JsonNode key = TextNode.valueOf(keyString);
    JsonNode value0 = IntNode.valueOf(0);
    JsonNode value1 = IntNode.valueOf(1);
    JsonNode value2 = IntNode.valueOf(2);
    JsonNode recordAge0 = prepareRecord(ImmutableMap.of(COLUMN_NAME_1, key, COLUMN_NAME_2, value0));
    JsonNode recordAge1 = prepareRecord(ImmutableMap.of(COLUMN_NAME_1, key, COLUMN_NAME_2, value1));
    JsonNode recordAge2 = prepareRecord(ImmutableMap.of(COLUMN_NAME_1, key, COLUMN_NAME_2, value2));
    JsonNode updateValues1 = mapper.createObjectNode().set(COLUMN_NAME_2, value1);
    JsonNode updateValues2 = mapper.createObjectNode().set(COLUMN_NAME_2, value2);
    JsonNode update1 = prepareUpdate(COMMON_TEST_TABLE, updateValues1, COLUMN_NAME_1, key);
    JsonNode update2 = prepareUpdate(COMMON_TEST_TABLE, updateValues2, COLUMN_NAME_1, key);
    JsonNode result0 =
        mapper
            .createObjectNode()
            .put(Constants.HISTORY_ASSET_AGE, 0)
            .set(Constants.RECORD_VALUES, recordAge0);
    JsonNode result1 =
        mapper
            .createObjectNode()
            .put(Constants.HISTORY_ASSET_AGE, 1)
            .set(Constants.RECORD_VALUES, recordAge1);
    JsonNode result2 =
        mapper
            .createObjectNode()
            .put(Constants.HISTORY_ASSET_AGE, 2)
            .set(Constants.RECORD_VALUES, recordAge2);
    JsonNode expectedWithoutLimit = createArrayNode(result2, result1, result0);
    JsonNode expectedWithLimit = createArrayNode(result2, result1);
    clientService.executeContract(CONTRACT_ID_INSERT, prepareInsert(COMMON_TEST_TABLE, recordAge0));
    clientService.executeContract(CONTRACT_ID_UPDATE, update1);
    clientService.executeContract(CONTRACT_ID_UPDATE, update2);

    // Act
    ContractExecutionResult actualWithoutLimit =
        clientService.executeContract(CONTRACT_ID_GET_HISTORY, argumentsWithoutLimit);
    ContractExecutionResult actualWithLimit =
        clientService.executeContract(CONTRACT_ID_GET_HISTORY, argumentsWithLimit);

    // Assert
    assertThat(actualWithoutLimit.getContractResult()).isPresent();
    assertThat(actualWithoutLimit.getContractResult().get())
        .isEqualTo(jacksonSerDe.serialize(expectedWithoutLimit));
    assertThat(actualWithLimit.getContractResult()).isPresent();
    assertThat(actualWithLimit.getContractResult().get())
        .isEqualTo(jacksonSerDe.serialize(expectedWithLimit));
  }

  @Test
  public void getHistory_NonExistingRecordGiven_ShouldReturnEmpty() {
    // Arrange
    JsonNode arguments =
        mapper
            .createObjectNode()
            .put(Constants.RECORD_TABLE, COMMON_TEST_TABLE)
            .put(Constants.RECORD_KEY, "key");

    // Act
    ContractExecutionResult actual =
        clientService.executeContract(CONTRACT_ID_GET_HISTORY, arguments);

    // Assert
    assertThat(actual.getContractResult()).isPresent();
    assertThat(actual.getContractResult().get())
        .isEqualTo(jacksonSerDe.serialize(mapper.createArrayNode()));
  }

  @Test
  public void getHistory_InvalidKeyTypeGiven_ShouldReturnEmpty() {
    // Arrange
    JsonNode arguments =
        mapper
            .createObjectNode()
            .put(Constants.RECORD_TABLE, COMMON_TEST_TABLE)
            .put(Constants.RECORD_KEY, 0);

    // Act Assert
    assertThatThrownBy(() -> clientService.executeContract(CONTRACT_ID_GET_HISTORY, arguments))
        .isExactlyInstanceOf(ClientException.class)
        .hasMessage(Constants.INVALID_KEY_TYPE);
  }
}
