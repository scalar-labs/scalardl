package com.scalar.dl.genericcontracts.object.v1_0_0;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.scalar.dl.genericcontracts.object.Constants;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.AssetFilter.AgeOrder;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ValidateTest {

  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String SOME_OBJECT_ID = "foo";
  private static final String SOME_OBJECT_ID_WITH_PREFIX =
      Constants.OBJECT_ID_PREFIX + SOME_OBJECT_ID;
  private static final String SOME_VERSION_ID_0 = "v3";
  private static final String SOME_VERSION_ID_1 = "v4";
  private static final String SOME_VERSION_ID_2 = "v5";
  private static final String SOME_HASH_VALUE_0 = "bar3";
  private static final String SOME_HASH_VALUE_1 = "bar4";
  private static final String SOME_HASH_VALUE_2 = "bar5";

  private final Validate validate = new Validate();

  @Mock private Ledger<JsonNode> ledger;

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this).close();
  }

  @Test
  public void invoke_CorrectArgumentsGiven_ShouldReturnStatusNormal() {
    // Arrange
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .set(
                Constants.VERSIONS,
                mapper
                    .createArrayNode()
                    .add(createVersion(SOME_VERSION_ID_2, SOME_HASH_VALUE_2))
                    .add(createVersion(SOME_VERSION_ID_1, SOME_HASH_VALUE_1))
                    .add(createVersion(SOME_VERSION_ID_0, SOME_HASH_VALUE_0)));
    AssetFilter filter =
        new AssetFilter(SOME_OBJECT_ID_WITH_PREFIX).withLimit(3).withAgeOrder(AgeOrder.DESC);
    List<Asset<JsonNode>> assets = createAssets();
    when(ledger.scan(filter)).thenReturn(assets);

    // Act
    JsonNode actual = validate.invoke(ledger, argument, null);

    // Assert
    assertThat(actual.get(Constants.STATUS).asText()).isEqualTo(Constants.STATUS_CORRECT);
    assertThat(actual.get(Constants.DETAILS).asText()).isEqualTo(Constants.DETAILS_CORRECT_STATUS);
    assertThat(actual.get(Constants.FAULTY_VERSIONS).isArray()).isTrue();
    assertThat(actual.get(Constants.FAULTY_VERSIONS).size()).isEqualTo(0);
    verify(ledger).scan(filter);
  }

  @Test
  public void invoke_CorrectArgumentsWithMetadataGiven_ShouldReturnStatusNormal() {
    // Arrange
    JsonNode metadata = mapper.createObjectNode().put("foo", "bar");
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .set(
                Constants.VERSIONS,
                mapper
                    .createArrayNode()
                    .add(createVersion(SOME_VERSION_ID_2, SOME_HASH_VALUE_2, metadata))
                    .add(createVersion(SOME_VERSION_ID_1, SOME_HASH_VALUE_1, metadata))
                    .add(createVersion(SOME_VERSION_ID_0, SOME_HASH_VALUE_0, metadata)));
    AssetFilter filter =
        new AssetFilter(SOME_OBJECT_ID_WITH_PREFIX).withLimit(3).withAgeOrder(AgeOrder.DESC);
    List<Asset<JsonNode>> assets = createAssets(metadata);
    when(ledger.scan(filter)).thenReturn(assets);

    // Act
    JsonNode actual = validate.invoke(ledger, argument, null);

    // Assert
    assertThat(actual.get(Constants.STATUS).asText()).isEqualTo(Constants.STATUS_CORRECT);
    assertThat(actual.get(Constants.DETAILS).asText()).isEqualTo(Constants.DETAILS_CORRECT_STATUS);
    assertThat(actual.get(Constants.FAULTY_VERSIONS).isArray()).isTrue();
    assertThat(actual.get(Constants.FAULTY_VERSIONS).size()).isEqualTo(0);
    verify(ledger).scan(filter);
  }

  @Test
  public void invoke_TamperedArgumentsGiven_ShouldReturnStatusFaulty() {
    // Arrange
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .set(
                Constants.VERSIONS,
                mapper
                    .createArrayNode()
                    .add(createVersion(SOME_VERSION_ID_2, "tampered"))
                    .add(createVersion(SOME_VERSION_ID_1, SOME_HASH_VALUE_1))
                    .add(createVersion(SOME_VERSION_ID_0, "tampered")));
    AssetFilter filter =
        new AssetFilter(SOME_OBJECT_ID_WITH_PREFIX).withLimit(3).withAgeOrder(AgeOrder.DESC);
    List<Asset<JsonNode>> assets = createAssets();
    when(ledger.scan(any(AssetFilter.class))).thenReturn(assets);

    // Act
    JsonNode actual = validate.invoke(ledger, argument, null);

    // Assert
    assertThat(actual.get(Constants.STATUS).asText()).isEqualTo(Constants.STATUS_FAULTY);
    assertThat(actual.get(Constants.DETAILS).asText())
        .isEqualTo(Constants.DETAILS_FAULTY_VERSIONS_EXIST);
    assertThat(actual.get(Constants.FAULTY_VERSIONS).isArray()).isTrue();
    assertThat(actual.get(Constants.FAULTY_VERSIONS).size()).isEqualTo(2);
    assertThat(actual.get(Constants.FAULTY_VERSIONS).get(0).asText()).isEqualTo(SOME_VERSION_ID_2);
    assertThat(actual.get(Constants.FAULTY_VERSIONS).get(1).asText()).isEqualTo(SOME_VERSION_ID_0);
    verify(ledger).scan(filter);
  }

  @Test
  public void invoke_ArgumentsWithTamperedMetadataGiven_ShouldReturnStatusFaulty() {
    // Arrange
    JsonNode metadata = mapper.createObjectNode().put("foo", "bar");
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .set(
                Constants.VERSIONS,
                mapper
                    .createArrayNode()
                    .add(createVersion(SOME_VERSION_ID_2, SOME_HASH_VALUE_2))
                    .add(createVersion(SOME_VERSION_ID_1, SOME_HASH_VALUE_1, metadata))
                    .add(createVersion(SOME_VERSION_ID_0, SOME_HASH_VALUE_0)));
    Asset<JsonNode> asset0 = (Asset<JsonNode>) mock(Asset.class);
    Asset<JsonNode> asset1 = (Asset<JsonNode>) mock(Asset.class);
    Asset<JsonNode> asset2 = (Asset<JsonNode>) mock(Asset.class);
    JsonNode data0 =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .put(Constants.HASH_VALUE, SOME_HASH_VALUE_0);
    JsonNode data1 =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .put(Constants.HASH_VALUE, SOME_HASH_VALUE_1)
            .set(Constants.METADATA, mapper.createObjectNode().put("foo", "baz"));
    JsonNode data2 =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .put(Constants.HASH_VALUE, SOME_HASH_VALUE_2);
    AssetFilter filter =
        new AssetFilter(SOME_OBJECT_ID_WITH_PREFIX).withLimit(3).withAgeOrder(AgeOrder.DESC);
    when(asset0.data()).thenReturn(data0);
    when(asset1.data()).thenReturn(data1);
    when(asset2.data()).thenReturn(data2);
    when(ledger.scan(any(AssetFilter.class))).thenReturn(ImmutableList.of(asset2, asset1, asset0));

    // Act
    JsonNode actual = validate.invoke(ledger, argument, null);

    // Assert
    assertThat(actual.get(Constants.STATUS).asText()).isEqualTo(Constants.STATUS_FAULTY);
    assertThat(actual.get(Constants.DETAILS).asText())
        .isEqualTo(Constants.DETAILS_FAULTY_VERSIONS_EXIST);
    assertThat(actual.get(Constants.FAULTY_VERSIONS).isArray()).isTrue();
    assertThat(actual.get(Constants.FAULTY_VERSIONS).size()).isEqualTo(1);
    assertThat(actual.get(Constants.FAULTY_VERSIONS).get(0).asText()).isEqualTo(SOME_VERSION_ID_1);
    verify(ledger).scan(filter);
  }

  @Test
  public void invoke_TamperedArgumentsWithVerboseOptionGiven_ShouldReturnStatusFaulty() {
    // Arrange
    JsonNode storedMetadata = mapper.createObjectNode().put("foo", "baz");
    JsonNode givenMetadata = mapper.createObjectNode().put("foo", "bar");
    ObjectNode argument = mapper.createObjectNode().put(Constants.OBJECT_ID, SOME_OBJECT_ID);
    argument.set(
        Constants.VERSIONS,
        mapper
            .createArrayNode()
            .add(createVersion(SOME_VERSION_ID_2, "tampered"))
            .add(createVersion(SOME_VERSION_ID_1, SOME_HASH_VALUE_1, givenMetadata))
            .add(createVersion(SOME_VERSION_ID_0, SOME_HASH_VALUE_0)));
    argument.set(Constants.OPTIONS, mapper.createObjectNode().put(Constants.OPTION_VERBOSE, true));
    Asset<JsonNode> asset0 = (Asset<JsonNode>) mock(Asset.class);
    Asset<JsonNode> asset1 = (Asset<JsonNode>) mock(Asset.class);
    Asset<JsonNode> asset2 = (Asset<JsonNode>) mock(Asset.class);
    JsonNode data0 =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .put(Constants.HASH_VALUE, SOME_HASH_VALUE_0);
    JsonNode data1 =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .put(Constants.HASH_VALUE, SOME_HASH_VALUE_1)
            .set(Constants.METADATA, storedMetadata);
    JsonNode data2 =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .put(Constants.HASH_VALUE, SOME_HASH_VALUE_2);
    AssetFilter filter =
        new AssetFilter(SOME_OBJECT_ID_WITH_PREFIX).withLimit(3).withAgeOrder(AgeOrder.DESC);
    when(asset0.data()).thenReturn(data0);
    when(asset1.data()).thenReturn(data1);
    when(asset2.data()).thenReturn(data2);
    when(ledger.scan(any(AssetFilter.class))).thenReturn(ImmutableList.of(asset2, asset1, asset0));

    // Act
    JsonNode actual = validate.invoke(ledger, argument, null);

    // Assert
    assertThat(actual.get(Constants.STATUS).asText()).isEqualTo(Constants.STATUS_FAULTY);
    assertThat(actual.get(Constants.DETAILS).asText())
        .isEqualTo(Constants.DETAILS_FAULTY_VERSIONS_EXIST);
    assertThat(actual.get(Constants.FAULTY_VERSIONS).isArray()).isTrue();
    assertThat(actual.get(Constants.FAULTY_VERSIONS).size()).isEqualTo(2);
    assertThat(actual.get(Constants.FAULTY_VERSIONS).get(0).get(Constants.VERSION_ID).asText())
        .isEqualTo(SOME_VERSION_ID_2);
    assertThat(actual.get(Constants.FAULTY_VERSIONS).get(0).get(Constants.HASH_VALUE).asText())
        .isEqualTo(SOME_HASH_VALUE_2);
    assertThat(actual.get(Constants.FAULTY_VERSIONS).get(0).has(Constants.METADATA)).isFalse();
    assertThat(actual.get(Constants.FAULTY_VERSIONS).get(1).get(Constants.VERSION_ID).asText())
        .isEqualTo(SOME_VERSION_ID_1);
    assertThat(actual.get(Constants.FAULTY_VERSIONS).get(1).get(Constants.HASH_VALUE).asText())
        .isEqualTo(SOME_HASH_VALUE_1);
    assertThat(actual.get(Constants.FAULTY_VERSIONS).get(1).get(Constants.METADATA))
        .isEqualTo(storedMetadata);
    assertThat(actual.get(Constants.GIVEN_VERSIONS).get(0).get(Constants.VERSION_ID).asText())
        .isEqualTo(SOME_VERSION_ID_2);
    assertThat(actual.get(Constants.GIVEN_VERSIONS).get(0).get(Constants.HASH_VALUE).asText())
        .isEqualTo("tampered");
    assertThat(actual.get(Constants.GIVEN_VERSIONS).get(0).has(Constants.METADATA)).isFalse();
    assertThat(actual.get(Constants.GIVEN_VERSIONS).get(1).get(Constants.VERSION_ID).asText())
        .isEqualTo(SOME_VERSION_ID_1);
    assertThat(actual.get(Constants.GIVEN_VERSIONS).get(1).get(Constants.HASH_VALUE).asText())
        .isEqualTo(SOME_HASH_VALUE_1);
    assertThat(actual.get(Constants.GIVEN_VERSIONS).get(1).get(Constants.METADATA))
        .isEqualTo(givenMetadata);
    verify(ledger).scan(filter);
  }

  @Test
  public void invoke_CorrectArgumentsGivenButNoHashValueInAsset_ShouldReturnStatusFaulty() {
    // Arrange
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .set(
                Constants.VERSIONS,
                mapper
                    .createArrayNode()
                    .add(createVersion(SOME_VERSION_ID_2, SOME_HASH_VALUE_2))
                    .add(createVersion(SOME_VERSION_ID_1, SOME_HASH_VALUE_1))
                    .add(createVersion(SOME_VERSION_ID_0, SOME_HASH_VALUE_0)));
    Asset<JsonNode> asset0 = (Asset<JsonNode>) mock(Asset.class);
    Asset<JsonNode> asset1 = (Asset<JsonNode>) mock(Asset.class);
    Asset<JsonNode> asset2 = (Asset<JsonNode>) mock(Asset.class);
    JsonNode data0 = mapper.createObjectNode().put(Constants.OBJECT_ID, SOME_OBJECT_ID);
    JsonNode data1 = mapper.createObjectNode().put(Constants.OBJECT_ID, SOME_OBJECT_ID);
    JsonNode data2 =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .put(Constants.HASH_VALUE, SOME_HASH_VALUE_2);
    AssetFilter filter =
        new AssetFilter(SOME_OBJECT_ID_WITH_PREFIX).withLimit(3).withAgeOrder(AgeOrder.DESC);
    when(asset0.data()).thenReturn(data0);
    when(asset1.data()).thenReturn(data1);
    when(asset2.data()).thenReturn(data2);
    when(ledger.scan(any(AssetFilter.class))).thenReturn(ImmutableList.of(asset2, asset1, asset0));

    // Act
    JsonNode actual = validate.invoke(ledger, argument, null);

    // Assert
    assertThat(actual.get(Constants.STATUS).asText()).isEqualTo(Constants.STATUS_FAULTY);
    assertThat(actual.get(Constants.DETAILS).asText())
        .isEqualTo(Constants.DETAILS_FAULTY_VERSIONS_EXIST);
    assertThat(actual.get(Constants.FAULTY_VERSIONS).isArray()).isTrue();
    assertThat(actual.get(Constants.FAULTY_VERSIONS).size()).isEqualTo(2);
    assertThat(actual.get(Constants.FAULTY_VERSIONS).get(0).asText()).isEqualTo(SOME_VERSION_ID_1);
    assertThat(actual.get(Constants.FAULTY_VERSIONS).get(1).asText()).isEqualTo(SOME_VERSION_ID_0);
    verify(ledger).scan(filter);
  }

  @Test
  public void invoke_ArgumentsWithMetadataGivenButNoMetadataInAsset_ShouldReturnStatusFaulty() {
    // Arrange
    JsonNode metadata = mapper.createObjectNode().put("foo", "bar");
    JsonNode argument =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .set(
                Constants.VERSIONS,
                mapper
                    .createArrayNode()
                    .add(createVersion(SOME_VERSION_ID_2, SOME_HASH_VALUE_2, metadata))
                    .add(createVersion(SOME_VERSION_ID_1, SOME_HASH_VALUE_1, metadata))
                    .add(createVersion(SOME_VERSION_ID_0, SOME_HASH_VALUE_0, metadata)));
    AssetFilter filter =
        new AssetFilter(SOME_OBJECT_ID_WITH_PREFIX).withLimit(3).withAgeOrder(AgeOrder.DESC);
    List<Asset<JsonNode>> assets = createAssets();
    when(ledger.scan(any(AssetFilter.class))).thenReturn(assets);

    // Act
    JsonNode actual = validate.invoke(ledger, argument, null);

    // Assert
    assertThat(actual.get(Constants.STATUS).asText()).isEqualTo(Constants.STATUS_FAULTY);
    assertThat(actual.get(Constants.DETAILS).asText())
        .isEqualTo(Constants.DETAILS_FAULTY_VERSIONS_EXIST);
    assertThat(actual.get(Constants.FAULTY_VERSIONS).isArray()).isTrue();
    assertThat(actual.get(Constants.FAULTY_VERSIONS).size()).isEqualTo(3);
    assertThat(actual.get(Constants.FAULTY_VERSIONS).get(0).asText()).isEqualTo(SOME_VERSION_ID_2);
    assertThat(actual.get(Constants.FAULTY_VERSIONS).get(1).asText()).isEqualTo(SOME_VERSION_ID_1);
    assertThat(actual.get(Constants.FAULTY_VERSIONS).get(2).asText()).isEqualTo(SOME_VERSION_ID_0);
    verify(ledger).scan(filter);
  }

  @Test
  public void invoke_InsufficientVersionsWithAllOptionGiven_ShouldReturnStatusFaulty() {
    // Arrange
    ObjectNode argument = mapper.createObjectNode().put(Constants.OBJECT_ID, SOME_OBJECT_ID);
    argument.set(
        Constants.VERSIONS,
        mapper
            .createArrayNode()
            .add(createVersion(SOME_VERSION_ID_2, SOME_HASH_VALUE_2))
            .add(createVersion(SOME_VERSION_ID_1, SOME_HASH_VALUE_1)));
    argument.set(Constants.OPTIONS, mapper.createObjectNode().put(Constants.OPTION_ALL, true));
    AssetFilter filter = new AssetFilter(SOME_OBJECT_ID_WITH_PREFIX).withAgeOrder(AgeOrder.DESC);
    List<Asset<JsonNode>> assets = createAssets();
    when(ledger.scan(filter)).thenReturn(assets);

    // Act
    JsonNode actual = validate.invoke(ledger, argument, null);

    // Assert
    assertThat(actual.get(Constants.STATUS).asText()).isEqualTo(Constants.STATUS_FAULTY);
    assertThat(actual.get(Constants.DETAILS).asText())
        .isEqualTo(Constants.DETAILS_NUMBER_OF_VERSIONS_MISMATCH);
    assertThat(actual.get(Constants.FAULTY_VERSIONS).isArray()).isTrue();
    assertThat(actual.get(Constants.FAULTY_VERSIONS).size()).isEqualTo(0);
    verify(ledger).scan(filter);
  }

  @Test
  public void invoke_ArgumentsWithoutObjectIdGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument = mapper.createObjectNode();

    // Act Assert
    assertThatThrownBy(() -> validate.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.OBJECT_ID_IS_MISSING_OR_INVALID);
    verify(ledger, never()).scan(any());
  }

  @Test
  public void invoke_ArgumentsWithInvalidObjectIdTypeGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument = mapper.createObjectNode().put(Constants.OBJECT_ID, 1);

    // Act Assert
    Assertions.assertThatThrownBy(() -> validate.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.OBJECT_ID_IS_MISSING_OR_INVALID);
    verify(ledger, never()).scan(any());
  }

  @Test
  public void invoke_ArgumentsWithoutVersionArrayGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument = mapper.createObjectNode().put(Constants.OBJECT_ID, SOME_OBJECT_ID);

    // Act Assert
    assertThatThrownBy(() -> validate.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.VERSIONS_ARE_MISSING);
    verify(ledger, never()).scan(any());
  }

  @Test
  public void invoke_ArgumentsWithInvalidVersionsGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument1 =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .put(Constants.VERSIONS, "");
    JsonNode argument2 =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .set(Constants.VERSIONS, mapper.createArrayNode().add("bar5").add("bar4").add("bar3"));
    JsonNode argument3 =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .set(
                Constants.VERSIONS,
                mapper
                    .createArrayNode()
                    .add(
                        mapper
                            .createObjectNode()
                            .put("v5", "bar5")
                            .put("v4", "bar4")
                            .put("v3", "bar3")));
    JsonNode argument4 =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .set(
                Constants.VERSIONS,
                mapper
                    .createArrayNode()
                    .add(
                        mapper
                            .createObjectNode()
                            .put(Constants.VERSION_ID, "v5")
                            .put(Constants.HASH_VALUE, 5))
                    .add(
                        mapper
                            .createObjectNode()
                            .put(Constants.VERSION_ID, "v4")
                            .put(Constants.HASH_VALUE, 4))
                    .add(
                        mapper
                            .createObjectNode()
                            .put(Constants.VERSION_ID, "v3")
                            .put(Constants.HASH_VALUE, 3)));
    JsonNode argument5 =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .set(
                Constants.VERSIONS,
                mapper
                    .createArrayNode()
                    .add(mapper.createObjectNode().put(Constants.VERSION_ID, "v5")));
    JsonNode argument6 =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .set(
                Constants.VERSIONS,
                mapper
                    .createArrayNode()
                    .add(
                        mapper
                            .createObjectNode()
                            .put(Constants.VERSION_ID, 5)
                            .put(Constants.HASH_VALUE, "bar5")));
    JsonNode argument7 =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .set(
                Constants.VERSIONS,
                mapper
                    .createArrayNode()
                    .add(
                        mapper
                            .createObjectNode()
                            .put(Constants.VERSION_ID, "v5")
                            .put(Constants.HASH_VALUE, "bar5")
                            .put(Constants.METADATA, "metadata")));
    JsonNode argument8 =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .set(Constants.VERSIONS, mapper.createArrayNode());

    // Act Assert
    validateHashValuesAndAssert(ledger, argument1);
    validateHashValuesAndAssert(ledger, argument2);
    validateHashValuesAndAssert(ledger, argument3);
    validateHashValuesAndAssert(ledger, argument4);
    validateHashValuesAndAssert(ledger, argument5);
    validateHashValuesAndAssert(ledger, argument6);
    validateHashValuesAndAssert(ledger, argument7);
    validateHashValuesAndAssert(ledger, argument8);
    verify(ledger, never()).scan(any());
  }

  private void validateHashValuesAndAssert(Ledger<JsonNode> ledger, JsonNode argument) {
    Assertions.assertThatThrownBy(() -> validate.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_VERSIONS_FORMAT);
  }

  private JsonNode createVersion(String versionId, String hashValue, JsonNode metadata) {
    ObjectNode version =
        mapper
            .createObjectNode()
            .put(Constants.VERSION_ID, versionId)
            .put(Constants.HASH_VALUE, hashValue);
    if (metadata != null) {
      version.set(Constants.METADATA, metadata);
    }
    return version;
  }

  private JsonNode createVersion(String versionId, String hashValue) {
    return createVersion(versionId, hashValue, null);
  }

  private List<Asset<JsonNode>> createAssets(JsonNode metadata) {
    Asset<JsonNode> asset0 = (Asset<JsonNode>) mock(Asset.class);
    Asset<JsonNode> asset1 = (Asset<JsonNode>) mock(Asset.class);
    Asset<JsonNode> asset2 = (Asset<JsonNode>) mock(Asset.class);
    ObjectNode data0 =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .put(Constants.HASH_VALUE, SOME_HASH_VALUE_0);
    ObjectNode data1 =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .put(Constants.HASH_VALUE, SOME_HASH_VALUE_1);
    ObjectNode data2 =
        mapper
            .createObjectNode()
            .put(Constants.OBJECT_ID, SOME_OBJECT_ID)
            .put(Constants.HASH_VALUE, SOME_HASH_VALUE_2);
    if (metadata != null) {
      data0.set(Constants.METADATA, metadata);
      data1.set(Constants.METADATA, metadata);
      data2.set(Constants.METADATA, metadata);
    }
    when(asset0.data()).thenReturn(data0);
    when(asset1.data()).thenReturn(data1);
    when(asset2.data()).thenReturn(data2);
    return ImmutableList.of(asset2, asset1, asset0);
  }

  private List<Asset<JsonNode>> createAssets() {
    return createAssets(null);
  }
}
