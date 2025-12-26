package com.scalar.dl.client.validation.contract.v1_0_0;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.AssetFilter.AgeOrder;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ValidateLedgerTest {
  private static final String SOME_NAMESPACE = "some_namespace";
  private static final String SOME_ASSET_ID = "some_asset_id";
  private static final int SOME_AGE = 2;
  private static final int SOME_START_AGE = 1;
  private static final int SOME_END_AGE = 3;
  @Mock private Ledger<JsonNode> ledger;
  private ValidateLedger validateLedger;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    validateLedger = new ValidateLedger();
  }

  private List<Asset<JsonNode>> createMockAssets(String assetId, int startAge, int endAge) {

    return IntStream.range(startAge, endAge + 1)
        .mapToObj(
            i -> {
              @SuppressWarnings("unchecked")
              Asset<JsonNode> asset = mock(Asset.class);
              when(asset.id()).thenReturn(assetId);
              when(asset.age()).thenReturn(i);
              when(asset.data()).thenReturn(JsonNodeFactory.instance.objectNode());
              return asset;
            })
        .collect(Collectors.toList());
  }

  @Test
  public void invoke_AgeGiven_ShouldReturnOneSpecifiedAssetOnly() {
    JsonNode argument =
        JsonNodeFactory.instance
            .objectNode()
            .put(ValidateLedger.ASSET_ID_KEY, SOME_ASSET_ID)
            .put(ValidateLedger.AGE_KEY, SOME_AGE);
    JsonNode properties = JsonNodeFactory.instance.objectNode();

    List<Asset<JsonNode>> assets = createMockAssets(SOME_ASSET_ID, SOME_AGE, SOME_AGE);
    when(ledger.scan(any(AssetFilter.class))).thenReturn(assets);

    // Act
    JsonNode result = validateLedger.invoke(ledger, argument, properties);

    // Assert
    AssetFilter expected =
        new AssetFilter(SOME_ASSET_ID)
            .withStartAge(SOME_AGE, true)
            .withEndAge(SOME_AGE, true)
            .withAgeOrder(AgeOrder.ASC);
    verify(ledger).scan(expected);
    assertThat(result.get(SOME_ASSET_ID).size()).isEqualTo(1);
    assertThat(result.get(SOME_ASSET_ID).get(0).get(ValidateLedger.AGE_KEY).asInt())
        .isEqualTo(assets.get(0).age());
  }

  @Test
  public void invoke_StartAgeEndAgeGiven_ShouldReturnSpecifiedAssets() {
    // Arrange
    JsonNode argument =
        JsonNodeFactory.instance
            .objectNode()
            .put(ValidateLedger.ASSET_ID_KEY, SOME_ASSET_ID)
            .put(ValidateLedger.START_AGE_KEY, SOME_START_AGE)
            .put(ValidateLedger.END_AGE_KEY, SOME_END_AGE);
    JsonNode properties = JsonNodeFactory.instance.objectNode();

    List<Asset<JsonNode>> assets = createMockAssets(SOME_ASSET_ID, SOME_START_AGE, SOME_END_AGE);
    when(ledger.scan(any(AssetFilter.class))).thenReturn(assets);

    // Act
    JsonNode result = validateLedger.invoke(ledger, argument, properties);

    // Assert
    AssetFilter expected =
        new AssetFilter(SOME_ASSET_ID)
            .withStartAge(SOME_START_AGE, true)
            .withEndAge(SOME_END_AGE, true)
            .withAgeOrder(AgeOrder.ASC);
    verify(ledger).scan(expected);
    assertThat(result.get(SOME_ASSET_ID).size()).isEqualTo(assets.size());
    for (int i = 0; i < assets.size(); i++) {
      assertThat(result.get(SOME_ASSET_ID).get(i).get(ValidateLedger.AGE_KEY).asInt())
          .isEqualTo(assets.get(i).age());
    }
  }

  @Test
  public void invoke_OnlyAssetIdGiven_ShouldReturnAllAssets() {
    // Arrange
    JsonNode argument =
        JsonNodeFactory.instance.objectNode().put(ValidateLedger.ASSET_ID_KEY, SOME_ASSET_ID);
    JsonNode properties = JsonNodeFactory.instance.objectNode();

    // create some asset but it's not used for verification since the range is open
    List<Asset<JsonNode>> assets = createMockAssets(SOME_ASSET_ID, 0, 0);
    when(ledger.scan(any(AssetFilter.class))).thenReturn(assets);

    // Act
    validateLedger.invoke(ledger, argument, properties);

    // Assert
    AssetFilter expected =
        new AssetFilter(SOME_ASSET_ID)
            .withStartAge(0, true)
            .withEndAge(Integer.MAX_VALUE, true)
            .withAgeOrder(AgeOrder.ASC);
    verify(ledger).scan(expected);
  }

  @Test
  public void invoke_AssetIdNotGiven_ShouldThrowContractContextException() {
    // Arrange
    JsonNode argument = JsonNodeFactory.instance.objectNode().put(ValidateLedger.AGE_KEY, SOME_AGE);
    JsonNode properties = JsonNodeFactory.instance.objectNode();

    // Act
    Throwable thrown = catchThrowable(() -> validateLedger.invoke(ledger, argument, properties));

    // Assert
    assertThat(thrown).isExactlyInstanceOf(ContractContextException.class);
  }

  @Test
  public void invoke_NamespaceAndAgeGiven_ShouldReturnOneSpecifiedAssetWithNamespaceAwareFilter() {
    // Arrange
    JsonNode argument =
        JsonNodeFactory.instance
            .objectNode()
            .put(ValidateLedger.NAMESPACE_KEY, SOME_NAMESPACE)
            .put(ValidateLedger.ASSET_ID_KEY, SOME_ASSET_ID)
            .put(ValidateLedger.AGE_KEY, SOME_AGE);
    JsonNode properties = JsonNodeFactory.instance.objectNode();

    List<Asset<JsonNode>> assets = createMockAssets(SOME_ASSET_ID, SOME_AGE, SOME_AGE);
    when(ledger.scan(any(AssetFilter.class))).thenReturn(assets);

    // Act
    JsonNode result = validateLedger.invoke(ledger, argument, properties);

    // Assert
    AssetFilter expected =
        new AssetFilter(SOME_NAMESPACE, SOME_ASSET_ID)
            .withStartAge(SOME_AGE, true)
            .withEndAge(SOME_AGE, true)
            .withAgeOrder(AgeOrder.ASC);
    verify(ledger).scan(expected);
    assertThat(result.get(SOME_ASSET_ID).size()).isEqualTo(1);
    assertThat(result.get(SOME_ASSET_ID).get(0).get(ValidateLedger.AGE_KEY).asInt())
        .isEqualTo(assets.get(0).age());
  }

  @Test
  public void
      invoke_NamespaceAndStartAgeEndAgeGiven_ShouldReturnSpecifiedAssetsWithNamespaceAwareFilter() {
    // Arrange
    JsonNode argument =
        JsonNodeFactory.instance
            .objectNode()
            .put(ValidateLedger.NAMESPACE_KEY, SOME_NAMESPACE)
            .put(ValidateLedger.ASSET_ID_KEY, SOME_ASSET_ID)
            .put(ValidateLedger.START_AGE_KEY, SOME_START_AGE)
            .put(ValidateLedger.END_AGE_KEY, SOME_END_AGE);
    JsonNode properties = JsonNodeFactory.instance.objectNode();

    List<Asset<JsonNode>> assets = createMockAssets(SOME_ASSET_ID, SOME_START_AGE, SOME_END_AGE);
    when(ledger.scan(any(AssetFilter.class))).thenReturn(assets);

    // Act
    JsonNode result = validateLedger.invoke(ledger, argument, properties);

    // Assert
    AssetFilter expected =
        new AssetFilter(SOME_NAMESPACE, SOME_ASSET_ID)
            .withStartAge(SOME_START_AGE, true)
            .withEndAge(SOME_END_AGE, true)
            .withAgeOrder(AgeOrder.ASC);
    verify(ledger).scan(expected);
    assertThat(result.get(SOME_ASSET_ID).size()).isEqualTo(assets.size());
    for (int i = 0; i < assets.size(); i++) {
      assertThat(result.get(SOME_ASSET_ID).get(i).get(ValidateLedger.AGE_KEY).asInt())
          .isEqualTo(assets.get(i).age());
    }
  }

  @Test
  public void invoke_NamespaceAndOnlyAssetIdGiven_ShouldReturnAllAssetsWithNamespaceAwareFilter() {
    // Arrange
    JsonNode argument =
        JsonNodeFactory.instance
            .objectNode()
            .put(ValidateLedger.NAMESPACE_KEY, SOME_NAMESPACE)
            .put(ValidateLedger.ASSET_ID_KEY, SOME_ASSET_ID);
    JsonNode properties = JsonNodeFactory.instance.objectNode();

    // create some asset but it's not used for verification since the range is open
    List<Asset<JsonNode>> assets = createMockAssets(SOME_ASSET_ID, 0, 0);
    when(ledger.scan(any(AssetFilter.class))).thenReturn(assets);

    // Act
    validateLedger.invoke(ledger, argument, properties);

    // Assert
    AssetFilter expected =
        new AssetFilter(SOME_NAMESPACE, SOME_ASSET_ID)
            .withStartAge(0, true)
            .withEndAge(Integer.MAX_VALUE, true)
            .withAgeOrder(AgeOrder.ASC);
    verify(ledger).scan(expected);
  }
}
