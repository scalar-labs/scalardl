package com.scalar.dl.genericcontracts.table.v1_0_0;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BigIntegerNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.scalar.dl.genericcontracts.table.Constants;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.math.BigInteger;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class GetAssetIdTest {
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final TextNode SOME_TABLE = TextNode.valueOf("table");
  private static final TextNode SOME_KEY = TextNode.valueOf("key");
  private static final TextNode SOME_VALUE_TEXT = TextNode.valueOf("value");
  private static final DoubleNode SOME_VALUE_DOUBLE = DoubleNode.valueOf(1.23);
  private static final DoubleNode SOME_VALUE_DOUBLE_INT = DoubleNode.valueOf(1.0);
  private static final BigIntegerNode SOME_VALUE_BIG_INTEGER =
      BigIntegerNode.valueOf(BigInteger.valueOf(Long.MIN_VALUE).multiply(BigInteger.TEN));

  private final GetAssetId getAssetId = new GetAssetId();

  @Mock private Ledger<JsonNode> ledger;

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this).close();
  }

  private ArrayNode createArrayNode(JsonNode... jsonNodes) {
    ArrayNode result = mapper.createArrayNode();
    Arrays.stream(jsonNodes).forEach(result::add);
    return result;
  }

  @Test
  public void invoke_CorrectArgumentsForTableGiven_ShouldReturnTableAssetId() {
    // Arrange
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.ASSET_ID_PREFIX, Constants.PREFIX_TABLE)
            .set(Constants.ASSET_ID_VALUES, createArrayNode(SOME_TABLE));
    String expected = Constants.PREFIX_TABLE + SOME_TABLE.asText();

    // Act
    JsonNode actual = getAssetId.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isNotNull();
    assertThat(actual.isTextual()).isTrue();
    assertThat(actual.textValue()).isEqualTo(expected);
  }

  @Test
  public void invoke_CorrectArgumentsForRecordGiven_ShouldReturnRecordAssetId() {
    // Arrange
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.ASSET_ID_PREFIX, Constants.PREFIX_RECORD)
            .set(Constants.ASSET_ID_VALUES, createArrayNode(SOME_TABLE, SOME_KEY, SOME_VALUE_TEXT));
    String expected =
        Constants.PREFIX_RECORD
            + SOME_TABLE.asText()
            + Constants.ASSET_ID_SEPARATOR
            + SOME_KEY.asText()
            + Constants.ASSET_ID_SEPARATOR
            + SOME_VALUE_TEXT.asText();

    // Act
    JsonNode actual = getAssetId.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isNotNull();
    assertThat(actual.isTextual()).isTrue();
    assertThat(actual.textValue()).isEqualTo(expected);
  }

  @Test
  public void invoke_CorrectArgumentsForRecordWithBigIntegerKeyGiven_ShouldReturnRecordAssetId() {
    // Arrange
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.ASSET_ID_PREFIX, Constants.PREFIX_RECORD)
            .set(
                Constants.ASSET_ID_VALUES,
                createArrayNode(SOME_TABLE, SOME_KEY, SOME_VALUE_BIG_INTEGER));
    String expected =
        Constants.PREFIX_RECORD
            + SOME_TABLE.asText()
            + Constants.ASSET_ID_SEPARATOR
            + SOME_KEY.asText()
            + Constants.ASSET_ID_SEPARATOR
            + Long.MIN_VALUE
            + "0";

    // Act
    JsonNode actual = getAssetId.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isNotNull();
    assertThat(actual.isTextual()).isTrue();
    assertThat(actual.textValue()).isEqualTo(expected);
  }

  @Test
  public void invoke_CorrectArgumentsForTextIndexValueGiven_ShouldReturnIndexAssetId() {
    // Arrange
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.ASSET_ID_PREFIX, Constants.PREFIX_INDEX)
            .set(Constants.ASSET_ID_VALUES, createArrayNode(SOME_TABLE, SOME_KEY, SOME_VALUE_TEXT));
    String expected =
        Constants.PREFIX_INDEX
            + SOME_TABLE.asText()
            + Constants.ASSET_ID_SEPARATOR
            + SOME_KEY.asText()
            + Constants.ASSET_ID_SEPARATOR
            + SOME_VALUE_TEXT.asText();

    // Act
    JsonNode actual = getAssetId.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isNotNull();
    assertThat(actual.isTextual()).isTrue();
    assertThat(actual.textValue()).isEqualTo(expected);
  }

  @Test
  public void invoke_CorrectArgumentsForDoubleIndexValueGiven_ShouldReturnIndexAssetId() {
    // Arrange
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.ASSET_ID_PREFIX, Constants.PREFIX_INDEX)
            .set(
                Constants.ASSET_ID_VALUES,
                createArrayNode(SOME_TABLE, SOME_KEY, SOME_VALUE_DOUBLE));
    String expected =
        Constants.PREFIX_INDEX
            + SOME_TABLE.asText()
            + Constants.ASSET_ID_SEPARATOR
            + SOME_KEY.asText()
            + Constants.ASSET_ID_SEPARATOR
            + SOME_VALUE_DOUBLE.asText();

    // Act
    JsonNode actual = getAssetId.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isNotNull();
    assertThat(actual.isTextual()).isTrue();
    assertThat(actual.textValue()).isEqualTo(expected);
  }

  @Test
  public void invoke_CorrectArgumentsForDoubleIntIndexValueGiven_ShouldReturnIndexAssetId() {
    // Arrange
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.ASSET_ID_PREFIX, Constants.PREFIX_INDEX)
            .set(
                Constants.ASSET_ID_VALUES,
                createArrayNode(SOME_TABLE, SOME_KEY, SOME_VALUE_DOUBLE_INT));
    String expected =
        Constants.PREFIX_INDEX
            + SOME_TABLE.asText()
            + Constants.ASSET_ID_SEPARATOR
            + SOME_KEY.asText()
            + Constants.ASSET_ID_SEPARATOR
            + SOME_VALUE_DOUBLE.asInt();

    // Act
    JsonNode actual = getAssetId.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isNotNull();
    assertThat(actual.isTextual()).isTrue();
    assertThat(actual.textValue()).isEqualTo(expected);
  }

  @Test
  public void invoke_CorrectArgumentsForNullIndexValueGiven_ShouldReturnNullIndexAssetId() {
    // Arrange
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.ASSET_ID_PREFIX, Constants.PREFIX_INDEX)
            .set(
                Constants.ASSET_ID_VALUES,
                createArrayNode(SOME_TABLE, SOME_KEY, NullNode.getInstance()));
    String expected =
        Constants.PREFIX_INDEX
            + SOME_TABLE.asText()
            + Constants.ASSET_ID_SEPARATOR
            + SOME_KEY.asText();

    // Act
    JsonNode actual = getAssetId.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isNotNull();
    assertThat(actual.isTextual()).isTrue();
    assertThat(actual.textValue()).isEqualTo(expected);
  }
}
