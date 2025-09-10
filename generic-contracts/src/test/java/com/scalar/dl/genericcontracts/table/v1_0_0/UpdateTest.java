package com.scalar.dl.genericcontracts.table.v1_0_0;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.ImmutableMap;
import com.scalar.dl.genericcontracts.table.Constants;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class UpdateTest {
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String SOME_TABLE_NAME = "tbl";
  private static final String SOME_TABLE_KEY = "pkey";
  private static final String SOME_INDEX_KEY = "some_idx";
  private static final int SOME_INDEX_VALUE = 1;
  private static final String SOME_COLUMN = "col1";
  private static final String SOME_VALUE = "value";
  private static final String SOME_KEY_TYPE_STRING = "string";
  private static final String SOME_KEY_TYPE_NUMBER = "number";
  private static final String SOME_INVALID_OBJECT_NAME = "invalid-object-name";
  private static final String SOME_INVALID_FIELD = "field";
  private static final String SOME_INVALID_VALUE = "value";
  private static final ObjectNode SOME_RECORD_VALUES =
      mapper.createObjectNode().put(SOME_COLUMN, SOME_VALUE);
  private static final ObjectNode SOME_TABLE =
      mapper
          .createObjectNode()
          .put(Constants.TABLE_NAME, SOME_TABLE_NAME)
          .put(Constants.TABLE_KEY, SOME_TABLE_KEY)
          .put(Constants.TABLE_KEY_TYPE, SOME_KEY_TYPE_STRING)
          .set(
              Constants.TABLE_INDEXES,
              mapper.createArrayNode().add(createIndexNode(SOME_INDEX_KEY, SOME_KEY_TYPE_NUMBER)));

  @Spy private final Update update = new Update();
  @Mock private Ledger<JsonNode> ledger;
  private String tableAssetId;

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this).close();
    tableAssetId = prepareTableAssetId();
  }

  private static JsonNode createIndexNode(String key, String type) {
    return mapper
        .createObjectNode()
        .put(Constants.INDEX_KEY, key)
        .put(Constants.INDEX_KEY_TYPE, type);
  }

  private JsonNode createConditions() {
    return mapper
        .createArrayNode()
        .add(
            mapper
                .createObjectNode()
                .put(Constants.CONDITION_COLUMN, SOME_INDEX_KEY)
                .put(Constants.CONDITION_VALUE, SOME_INDEX_VALUE)
                .put(Constants.CONDITION_OPERATOR, Constants.OPERATOR_EQ));
  }

  private JsonNode createScan(JsonNode conditions) {
    ObjectNode scan = mapper.createObjectNode();
    scan.put(Constants.QUERY_TABLE, SOME_TABLE_NAME);
    scan.set(Constants.QUERY_CONDITIONS, conditions);
    scan.set(
        Constants.SCAN_OPTIONS,
        mapper.createObjectNode().put(Constants.SCAN_OPTIONS_INCLUDE_METADATA, true));
    return scan;
  }

  private JsonNode createIndexEntry(String key, int age, boolean deleted) {
    ObjectNode indexEntry =
        mapper
            .createObjectNode()
            .put(SOME_TABLE_KEY, key)
            .put(Constants.INDEX_ASSET_ADDED_AGE, age);
    if (deleted) {
      indexEntry.put(Constants.INDEX_ASSET_DELETE_MARKER, true);
    }
    return indexEntry;
  }

  private JsonNode createIndexEntries(Map<String, Integer> keyAges, boolean deleted) {
    ArrayNode indexEntries = mapper.createArrayNode();
    keyAges.forEach((key, age) -> indexEntries.add(createIndexEntry(key, age, deleted)));
    return indexEntries;
  }

  private Asset<JsonNode> createAsset(JsonNode data) {
    Asset<JsonNode> asset = (Asset<JsonNode>) mock(Asset.class);
    when(asset.data()).thenReturn(data);
    return asset;
  }

  private String prepareTableAssetId() {
    String assetId = GetAssetId.getAssetIdForTable(SOME_TABLE_NAME);
    doReturn(assetId)
        .when(update)
        .getAssetId(ledger, Constants.PREFIX_TABLE, TextNode.valueOf(SOME_TABLE_NAME));
    return assetId;
  }

  private String prepareRecordAssetId(String tableName, String key, JsonNode value) {
    String assetId = GetAssetId.getAssetIdForRecord(tableName, key, value);
    doReturn(assetId)
        .when(update)
        .getAssetId(
            ledger,
            Constants.PREFIX_RECORD,
            TextNode.valueOf(tableName),
            TextNode.valueOf(key),
            value);
    return assetId;
  }

  private String prepareIndexAssetId(String tableName, String key, JsonNode value) {
    String assetId =
        value.isNull()
            ? GetAssetId.getAssetIdForNullIndex(tableName, key)
            : GetAssetId.getAssetIdForIndex(tableName, key, value);
    doReturn(assetId)
        .when(update)
        .getAssetId(
            ledger,
            Constants.PREFIX_INDEX,
            TextNode.valueOf(tableName),
            TextNode.valueOf(key),
            value);
    return assetId;
  }

  @Test
  public void invoke_CorrectArgumentGiven_ShouldUpdateRecords() {
    // Arrange
    String key1 = "pkey1";
    String key2 = "pkey2";
    int newIndexValue = 10;
    String newValue = "newValue";
    int currentAge = 1;
    ObjectNode argument = mapper.createObjectNode();
    JsonNode conditions = createConditions();
    argument.put(Constants.UPDATE_TABLE, SOME_TABLE_NAME);
    argument.set(
        Constants.UPDATE_VALUES,
        mapper.createObjectNode().put(SOME_INDEX_KEY, newIndexValue).put(SOME_COLUMN, newValue));
    argument.set(Constants.UPDATE_CONDITIONS, conditions);
    Asset<JsonNode> tableMetadata = createAsset(SOME_TABLE);
    when(ledger.get(tableAssetId)).thenReturn(Optional.of(tableMetadata));
    JsonNode scan = createScan(conditions);
    JsonNode records =
        mapper
            .createArrayNode()
            .add(
                mapper
                    .createObjectNode()
                    .put(SOME_TABLE_KEY, key1)
                    .put(SOME_INDEX_KEY, SOME_INDEX_VALUE)
                    .put(SOME_COLUMN, "val1")
                    .put(Constants.SCAN_METADATA_AGE, currentAge))
            .add(
                mapper
                    .createObjectNode()
                    .put(SOME_TABLE_KEY, key2)
                    .put(SOME_INDEX_KEY, SOME_INDEX_VALUE)
                    .put(SOME_COLUMN, "val2")
                    .put(Constants.SCAN_METADATA_AGE, currentAge));
    doReturn(records).when(update).invokeSubContract(Constants.CONTRACT_SCAN, ledger, scan);
    String recordAssetId1 =
        prepareRecordAssetId(SOME_TABLE_NAME, SOME_TABLE_KEY, TextNode.valueOf(key1));
    String recordAssetId2 =
        prepareRecordAssetId(SOME_TABLE_NAME, SOME_TABLE_KEY, TextNode.valueOf(key2));
    String oldIndexAssetId =
        prepareIndexAssetId(SOME_TABLE_NAME, SOME_INDEX_KEY, IntNode.valueOf(SOME_INDEX_VALUE));
    String newIndexAssetId =
        prepareIndexAssetId(SOME_TABLE_NAME, SOME_INDEX_KEY, IntNode.valueOf(newIndexValue));
    JsonNode oldIndexEntries =
        createIndexEntries(ImmutableMap.of(key1, currentAge + 1, key2, currentAge + 1), true);
    JsonNode newIndexEntries =
        createIndexEntries(ImmutableMap.of(key1, currentAge + 1, key2, currentAge + 1), false);
    JsonNode expectedRecord1 =
        mapper
            .createObjectNode()
            .put(SOME_TABLE_KEY, key1)
            .put(SOME_INDEX_KEY, newIndexValue)
            .put(SOME_COLUMN, newValue);
    JsonNode expectedRecord2 =
        mapper
            .createObjectNode()
            .put(SOME_TABLE_KEY, key2)
            .put(SOME_INDEX_KEY, newIndexValue)
            .put(SOME_COLUMN, newValue);

    // Act
    JsonNode actual = update.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isNull();
    verify(update).invokeSubContract(eq(Constants.CONTRACT_SCAN), eq(ledger), eq(scan));
    verify(ledger).get(tableAssetId);
    verify(ledger).put(recordAssetId1, expectedRecord1);
    verify(ledger).put(recordAssetId2, expectedRecord2);
    verify(ledger).put(oldIndexAssetId, oldIndexEntries);
    verify(ledger).put(newIndexAssetId, newIndexEntries);
    verify(ledger, times(4)).put(any(), any());
  }

  @Test
  public void invoke_CorrectArgumentGivenToUpdateNullValue_ShouldUpdateRecords() {
    // Arrange
    String key1 = "pkey1";
    String key2 = "pkey2";
    int currentAge = 1;
    ObjectNode argument = mapper.createObjectNode();
    JsonNode conditions =
        mapper
            .createArrayNode()
            .add(
                mapper
                    .createObjectNode()
                    .put(Constants.CONDITION_COLUMN, SOME_INDEX_KEY)
                    .put(Constants.CONDITION_OPERATOR, Constants.OPERATOR_IS_NULL));
    argument.put(Constants.UPDATE_TABLE, SOME_TABLE_NAME);
    argument.set(
        Constants.UPDATE_VALUES, mapper.createObjectNode().put(SOME_INDEX_KEY, SOME_INDEX_VALUE));
    argument.set(Constants.UPDATE_CONDITIONS, conditions);
    Asset<JsonNode> tableMetadata = createAsset(SOME_TABLE);
    when(ledger.get(tableAssetId)).thenReturn(Optional.of(tableMetadata));
    JsonNode scan = createScan(conditions);
    JsonNode records =
        mapper
            .createArrayNode()
            .add(
                mapper
                    .createObjectNode()
                    .put(SOME_TABLE_KEY, key1)
                    .put(SOME_COLUMN, SOME_VALUE)
                    .put(Constants.SCAN_METADATA_AGE, currentAge)
                    .set(SOME_INDEX_KEY, null))
            .add(
                mapper
                    .createObjectNode()
                    .put(SOME_TABLE_KEY, key2)
                    .put(SOME_COLUMN, SOME_VALUE)
                    .put(Constants.SCAN_METADATA_AGE, currentAge));
    doReturn(records).when(update).invokeSubContract(Constants.CONTRACT_SCAN, ledger, scan);
    String recordAssetId1 =
        prepareRecordAssetId(SOME_TABLE_NAME, SOME_TABLE_KEY, TextNode.valueOf(key1));
    String recordAssetId2 =
        prepareRecordAssetId(SOME_TABLE_NAME, SOME_TABLE_KEY, TextNode.valueOf(key2));
    String oldIndexAssetId =
        prepareIndexAssetId(SOME_TABLE_NAME, SOME_INDEX_KEY, NullNode.getInstance());
    String newIndexAssetId =
        prepareIndexAssetId(SOME_TABLE_NAME, SOME_INDEX_KEY, IntNode.valueOf(SOME_INDEX_VALUE));
    JsonNode oldIndexEntries =
        createIndexEntries(ImmutableMap.of(key1, currentAge + 1, key2, currentAge + 1), true);
    JsonNode newIndexEntries =
        createIndexEntries(ImmutableMap.of(key1, currentAge + 1, key2, currentAge + 1), false);
    JsonNode expectedRecord1 =
        mapper
            .createObjectNode()
            .put(SOME_TABLE_KEY, key1)
            .put(SOME_INDEX_KEY, SOME_INDEX_VALUE)
            .put(SOME_COLUMN, SOME_VALUE);
    JsonNode expectedRecord2 =
        mapper
            .createObjectNode()
            .put(SOME_TABLE_KEY, key2)
            .put(SOME_INDEX_KEY, SOME_INDEX_VALUE)
            .put(SOME_COLUMN, SOME_VALUE);

    // Act
    JsonNode actual = update.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isNull();
    verify(update).invokeSubContract(eq(Constants.CONTRACT_SCAN), eq(ledger), eq(scan));
    verify(ledger).get(tableAssetId);
    verify(ledger).put(recordAssetId1, expectedRecord1);
    verify(ledger).put(recordAssetId2, expectedRecord2);
    verify(ledger).put(oldIndexAssetId, oldIndexEntries);
    verify(ledger).put(newIndexAssetId, newIndexEntries);
    verify(ledger, times(4)).put(any(), any());
  }

  @Test
  public void invoke_CorrectArgumentGivenToSetNullValue_ShouldUpdateRecords() {
    // Arrange
    String key = "pkey";
    int currentAge = 1;
    ObjectNode argument = mapper.createObjectNode();
    JsonNode conditions = createConditions();
    argument.put(Constants.UPDATE_TABLE, SOME_TABLE_NAME);
    argument.set(Constants.UPDATE_VALUES, mapper.createObjectNode().set(SOME_INDEX_KEY, null));
    argument.set(Constants.UPDATE_CONDITIONS, conditions);
    Asset<JsonNode> tableMetadata = createAsset(SOME_TABLE);
    when(ledger.get(tableAssetId)).thenReturn(Optional.of(tableMetadata));
    JsonNode scan = createScan(conditions);
    JsonNode records =
        mapper
            .createArrayNode()
            .add(
                mapper
                    .createObjectNode()
                    .put(SOME_TABLE_KEY, key)
                    .put(SOME_INDEX_KEY, SOME_INDEX_VALUE)
                    .put(SOME_COLUMN, SOME_VALUE)
                    .put(Constants.SCAN_METADATA_AGE, currentAge));
    doReturn(records).when(update).invokeSubContract(Constants.CONTRACT_SCAN, ledger, scan);
    String recordAssetId =
        prepareRecordAssetId(SOME_TABLE_NAME, SOME_TABLE_KEY, TextNode.valueOf(key));
    String oldIndexAssetId =
        prepareIndexAssetId(SOME_TABLE_NAME, SOME_INDEX_KEY, IntNode.valueOf(SOME_INDEX_VALUE));
    String newIndexAssetId =
        prepareIndexAssetId(SOME_TABLE_NAME, SOME_INDEX_KEY, NullNode.getInstance());
    JsonNode oldIndexEntries = createIndexEntries(ImmutableMap.of(key, currentAge + 1), true);
    JsonNode newIndexEntries = createIndexEntries(ImmutableMap.of(key, currentAge + 1), false);
    JsonNode expectedRecord =
        mapper
            .createObjectNode()
            .put(SOME_TABLE_KEY, key)
            .put(SOME_COLUMN, SOME_VALUE)
            .set(SOME_INDEX_KEY, null);

    // Act
    JsonNode actual = update.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isNull();
    verify(update).invokeSubContract(eq(Constants.CONTRACT_SCAN), eq(ledger), eq(scan));
    verify(ledger).get(tableAssetId);
    verify(ledger).put(recordAssetId, expectedRecord);
    verify(ledger).put(oldIndexAssetId, oldIndexEntries);
    verify(ledger).put(newIndexAssetId, newIndexEntries);
    verify(ledger, times(3)).put(any(), any());
  }

  @Test
  public void invoke_CorrectArgumentWithSameValuesGiven_ShouldNotUpdateRecords() {
    // Arrange
    String key = "pkey";
    int currentAge = 1;
    ObjectNode argument = mapper.createObjectNode();
    JsonNode conditions = createConditions();
    argument.put(Constants.UPDATE_TABLE, SOME_TABLE_NAME);
    argument.set(
        Constants.UPDATE_VALUES,
        mapper
            .createObjectNode()
            .put(SOME_INDEX_KEY, SOME_INDEX_VALUE)
            .put(SOME_COLUMN, SOME_VALUE));
    argument.set(Constants.UPDATE_CONDITIONS, conditions);
    Asset<JsonNode> tableMetadata = createAsset(SOME_TABLE);
    when(ledger.get(tableAssetId)).thenReturn(Optional.of(tableMetadata));
    JsonNode scan = createScan(conditions);
    JsonNode records =
        mapper
            .createArrayNode()
            .add(
                mapper
                    .createObjectNode()
                    .put(SOME_TABLE_KEY, key)
                    .put(SOME_INDEX_KEY, SOME_INDEX_VALUE)
                    .put(SOME_COLUMN, SOME_VALUE)
                    .put(Constants.SCAN_METADATA_AGE, currentAge));
    doReturn(records).when(update).invokeSubContract(Constants.CONTRACT_SCAN, ledger, scan);
    prepareIndexAssetId(SOME_TABLE_NAME, SOME_INDEX_KEY, IntNode.valueOf(SOME_INDEX_VALUE));

    // Act
    JsonNode actual = update.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isNull();
    verify(update).invokeSubContract(eq(Constants.CONTRACT_SCAN), eq(ledger), eq(scan));
    verify(ledger).get(tableAssetId);
    verify(ledger, never()).put(any(), any());
  }

  @Test
  public void invoke_InvalidArgumentsGiven_ShouldThrowContractContextException() {
    // Arrange
    ObjectNode argument1 = mapper.createObjectNode();
    ObjectNode argument2 = mapper.createObjectNode();
    argument2.put(SOME_INVALID_FIELD, SOME_INVALID_VALUE);
    argument2.set(Constants.UPDATE_VALUES, SOME_RECORD_VALUES);
    argument2.set(Constants.UPDATE_CONDITIONS, mapper.createArrayNode());
    ObjectNode argument3 = mapper.createObjectNode();
    argument3.put(Constants.UPDATE_TABLE, 0);
    argument3.set(Constants.UPDATE_VALUES, SOME_RECORD_VALUES);
    argument3.set(Constants.UPDATE_CONDITIONS, mapper.createArrayNode());
    ObjectNode argument4 = mapper.createObjectNode();
    argument4.put(Constants.UPDATE_TABLE, SOME_TABLE_NAME);
    argument4.put(SOME_INVALID_FIELD, SOME_INVALID_VALUE);
    argument4.set(Constants.UPDATE_CONDITIONS, mapper.createArrayNode());
    ObjectNode argument5 = mapper.createObjectNode();
    argument5.put(Constants.UPDATE_TABLE, SOME_TABLE_NAME);
    argument5.put(Constants.UPDATE_VALUES, SOME_INVALID_VALUE);
    argument5.set(Constants.UPDATE_CONDITIONS, mapper.createArrayNode());
    ObjectNode argument6 = mapper.createObjectNode();
    argument6.put(Constants.UPDATE_TABLE, SOME_TABLE_NAME);
    argument6.set(Constants.UPDATE_VALUES, SOME_RECORD_VALUES);
    argument6.put(SOME_INVALID_FIELD, SOME_INVALID_VALUE);
    ObjectNode argument7 = mapper.createObjectNode();
    argument7.put(Constants.UPDATE_TABLE, SOME_TABLE_NAME);
    argument7.set(Constants.UPDATE_VALUES, SOME_RECORD_VALUES);
    argument7.put(Constants.UPDATE_CONDITIONS, SOME_INVALID_VALUE);

    // Act Assert
    assertThatThrownBy(() -> update.invoke(ledger, argument1, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_UPDATE_FORMAT);
    assertThatThrownBy(() -> update.invoke(ledger, argument2, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_UPDATE_FORMAT);
    assertThatThrownBy(() -> update.invoke(ledger, argument3, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_UPDATE_FORMAT);
    assertThatThrownBy(() -> update.invoke(ledger, argument4, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_UPDATE_FORMAT);
    assertThatThrownBy(() -> update.invoke(ledger, argument5, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_UPDATE_FORMAT);
    assertThatThrownBy(() -> update.invoke(ledger, argument6, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_UPDATE_FORMAT);
    assertThatThrownBy(() -> update.invoke(ledger, argument7, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_UPDATE_FORMAT);
    verify(ledger, never()).get(any());
    verify(ledger, never()).put(any(), any());
  }

  @Test
  public void invoke_UnsupportedTableNameGiven_ShouldThrowContractContextException() {
    // Arrange
    ObjectNode argument = mapper.createObjectNode();
    argument.put(Constants.UPDATE_TABLE, SOME_INVALID_OBJECT_NAME);
    argument.set(Constants.UPDATE_VALUES, SOME_RECORD_VALUES);
    argument.set(Constants.UPDATE_CONDITIONS, createConditions());

    // Act Assert
    assertThatThrownBy(() -> update.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_OBJECT_NAME + SOME_INVALID_OBJECT_NAME);
    verify(ledger, never()).get(any());
    verify(ledger, never()).put(any(), any());
  }

  @Test
  public void invoke_UnsupportedFieldNameGiven_ShouldThrowContractContextException() {
    // Arrange
    ObjectNode argument = mapper.createObjectNode();
    argument.put(Constants.UPDATE_TABLE, SOME_INVALID_OBJECT_NAME);
    argument.set(
        Constants.UPDATE_VALUES,
        mapper.createObjectNode().put(SOME_INVALID_OBJECT_NAME, SOME_VALUE));
    argument.set(Constants.UPDATE_CONDITIONS, createConditions());

    // Act Assert
    assertThatThrownBy(() -> update.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_OBJECT_NAME + SOME_INVALID_OBJECT_NAME);
    verify(ledger, never()).get(any());
    verify(ledger, never()).put(any(), any());
  }

  @Test
  public void invoke_NonExistingTableGiven_ShouldThrowContractContextException() {
    // Arrange
    ObjectNode argument = mapper.createObjectNode();
    argument.put(Constants.UPDATE_TABLE, SOME_TABLE_NAME);
    argument.set(Constants.UPDATE_VALUES, SOME_RECORD_VALUES);
    argument.set(Constants.UPDATE_CONDITIONS, mapper.createArrayNode());
    when(ledger.get(tableAssetId)).thenReturn(Optional.empty());

    // Act Assert
    assertThatThrownBy(() -> update.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.TABLE_NOT_EXIST + SOME_TABLE_NAME);
    verify(ledger).get(tableAssetId);
    verify(ledger, times(1)).get(any());
    verify(ledger, never()).put(any(), any());
  }

  @Test
  public void invoke_PrimaryKeyGivenInUpdateValues_ShouldThrowContractContextException() {
    // Arrange
    ObjectNode argument = mapper.createObjectNode();
    argument.put(Constants.UPDATE_TABLE, SOME_TABLE_NAME);
    argument.set(
        Constants.UPDATE_VALUES, mapper.createObjectNode().put(SOME_TABLE_KEY, SOME_VALUE));
    argument.set(Constants.UPDATE_CONDITIONS, mapper.createArrayNode());
    Asset<JsonNode> tableMetadata = createAsset(SOME_TABLE);
    when(ledger.get(tableAssetId)).thenReturn(Optional.of(tableMetadata));

    // Act Assert
    assertThatThrownBy(() -> update.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.CANNOT_UPDATE_KEY);
    verify(ledger).get(tableAssetId);
    verify(ledger, times(1)).get(any());
    verify(ledger, never()).put(any(), any());
  }

  @Test
  public void
      invoke_DifferentTypeIndexKeyGivenInUpdateValues_ShouldThrowContractContextException() {
    // Arrange
    ObjectNode argument = mapper.createObjectNode();
    argument.put(Constants.UPDATE_TABLE, SOME_TABLE_NAME);
    argument.set(
        Constants.UPDATE_VALUES, mapper.createObjectNode().put(SOME_INDEX_KEY, SOME_VALUE));
    argument.set(Constants.UPDATE_CONDITIONS, mapper.createArrayNode());
    Asset<JsonNode> tableMetadata = createAsset(SOME_TABLE);
    when(ledger.get(tableAssetId)).thenReturn(Optional.of(tableMetadata));

    // Act Assert
    assertThatThrownBy(() -> update.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(
            Constants.INVALID_INDEX_KEY_TYPE + TextNode.valueOf(SOME_VALUE).getNodeType().name());
    verify(ledger).get(tableAssetId);
    verify(ledger, times(1)).get(any());
    verify(ledger, never()).put(any(), any());
  }
}
