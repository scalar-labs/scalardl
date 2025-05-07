package com.scalar.dl.genericcontracts.table.v1_0_0;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ScanTest {
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String SOME_TABLE_NAME = "Person";
  private static final String SOME_TABLE_ASSET_ID = Constants.PREFIX_TABLE + SOME_TABLE_NAME;
  private static final String SOME_KEY_TYPE_STRING = "string";
  private static final String SOME_KEY_TYPE_NUMBER = "number";
  private static final String SOME_KEY_TYPE_BOOLEAN = "boolean";
  private static final String SOME_PRIMARY_KEY_COLUMN = "GovId";
  private static final String SOME_PRIMARY_KEY_VALUE_1 = "001";
  private static final String SOME_PRIMARY_KEY_VALUE_2 = "002";
  private static final String SOME_INDEX_KEY_COLUMN_1 = "lastName";
  private static final String SOME_INDEX_KEY_COLUMN_VALUE_1 = "Doe";
  private static final String SOME_INDEX_KEY_COLUMN_2 = "amount";
  private static final double SOME_INDEX_KEY_COLUMN_VALUE_2 = 130.75;
  private static final String SOME_INDEX_KEY_COLUMN_3 = "indexFlag";
  private static final boolean SOME_INDEX_KEY_COLUMN_VALUE_3 = true;
  private static final String SOME_COLUMN_STRING = "firstName";
  private static final String SOME_COLUMN_STRING_VALUE_1 = "John";
  private static final String SOME_COLUMN_STRING_VALUE_2 = "Joe";
  private static final String SOME_COLUMN_NUMBER = "balance";
  private static final String SOME_COLUMN_NULLABLE = "nullable";
  private static final int SOME_COLUMN_NUMBER_VALUE = 10;
  private static final String SOME_COLUMN_BOOLEAN = "flag";
  private static final boolean SOME_COLUMN_BOOLEAN_VALUE = false;
  private static final String SOME_INDEX_ASSET_ID_1 =
      getIndexAssetId(SOME_INDEX_KEY_COLUMN_1, SOME_INDEX_KEY_COLUMN_VALUE_1);
  private static final String SOME_INDEX_ASSET_ID_3 =
      getIndexAssetId(SOME_INDEX_KEY_COLUMN_3, String.valueOf(SOME_INDEX_KEY_COLUMN_VALUE_3));
  private static final String SOME_RECORD_ASSET_ID_1 =
      getRecordAssetId(SOME_PRIMARY_KEY_COLUMN, SOME_PRIMARY_KEY_VALUE_1);
  private static final String SOME_RECORD_ASSET_ID_2 =
      getRecordAssetId(SOME_PRIMARY_KEY_COLUMN, SOME_PRIMARY_KEY_VALUE_2);
  private static final JsonNode SOME_TABLE =
      mapper
          .createObjectNode()
          .put(Constants.TABLE_NAME, SOME_TABLE_NAME)
          .put(Constants.TABLE_KEY, SOME_PRIMARY_KEY_COLUMN)
          .put(Constants.TABLE_KEY_TYPE, SOME_KEY_TYPE_STRING)
          .set(
              Constants.TABLE_INDEXES,
              mapper
                  .createArrayNode()
                  .add(createIndexNode(SOME_INDEX_KEY_COLUMN_1, SOME_KEY_TYPE_STRING))
                  .add(createIndexNode(SOME_INDEX_KEY_COLUMN_2, SOME_KEY_TYPE_NUMBER))
                  .add(createIndexNode(SOME_INDEX_KEY_COLUMN_3, SOME_KEY_TYPE_BOOLEAN)));
  private static final int SOME_ASSET_AGE = 1;

  private final Scan scan = new Scan();

  @Mock private Ledger<JsonNode> ledger;

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this).close();
  }

  private static String getRecordAssetId(String key, String value) {
    return Constants.PREFIX_RECORD
        + SOME_TABLE_NAME
        + Constants.ASSET_ID_SEPARATOR
        + key
        + Constants.ASSET_ID_SEPARATOR
        + value;
  }

  private static String getIndexAssetId(String key, String value) {
    return Constants.PREFIX_INDEX
        + SOME_TABLE_NAME
        + Constants.ASSET_ID_SEPARATOR
        + key
        + Constants.ASSET_ID_SEPARATOR
        + value;
  }

  private static ObjectNode createIndexNode(String key, String type) {
    return mapper
        .createObjectNode()
        .put(Constants.INDEX_KEY, key)
        .put(Constants.INDEX_KEY_TYPE, type);
  }

  private static ObjectNode createRecord(String primaryKey, String indexKey, String stringValue) {
    return mapper
        .createObjectNode()
        .put(SOME_PRIMARY_KEY_COLUMN, primaryKey)
        .put(SOME_INDEX_KEY_COLUMN_1, indexKey)
        .put(SOME_COLUMN_STRING, stringValue)
        .set(SOME_COLUMN_NULLABLE, null);
  }

  private static ObjectNode createRecord(String primaryKey, String indexKey, int numberValue) {
    return mapper
        .createObjectNode()
        .put(SOME_PRIMARY_KEY_COLUMN, primaryKey)
        .put(SOME_INDEX_KEY_COLUMN_1, indexKey)
        .put(SOME_COLUMN_NUMBER, numberValue)
        .set(SOME_COLUMN_NULLABLE, null);
  }

  private static ObjectNode createRecord(String primaryKey, String indexKey, boolean flag) {
    return mapper
        .createObjectNode()
        .put(SOME_PRIMARY_KEY_COLUMN, primaryKey)
        .put(SOME_INDEX_KEY_COLUMN_1, indexKey)
        .put(SOME_COLUMN_BOOLEAN, flag)
        .set(SOME_COLUMN_NULLABLE, null);
  }

  private Asset<JsonNode> createIndexAsset(
      String primaryKey, List<String> values, boolean deleted) {
    ArrayNode indexEntries = mapper.createArrayNode();
    values.forEach(
        value -> {
          ObjectNode indexEntry =
              mapper
                  .createObjectNode()
                  .put(primaryKey, value)
                  .put(Constants.INDEX_ASSET_ADDED_AGE, 0);
          if (deleted) {
            indexEntry.put(Constants.INDEX_ASSET_DELETE_MARKER, true);
          }
          indexEntries.add(indexEntry);
        });
    return createAsset(indexEntries);
  }

  private Asset<JsonNode> createIndexAsset(String primaryKey, String value) {
    return createIndexAsset(primaryKey, ImmutableList.of(value), false);
  }

  private Asset<JsonNode> createAsset(JsonNode data) {
    Asset<JsonNode> asset = (Asset<JsonNode>) mock(Asset.class);
    when(asset.data()).thenReturn(data);
    when(asset.age()).thenReturn(SOME_ASSET_AGE);
    return asset;
  }

  private AssetFilter createAssetFilter(String assetId) {
    AssetFilter filter = new AssetFilter(assetId);
    return filter.withAgeOrder(AgeOrder.ASC);
  }

  private static JsonNode createCondition(String column, String value, String operator) {
    return mapper
        .createObjectNode()
        .put(Constants.CONDITION_COLUMN, column)
        .put(Constants.CONDITION_VALUE, value)
        .put(Constants.CONDITION_OPERATOR, operator);
  }

  private static JsonNode createCondition(String column, int value, String operator) {
    return mapper
        .createObjectNode()
        .put(Constants.CONDITION_COLUMN, column)
        .put(Constants.CONDITION_VALUE, value)
        .put(Constants.CONDITION_OPERATOR, operator);
  }

  private static JsonNode createCondition(String column, double value, String operator) {
    return mapper
        .createObjectNode()
        .put(Constants.CONDITION_COLUMN, column)
        .put(Constants.CONDITION_VALUE, value)
        .put(Constants.CONDITION_OPERATOR, operator);
  }

  private static JsonNode createCondition(String column, boolean value, String operator) {
    return mapper
        .createObjectNode()
        .put(Constants.CONDITION_COLUMN, column)
        .put(Constants.CONDITION_VALUE, value)
        .put(Constants.CONDITION_OPERATOR, operator);
  }

  private static JsonNode createCondition(String column, String operator) {
    return mapper
        .createObjectNode()
        .put(Constants.CONDITION_COLUMN, column)
        .put(Constants.CONDITION_OPERATOR, operator);
  }

  private ArrayNode createConditions(String column, String value, String operator) {
    return mapper
        .createArrayNode()
        .add(
            createCondition(
                SOME_INDEX_KEY_COLUMN_1, SOME_INDEX_KEY_COLUMN_VALUE_1, Constants.OPERATOR_EQ))
        .add(createCondition(column, value, operator));
  }

  private ArrayNode createConditions(String column, String operator) {
    return mapper
        .createArrayNode()
        .add(
            createCondition(
                SOME_INDEX_KEY_COLUMN_1, SOME_INDEX_KEY_COLUMN_VALUE_1, Constants.OPERATOR_EQ))
        .add(createCondition(column, operator));
  }

  private ArrayNode createConditions(String column, int value, String operator) {
    return mapper
        .createArrayNode()
        .add(
            createCondition(
                SOME_INDEX_KEY_COLUMN_1, SOME_INDEX_KEY_COLUMN_VALUE_1, Constants.OPERATOR_EQ))
        .add(createCondition(column, value, operator));
  }

  private ArrayNode createConditions(String column, boolean value, String operator) {
    return mapper
        .createArrayNode()
        .add(
            createCondition(
                SOME_INDEX_KEY_COLUMN_1, SOME_INDEX_KEY_COLUMN_VALUE_1, Constants.OPERATOR_EQ))
        .add(createCondition(column, value, operator));
  }

  private ObjectNode createQueryArguments(ArrayNode conditions) {
    return mapper
        .createObjectNode()
        .put(Constants.QUERY_TABLE, SOME_TABLE_NAME)
        .set(Constants.QUERY_CONDITIONS, conditions);
  }

  private void assertMatchedRecordExists(JsonNode actual, JsonNode expected) {
    assertThat(actual).isNotNull();
    assertThat(actual.isArray()).isTrue();
    assertThat(actual.size()).isEqualTo(1);
    assertThat(actual.get(0)).isEqualTo(expected);
  }

  private void assertMatchedRecordNotExist(JsonNode actual) {
    assertThat(actual).isNotNull();
    assertThat(actual.isArray()).isTrue();
    assertThat(actual.size()).isEqualTo(0);
  }

  @Test
  public void invoke_CorrectArgumentsWithStringPrimaryKeyGiven_ShouldReturnRecords() {
    // Arrange
    ArrayNode conditions =
        mapper
            .createArrayNode()
            .add(
                createCondition(
                    SOME_PRIMARY_KEY_COLUMN, SOME_PRIMARY_KEY_VALUE_1, Constants.OPERATOR_EQ));
    JsonNode argument = createQueryArguments(conditions);
    JsonNode expected =
        mapper.createObjectNode().put(SOME_PRIMARY_KEY_COLUMN, SOME_PRIMARY_KEY_VALUE_1);
    Asset<JsonNode> table = createAsset(SOME_TABLE);
    Asset<JsonNode> record = createAsset(expected);
    when(ledger.get(SOME_TABLE_ASSET_ID)).thenReturn(Optional.of(table));
    when(ledger.get(SOME_RECORD_ASSET_ID_1)).thenReturn(Optional.of(record));

    // Act
    JsonNode actual = scan.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isNotNull();
    assertThat(actual.isArray()).isTrue();
    assertThat(actual.size()).isEqualTo(1);
    assertThat(actual.get(0)).isEqualTo(expected);
    verify(ledger).get(SOME_TABLE_ASSET_ID);
    verify(ledger).get(SOME_RECORD_ASSET_ID_1);
  }

  @Test
  public void invoke_CorrectArgumentsWithNumberPrimaryKeyGiven_ShouldReturnRecords() {
    // Arrange
    JsonNode table =
        mapper
            .createObjectNode()
            .put(Constants.TABLE_NAME, SOME_TABLE_NAME)
            .put(Constants.TABLE_KEY, SOME_PRIMARY_KEY_COLUMN)
            .put(Constants.TABLE_KEY_TYPE, SOME_KEY_TYPE_NUMBER);
    double primaryKey = 1.234;
    String recordAssetId = getRecordAssetId(SOME_PRIMARY_KEY_COLUMN, String.valueOf(primaryKey));
    ArrayNode conditions =
        mapper
            .createArrayNode()
            .add(createCondition(SOME_PRIMARY_KEY_COLUMN, primaryKey, Constants.OPERATOR_EQ));
    JsonNode argument = createQueryArguments(conditions);
    JsonNode expected = mapper.createObjectNode().put(SOME_PRIMARY_KEY_COLUMN, primaryKey);
    Asset<JsonNode> tableAsset = createAsset(table);
    Asset<JsonNode> recordAsset = createAsset(expected);
    when(ledger.get(SOME_TABLE_ASSET_ID)).thenReturn(Optional.of(tableAsset));
    when(ledger.get(recordAssetId)).thenReturn(Optional.of(recordAsset));

    // Act
    JsonNode actual = scan.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isNotNull();
    assertThat(actual.isArray()).isTrue();
    assertThat(actual.size()).isEqualTo(1);
    assertThat(actual.get(0)).isEqualTo(expected);
    verify(ledger).get(SOME_TABLE_ASSET_ID);
    verify(ledger).get(recordAssetId);
  }

  @Test
  public void invoke_CorrectArgumentsWithBooleanPrimaryKeyGiven_ShouldReturnRecords() {
    // Arrange
    JsonNode table =
        mapper
            .createObjectNode()
            .put(Constants.TABLE_NAME, SOME_TABLE_NAME)
            .put(Constants.TABLE_KEY, SOME_PRIMARY_KEY_COLUMN)
            .put(Constants.TABLE_KEY_TYPE, SOME_KEY_TYPE_BOOLEAN);
    boolean primaryKey = true;
    String recordAssetId = getRecordAssetId(SOME_PRIMARY_KEY_COLUMN, String.valueOf(primaryKey));
    ArrayNode conditions =
        mapper
            .createArrayNode()
            .add(createCondition(SOME_PRIMARY_KEY_COLUMN, primaryKey, Constants.OPERATOR_EQ));
    JsonNode argument = createQueryArguments(conditions);
    JsonNode expected = mapper.createObjectNode().put(SOME_PRIMARY_KEY_COLUMN, primaryKey);
    Asset<JsonNode> tableAsset = createAsset(table);
    Asset<JsonNode> recordAsset = createAsset(expected);
    when(ledger.get(SOME_TABLE_ASSET_ID)).thenReturn(Optional.of(tableAsset));
    when(ledger.get(recordAssetId)).thenReturn(Optional.of(recordAsset));

    // Act
    JsonNode actual = scan.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isNotNull();
    assertThat(actual.isArray()).isTrue();
    assertThat(actual.size()).isEqualTo(1);
    assertThat(actual.get(0)).isEqualTo(expected);
    verify(ledger).get(SOME_TABLE_ASSET_ID);
    verify(ledger).get(recordAssetId);
  }

  @Test
  public void invoke_CorrectArgumentsWithStringIndexKeyGiven_ShouldReturnRecords() {
    // Arrange
    ArrayNode conditions =
        mapper
            .createArrayNode()
            .add(
                createCondition(
                    SOME_INDEX_KEY_COLUMN_1, SOME_INDEX_KEY_COLUMN_VALUE_1, Constants.OPERATOR_EQ));
    JsonNode argument = createQueryArguments(conditions);
    JsonNode expected1 =
        createRecord(
            SOME_PRIMARY_KEY_VALUE_1, SOME_INDEX_KEY_COLUMN_VALUE_1, SOME_COLUMN_STRING_VALUE_1);
    JsonNode expected2 =
        createRecord(
            SOME_PRIMARY_KEY_VALUE_2, SOME_INDEX_KEY_COLUMN_VALUE_1, SOME_COLUMN_STRING_VALUE_2);
    Asset<JsonNode> table = createAsset(SOME_TABLE);
    Asset<JsonNode> index1 = createIndexAsset(SOME_PRIMARY_KEY_COLUMN, SOME_PRIMARY_KEY_VALUE_1);
    Asset<JsonNode> index2 = createIndexAsset(SOME_PRIMARY_KEY_COLUMN, SOME_PRIMARY_KEY_VALUE_2);
    Asset<JsonNode> record1 = createAsset(expected1);
    Asset<JsonNode> record2 = createAsset(expected2);
    when(ledger.get(SOME_TABLE_ASSET_ID)).thenReturn(Optional.of(table));
    when(ledger.get(SOME_RECORD_ASSET_ID_1)).thenReturn(Optional.of(record1));
    when(ledger.get(SOME_RECORD_ASSET_ID_2)).thenReturn(Optional.of(record2));
    when(ledger.scan(createAssetFilter(SOME_INDEX_ASSET_ID_1)))
        .thenReturn(ImmutableList.of(index1, index2));

    // Act
    JsonNode actual = scan.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isNotNull();
    assertThat(actual.isArray()).isTrue();
    assertThat(StreamSupport.stream(actual.spliterator(), false).collect(Collectors.toList()))
        .containsExactlyInAnyOrderElementsOf(ImmutableList.of(expected1, expected2));
    verify(ledger).get(SOME_TABLE_ASSET_ID);
    verify(ledger).get(SOME_RECORD_ASSET_ID_1);
  }

  @Test
  public void invoke_CorrectArgumentsWithNumberIndexKeyGiven_ShouldReturnRecords() {
    // Arrange
    String indexAssetId =
        getIndexAssetId(SOME_INDEX_KEY_COLUMN_2, String.valueOf(SOME_INDEX_KEY_COLUMN_VALUE_2));
    ArrayNode conditions =
        mapper
            .createArrayNode()
            .add(
                createCondition(
                    SOME_INDEX_KEY_COLUMN_1, SOME_INDEX_KEY_COLUMN_VALUE_1, Constants.OPERATOR_GTE))
            .add(
                createCondition(
                    SOME_INDEX_KEY_COLUMN_2, SOME_INDEX_KEY_COLUMN_VALUE_2, Constants.OPERATOR_EQ));
    JsonNode argument = createQueryArguments(conditions);
    JsonNode expected =
        createRecord(
                SOME_PRIMARY_KEY_VALUE_1, SOME_INDEX_KEY_COLUMN_VALUE_1, SOME_COLUMN_STRING_VALUE_1)
            .put(SOME_INDEX_KEY_COLUMN_2, SOME_INDEX_KEY_COLUMN_VALUE_2);
    Asset<JsonNode> table = createAsset(SOME_TABLE);
    Asset<JsonNode> index = createIndexAsset(SOME_PRIMARY_KEY_COLUMN, SOME_PRIMARY_KEY_VALUE_1);
    Asset<JsonNode> record = createAsset(expected);
    when(ledger.get(SOME_TABLE_ASSET_ID)).thenReturn(Optional.of(table));
    when(ledger.get(SOME_RECORD_ASSET_ID_1)).thenReturn(Optional.of(record));
    when(ledger.scan(createAssetFilter(indexAssetId))).thenReturn(ImmutableList.of(index));

    // Act
    JsonNode actual = scan.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isNotNull();
    assertThat(actual.isArray()).isTrue();
    assertThat(actual.size()).isEqualTo(1);
    assertThat(actual.get(0)).isEqualTo(expected);
    verify(ledger).get(SOME_TABLE_ASSET_ID);
    verify(ledger).get(SOME_RECORD_ASSET_ID_1);
  }

  @Test
  public void invoke_CorrectArgumentsWithBooleanIndexKeyGiven_ShouldReturnRecords() {
    // Arrange
    ArrayNode conditions =
        mapper
            .createArrayNode()
            .add(
                createCondition(
                    SOME_INDEX_KEY_COLUMN_3, SOME_INDEX_KEY_COLUMN_VALUE_3, Constants.OPERATOR_EQ));
    JsonNode argument = createQueryArguments(conditions);
    JsonNode expected =
        createRecord(
                SOME_PRIMARY_KEY_VALUE_1, SOME_INDEX_KEY_COLUMN_VALUE_1, SOME_COLUMN_STRING_VALUE_1)
            .put(SOME_INDEX_KEY_COLUMN_3, SOME_INDEX_KEY_COLUMN_VALUE_3);
    Asset<JsonNode> table = createAsset(SOME_TABLE);
    Asset<JsonNode> index = createIndexAsset(SOME_PRIMARY_KEY_COLUMN, SOME_PRIMARY_KEY_VALUE_1);
    Asset<JsonNode> record = createAsset(expected);
    when(ledger.get(SOME_TABLE_ASSET_ID)).thenReturn(Optional.of(table));
    when(ledger.get(SOME_RECORD_ASSET_ID_1)).thenReturn(Optional.of(record));
    when(ledger.scan(createAssetFilter(SOME_INDEX_ASSET_ID_3))).thenReturn(ImmutableList.of(index));

    // Act
    JsonNode actual = scan.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isNotNull();
    assertThat(actual.isArray()).isTrue();
    assertThat(actual.size()).isEqualTo(1);
    assertThat(actual.get(0)).isEqualTo(expected);
    verify(ledger).get(SOME_TABLE_ASSET_ID);
    verify(ledger).get(SOME_RECORD_ASSET_ID_1);
  }

  @Test
  public void invoke_CorrectArgumentsWithNullIndexKeyGiven_ShouldReturnEmptyArray() {
    // Arrange
    ArrayNode conditions =
        mapper
            .createArrayNode()
            .add(
                mapper
                    .createObjectNode()
                    .put(Constants.CONDITION_COLUMN, SOME_INDEX_KEY_COLUMN_1)
                    .put(Constants.CONDITION_OPERATOR, Constants.OPERATOR_IS_NULL));
    JsonNode argument = createQueryArguments(conditions);
    Asset<JsonNode> table = createAsset(SOME_TABLE);
    when(ledger.get(SOME_TABLE_ASSET_ID)).thenReturn(Optional.of(table));
    when(ledger.scan(createAssetFilter(SOME_INDEX_ASSET_ID_1))).thenReturn(ImmutableList.of());

    // Act
    JsonNode actual = scan.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isNotNull();
    assertThat(actual.isArray()).isTrue();
    assertThat(actual.size()).isEqualTo(0);
    verify(ledger).get(SOME_TABLE_ASSET_ID);
  }

  @Test
  public void invoke_CorrectArgumentsWithIndexKeyAndConditionGiven_ShouldReturnCorrectRecords() {
    // Arrange
    ArrayNode conditions =
        mapper
            .createArrayNode()
            .add(
                createCondition(
                    SOME_INDEX_KEY_COLUMN_1, SOME_INDEX_KEY_COLUMN_VALUE_1, Constants.OPERATOR_EQ))
            .add(
                createCondition(
                    SOME_COLUMN_STRING, SOME_COLUMN_STRING_VALUE_2, Constants.OPERATOR_EQ));
    JsonNode argument = createQueryArguments(conditions);
    JsonNode expected =
        createRecord(
            SOME_PRIMARY_KEY_VALUE_2, SOME_INDEX_KEY_COLUMN_VALUE_1, SOME_COLUMN_STRING_VALUE_2);
    Asset<JsonNode> table = createAsset(SOME_TABLE);
    Asset<JsonNode> index1 = createIndexAsset(SOME_PRIMARY_KEY_COLUMN, SOME_PRIMARY_KEY_VALUE_1);
    Asset<JsonNode> index2 = createIndexAsset(SOME_PRIMARY_KEY_COLUMN, SOME_PRIMARY_KEY_VALUE_2);
    Asset<JsonNode> record1 =
        createAsset(
            createRecord(
                SOME_PRIMARY_KEY_VALUE_1,
                SOME_INDEX_KEY_COLUMN_VALUE_1,
                SOME_COLUMN_STRING_VALUE_1));
    Asset<JsonNode> record2 = createAsset(expected);
    when(ledger.get(SOME_TABLE_ASSET_ID)).thenReturn(Optional.of(table));
    when(ledger.get(SOME_RECORD_ASSET_ID_1)).thenReturn(Optional.of(record1));
    when(ledger.get(SOME_RECORD_ASSET_ID_2)).thenReturn(Optional.of(record2));
    when(ledger.scan(createAssetFilter(SOME_INDEX_ASSET_ID_1)))
        .thenReturn(ImmutableList.of(index1, index2));

    // Act
    JsonNode actual = scan.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isNotNull();
    assertThat(actual.isArray()).isTrue();
    assertThat(actual.size()).isEqualTo(1);
    assertThat(actual.get(0)).isEqualTo(expected);
    verify(ledger).get(SOME_TABLE_ASSET_ID);
    verify(ledger).get(SOME_RECORD_ASSET_ID_1);
  }

  @Test
  public void
      invoke_CorrectArgumentsWithIncludeMetadataOptionGiven_ShouldReturnRecordsWithMetadata() {
    // Arrange
    ArrayNode conditions =
        mapper
            .createArrayNode()
            .add(
                createCondition(
                    SOME_PRIMARY_KEY_COLUMN, SOME_PRIMARY_KEY_VALUE_1, Constants.OPERATOR_EQ));
    JsonNode argument =
        createQueryArguments(conditions)
            .set(
                Constants.SCAN_OPTIONS,
                mapper.createObjectNode().put(Constants.SCAN_OPTIONS_INCLUDE_METADATA, true));
    JsonNode recordJson =
        mapper.createObjectNode().put(SOME_PRIMARY_KEY_COLUMN, SOME_PRIMARY_KEY_VALUE_1);
    ObjectNode expected = recordJson.deepCopy();
    expected.put(Constants.SCAN_METADATA_AGE, SOME_ASSET_AGE);
    Asset<JsonNode> table = createAsset(SOME_TABLE);
    Asset<JsonNode> record = createAsset(recordJson);
    when(ledger.get(SOME_TABLE_ASSET_ID)).thenReturn(Optional.of(table));
    when(ledger.get(SOME_RECORD_ASSET_ID_1)).thenReturn(Optional.of(record));

    // Act
    JsonNode actual = scan.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isNotNull();
    assertThat(actual.isArray()).isTrue();
    assertThat(actual.size()).isEqualTo(1);
    assertThat(actual.get(0)).isEqualTo(expected);
    verify(ledger).get(SOME_TABLE_ASSET_ID);
    verify(ledger).get(SOME_RECORD_ASSET_ID_1);
  }

  @Test
  public void
      invoke_CorrectArgumentsWithIndexKeyConditionAndIncludeMetadataOptionGiven_ShouldReturnCorrectRecords() {
    // Arrange
    ArrayNode conditions =
        mapper
            .createArrayNode()
            .add(
                createCondition(
                    SOME_INDEX_KEY_COLUMN_1, SOME_INDEX_KEY_COLUMN_VALUE_1, Constants.OPERATOR_EQ));
    JsonNode argument =
        createQueryArguments(conditions)
            .set(
                Constants.SCAN_OPTIONS,
                mapper.createObjectNode().put(Constants.SCAN_OPTIONS_INCLUDE_METADATA, true));
    JsonNode recordJson =
        createRecord(
            SOME_PRIMARY_KEY_VALUE_1, SOME_INDEX_KEY_COLUMN_VALUE_1, SOME_COLUMN_STRING_VALUE_1);
    ObjectNode expected = recordJson.deepCopy();
    expected.put(Constants.SCAN_METADATA_AGE, SOME_ASSET_AGE);
    Asset<JsonNode> table = createAsset(SOME_TABLE);
    Asset<JsonNode> index = createIndexAsset(SOME_PRIMARY_KEY_COLUMN, SOME_PRIMARY_KEY_VALUE_1);
    Asset<JsonNode> record = createAsset(recordJson);
    when(ledger.get(SOME_TABLE_ASSET_ID)).thenReturn(Optional.of(table));
    when(ledger.get(SOME_RECORD_ASSET_ID_1)).thenReturn(Optional.of(record));
    when(ledger.scan(createAssetFilter(SOME_INDEX_ASSET_ID_1))).thenReturn(ImmutableList.of(index));

    // Act
    JsonNode actual = scan.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isNotNull();
    assertThat(actual.isArray()).isTrue();
    assertThat(actual.size()).isEqualTo(1);
    assertThat(actual.get(0)).isEqualTo(expected);
    verify(ledger).get(SOME_TABLE_ASSET_ID);
    verify(ledger).get(SOME_RECORD_ASSET_ID_1);
  }

  @Test
  public void invoke_CorrectArgumentsGivenAndDeletedIndexAssetsReturned_ShouldNotReturnRecords() {
    // Arrange
    ArrayNode conditions =
        mapper
            .createArrayNode()
            .add(
                createCondition(
                    SOME_INDEX_KEY_COLUMN_1, SOME_INDEX_KEY_COLUMN_VALUE_1, Constants.OPERATOR_EQ));
    JsonNode argument = createQueryArguments(conditions);
    JsonNode recordJson =
        createRecord(
            SOME_PRIMARY_KEY_VALUE_1, SOME_INDEX_KEY_COLUMN_VALUE_1, SOME_COLUMN_STRING_VALUE_1);
    ObjectNode expected = recordJson.deepCopy();
    expected.put(Constants.SCAN_METADATA_AGE, SOME_ASSET_AGE);
    Asset<JsonNode> table = createAsset(SOME_TABLE);
    Asset<JsonNode> index1 = createIndexAsset(SOME_PRIMARY_KEY_COLUMN, SOME_PRIMARY_KEY_VALUE_1);
    Asset<JsonNode> index2 = createIndexAsset(SOME_PRIMARY_KEY_COLUMN, SOME_PRIMARY_KEY_VALUE_2);
    Asset<JsonNode> index3 =
        createIndexAsset(
            SOME_PRIMARY_KEY_COLUMN,
            ImmutableList.of(SOME_PRIMARY_KEY_VALUE_1, SOME_PRIMARY_KEY_VALUE_2),
            true);
    Asset<JsonNode> record = createAsset(recordJson);
    when(ledger.get(SOME_TABLE_ASSET_ID)).thenReturn(Optional.of(table));
    when(ledger.get(SOME_RECORD_ASSET_ID_1)).thenReturn(Optional.of(record));
    when(ledger.scan(createAssetFilter(SOME_INDEX_ASSET_ID_1)))
        .thenReturn(ImmutableList.of(index1, index2, index3));

    // Act
    JsonNode actual = scan.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isNotNull();
    assertThat(actual.isArray()).isTrue();
    assertThat(actual.size()).isEqualTo(0);
    verify(ledger).get(SOME_TABLE_ASSET_ID);
    verify(ledger, never()).get(SOME_RECORD_ASSET_ID_1);
  }

  @Test
  public void invoke_CorrectArgumentsGivenButNonArrayIndexAssetFound_ShouldThrowException() {
    // Arrange
    // Arrange
    ArrayNode conditions =
        mapper
            .createArrayNode()
            .add(
                createCondition(
                    SOME_INDEX_KEY_COLUMN_1, SOME_INDEX_KEY_COLUMN_VALUE_1, Constants.OPERATOR_EQ));
    JsonNode argument = createQueryArguments(conditions);
    Asset<JsonNode> table = createAsset(SOME_TABLE);
    Asset<JsonNode> index = createAsset(mapper.createArrayNode().add(mapper.createObjectNode()));
    when(ledger.get(SOME_TABLE_ASSET_ID)).thenReturn(Optional.of(table));
    when(ledger.scan(createAssetFilter(SOME_INDEX_ASSET_ID_1))).thenReturn(ImmutableList.of(index));

    // Act Assert
    assertThatThrownBy(() -> scan.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.ILLEGAL_INDEX_STATE);
    verify(ledger).get(SOME_TABLE_ASSET_ID);
    verify(ledger).scan(createAssetFilter(SOME_INDEX_ASSET_ID_1));
    verify(ledger, never()).get(SOME_RECORD_ASSET_ID_1);
  }

  @Test
  public void invoke_CorrectArgumentsGivenButMalformedIndexAssetFound_ShouldThrowException() {
    // Arrange
    // Arrange
    ArrayNode conditions =
        mapper
            .createArrayNode()
            .add(
                createCondition(
                    SOME_INDEX_KEY_COLUMN_1, SOME_INDEX_KEY_COLUMN_VALUE_1, Constants.OPERATOR_EQ));
    JsonNode argument = createQueryArguments(conditions);
    Asset<JsonNode> table = createAsset(SOME_TABLE);
    Asset<JsonNode> index = createAsset(mapper.createObjectNode());
    when(ledger.get(SOME_TABLE_ASSET_ID)).thenReturn(Optional.of(table));
    when(ledger.scan(createAssetFilter(SOME_INDEX_ASSET_ID_1))).thenReturn(ImmutableList.of(index));

    // Act Assert
    assertThatThrownBy(() -> scan.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.ILLEGAL_INDEX_STATE);
    verify(ledger).get(SOME_TABLE_ASSET_ID);
    verify(ledger).scan(createAssetFilter(SOME_INDEX_ASSET_ID_1));
    verify(ledger, never()).get(SOME_RECORD_ASSET_ID_1);
  }

  @Test
  public void invoke_CorrectArgumentsGivenButAssetNotFoundByIndex_ShouldThrowException() {
    // Arrange
    // Arrange
    ArrayNode conditions =
        mapper
            .createArrayNode()
            .add(
                createCondition(
                    SOME_INDEX_KEY_COLUMN_1, SOME_INDEX_KEY_COLUMN_VALUE_1, Constants.OPERATOR_EQ));
    JsonNode argument = createQueryArguments(conditions);
    Asset<JsonNode> table = createAsset(SOME_TABLE);
    Asset<JsonNode> index = createIndexAsset(SOME_PRIMARY_KEY_COLUMN, SOME_PRIMARY_KEY_VALUE_1);
    when(ledger.get(SOME_TABLE_ASSET_ID)).thenReturn(Optional.of(table));
    when(ledger.get(SOME_RECORD_ASSET_ID_1)).thenReturn(Optional.empty());
    when(ledger.scan(createAssetFilter(SOME_INDEX_ASSET_ID_1))).thenReturn(ImmutableList.of(index));

    // Act Assert
    assertThatThrownBy(() -> scan.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.ILLEGAL_INDEX_STATE);
    verify(ledger).get(SOME_TABLE_ASSET_ID);
    verify(ledger).scan(createAssetFilter(SOME_INDEX_ASSET_ID_1));
    verify(ledger).get(SOME_RECORD_ASSET_ID_1);
  }

  @Test
  public void invoke_ArgumentsWithoutPrimaryKeyAndIndexKeyConditionsGiven_ShouldThrowException() {
    // Arrange
    ArrayNode conditions =
        mapper
            .createArrayNode()
            .add(
                createCondition(
                    SOME_PRIMARY_KEY_COLUMN, SOME_PRIMARY_KEY_VALUE_1, Constants.OPERATOR_GT))
            .add(
                createCondition(
                    SOME_INDEX_KEY_COLUMN_1, SOME_INDEX_KEY_COLUMN_VALUE_1, Constants.OPERATOR_LT))
            .add(
                createCondition(
                    SOME_COLUMN_STRING, SOME_COLUMN_STRING_VALUE_1, Constants.OPERATOR_EQ));
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.QUERY_TABLE, SOME_TABLE_NAME)
            .set(Constants.QUERY_CONDITIONS, conditions);
    Asset<JsonNode> table = createAsset(SOME_TABLE);
    when(ledger.get(SOME_TABLE_ASSET_ID)).thenReturn(Optional.of(table));

    // Act Assert
    assertThatThrownBy(() -> scan.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_KEY_SPECIFICATION);
    verify(ledger).get(SOME_TABLE_ASSET_ID);
  }

  @Test
  public void invoke_ArgumentsWithInvalidPrimaryKeyConditionGiven_ShouldThrowException() {
    // Arrange
    ArrayNode conditions =
        mapper
            .createArrayNode()
            .add(createCondition(SOME_PRIMARY_KEY_COLUMN, 0, Constants.OPERATOR_EQ));
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.QUERY_TABLE, SOME_TABLE_NAME)
            .set(Constants.QUERY_CONDITIONS, conditions);
    Asset<JsonNode> table = createAsset(SOME_TABLE);
    when(ledger.get(SOME_TABLE_ASSET_ID)).thenReturn(Optional.of(table));

    // Act Assert
    assertThatThrownBy(() -> scan.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_KEY_TYPE);
    verify(ledger).get(SOME_TABLE_ASSET_ID);
  }

  @Test
  public void invoke_ArgumentsWithInvalidIndexKeyConditionGiven_ShouldThrowException() {
    // Arrange
    ArrayNode conditions =
        mapper
            .createArrayNode()
            .add(createCondition(SOME_INDEX_KEY_COLUMN_1, 0, Constants.OPERATOR_EQ));
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.QUERY_TABLE, SOME_TABLE_NAME)
            .set(Constants.QUERY_CONDITIONS, conditions);
    Asset<JsonNode> table = createAsset(SOME_TABLE);
    when(ledger.get(SOME_TABLE_ASSET_ID)).thenReturn(Optional.of(table));

    // Act Assert
    assertThatThrownBy(() -> scan.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_INDEX_KEY_TYPE);
    verify(ledger).get(SOME_TABLE_ASSET_ID);
  }

  @Test
  public void match_CorrectArgumentsWithConditionForStringColumnGiven_ShouldReturnCorrectResults() {
    // Arrange
    Asset<JsonNode> table = createAsset(SOME_TABLE);
    Asset<JsonNode> index = createIndexAsset(SOME_PRIMARY_KEY_COLUMN, SOME_PRIMARY_KEY_VALUE_1);
    JsonNode record =
        createRecord(
            SOME_PRIMARY_KEY_VALUE_1, SOME_INDEX_KEY_COLUMN_VALUE_1, SOME_COLUMN_STRING_VALUE_1);
    Asset<JsonNode> asset = createAsset(record);
    when(ledger.get(SOME_TABLE_ASSET_ID)).thenReturn(Optional.of(table));
    when(ledger.scan(createAssetFilter(SOME_INDEX_ASSET_ID_1))).thenReturn(ImmutableList.of(index));
    when(ledger.get(SOME_RECORD_ASSET_ID_1)).thenReturn(Optional.of(asset));
    ArrayNode conditions1 = createConditions(SOME_COLUMN_STRING, "John", Constants.OPERATOR_EQ);
    ArrayNode conditions2 = createConditions(SOME_COLUMN_STRING, "", Constants.OPERATOR_NE);
    ArrayNode conditions3 = createConditions(SOME_COLUMN_STRING, "I", Constants.OPERATOR_GT);
    ArrayNode conditions4 = createConditions(SOME_COLUMN_STRING, "I", Constants.OPERATOR_GTE);
    ArrayNode conditions5 = createConditions(SOME_COLUMN_STRING, "K", Constants.OPERATOR_LT);
    ArrayNode conditions6 = createConditions(SOME_COLUMN_STRING, "K", Constants.OPERATOR_LTE);

    // Act Assert
    assertMatchedRecordExists(scan.invoke(ledger, createQueryArguments(conditions1), null), record);
    assertMatchedRecordExists(scan.invoke(ledger, createQueryArguments(conditions2), null), record);
    assertMatchedRecordExists(scan.invoke(ledger, createQueryArguments(conditions3), null), record);
    assertMatchedRecordExists(scan.invoke(ledger, createQueryArguments(conditions4), null), record);
    assertMatchedRecordExists(scan.invoke(ledger, createQueryArguments(conditions5), null), record);
    assertMatchedRecordExists(scan.invoke(ledger, createQueryArguments(conditions6), null), record);
  }

  @Test
  public void match_CorrectArgumentsWithConditionForNumberColumnGiven_ShouldReturnCorrectResults() {
    // Arrange
    Asset<JsonNode> table = createAsset(SOME_TABLE);
    Asset<JsonNode> index = createIndexAsset(SOME_PRIMARY_KEY_COLUMN, SOME_PRIMARY_KEY_VALUE_1);
    JsonNode record =
        createRecord(
            SOME_PRIMARY_KEY_VALUE_1, SOME_INDEX_KEY_COLUMN_VALUE_1, SOME_COLUMN_NUMBER_VALUE);
    Asset<JsonNode> asset = createAsset(record);
    when(ledger.get(SOME_TABLE_ASSET_ID)).thenReturn(Optional.of(table));
    when(ledger.scan(createAssetFilter(SOME_INDEX_ASSET_ID_1))).thenReturn(ImmutableList.of(index));
    when(ledger.get(SOME_RECORD_ASSET_ID_1)).thenReturn(Optional.of(asset));
    ArrayNode conditions1 = createConditions(SOME_COLUMN_NUMBER, 10, Constants.OPERATOR_EQ);
    ArrayNode conditions2 = createConditions(SOME_COLUMN_NUMBER, 20, Constants.OPERATOR_NE);
    ArrayNode conditions3 = createConditions(SOME_COLUMN_NUMBER, 5, Constants.OPERATOR_GT);
    ArrayNode conditions4 = createConditions(SOME_COLUMN_NUMBER, 10, Constants.OPERATOR_GTE);
    ArrayNode conditions5 = createConditions(SOME_COLUMN_NUMBER, 20, Constants.OPERATOR_LT);
    ArrayNode conditions6 = createConditions(SOME_COLUMN_NUMBER, 10, Constants.OPERATOR_LTE);
    ArrayNode conditions7 = createConditions(SOME_COLUMN_NUMBER, 10, Constants.OPERATOR_NE);
    ArrayNode conditions8 = createConditions(SOME_COLUMN_NUMBER, 20, Constants.OPERATOR_GTE);
    ArrayNode conditions9 = createConditions(SOME_COLUMN_NUMBER, 5, Constants.OPERATOR_LTE);
    ArrayNode conditions10 = createConditions(SOME_COLUMN_NUMBER, "10", Constants.OPERATOR_EQ);

    // Act Assert
    assertMatchedRecordExists(scan.invoke(ledger, createQueryArguments(conditions1), null), record);
    assertMatchedRecordExists(scan.invoke(ledger, createQueryArguments(conditions2), null), record);
    assertMatchedRecordExists(scan.invoke(ledger, createQueryArguments(conditions3), null), record);
    assertMatchedRecordExists(scan.invoke(ledger, createQueryArguments(conditions4), null), record);
    assertMatchedRecordExists(scan.invoke(ledger, createQueryArguments(conditions5), null), record);
    assertMatchedRecordExists(scan.invoke(ledger, createQueryArguments(conditions6), null), record);
    assertMatchedRecordNotExist(scan.invoke(ledger, createQueryArguments(conditions7), null));
    assertMatchedRecordNotExist(scan.invoke(ledger, createQueryArguments(conditions8), null));
    assertMatchedRecordNotExist(scan.invoke(ledger, createQueryArguments(conditions9), null));
    assertMatchedRecordNotExist(scan.invoke(ledger, createQueryArguments(conditions10), null));
  }

  @Test
  public void
      match_CorrectArgumentsWithConditionForBooleanColumnGiven_ShouldReturnCorrectResults() {
    // Arrange
    Asset<JsonNode> table = createAsset(SOME_TABLE);
    Asset<JsonNode> index = createIndexAsset(SOME_PRIMARY_KEY_COLUMN, SOME_PRIMARY_KEY_VALUE_1);
    JsonNode record =
        createRecord(
            SOME_PRIMARY_KEY_VALUE_1, SOME_INDEX_KEY_COLUMN_VALUE_1, SOME_COLUMN_BOOLEAN_VALUE);
    Asset<JsonNode> asset = createAsset(record);
    when(ledger.get(SOME_TABLE_ASSET_ID)).thenReturn(Optional.of(table));
    when(ledger.scan(createAssetFilter(SOME_INDEX_ASSET_ID_1))).thenReturn(ImmutableList.of(index));
    when(ledger.get(SOME_RECORD_ASSET_ID_1)).thenReturn(Optional.of(asset));
    ArrayNode conditions1 = createConditions(SOME_COLUMN_BOOLEAN, false, Constants.OPERATOR_EQ);
    ArrayNode conditions2 = createConditions(SOME_COLUMN_BOOLEAN, false, Constants.OPERATOR_NE);

    // Act Assert
    assertMatchedRecordExists(scan.invoke(ledger, createQueryArguments(conditions1), null), record);
    assertMatchedRecordNotExist(scan.invoke(ledger, createQueryArguments(conditions2), null));
  }

  @Test
  public void match_CorrectArgumentsWithConditionForNullColumnGiven_ShouldReturnCorrectResults() {
    // Arrange
    Asset<JsonNode> table = createAsset(SOME_TABLE);
    Asset<JsonNode> index = createIndexAsset(SOME_PRIMARY_KEY_COLUMN, SOME_PRIMARY_KEY_VALUE_1);
    JsonNode record =
        createRecord(
            SOME_PRIMARY_KEY_VALUE_1, SOME_INDEX_KEY_COLUMN_VALUE_1, SOME_COLUMN_STRING_VALUE_1);
    Asset<JsonNode> asset = createAsset(record);
    when(ledger.get(SOME_TABLE_ASSET_ID)).thenReturn(Optional.of(table));
    when(ledger.scan(createAssetFilter(SOME_INDEX_ASSET_ID_1))).thenReturn(ImmutableList.of(index));
    when(ledger.get(SOME_RECORD_ASSET_ID_1)).thenReturn(Optional.of(asset));
    ArrayNode conditions1 = createConditions(SOME_COLUMN_NULLABLE, Constants.OPERATOR_IS_NULL);
    ArrayNode conditions2 = createConditions(SOME_COLUMN_BOOLEAN, Constants.OPERATOR_IS_NULL);
    ArrayNode conditions3 = createConditions(SOME_COLUMN_STRING, Constants.OPERATOR_IS_NOT_NULL);
    ArrayNode conditions4 = createConditions(SOME_COLUMN_NULLABLE, Constants.OPERATOR_IS_NOT_NULL);

    // Act Assert
    assertMatchedRecordExists(scan.invoke(ledger, createQueryArguments(conditions1), null), record);
    assertMatchedRecordExists(scan.invoke(ledger, createQueryArguments(conditions2), null), record);
    assertMatchedRecordExists(scan.invoke(ledger, createQueryArguments(conditions3), null), record);
    assertMatchedRecordNotExist(scan.invoke(ledger, createQueryArguments(conditions4), null));
  }
}
