package com.scalar.dl.ledger.validation;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.scalar.dl.ledger.asset.Asset;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.AssetScanner;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import com.scalar.dl.ledger.util.JsonpSerDe;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class LedgerTracerTest {
  private static final JsonpSerDe serde = new JsonpSerDe();
  private static final String SOME_ASSET_ID = "some_asset_id";
  private static final int SOME_AGE = 10;
  private static final String SOME_JSON_STRING =
      "{\"X\":{\"age\":0,\"data\":{\"balance\":100}},\"Y\":{\"age\":0,\"data\":{\"balance\":200}}}";
  private Map<String, Optional<Asset>> inputs;
  private LedgerTracer ledger;
  @Mock private AssetScanner scanner;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    inputs = new HashMap<>();
    Map<String, JsonObject> outputs = new HashMap<>();
    ledger = new LedgerTracer(scanner, inputs, outputs);
  }

  @Test
  public void setInput_JsonGiven_ShouldPutAssetsInInputs() {
    // Arrange
    JsonObject json = new JsonpSerDe().deserialize(SOME_JSON_STRING);
    InternalAsset assetX = mock(InternalAsset.class);
    when(assetX.id()).thenReturn("X");
    when(assetX.age()).thenReturn(0);
    when(assetX.data()).thenReturn(serde.serialize(JsonValue.EMPTY_JSON_OBJECT));
    InternalAsset assetY = mock(InternalAsset.class);
    when(assetY.id()).thenReturn("Y");
    when(assetY.age()).thenReturn(0);
    when(assetY.data()).thenReturn(serde.serialize(JsonValue.EMPTY_JSON_OBJECT));
    when(scanner.doGet("X", 0)).thenReturn(assetX);
    when(scanner.doGet("Y", 0)).thenReturn(assetY);

    // Act
    ledger.setInput(json);

    // Assert
    assertThat(inputs.get("X").get().id()).isEqualTo("X");
    assertThat(inputs.get("X").get().age()).isEqualTo(0);
    assertThat(inputs.get("X").get().data()).isEqualTo(JsonValue.EMPTY_JSON_OBJECT);
    assertThat(inputs.get("Y").get().id()).isEqualTo("Y");
    assertThat(inputs.get("Y").get().age()).isEqualTo(0);
    assertThat(inputs.get("Y").get().data()).isEqualTo(JsonValue.EMPTY_JSON_OBJECT);
  }

  @Test
  public void getPutGet_InitialAssetNull_LastGetShouldBePutOne() {
    // Arrange
    ledger.setInput(SOME_ASSET_ID, null);

    // Act
    Optional<Asset> gotten = ledger.get(SOME_ASSET_ID);
    JsonObject data = Json.createObjectBuilder().build();
    ledger.put(SOME_ASSET_ID, data);
    Optional<Asset> updated = ledger.get(SOME_ASSET_ID);

    // Assert
    assertThat(gotten).isEmpty();
    assertThat(updated).isPresent();
    assertThat(updated.get().age()).isEqualTo(0);
    assertThat(updated.get().data()).isEqualTo(data);
    assertThat(ledger.getOutput(SOME_ASSET_ID)).isEqualTo(data);
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
    Optional<Asset> gotten = ledger.get(SOME_ASSET_ID);
    JsonObject data = Json.createObjectBuilder().add("data", "bar").build();
    ledger.put(SOME_ASSET_ID, data);
    Optional<Asset> updated = ledger.get(SOME_ASSET_ID);

    // Assert
    assertThat(gotten).isPresent();
    assertThat(updated).isPresent();
    assertThat(gotten.get().data()).isEqualTo(existing);
    assertThat(updated.get().data()).isEqualTo(data);
    assertThat(gotten.get().age()).isEqualTo(1);
    assertThat(updated.get().age()).isEqualTo(2);
    assertThat(ledger.getOutput(SOME_ASSET_ID)).isEqualTo(data);
  }

  @Test
  public void scan_AssetFilterGiven_ShouldScanAndReturnAssetList() {
    // Arrange
    InternalAsset asset = mock(InternalAsset.class);
    when(asset.id()).thenReturn(SOME_ASSET_ID);
    when(asset.age()).thenReturn(SOME_AGE);
    when(asset.data()).thenReturn(SOME_JSON_STRING);
    when(scanner.doScan(any(AssetFilter.class))).thenReturn(Collections.singletonList(asset));
    AssetFilter filter = new AssetFilter(SOME_ASSET_ID);

    // Act
    List<Asset> assets = ledger.scan(filter);

    // Assert
    assertThat(assets.size()).isEqualTo(1);
    assertThat(assets.get(0).id()).isEqualTo(SOME_ASSET_ID);
    assertThat(assets.get(0).age()).isEqualTo(SOME_AGE);
    assertThat(assets.get(0).data())
        .isEqualTo(Json.createReader(new StringReader(SOME_JSON_STRING)).readObject());
  }
}
