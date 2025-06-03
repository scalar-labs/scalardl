package com.scalar.dl.genericcontracts.table.v1_0_0;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.ImmutableList;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.AssetFilter.AgeOrder;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class ShowTablesTest {

  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String SOME_TABLE_NAME_1 = "tbl1";
  private static final String SOME_TABLE_NAME_2 = "tbl2";
  private static final String SOME_TABLE_KEY = "key";
  private static final String SOME_KEY_TYPE_STRING = "string";
  private static final String SOME_INDEX_KEY_1 = "key1";
  private static final String SOME_INDEX_KEY_2 = "key2";
  private static final String SOME_INVALID_TABLE_NAME = "invalid-table-name";
  private static final JsonNode SOME_TABLE_1 = createTable(SOME_TABLE_NAME_1);
  private static final JsonNode SOME_TABLE_2 = createTable(SOME_TABLE_NAME_2);

  @Spy private final ShowTables showTables = new ShowTables();
  @Mock private Ledger<JsonNode> ledger;

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this).close();
  }

  private static JsonNode createIndexNode(String key, String type) {
    return mapper
        .createObjectNode()
        .put(Constants.INDEX_KEY, key)
        .put(Constants.INDEX_KEY_TYPE, type);
  }

  private static JsonNode createTable(String tableName) {
    return mapper
        .createObjectNode()
        .put(Constants.TABLE_NAME, tableName)
        .put(Constants.TABLE_KEY, SOME_TABLE_KEY)
        .put(Constants.TABLE_KEY_TYPE, SOME_KEY_TYPE_STRING)
        .set(
            Constants.TABLE_INDEXES,
            mapper
                .createArrayNode()
                .add(createIndexNode(SOME_INDEX_KEY_1, SOME_KEY_TYPE_STRING))
                .add(createIndexNode(SOME_INDEX_KEY_2, SOME_KEY_TYPE_STRING)));
  }

  private String prepareTableAssetId(String tableName) {
    String assetId = GetAssetId.getAssetIdForTable(tableName);
    doReturn(assetId)
        .when(showTables)
        .getAssetId(ledger, Constants.PREFIX_TABLE, TextNode.valueOf(tableName));
    return assetId;
  }

  @Test
  public void invoke_CorrectArgumentsWithTableNameGiven_ShouldShowSingleTable() {
    // Arrange
    JsonNode argument = mapper.createObjectNode().put(Constants.TABLE_NAME, SOME_TABLE_NAME_1);
    JsonNode expected = mapper.createArrayNode().add(SOME_TABLE_1);
    String tableAssetId = prepareTableAssetId(SOME_TABLE_NAME_1);
    Asset<JsonNode> tableAsset = (Asset<JsonNode>) mock(Asset.class);
    when(tableAsset.data()).thenReturn(SOME_TABLE_1);
    when(ledger.get(tableAssetId)).thenReturn(Optional.of(tableAsset));

    // Act
    JsonNode actual = showTables.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isEqualTo(expected);
    verify(ledger).get(tableAssetId);
    verify(ledger, never()).scan(any());
  }

  @Test
  public void invoke_CorrectArgumentsWithoutTableNameGiven_ShouldShowAllTables() {
    // Arrange
    JsonNode argument = mapper.createObjectNode();
    JsonNode expected = mapper.createArrayNode().add(SOME_TABLE_1).add(SOME_TABLE_2);
    AssetFilter filter = new AssetFilter(Constants.ASSET_ID_METADATA_TABLES);
    filter.withAgeOrder(AgeOrder.ASC);
    Asset<JsonNode> tableAsset1 = (Asset<JsonNode>) mock(Asset.class);
    Asset<JsonNode> tableAsset2 = (Asset<JsonNode>) mock(Asset.class);
    when(tableAsset1.data()).thenReturn(SOME_TABLE_1);
    when(tableAsset2.data()).thenReturn(SOME_TABLE_2);
    when(ledger.scan(filter)).thenReturn(ImmutableList.of(tableAsset1, tableAsset2));

    // Act
    JsonNode actual = showTables.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isEqualTo(expected);
    verify(ledger).scan(filter);
    verify(ledger, never()).get(any());
  }

  @Test
  public void invoke_InvalidArgumentsGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument1 =
        mapper
            .createObjectNode()
            .put(Constants.TABLE_NAME, SOME_TABLE_NAME_1)
            .put(Constants.TABLE_KEY, SOME_TABLE_KEY);
    JsonNode argument2 = mapper.createObjectNode().put(Constants.TABLE_KEY, SOME_TABLE_KEY);
    JsonNode argument3 = mapper.createObjectNode().put(Constants.TABLE_NAME, 0);

    // Act Assert
    assertThatThrownBy(() -> showTables.invoke(ledger, argument1, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_CONTRACT_ARGUMENTS);
    assertThatThrownBy(() -> showTables.invoke(ledger, argument2, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_CONTRACT_ARGUMENTS);
    assertThatThrownBy(() -> showTables.invoke(ledger, argument3, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_CONTRACT_ARGUMENTS);
    verify(ledger, never()).get(any());
    verify(ledger, never()).scan(any());
  }

  @Test
  public void invoke_NonExistingTableGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument = mapper.createObjectNode().put(Constants.TABLE_NAME, SOME_TABLE_NAME_1);
    String tableAssetId = prepareTableAssetId(SOME_TABLE_NAME_1);
    when(ledger.get(tableAssetId)).thenReturn(Optional.empty());

    // Act Assert
    assertThatThrownBy(() -> showTables.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.TABLE_NOT_EXIST + SOME_TABLE_NAME_1);
    verify(ledger).get(tableAssetId);
  }

  @Test
  public void invoke_UnsupportedTableNameGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument =
        mapper.createObjectNode().put(Constants.TABLE_NAME, SOME_INVALID_TABLE_NAME);

    // Act Assert
    assertThatThrownBy(() -> showTables.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_OBJECT_NAME + SOME_INVALID_TABLE_NAME);
  }
}
