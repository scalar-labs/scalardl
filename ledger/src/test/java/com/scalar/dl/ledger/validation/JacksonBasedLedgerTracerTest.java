package com.scalar.dl.ledger.validation;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.AssetScanner;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.AssetKey;
import com.scalar.dl.ledger.statemachine.Context;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import com.scalar.dl.ledger.util.JacksonSerDe;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class JacksonBasedLedgerTracerTest {
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final JacksonSerDe serde = new JacksonSerDe(mapper);
  private static final String SOME_DEFAULT_NAMESPACE = "scalar";
  private static final String SOME_NAMESPACE = "some_namespace";
  private static final String SOME_ASSET_ID = "some_asset_id";
  private static final String SOME_ASSET_DATA = "{\"output\": 0}";
  private static final String SOME_V1_JSON_STRING =
      "{\"X\":{\"age\":0,\"data\":{\"balance\":100}},\"Y\":{\"age\":0,\"data\":{\"balance\":200}}}";
  private static final String SOME_V2_JSON_STRING =
      "{\"_version\":2,\"namespace1\":{\"X\":{\"age\":0,\"data\":{\"balance\":100}},\"Y\":{\"age\":0,\"data\":{\"balance\":200}}}}";
  private JacksonBasedLedgerTracer ledger;
  @Mock private AssetScanner scanner;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    ledger = new JacksonBasedLedgerTracer(Context.withNamespace(SOME_DEFAULT_NAMESPACE), scanner);
  }

  @Test
  public void setInput_V1FormatJsonGiven_ShouldPutAssetsInInputsWithDefaultNamespace() {
    // Arrange - V1 format without namespace uses the default namespace from context
    InternalAsset assetX = mock(InternalAsset.class);
    when(assetX.id()).thenReturn("X");
    when(assetX.age()).thenReturn(0);
    when(assetX.data()).thenReturn("{\"balance\":100}");
    InternalAsset assetY = mock(InternalAsset.class);
    when(assetY.id()).thenReturn("Y");
    when(assetY.age()).thenReturn(0);
    when(assetY.data()).thenReturn("{\"balance\":200}");
    when(scanner.doGet(SOME_DEFAULT_NAMESPACE, "X", 0)).thenReturn(assetX);
    when(scanner.doGet(SOME_DEFAULT_NAMESPACE, "Y", 0)).thenReturn(assetY);

    // Act
    ledger.setInput(SOME_V1_JSON_STRING);

    // Assert - assets should be stored with the default namespace
    Map<AssetKey, Asset<JsonNode>> inputs = ledger.getInputs();
    assertThat(inputs.get(AssetKey.of(SOME_DEFAULT_NAMESPACE, "X")).age()).isEqualTo(0);
    assertThat(inputs.get(AssetKey.of(SOME_DEFAULT_NAMESPACE, "X")).data())
        .isEqualTo(mapper.createObjectNode().put("balance", 100));
    assertThat(inputs.get(AssetKey.of(SOME_DEFAULT_NAMESPACE, "Y")).id()).isEqualTo("Y");
    assertThat(inputs.get(AssetKey.of(SOME_DEFAULT_NAMESPACE, "Y")).age()).isEqualTo(0);
    assertThat(inputs.get(AssetKey.of(SOME_DEFAULT_NAMESPACE, "Y")).data())
        .isEqualTo(mapper.createObjectNode().put("balance", 200));
  }

  @Test
  public void setInput_V2FormatJsonGiven_ShouldPutAssetsInInputsWithNamespaceFromInput() {
    // Arrange - V2 format with namespace uses the namespace from the input
    InternalAsset assetX = mock(InternalAsset.class);
    when(assetX.id()).thenReturn("X");
    when(assetX.age()).thenReturn(0);
    when(assetX.data()).thenReturn("{\"balance\":100}");
    InternalAsset assetY = mock(InternalAsset.class);
    when(assetY.id()).thenReturn("Y");
    when(assetY.age()).thenReturn(0);
    when(assetY.data()).thenReturn("{\"balance\":200}");
    when(scanner.doGet("namespace1", "X", 0)).thenReturn(assetX);
    when(scanner.doGet("namespace1", "Y", 0)).thenReturn(assetY);

    // Act
    ledger.setInput(SOME_V2_JSON_STRING);

    // Assert - assets should be stored with the namespace from input, not the default namespace
    Map<AssetKey, Asset<JsonNode>> inputs = ledger.getInputs();
    assertThat(inputs.get(AssetKey.of("namespace1", "X")).age()).isEqualTo(0);
    assertThat(inputs.get(AssetKey.of("namespace1", "X")).data())
        .isEqualTo(mapper.createObjectNode().put("balance", 100));
    assertThat(inputs.get(AssetKey.of("namespace1", "Y")).id()).isEqualTo("Y");
    assertThat(inputs.get(AssetKey.of("namespace1", "Y")).age()).isEqualTo(0);
    assertThat(inputs.get(AssetKey.of("namespace1", "Y")).data())
        .isEqualTo(mapper.createObjectNode().put("balance", 200));
  }

  @Test
  public void getPutGet_InitialAssetNull_LastGetShouldBePutOne() {
    // Arrange
    ledger.setInput(AssetKey.of(SOME_NAMESPACE, SOME_ASSET_ID), null);

    // Act
    Optional<Asset<JsonNode>> gotten = ledger.get(SOME_NAMESPACE, SOME_ASSET_ID);
    JsonNode data = mapper.createObjectNode();
    ledger.put(SOME_NAMESPACE, SOME_ASSET_ID, data);
    Optional<Asset<JsonNode>> updated = ledger.get(SOME_NAMESPACE, SOME_ASSET_ID);

    // Assert
    assertThat(gotten).isEmpty();
    assertThat(updated).isPresent();
    assertThat(updated.get().age()).isEqualTo(0);
    assertThat(updated.get().data()).isEqualTo(data);
    assertThat(ledger.getOutput(AssetKey.of(SOME_NAMESPACE, SOME_ASSET_ID)))
        .isEqualTo(serde.serialize(data));
  }

  @Test
  public void getPutGet_InitialAssetNotNull_LastGetShouldBePutOne() {
    // Arrange
    JsonNode existing = mapper.createObjectNode().put("data", "foo");
    InternalAsset asset = mock(InternalAsset.class);
    when(asset.id()).thenReturn(SOME_ASSET_ID);
    when(asset.age()).thenReturn(1);
    when(asset.data()).thenReturn(serde.serialize(existing));
    ledger.setInput(AssetKey.of(SOME_NAMESPACE, SOME_ASSET_ID), asset);

    // Act
    Optional<Asset<JsonNode>> gotten = ledger.get(SOME_NAMESPACE, SOME_ASSET_ID);
    JsonNode data = mapper.createObjectNode().put("data", "bar");
    ledger.put(SOME_NAMESPACE, SOME_ASSET_ID, data);
    Optional<Asset<JsonNode>> updated = ledger.get(SOME_NAMESPACE, SOME_ASSET_ID);

    // Assert
    assertThat(gotten).isPresent();
    assertThat(updated).isPresent();
    assertThat(gotten.get().data()).isEqualTo(existing);
    assertThat(updated.get().data()).isEqualTo(data);
    assertThat(gotten.get().age()).isEqualTo(1);
    assertThat(updated.get().age()).isEqualTo(2);
    assertThat(ledger.getOutput(AssetKey.of(SOME_NAMESPACE, SOME_ASSET_ID)))
        .isEqualTo(serde.serialize(data));
  }

  @Test
  public void scan_AssetFilterGiven_ShouldScanWithProperSerialization() {
    // Arrange
    AssetFilter filter = new AssetFilter(SOME_ASSET_ID);
    InternalAsset asset1 = mock(InternalAsset.class);
    when(asset1.id()).thenReturn(SOME_ASSET_ID);
    when(asset1.age()).thenReturn(0);
    when(asset1.data()).thenReturn(SOME_ASSET_DATA);
    InternalAsset asset2 = mock(InternalAsset.class);
    when(asset2.id()).thenReturn(SOME_ASSET_ID);
    when(asset2.age()).thenReturn(1);
    when(asset2.data()).thenReturn(SOME_ASSET_DATA);
    when(scanner.doScan(any(AssetFilter.class))).thenReturn(Arrays.asList(asset1, asset2));
    ledger = new JacksonBasedLedgerTracer(Context.withNamespace(SOME_NAMESPACE), scanner);

    // Act
    List<Asset<JsonNode>> assets = ledger.scan(filter);

    // Assert
    assertThat(assets.get(0).id()).isEqualTo(SOME_ASSET_ID);
    assertThat(assets.get(0).age()).isEqualTo(0);
    assertThat(assets.get(0).data()).isEqualTo(serde.deserialize(SOME_ASSET_DATA));
    assertThat(assets.get(1).id()).isEqualTo(SOME_ASSET_ID);
    assertThat(assets.get(1).age()).isEqualTo(1);
    assertThat(assets.get(1).data()).isEqualTo(serde.deserialize(SOME_ASSET_DATA));
  }
}
