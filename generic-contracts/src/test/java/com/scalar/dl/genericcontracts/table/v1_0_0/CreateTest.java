package com.scalar.dl.genericcontracts.table.v1_0_0;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class CreateTest {

  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String SOME_TABLE_NAME = "table";
  private static final String SOME_TABLE_ASSET_ID = Constants.PREFIX_TABLE + SOME_TABLE_NAME;
  private static final String SOME_TABLE_KEY = "key";
  private static final String SOME_INVALID_OBJECT_NAME = "invalid-object-name";
  private static final String SOME_INVALID_FIELD = "field";
  private static final String SOME_INVALID_VALUE = "value";
  private static final String SOME_KEY_TYPE_STRING = "string";
  private static final String SOME_KEY_TYPE_BOOLEAN = "boolean";
  private static final String SOME_KEY_TYPE_NUMBER = "number";
  private static final String SOME_KEY_TYPE_INVALID = "invalid";
  private static final String SOME_INDEX_KEY_1 = "key1";
  private static final String SOME_INDEX_KEY_2 = "key2";
  private static final ArrayNode SOME_INDEXES =
      mapper
          .createArrayNode()
          .add(createIndexNode(SOME_INDEX_KEY_1, SOME_KEY_TYPE_BOOLEAN))
          .add(createIndexNode(SOME_INDEX_KEY_2, SOME_KEY_TYPE_NUMBER));

  @Spy private final Create create = new Create();
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

  @Test
  public void invoke_CorrectArgumentsGiven_ShouldCreateTable() {
    // Arrange
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.TABLE_NAME, SOME_TABLE_NAME)
            .put(Constants.TABLE_KEY, SOME_TABLE_KEY)
            .put(Constants.TABLE_KEY_TYPE, SOME_KEY_TYPE_STRING)
            .set(Constants.TABLE_INDEXES, SOME_INDEXES);
    doReturn(GetAssetId.getAssetIdForTable(SOME_TABLE_NAME))
        .when(create)
        .getAssetIdForTable(ledger, SOME_TABLE_NAME);
    when(ledger.get(SOME_TABLE_ASSET_ID)).thenReturn(Optional.empty());

    // Act
    JsonNode actual = create.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isNull();
    verify(ledger).get(SOME_TABLE_ASSET_ID);
    verify(ledger).put(SOME_TABLE_ASSET_ID, argument);
    verify(ledger).put(Constants.ASSET_ID_METADATA_TABLES, argument);
  }

  @Test
  public void invoke_CorrectArgumentsWithoutIndexesGiven_ShouldCreateTableWithEmptyIndexes() {
    // Arrange
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.TABLE_NAME, SOME_TABLE_NAME)
            .put(Constants.TABLE_KEY, SOME_TABLE_KEY)
            .put(Constants.TABLE_KEY_TYPE, SOME_KEY_TYPE_STRING);
    ObjectNode expected = argument.deepCopy();
    expected.set(Constants.TABLE_INDEXES, mapper.createArrayNode());
    doReturn(GetAssetId.getAssetIdForTable(SOME_TABLE_NAME))
        .when(create)
        .getAssetIdForTable(ledger, SOME_TABLE_NAME);
    when(ledger.get(SOME_TABLE_ASSET_ID)).thenReturn(Optional.empty());

    // Act
    JsonNode actual = create.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isNull();
    verify(ledger).get(SOME_TABLE_ASSET_ID);
    verify(ledger).put(SOME_TABLE_ASSET_ID, expected);
    verify(ledger).put(Constants.ASSET_ID_METADATA_TABLES, expected);
  }

  @Test
  public void invoke_InvalidArgumentsGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument1 = mapper.createObjectNode();
    JsonNode argument2 =
        mapper
            .createObjectNode()
            .put(Constants.TABLE_NAME, SOME_TABLE_NAME)
            .put(Constants.TABLE_KEY, SOME_TABLE_KEY)
            .put(Constants.TABLE_KEY_TYPE, SOME_KEY_TYPE_STRING)
            .put(SOME_INVALID_FIELD, SOME_INVALID_VALUE)
            .set(Constants.TABLE_INDEXES, SOME_INDEXES);
    JsonNode argument3 =
        mapper
            .createObjectNode()
            .put(Constants.TABLE_KEY, SOME_TABLE_KEY)
            .put(Constants.TABLE_KEY_TYPE, SOME_KEY_TYPE_STRING)
            .set(Constants.TABLE_INDEXES, SOME_INDEXES);
    JsonNode argument4 =
        mapper
            .createObjectNode()
            .put(Constants.TABLE_NAME, SOME_TABLE_NAME)
            .put(Constants.TABLE_KEY_TYPE, SOME_KEY_TYPE_STRING)
            .set(Constants.TABLE_INDEXES, SOME_INDEXES);
    JsonNode argument5 =
        mapper
            .createObjectNode()
            .put(Constants.TABLE_NAME, SOME_TABLE_NAME)
            .put(Constants.TABLE_KEY, SOME_TABLE_KEY)
            .set(Constants.TABLE_INDEXES, SOME_INDEXES);
    JsonNode argument6 =
        mapper
            .createObjectNode()
            .put(Constants.TABLE_NAME, 0)
            .put(Constants.TABLE_KEY, SOME_TABLE_KEY)
            .put(Constants.TABLE_KEY_TYPE, SOME_KEY_TYPE_STRING);
    JsonNode argument7 =
        mapper
            .createObjectNode()
            .put(Constants.TABLE_NAME, SOME_TABLE_NAME)
            .put(Constants.TABLE_KEY, 0)
            .put(Constants.TABLE_KEY_TYPE, SOME_KEY_TYPE_STRING);
    JsonNode argument8 =
        mapper
            .createObjectNode()
            .put(Constants.TABLE_NAME, SOME_TABLE_NAME)
            .put(Constants.TABLE_KEY, SOME_TABLE_KEY)
            .put(Constants.TABLE_KEY_TYPE, 0);

    // Act Assert
    assertThatThrownBy(() -> create.invoke(ledger, argument1, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_TABLE_FORMAT);
    assertThatThrownBy(() -> create.invoke(ledger, argument2, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_TABLE_FORMAT);
    assertThatThrownBy(() -> create.invoke(ledger, argument3, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_TABLE_FORMAT);
    assertThatThrownBy(() -> create.invoke(ledger, argument4, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_TABLE_FORMAT);
    assertThatThrownBy(() -> create.invoke(ledger, argument5, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_TABLE_FORMAT);
    assertThatThrownBy(() -> create.invoke(ledger, argument6, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_TABLE_FORMAT);
    assertThatThrownBy(() -> create.invoke(ledger, argument7, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_TABLE_FORMAT);
    assertThatThrownBy(() -> create.invoke(ledger, argument8, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_TABLE_FORMAT);
    verify(ledger, never()).get(SOME_TABLE_ASSET_ID);
    verify(ledger, never()).put(eq(SOME_TABLE_ASSET_ID), any(JsonNode.class));
  }

  @Test
  public void invoke_InvalidIndexArgumentsGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument1 =
        mapper
            .createObjectNode()
            .put(Constants.TABLE_NAME, SOME_TABLE_NAME)
            .put(Constants.TABLE_KEY, SOME_TABLE_KEY)
            .put(Constants.TABLE_KEY_TYPE, SOME_KEY_TYPE_STRING)
            .put(Constants.TABLE_INDEXES, SOME_INVALID_VALUE);
    JsonNode argument2 =
        mapper
            .createObjectNode()
            .put(Constants.TABLE_NAME, SOME_TABLE_NAME)
            .put(Constants.TABLE_KEY, SOME_TABLE_KEY)
            .put(Constants.TABLE_KEY_TYPE, SOME_KEY_TYPE_STRING)
            .set(Constants.TABLE_INDEXES, mapper.createArrayNode().add(SOME_INDEX_KEY_1));
    JsonNode argument3 =
        mapper
            .createObjectNode()
            .put(Constants.TABLE_NAME, SOME_TABLE_NAME)
            .put(Constants.TABLE_KEY, SOME_TABLE_KEY)
            .put(Constants.TABLE_KEY_TYPE, SOME_KEY_TYPE_STRING)
            .set(
                Constants.TABLE_INDEXES,
                mapper
                    .createArrayNode()
                    .add(mapper.createObjectNode().put(SOME_INVALID_FIELD, SOME_INVALID_VALUE)));
    JsonNode argument4 =
        mapper
            .createObjectNode()
            .put(Constants.TABLE_NAME, SOME_TABLE_NAME)
            .put(Constants.TABLE_KEY, SOME_TABLE_KEY)
            .put(Constants.TABLE_KEY_TYPE, SOME_KEY_TYPE_STRING)
            .set(
                Constants.TABLE_INDEXES,
                mapper
                    .createArrayNode()
                    .add(
                        mapper
                            .createObjectNode()
                            .put(SOME_INVALID_FIELD, SOME_INVALID_VALUE)
                            .put(Constants.INDEX_KEY, SOME_INDEX_KEY_1)));
    JsonNode argument5 =
        mapper
            .createObjectNode()
            .put(Constants.TABLE_NAME, SOME_TABLE_NAME)
            .put(Constants.TABLE_KEY, SOME_TABLE_KEY)
            .put(Constants.TABLE_KEY_TYPE, SOME_KEY_TYPE_STRING)
            .set(
                Constants.TABLE_INDEXES,
                mapper
                    .createArrayNode()
                    .add(
                        mapper
                            .createObjectNode()
                            .put(SOME_INVALID_FIELD, SOME_INVALID_VALUE)
                            .put(Constants.INDEX_KEY_TYPE, SOME_KEY_TYPE_NUMBER)));
    JsonNode argument6 =
        mapper
            .createObjectNode()
            .put(Constants.TABLE_NAME, SOME_TABLE_NAME)
            .put(Constants.TABLE_KEY, SOME_TABLE_KEY)
            .put(Constants.TABLE_KEY_TYPE, SOME_KEY_TYPE_STRING)
            .set(
                Constants.TABLE_INDEXES,
                mapper
                    .createArrayNode()
                    .add(
                        mapper
                            .createObjectNode()
                            .put(Constants.INDEX_KEY_TYPE, SOME_KEY_TYPE_NUMBER)
                            .set(Constants.INDEX_KEY, null)));
    JsonNode argument7 =
        mapper
            .createObjectNode()
            .put(Constants.TABLE_NAME, SOME_TABLE_NAME)
            .put(Constants.TABLE_KEY, SOME_TABLE_KEY)
            .put(Constants.TABLE_KEY_TYPE, SOME_KEY_TYPE_STRING)
            .set(
                Constants.TABLE_INDEXES,
                mapper
                    .createArrayNode()
                    .add(
                        mapper
                            .createObjectNode()
                            .put(Constants.INDEX_KEY, SOME_INDEX_KEY_1)
                            .set(Constants.INDEX_KEY_TYPE, null)));

    // Act Assert
    assertThatThrownBy(() -> create.invoke(ledger, argument1, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_INDEX_FORMAT);
    assertThatThrownBy(() -> create.invoke(ledger, argument2, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_INDEX_FORMAT);
    assertThatThrownBy(() -> create.invoke(ledger, argument3, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_INDEX_FORMAT);
    assertThatThrownBy(() -> create.invoke(ledger, argument4, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_INDEX_FORMAT);
    assertThatThrownBy(() -> create.invoke(ledger, argument5, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_INDEX_FORMAT);
    assertThatThrownBy(() -> create.invoke(ledger, argument6, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_INDEX_FORMAT);
    assertThatThrownBy(() -> create.invoke(ledger, argument7, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_INDEX_FORMAT);
    verify(ledger, never()).get(SOME_TABLE_ASSET_ID);
    verify(ledger, never()).put(eq(SOME_TABLE_ASSET_ID), any(JsonNode.class));
  }

  @Test
  public void invoke_UnsupportedTableNameGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.TABLE_NAME, SOME_INVALID_OBJECT_NAME)
            .put(Constants.TABLE_KEY, SOME_TABLE_KEY)
            .put(Constants.TABLE_KEY_TYPE, SOME_KEY_TYPE_STRING);

    // Act Assert
    assertThatThrownBy(() -> create.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_OBJECT_NAME + SOME_INVALID_OBJECT_NAME);
    verify(ledger, never()).get(SOME_TABLE_ASSET_ID);
    verify(ledger, never()).put(eq(SOME_TABLE_ASSET_ID), any(JsonNode.class));
  }

  @Test
  public void invoke_UnsupportedPrimaryKeyNameGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.TABLE_NAME, SOME_TABLE_NAME)
            .put(Constants.TABLE_KEY, SOME_INVALID_OBJECT_NAME)
            .put(Constants.TABLE_KEY_TYPE, SOME_KEY_TYPE_STRING);

    // Act Assert
    assertThatThrownBy(() -> create.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_OBJECT_NAME + SOME_INVALID_OBJECT_NAME);
    verify(ledger, never()).get(SOME_TABLE_ASSET_ID);
    verify(ledger, never()).put(eq(SOME_TABLE_ASSET_ID), any(JsonNode.class));
  }

  @Test
  public void invoke_UnsupportedIndexKeyNameGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.TABLE_NAME, SOME_TABLE_NAME)
            .put(Constants.TABLE_KEY, SOME_TABLE_KEY)
            .put(Constants.TABLE_KEY_TYPE, SOME_KEY_TYPE_STRING)
            .set(
                Constants.TABLE_INDEXES,
                mapper
                    .createArrayNode()
                    .add(
                        mapper
                            .createObjectNode()
                            .put(Constants.INDEX_KEY, SOME_INVALID_OBJECT_NAME)
                            .put(Constants.INDEX_KEY_TYPE, SOME_KEY_TYPE_NUMBER)));

    // Act Assert
    assertThatThrownBy(() -> create.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_OBJECT_NAME + SOME_INVALID_OBJECT_NAME);
    verify(ledger, never()).get(SOME_TABLE_ASSET_ID);
    verify(ledger, never()).put(eq(SOME_TABLE_ASSET_ID), any(JsonNode.class));
  }

  @Test
  public void invoke_UnsupportedKeyTypeGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.TABLE_NAME, SOME_TABLE_NAME)
            .put(Constants.TABLE_KEY, SOME_TABLE_KEY)
            .put(Constants.TABLE_KEY_TYPE, SOME_KEY_TYPE_INVALID);

    // Act Assert
    assertThatThrownBy(() -> create.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_KEY_TYPE + SOME_KEY_TYPE_INVALID);
    verify(ledger, never()).get(SOME_TABLE_ASSET_ID);
    verify(ledger, never()).put(eq(SOME_TABLE_ASSET_ID), any(JsonNode.class));
  }

  @Test
  public void invoke_UnsupportedIndexKeyTypeGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.TABLE_NAME, SOME_TABLE_NAME)
            .put(Constants.TABLE_KEY, SOME_TABLE_KEY)
            .put(Constants.TABLE_KEY_TYPE, SOME_KEY_TYPE_STRING)
            .set(
                Constants.TABLE_INDEXES,
                mapper
                    .createArrayNode()
                    .add(
                        mapper
                            .createObjectNode()
                            .put(Constants.INDEX_KEY, SOME_INDEX_KEY_1)
                            .put(Constants.INDEX_KEY_TYPE, SOME_KEY_TYPE_INVALID)));

    // Act Assert
    assertThatThrownBy(() -> create.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_INDEX_KEY_TYPE + SOME_KEY_TYPE_INVALID);
    verify(ledger, never()).get(SOME_TABLE_ASSET_ID);
    verify(ledger, never()).put(eq(SOME_TABLE_ASSET_ID), any(JsonNode.class));
  }

  @Test
  public void invoke_ExistingTableGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.TABLE_NAME, SOME_TABLE_NAME)
            .put(Constants.TABLE_KEY, SOME_TABLE_KEY)
            .put(Constants.TABLE_KEY_TYPE, SOME_KEY_TYPE_STRING);
    Asset<JsonNode> asset = (Asset<JsonNode>) mock(Asset.class);
    doReturn(GetAssetId.getAssetIdForTable(SOME_TABLE_NAME))
        .when(create)
        .getAssetIdForTable(ledger, SOME_TABLE_NAME);
    when(ledger.get(SOME_TABLE_ASSET_ID)).thenReturn(Optional.of(asset));

    // Act Assert
    assertThatThrownBy(() -> create.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.TABLE_ALREADY_EXISTS);
    verify(ledger).get(SOME_TABLE_ASSET_ID);
    verify(ledger, never()).put(eq(SOME_TABLE_ASSET_ID), any(JsonNode.class));
  }
}
