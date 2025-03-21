package com.scalar.dl.genericcontracts.table.v1_0_0;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class InsertTest {
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String SOME_TABLE_NAME = "Person";
  private static final String SOME_TABLE_ASSET_ID = Constants.PREFIX_TABLE + SOME_TABLE_NAME;
  private static final String SOME_TABLE_KEY = "GovId";
  private static final String SOME_INVALID_FIELD = "field";
  private static final String SOME_INVALID_VALUE = "value";
  private static final String SOME_KEY_TYPE_STRING = "string";
  private static final String SOME_KEY_TYPE_BOOLEAN = "boolean";
  private static final String SOME_KEY_TYPE_NUMBER = "number";
  private static final String SOME_INDEX_KEY_1 = "firstName";
  private static final String SOME_INDEX_KEY_2 = "lastName";
  private static final String SOME_INDEX_KEY_3 = "boolean_column";
  private static final String SOME_RECORD_KEY = "001";
  private static final double SOME_RECORD_NUMBER_KEY = 0.01;
  private static final String SOME_RECORD_ASSET_ID =
      Constants.PREFIX_RECORD
          + SOME_TABLE_NAME
          + Constants.ASSET_ID_SEPARATOR
          + SOME_TABLE_KEY
          + Constants.ASSET_ID_SEPARATOR
          + SOME_RECORD_KEY;
  private static final String SOME_RECORD_ASSET_ID_WITH_NUMBER_KEY =
      Constants.PREFIX_RECORD
          + SOME_TABLE_NAME
          + Constants.ASSET_ID_SEPARATOR
          + SOME_TABLE_KEY
          + Constants.ASSET_ID_SEPARATOR
          + "0.01";
  private static final String SOME_RECORD_COLUMN = "address";
  private static final String SOME_RECORD_COLUMN_VALUE = "Tokyo";
  private static final boolean SOME_RECORD_COLUMN_VALUE_BOOLEAN = false;
  private static final String SOME_INDEX_COLUMN_VALUE = "John";
  private static final String SOME_INDEX_ASSET_ID_1 =
      Constants.PREFIX_INDEX
          + SOME_TABLE_NAME
          + Constants.ASSET_ID_SEPARATOR
          + SOME_INDEX_KEY_1
          + Constants.ASSET_ID_SEPARATOR
          + SOME_INDEX_COLUMN_VALUE;
  private static final String SOME_INDEX_ASSET_ID_2 =
      Constants.PREFIX_INDEX + SOME_TABLE_NAME + Constants.ASSET_ID_SEPARATOR + SOME_INDEX_KEY_2;
  private static final String SOME_INDEX_ASSET_ID_3 =
      Constants.PREFIX_INDEX
          + SOME_TABLE_NAME
          + Constants.ASSET_ID_SEPARATOR
          + SOME_INDEX_KEY_3
          + Constants.ASSET_ID_SEPARATOR
          + "false";
  private static final ObjectNode SOME_INDEX_ASSET =
      mapper.createObjectNode().put(SOME_TABLE_KEY, SOME_RECORD_KEY).put(Constants.ASSET_AGE, 0);
  private static final ObjectNode SOME_INDEX_ASSET_WITH_NUMBER_KEY =
      mapper
          .createObjectNode()
          .put(SOME_TABLE_KEY, SOME_RECORD_NUMBER_KEY)
          .put(Constants.ASSET_AGE, 0);
  private static final ObjectNode SOME_TABLE =
      mapper
          .createObjectNode()
          .put(Constants.TABLE_NAME, SOME_TABLE_NAME)
          .put(Constants.TABLE_KEY, SOME_TABLE_KEY)
          .put(Constants.TABLE_KEY_TYPE, SOME_KEY_TYPE_STRING)
          .set(
              Constants.TABLE_INDEXES,
              mapper
                  .createArrayNode()
                  .add(createIndexNode(SOME_INDEX_KEY_1, SOME_KEY_TYPE_STRING))
                  .add(createIndexNode(SOME_INDEX_KEY_2, SOME_KEY_TYPE_STRING)));
  private static final ObjectNode SOME_TABLE_WITH_NUMBER_KEY =
      mapper
          .createObjectNode()
          .put(Constants.TABLE_NAME, SOME_TABLE_NAME)
          .put(Constants.TABLE_KEY, SOME_TABLE_KEY)
          .put(Constants.TABLE_KEY_TYPE, SOME_KEY_TYPE_NUMBER)
          .set(
              Constants.TABLE_INDEXES,
              mapper
                  .createArrayNode()
                  .add(createIndexNode(SOME_INDEX_KEY_2, SOME_KEY_TYPE_STRING))
                  .add(createIndexNode(SOME_INDEX_KEY_3, SOME_KEY_TYPE_BOOLEAN)));
  private static final ObjectNode SOME_RECORD_VALUES =
      mapper
          .createObjectNode()
          .put(SOME_TABLE_KEY, SOME_RECORD_KEY)
          .put(SOME_INDEX_KEY_1, SOME_INDEX_COLUMN_VALUE)
          .put(SOME_RECORD_COLUMN, SOME_RECORD_COLUMN_VALUE);

  private final Insert insert = new Insert();

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
  public void invoke_CorrectArgumentsGiven_ShouldInsertRecord() {
    // Arrange
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.RECORD_TABLE, SOME_TABLE_NAME)
            .set(Constants.RECORD_VALUES, SOME_RECORD_VALUES);
    Asset<JsonNode> tableAsset = (Asset<JsonNode>) mock(Asset.class);
    when(tableAsset.data()).thenReturn(SOME_TABLE);
    when(ledger.get(SOME_TABLE_ASSET_ID)).thenReturn(Optional.of(tableAsset));
    when(ledger.get(SOME_RECORD_ASSET_ID)).thenReturn(Optional.empty());

    // Act
    JsonNode actual = insert.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isNull();
    verify(ledger).get(SOME_TABLE_ASSET_ID);
    verify(ledger).get(SOME_RECORD_ASSET_ID);
    verify(ledger).put(SOME_RECORD_ASSET_ID, SOME_RECORD_VALUES);
    verify(ledger).put(SOME_INDEX_ASSET_ID_1, SOME_INDEX_ASSET);
    verify(ledger).put(SOME_INDEX_ASSET_ID_2, SOME_INDEX_ASSET);
    verify(ledger, times(3)).put(any(), any());
  }

  @Test
  public void invoke_CorrectArgumentsWithNumberKeyGiven_ShouldInsertRecord() {
    // Arrange
    ObjectNode recordValuesWithNumberKey =
        mapper
            .createObjectNode()
            .put(SOME_TABLE_KEY, SOME_RECORD_NUMBER_KEY)
            .put(SOME_INDEX_KEY_3, SOME_RECORD_COLUMN_VALUE_BOOLEAN)
            .set(SOME_INDEX_KEY_2, null);
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.RECORD_TABLE, SOME_TABLE_NAME)
            .set(Constants.RECORD_VALUES, recordValuesWithNumberKey);
    Asset<JsonNode> tableAsset = (Asset<JsonNode>) mock(Asset.class);
    when(tableAsset.data()).thenReturn(SOME_TABLE_WITH_NUMBER_KEY);
    when(ledger.get(SOME_TABLE_ASSET_ID)).thenReturn(Optional.of(tableAsset));
    when(ledger.get(SOME_RECORD_ASSET_ID_WITH_NUMBER_KEY)).thenReturn(Optional.empty());

    // Act
    JsonNode actual = insert.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isNull();
    verify(ledger).get(SOME_TABLE_ASSET_ID);
    verify(ledger).get(SOME_RECORD_ASSET_ID_WITH_NUMBER_KEY);
    verify(ledger).put(SOME_RECORD_ASSET_ID_WITH_NUMBER_KEY, recordValuesWithNumberKey);
    verify(ledger).put(SOME_INDEX_ASSET_ID_2, SOME_INDEX_ASSET_WITH_NUMBER_KEY);
    verify(ledger).put(SOME_INDEX_ASSET_ID_3, SOME_INDEX_ASSET_WITH_NUMBER_KEY);
    verify(ledger, times(3)).put(any(), any());
  }

  @Test
  public void invoke_InvalidArgumentsGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument1 = mapper.createObjectNode();
    JsonNode argument2 =
        mapper
            .createObjectNode()
            .put(SOME_INVALID_FIELD, SOME_INVALID_VALUE)
            .set(Constants.RECORD_VALUES, SOME_RECORD_VALUES);
    JsonNode argument3 =
        mapper
            .createObjectNode()
            .put(Constants.RECORD_TABLE, SOME_TABLE_NAME)
            .put(SOME_INVALID_FIELD, SOME_INVALID_VALUE);
    JsonNode argument4 =
        mapper
            .createObjectNode()
            .put(Constants.RECORD_TABLE, 0)
            .set(Constants.RECORD_VALUES, SOME_RECORD_VALUES);
    JsonNode argument5 =
        mapper
            .createObjectNode()
            .put(Constants.RECORD_TABLE, SOME_TABLE_NAME)
            .put(Constants.RECORD_VALUES, SOME_INVALID_VALUE);

    // Act Assert
    assertThatThrownBy(() -> insert.invoke(ledger, argument1, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_RECORD_FORMAT);
    assertThatThrownBy(() -> insert.invoke(ledger, argument2, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_RECORD_FORMAT);
    assertThatThrownBy(() -> insert.invoke(ledger, argument3, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_RECORD_FORMAT);
    assertThatThrownBy(() -> insert.invoke(ledger, argument4, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_RECORD_FORMAT);
    assertThatThrownBy(() -> insert.invoke(ledger, argument5, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_RECORD_FORMAT);
    verify(ledger, never()).get(SOME_TABLE_ASSET_ID);
    verify(ledger, never()).put(any(), any());
  }

  @Test
  public void invoke_NonExistingTableGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.RECORD_TABLE, SOME_TABLE_NAME)
            .set(Constants.RECORD_VALUES, SOME_RECORD_VALUES);
    when(ledger.get(SOME_TABLE_ASSET_ID)).thenReturn(Optional.empty());

    // Act Assert
    assertThatThrownBy(() -> insert.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.TABLE_NOT_EXIST);
    verify(ledger).get(SOME_TABLE_ASSET_ID);
    verify(ledger, never()).get(SOME_RECORD_ASSET_ID);
    verify(ledger, never()).put(any(), any());
  }

  @Test
  public void invoke_ArgumentWithoutRecordKeyGiven_ShouldThrowContractContextException() {
    // Arrange
    ObjectNode recordValuesWithoutKey =
        mapper
            .createObjectNode()
            .put(SOME_INDEX_KEY_1, SOME_INDEX_COLUMN_VALUE)
            .put(SOME_RECORD_COLUMN, SOME_RECORD_COLUMN_VALUE);
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.RECORD_TABLE, SOME_TABLE_NAME)
            .set(Constants.RECORD_VALUES, recordValuesWithoutKey);
    Asset<JsonNode> tableAsset = (Asset<JsonNode>) mock(Asset.class);
    when(tableAsset.data()).thenReturn(SOME_TABLE);
    when(ledger.get(SOME_TABLE_ASSET_ID)).thenReturn(Optional.of(tableAsset));

    // Act Assert
    assertThatThrownBy(() -> insert.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.RECORD_KEY_NOT_EXIST);
    verify(ledger).get(SOME_TABLE_ASSET_ID);
    verify(ledger, never()).get(SOME_RECORD_ASSET_ID);
    verify(ledger, never()).put(any(), any());
  }

  @Test
  public void invoke_ArgumentWithInvalidRecordKeyGiven_ShouldThrowContractContextException() {
    // Arrange
    ObjectNode recordValuesWithInvalidKey =
        mapper
            .createObjectNode()
            .put(SOME_TABLE_KEY, 0)
            .put(SOME_INDEX_KEY_1, SOME_INDEX_COLUMN_VALUE)
            .put(SOME_RECORD_COLUMN, SOME_RECORD_COLUMN_VALUE);
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.RECORD_TABLE, SOME_TABLE_NAME)
            .set(Constants.RECORD_VALUES, recordValuesWithInvalidKey);
    Asset<JsonNode> tableAsset = (Asset<JsonNode>) mock(Asset.class);
    when(tableAsset.data()).thenReturn(SOME_TABLE);
    when(ledger.get(SOME_TABLE_ASSET_ID)).thenReturn(Optional.of(tableAsset));

    // Act Assert
    assertThatThrownBy(() -> insert.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_KEY_TYPE);
    verify(ledger).get(SOME_TABLE_ASSET_ID);
    verify(ledger, never()).get(SOME_RECORD_ASSET_ID);
    verify(ledger, never()).put(any(), any());
  }

  @Test
  public void invoke_UnsupportedIndexKeyTypeGiven_ShouldThrowContractContextException() {
    // Arrange
    ObjectNode recordValuesWithInvalidIndexKey =
        mapper
            .createObjectNode()
            .put(SOME_TABLE_KEY, SOME_RECORD_KEY)
            .put(SOME_INDEX_KEY_1, SOME_INDEX_COLUMN_VALUE)
            .put(SOME_INDEX_KEY_2, 0)
            .put(SOME_RECORD_COLUMN, SOME_RECORD_COLUMN_VALUE);
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.RECORD_TABLE, SOME_TABLE_NAME)
            .set(Constants.RECORD_VALUES, recordValuesWithInvalidIndexKey);
    Asset<JsonNode> tableAsset = (Asset<JsonNode>) mock(Asset.class);
    when(tableAsset.data()).thenReturn(SOME_TABLE);
    when(ledger.get(SOME_TABLE_ASSET_ID)).thenReturn(Optional.of(tableAsset));
    when(ledger.get(SOME_RECORD_ASSET_ID)).thenReturn(Optional.empty());

    // Act Assert
    assertThatThrownBy(() -> insert.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_INDEX_KEY_TYPE);
    verify(ledger).get(SOME_TABLE_ASSET_ID);
    verify(ledger).get(SOME_RECORD_ASSET_ID);
    verify(ledger).put(SOME_INDEX_ASSET_ID_1, SOME_INDEX_ASSET);
    verify(ledger, times(1)).put(any(), any());
  }

  @Test
  public void invoke_ExistingRecordGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.RECORD_TABLE, SOME_TABLE_NAME)
            .set(Constants.RECORD_VALUES, SOME_RECORD_VALUES);
    Asset<JsonNode> tableAsset = (Asset<JsonNode>) mock(Asset.class);
    Asset<JsonNode> recordAsset = (Asset<JsonNode>) mock(Asset.class);
    when(tableAsset.data()).thenReturn(SOME_TABLE);
    when(ledger.get(SOME_TABLE_ASSET_ID)).thenReturn(Optional.of(tableAsset));
    when(ledger.get(SOME_RECORD_ASSET_ID)).thenReturn(Optional.of(recordAsset));

    // Act Assert
    assertThatThrownBy(() -> insert.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.RECORD_ALREADY_EXISTS);
    verify(ledger).get(SOME_TABLE_ASSET_ID);
    verify(ledger).get(SOME_RECORD_ASSET_ID);
    verify(ledger, never()).put(eq(SOME_RECORD_ASSET_ID), any(JsonNode.class));
  }
}
