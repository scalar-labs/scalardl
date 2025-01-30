package com.scalar.dl.ledger.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.AssetScanner;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import com.scalar.dl.ledger.util.JsonpSerDe;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.json.JsonObject;
import javax.json.JsonValue;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DeprecatedLedgerTracerTest {
  private static final JsonpSerDe serde = new JsonpSerDe();
  private static final String SOME_ASSET_ID = "some_asset_id";
  private static final int SOME_AGE = 10;
  private static final String SOME_JSON_STRING =
      "{\"X\":{\"age\":0,\"data\":{\"balance\":100}},\"Y\":{\"age\":0,\"data\":{\"balance\":200}}}";
  private DeprecatedLedgerTracer ledger;
  @Mock private LedgerTracer tracer;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    ledger = new DeprecatedLedgerTracer(tracer);
  }

  @Test
  public void setInput_JsonGiven_ShouldDelegateToTracerProperly() {
    // Arrange

    // Act
    ledger.setInput(SOME_JSON_STRING);

    // Assert
    verify(tracer).setInput(serde.deserialize(SOME_JSON_STRING));
  }

  @Test
  public void setInput_AssetIdAndInternalAssetGiven_ShouldDelegateToTracerProperly() {
    // Arrange
    InternalAsset asset = mock(InternalAsset.class);

    // Act
    ledger.setInput(SOME_ASSET_ID, asset);

    // Assert
    verify(tracer).setInput(SOME_ASSET_ID, asset);
  }

  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
  @Test
  public void getOutput_AssetIdGiven_ShouldDelegateToTracerProperly() {
    // Arrange
    when(tracer.getOutput(SOME_ASSET_ID)).thenReturn(serde.deserialize(SOME_JSON_STRING));

    // Act
    String actual = ledger.getOutput(SOME_ASSET_ID);

    // Assert
    verify(tracer).getOutput(SOME_ASSET_ID);
    assertThat(actual).isEqualTo(SOME_JSON_STRING);
  }

  @Test
  public void get_AssetIdGiven_ShouldDelegateToTracerProperly() {
    // Arrange
    com.scalar.dl.ledger.asset.Asset asset = mock(com.scalar.dl.ledger.asset.Asset.class);
    when(asset.id()).thenReturn(SOME_ASSET_ID);
    when(asset.age()).thenReturn(SOME_AGE);
    when(asset.data()).thenReturn(serde.deserialize(SOME_JSON_STRING));
    when(tracer.get(SOME_ASSET_ID)).thenReturn(Optional.of(asset));

    // Act
    Optional<Asset<JsonObject>> actual = ledger.get(SOME_ASSET_ID);

    // Assert
    assertThat(actual.isPresent()).isTrue();
    assertThat(actual.get().id()).isEqualTo(SOME_ASSET_ID);
    assertThat(actual.get().age()).isEqualTo(SOME_AGE);
    assertThat(actual.get().data()).isEqualTo(serde.deserialize(SOME_JSON_STRING));
    verify(tracer).get(SOME_ASSET_ID);
  }

  @Test
  public void scan_AssetFilterGiven_ShouldDelegateToTracerProperly() {
    // Arrange
    InternalAsset asset = mock(InternalAsset.class);
    when(asset.id()).thenReturn(SOME_ASSET_ID);
    when(asset.age()).thenReturn(SOME_AGE);
    when(asset.data()).thenReturn(SOME_JSON_STRING);
    AssetScanner scanner = mock(AssetScanner.class);
    when(scanner.doScan(any(AssetFilter.class))).thenReturn(Collections.singletonList(asset));
    tracer = spy(new LedgerTracer(scanner));
    ledger = new DeprecatedLedgerTracer(tracer);

    // Act
    AssetFilter filter = new AssetFilter(SOME_ASSET_ID);
    List<Asset<JsonObject>> actual = ledger.scan(filter);

    // Assert
    assertThat(actual.size()).isEqualTo(1);
    assertThat(actual.get(0).id()).isEqualTo(SOME_ASSET_ID);
    assertThat(actual.get(0).age()).isEqualTo(SOME_AGE);
    assertThat(actual.get(0).data()).isEqualTo(serde.deserialize(SOME_JSON_STRING));
    verify(tracer).scan(filter);
  }

  @Test
  public void put_AssetIdAndDataGiven_ShouldDelegateToTracerProperly() {
    // Arrange
    JsonObject data = JsonValue.EMPTY_JSON_OBJECT;

    // Act
    ledger.put(SOME_ASSET_ID, data);

    // Assert
    verify(tracer).put(SOME_ASSET_ID, data);
  }
}
