package com.scalar.dl.ledger.validation;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.AssetScanner;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class StringBasedLedgerTracerTest {
  private static final String SOME_ASSET_ID = "some_asset_id";
  private static final String SOME_ASSET_DATA = "output:0";
  private static final String jsonString =
      "{\"X\":{\"age\":0,\"data\":\"balance:100\"},\"Y\":{\"age\":0,\"data\":\"balance:200\"}}";
  private StringBasedLedgerTracer ledger;
  @Mock private AssetScanner scanner;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    ledger = new StringBasedLedgerTracer(scanner);
  }

  @Test
  public void setInput_JsonGiven_ShouldPutAssetsInInputs() {
    // Arrange
    InternalAsset assetX = mock(InternalAsset.class);
    when(assetX.id()).thenReturn("X");
    when(assetX.age()).thenReturn(0);
    when(assetX.data()).thenReturn("balance:100");
    InternalAsset assetY = mock(InternalAsset.class);
    when(assetY.id()).thenReturn("Y");
    when(assetY.age()).thenReturn(0);
    when(assetY.data()).thenReturn("balance:200");
    when(scanner.doGet("X", 0)).thenReturn(assetX);
    when(scanner.doGet("Y", 0)).thenReturn(assetY);

    // Act
    ledger.setInput(jsonString);

    // Assert
    Map<String, Asset<String>> inputs = ledger.getInputs();
    assertThat(inputs.get("X").id()).isEqualTo("X");
    assertThat(inputs.get("X").age()).isEqualTo(0);
    assertThat(inputs.get("X").data()).isEqualTo("balance:100");
    assertThat(inputs.get("Y").id()).isEqualTo("Y");
    assertThat(inputs.get("Y").age()).isEqualTo(0);
    assertThat(inputs.get("Y").data()).isEqualTo("balance:200");
  }

  @Test
  public void getPutGet_InitialAssetNull_LastGetShouldBePutOne() {
    // Arrange
    ledger.setInput(SOME_ASSET_ID, null);

    // Act
    Optional<Asset<String>> gotten = ledger.get(SOME_ASSET_ID);
    String data = "";
    ledger.put(SOME_ASSET_ID, data);
    Optional<Asset<String>> updated = ledger.get(SOME_ASSET_ID);

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
    String existing = "data:foo";
    InternalAsset asset = mock(InternalAsset.class);
    when(asset.id()).thenReturn(SOME_ASSET_ID);
    when(asset.age()).thenReturn(1);
    when(asset.data()).thenReturn(existing);
    ledger.setInput(SOME_ASSET_ID, asset);

    // Act
    Optional<Asset<String>> gotten = ledger.get(SOME_ASSET_ID);
    String data = "data:bar";
    ledger.put(SOME_ASSET_ID, data);
    Optional<Asset<String>> updated = ledger.get(SOME_ASSET_ID);

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
    ledger = new StringBasedLedgerTracer(scanner);

    // Act
    List<Asset<String>> assets = ledger.scan(filter);

    // Assert
    assertThat(assets.get(0).id()).isEqualTo(SOME_ASSET_ID);
    assertThat(assets.get(0).age()).isEqualTo(0);
    assertThat(assets.get(0).data()).isEqualTo(SOME_ASSET_DATA);
    assertThat(assets.get(1).id()).isEqualTo(SOME_ASSET_ID);
    assertThat(assets.get(1).age()).isEqualTo(1);
    assertThat(assets.get(1).data()).isEqualTo(SOME_ASSET_DATA);
  }
}
