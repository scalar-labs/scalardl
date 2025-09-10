package com.scalar.dl.genericcontracts.collection.v1_0_0;

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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.scalar.dl.genericcontracts.collection.Constants;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.AssetFilter.AgeOrder;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class GetTest {

  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String SOME_COLLECTION_ID = "set";
  private static final String SOME_COLLECTION_ID_WITH_PREFIX =
      Constants.COLLECTION_ID_PREFIX + SOME_COLLECTION_ID;
  private static final int SOME_CHECKPOINT_AGE = 10;
  private static final int SOME_NON_CHECKPOINT_AGE = 12;
  private static final JsonNode SOME_CHECKPOINT_INTERVAL =
      mapper
          .createObjectNode()
          .put(
              Constants.COLLECTION_CHECKPOINT_INTERVAL,
              Constants.DEFAULT_COLLECTION_CHECKPOINT_INTERVAL);
  private static final int SOME_NON_DEFAULT_CHECKPOINT_INTERVAL = 2;

  @Spy private Get get = new Get();
  @Mock private Ledger<JsonNode> ledger;

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this).close();
  }

  @Test
  public void
      invoke_CorrectArgumentsGivenAndCheckpointAgeAssetWithAddOperationFound_ShouldGetCollection() {
    // Arrange
    JsonNode argument = mapper.createObjectNode().put(Constants.COLLECTION_ID, SOME_COLLECTION_ID);
    Asset<JsonNode> asset = (Asset<JsonNode>) mock(Asset.class);
    ObjectNode data = mapper.createObjectNode();
    data.put(Constants.COLLECTION_ID, SOME_COLLECTION_ID);
    data.put(Constants.OPERATION_TYPE, Constants.OPERATION_ADD);
    data.set(Constants.OBJECT_IDS, mapper.createArrayNode().add("baz"));
    data.set(Constants.COLLECTION_SNAPSHOT, mapper.createArrayNode().add("foo").add("bar"));
    data.put(
        Constants.COLLECTION_CHECKPOINT_INTERVAL, Constants.DEFAULT_COLLECTION_CHECKPOINT_INTERVAL);
    when(ledger.get(SOME_COLLECTION_ID_WITH_PREFIX)).thenReturn(Optional.of(asset));
    when(asset.age()).thenReturn(SOME_CHECKPOINT_AGE);
    when(asset.data()).thenReturn(data);
    doReturn(SOME_CHECKPOINT_INTERVAL)
        .when(get)
        .invokeSubContract(Constants.CONTRACT_GET_CHECKPOINT_INTERVAL, ledger, argument);
    JsonNode expected =
        mapper
            .createObjectNode()
            .set(Constants.OBJECT_IDS, mapper.createArrayNode().add("foo").add("bar").add("baz"));

    // Act
    JsonNode actual = get.invoke(ledger, argument, null);

    // Assert
    assertCollectionEquals(expected, actual);
    verify(ledger).get(SOME_COLLECTION_ID_WITH_PREFIX);
  }

  @Test
  public void
      invoke_CorrectArgumentsGivenAndCheckpointAgeAssetWithRemoveOperationFound_ShouldGetCollection() {
    // Arrange
    JsonNode argument = mapper.createObjectNode().put(Constants.COLLECTION_ID, SOME_COLLECTION_ID);
    Asset<JsonNode> asset = (Asset<JsonNode>) mock(Asset.class);
    ObjectNode data = mapper.createObjectNode();
    data.put(Constants.COLLECTION_ID, SOME_COLLECTION_ID);
    data.put(Constants.OPERATION_TYPE, Constants.OPERATION_REMOVE);
    data.set(Constants.OBJECT_IDS, mapper.createArrayNode().add("bar"));
    data.set(
        Constants.COLLECTION_SNAPSHOT, mapper.createArrayNode().add("foo").add("bar").add("baz"));
    data.put(
        Constants.COLLECTION_CHECKPOINT_INTERVAL, Constants.DEFAULT_COLLECTION_CHECKPOINT_INTERVAL);
    when(ledger.get(SOME_COLLECTION_ID_WITH_PREFIX)).thenReturn(Optional.of(asset));
    when(asset.age()).thenReturn(SOME_CHECKPOINT_AGE);
    when(asset.data()).thenReturn(data);
    doReturn(SOME_CHECKPOINT_INTERVAL)
        .when(get)
        .invokeSubContract(Constants.CONTRACT_GET_CHECKPOINT_INTERVAL, ledger, argument);
    JsonNode expected =
        mapper
            .createObjectNode()
            .set(Constants.OBJECT_IDS, mapper.createArrayNode().add("foo").add("baz"));

    // Act
    JsonNode actual = get.invoke(ledger, argument, null);

    // Assert
    assertCollectionEquals(expected, actual);
    verify(ledger).get(SOME_COLLECTION_ID_WITH_PREFIX);
  }

  @Test
  public void
      invoke_CorrectArgumentsGivenAndNonCheckpointAgeAssetFound_ShouldGetMergedCollection() {
    // Arrange
    JsonNode argument = mapper.createObjectNode().put(Constants.COLLECTION_ID, SOME_COLLECTION_ID);
    Asset<JsonNode> asset = (Asset<JsonNode>) mock(Asset.class);
    when(ledger.get(SOME_COLLECTION_ID_WITH_PREFIX)).thenReturn(Optional.of(asset));
    when(asset.age()).thenReturn(SOME_NON_CHECKPOINT_AGE);
    Asset<JsonNode> asset0 = (Asset<JsonNode>) mock(Asset.class);
    Asset<JsonNode> asset1 = (Asset<JsonNode>) mock(Asset.class);
    Asset<JsonNode> asset2 = (Asset<JsonNode>) mock(Asset.class);
    ObjectNode data0 = mapper.createObjectNode();
    data0.put(Constants.COLLECTION_ID, SOME_COLLECTION_ID);
    data0.put(Constants.OPERATION_TYPE, Constants.OPERATION_CREATE);
    data0.set(Constants.OBJECT_IDS, mapper.createArrayNode().add("foo").add("bar"));
    data0.set(Constants.COLLECTION_SNAPSHOT, mapper.createArrayNode());
    data0.put(
        Constants.COLLECTION_CHECKPOINT_INTERVAL, Constants.DEFAULT_COLLECTION_CHECKPOINT_INTERVAL);
    JsonNode data1 =
        mapper
            .createObjectNode()
            .put(Constants.COLLECTION_ID, SOME_COLLECTION_ID)
            .put(Constants.OPERATION_TYPE, Constants.OPERATION_ADD)
            .set(Constants.OBJECT_IDS, mapper.createArrayNode().add("baz"));
    JsonNode data2 =
        mapper
            .createObjectNode()
            .put(Constants.COLLECTION_ID, SOME_COLLECTION_ID)
            .put(Constants.OPERATION_TYPE, Constants.OPERATION_REMOVE)
            .set(Constants.OBJECT_IDS, mapper.createArrayNode().add("foo"));
    AssetFilter filter =
        new AssetFilter(SOME_COLLECTION_ID_WITH_PREFIX)
            .withStartAge(SOME_CHECKPOINT_AGE, true)
            .withEndAge(SOME_NON_CHECKPOINT_AGE, true)
            .withAgeOrder(AgeOrder.ASC);
    when(asset0.data()).thenReturn(data0);
    when(asset1.data()).thenReturn(data1);
    when(asset2.data()).thenReturn(data2);
    when(ledger.scan(filter)).thenReturn(ImmutableList.of(asset0, asset1, asset2));
    JsonNode expected =
        mapper
            .createObjectNode()
            .set(Constants.OBJECT_IDS, mapper.createArrayNode().add("bar").add("baz"));
    doReturn(SOME_CHECKPOINT_INTERVAL)
        .when(get)
        .invokeSubContract(Constants.CONTRACT_GET_CHECKPOINT_INTERVAL, ledger, argument);

    // Act
    JsonNode actual = get.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isEqualTo(expected);
    verify(ledger).get(SOME_COLLECTION_ID_WITH_PREFIX);
    verify(ledger).scan(filter);
  }

  @Test
  public void invoke_CorrectArgumentsGivenAndCollectionNotFound_ShouldReturnNull() {
    // Arrange
    JsonNode argument = mapper.createObjectNode().put(Constants.COLLECTION_ID, SOME_COLLECTION_ID);
    when(ledger.get(SOME_COLLECTION_ID_WITH_PREFIX)).thenReturn(Optional.empty());

    // Act
    JsonNode actual = get.invoke(ledger, argument, null);

    // Assert
    assertThat(actual).isNull();
    verify(ledger).get(SOME_COLLECTION_ID_WITH_PREFIX);
  }

  @Test
  public void invoke_ArgumentsWithInvalidCollectionIdGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument = mapper.createObjectNode().put(Constants.COLLECTION_ID, 1);

    // Act Assert
    assertThatThrownBy(() -> get.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.COLLECTION_ID_IS_MISSING_OR_INVALID);
    verify(ledger, never()).get(SOME_COLLECTION_ID_WITH_PREFIX);
    verify(ledger, never()).put(eq(SOME_COLLECTION_ID_WITH_PREFIX), any(JsonNode.class));
  }

  @Test
  public void
      invoke_NonCheckpointAgeAssetWithInvalidOperationTypeFound_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument = mapper.createObjectNode().put(Constants.COLLECTION_ID, SOME_COLLECTION_ID);
    Asset<JsonNode> asset = (Asset<JsonNode>) mock(Asset.class);
    when(ledger.get(SOME_COLLECTION_ID_WITH_PREFIX)).thenReturn(Optional.of(asset));
    when(asset.age()).thenReturn(SOME_NON_CHECKPOINT_AGE);
    Asset<JsonNode> asset0 = (Asset<JsonNode>) mock(Asset.class);
    Asset<JsonNode> asset1 = (Asset<JsonNode>) mock(Asset.class);
    ObjectNode data0 = mapper.createObjectNode();
    data0.put(Constants.COLLECTION_ID, SOME_COLLECTION_ID);
    data0.put(Constants.OPERATION_TYPE, Constants.OPERATION_CREATE);
    data0.set(Constants.OBJECT_IDS, mapper.createArrayNode().add("foo").add("bar"));
    data0.set(Constants.COLLECTION_SNAPSHOT, mapper.createArrayNode());
    data0.put(
        Constants.COLLECTION_CHECKPOINT_INTERVAL, Constants.DEFAULT_COLLECTION_CHECKPOINT_INTERVAL);
    JsonNode data1 =
        mapper
            .createObjectNode()
            .put(Constants.COLLECTION_ID, SOME_COLLECTION_ID)
            .put(Constants.OPERATION_TYPE, "baz")
            .set(Constants.OBJECT_IDS, mapper.createArrayNode().add("baz"));
    AssetFilter filter =
        new AssetFilter(SOME_COLLECTION_ID_WITH_PREFIX)
            .withStartAge(SOME_CHECKPOINT_AGE, true)
            .withEndAge(SOME_NON_CHECKPOINT_AGE, true)
            .withAgeOrder(AgeOrder.ASC);
    when(asset0.data()).thenReturn(data0);
    when(asset1.data()).thenReturn(data1);
    when(ledger.scan(filter)).thenReturn(ImmutableList.of(asset0, asset1));
    doReturn(SOME_CHECKPOINT_INTERVAL)
        .when(get)
        .invokeSubContract(Constants.CONTRACT_GET_CHECKPOINT_INTERVAL, ledger, argument);

    // Act Assert
    assertThatThrownBy(() -> get.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.ILLEGAL_ASSET_STATE);
    verify(ledger).get(SOME_COLLECTION_ID_WITH_PREFIX);
    verify(ledger).scan(filter);
  }

  @Test
  public void invoke_DifferentCheckpointIntervalGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument = mapper.createObjectNode().put(Constants.COLLECTION_ID, SOME_COLLECTION_ID);
    Asset<JsonNode> asset = (Asset<JsonNode>) mock(Asset.class);
    when(ledger.get(SOME_COLLECTION_ID_WITH_PREFIX)).thenReturn(Optional.of(asset));
    when(asset.age()).thenReturn(SOME_NON_CHECKPOINT_AGE);
    Asset<JsonNode> asset0 = (Asset<JsonNode>) mock(Asset.class);
    Asset<JsonNode> asset1 = (Asset<JsonNode>) mock(Asset.class);
    ObjectNode data0 = mapper.createObjectNode();
    data0.put(Constants.COLLECTION_ID, SOME_COLLECTION_ID);
    data0.put(Constants.OPERATION_TYPE, Constants.OPERATION_CREATE);
    data0.set(Constants.OBJECT_IDS, mapper.createArrayNode().add("foo").add("bar"));
    data0.set(Constants.COLLECTION_SNAPSHOT, mapper.createArrayNode());
    data0.put(Constants.COLLECTION_CHECKPOINT_INTERVAL, SOME_NON_DEFAULT_CHECKPOINT_INTERVAL);
    JsonNode data1 =
        mapper
            .createObjectNode()
            .put(Constants.COLLECTION_ID, SOME_COLLECTION_ID)
            .put(Constants.OPERATION_TYPE, "baz")
            .set(Constants.OBJECT_IDS, mapper.createArrayNode().add("baz"));
    AssetFilter filter =
        new AssetFilter(SOME_COLLECTION_ID_WITH_PREFIX)
            .withStartAge(SOME_CHECKPOINT_AGE, true)
            .withEndAge(SOME_NON_CHECKPOINT_AGE, true)
            .withAgeOrder(AgeOrder.ASC);
    when(asset0.data()).thenReturn(data0);
    when(asset1.data()).thenReturn(data1);
    when(ledger.scan(filter)).thenReturn(ImmutableList.of(asset0, asset1));
    doReturn(SOME_CHECKPOINT_INTERVAL)
        .when(get)
        .invokeSubContract(Constants.CONTRACT_GET_CHECKPOINT_INTERVAL, ledger, argument);

    // Act Assert
    assertThatThrownBy(() -> get.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_CHECKPOINT);
    verify(ledger).get(SOME_COLLECTION_ID_WITH_PREFIX);
    verify(ledger).scan(filter);
  }

  @Test
  public void invoke_AssetWithoutCheckpointGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument = mapper.createObjectNode().put(Constants.COLLECTION_ID, SOME_COLLECTION_ID);
    Asset<JsonNode> asset = (Asset<JsonNode>) mock(Asset.class);
    ObjectNode data = mapper.createObjectNode();
    data.put(Constants.COLLECTION_ID, SOME_COLLECTION_ID);
    data.put(Constants.OPERATION_TYPE, Constants.OPERATION_ADD);
    data.set(Constants.OBJECT_IDS, mapper.createArrayNode().add("baz"));
    when(ledger.get(SOME_COLLECTION_ID_WITH_PREFIX)).thenReturn(Optional.of(asset));
    when(asset.age()).thenReturn(SOME_CHECKPOINT_AGE);
    when(asset.data()).thenReturn(data);
    doReturn(SOME_CHECKPOINT_INTERVAL)
        .when(get)
        .invokeSubContract(Constants.CONTRACT_GET_CHECKPOINT_INTERVAL, ledger, argument);

    // Act Assert
    assertThatThrownBy(() -> get.invoke(ledger, argument, null))
        .isExactlyInstanceOf(ContractContextException.class)
        .hasMessage(Constants.INVALID_CHECKPOINT);
    verify(ledger).get(SOME_COLLECTION_ID_WITH_PREFIX);
  }

  private void assertCollectionEquals(JsonNode expected, JsonNode actual) {
    Set<String> expectedSet = new HashSet<>();
    Set<String> actualSet = new HashSet<>();
    expected.get(Constants.OBJECT_IDS).forEach(id -> expectedSet.add(id.asText()));
    actual.get(Constants.OBJECT_IDS).forEach(id -> actualSet.add(id.asText()));
    assertThat(expectedSet).isEqualTo(actualSet);
  }
}
