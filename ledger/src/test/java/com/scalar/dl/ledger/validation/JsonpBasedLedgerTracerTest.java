package com.scalar.dl.ledger.validation;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.AssetScanner;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import com.scalar.dl.ledger.util.JsonpSerDe;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class JsonpBasedLedgerTracerTest {
  private static final JsonpSerDe serde = new JsonpSerDe();
  private static final String SOME_ASSET_ID = "some_asset_id";
  private static final String SOME_ASSET_DATA = "{\"output\": 0}";
  private static final String jsonString =
      "{\"X\":{\"age\":0,\"data\":{\"balance\":100}},\"Y\":{\"age\":0,\"data\":{\"balance\":200}}}";
  private JsonpBasedLedgerTracer ledger;
  @Mock private AssetScanner scanner;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    ledger = new JsonpBasedLedgerTracer(scanner);
  }

  @Test
  public void setInput_JsonGiven_ShouldPutAssetsInInputs() {
    // Arrange
    InternalAsset assetX = mock(InternalAsset.class);
    when(assetX.id()).thenReturn("X");
    when(assetX.age()).thenReturn(0);
    when(assetX.data()).thenReturn("{\"balance\":100}");
    InternalAsset assetY = mock(InternalAsset.class);
    when(assetY.id()).thenReturn("Y");
    when(assetY.age()).thenReturn(0);
    when(assetY.data()).thenReturn("{\"balance\":200}");
    when(scanner.doGet("X", 0)).thenReturn(assetX);
    when(scanner.doGet("Y", 0)).thenReturn(assetY);

    // Act
    ledger.setInput(jsonString);

    // Assert
    Map<String, Asset<JsonObject>> inputs = ledger.getInputs();
    assertThat(inputs.get("X").id()).isEqualTo("X");
    assertThat(inputs.get("X").age()).isEqualTo(0);
    assertThat(inputs.get("X").data())
        .isEqualTo(Json.createObjectBuilder().add("balance", 100).build());
    assertThat(inputs.get("Y").id()).isEqualTo("Y");
    assertThat(inputs.get("Y").age()).isEqualTo(0);
    assertThat(inputs.get("Y").data())
        .isEqualTo(Json.createObjectBuilder().add("balance", 200).build());
  }

  @Test
  public void getPutGet_InitialAssetNull_LastGetShouldBePutOne() {
    // Arrange
    ledger.setInput(SOME_ASSET_ID, null);

    // Act
    Optional<Asset<JsonObject>> gotten = ledger.get(SOME_ASSET_ID);
    JsonObject data = Json.createObjectBuilder().build();
    ledger.put(SOME_ASSET_ID, data);
    Optional<Asset<JsonObject>> updated = ledger.get(SOME_ASSET_ID);

    // Assert
    assertThat(gotten).isEmpty();
    assertThat(updated).isPresent();
    assertThat(updated.get().age()).isEqualTo(0);
    assertThat(updated.get().data()).isEqualTo(data);
    assertThat(ledger.getOutput(SOME_ASSET_ID)).isEqualTo(serde.serialize(data));
  }

  @Test
  public void getPutGet_InitialAssetNotNull_LastGetShouldBePutOne() {
    // Arrange
    JsonObject existing = Json.createObjectBuilder().add("data", "foo").build();
    InternalAsset asset = mock(InternalAsset.class);
    when(asset.id()).thenReturn(SOME_ASSET_ID);
    when(asset.age()).thenReturn(1);
    when(asset.data()).thenReturn(serde.serialize(existing));
    ledger.setInput(SOME_ASSET_ID, asset);

    // Act
    Optional<Asset<JsonObject>> gotten = ledger.get(SOME_ASSET_ID);
    JsonObject data = Json.createObjectBuilder().add("data", "bar").build();
    ledger.put(SOME_ASSET_ID, data);
    Optional<Asset<JsonObject>> updated = ledger.get(SOME_ASSET_ID);

    // Assert
    assertThat(gotten).isPresent();
    assertThat(updated).isPresent();
    assertThat(gotten.get().data()).isEqualTo(existing);
    assertThat(updated.get().data()).isEqualTo(data);
    assertThat(gotten.get().age()).isEqualTo(1);
    assertThat(updated.get().age()).isEqualTo(2);
    assertThat(ledger.getOutput(SOME_ASSET_ID)).isEqualTo(serde.serialize(data));
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
    ledger = new JsonpBasedLedgerTracer(scanner);

    // Act
    List<Asset<JsonObject>> assets = ledger.scan(filter);

    // Assert
    assertThat(assets.get(0).id()).isEqualTo(SOME_ASSET_ID);
    assertThat(assets.get(0).age()).isEqualTo(0);
    assertThat(assets.get(0).data()).isEqualTo(serde.deserialize(SOME_ASSET_DATA));
    assertThat(assets.get(1).id()).isEqualTo(SOME_ASSET_ID);
    assertThat(assets.get(1).age()).isEqualTo(1);
    assertThat(assets.get(1).data()).isEqualTo(serde.deserialize(SOME_ASSET_DATA));
  }
}
