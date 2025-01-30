package com.scalar.dl.ledger.statemachine;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

public class AssetInputTest {
  private static final String SOME_ASSET_ID1 = "asset_id1";
  private static final String SOME_ASSET_ID2 = "asset_id2";
  private static final int SOME_ASSET_AGE1 = 1;
  private static final int SOME_ASSET_AGE2 = 2;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
  private InternalAsset createAsset(String assetId, int age) {
    InternalAsset asset = mock(InternalAsset.class);
    doReturn(assetId).when(asset).id();
    doReturn(age).when(asset).age();
    return asset;
  }

  // old version for verification
  private String toInputFrom(Map<String, InternalAsset> readSet) {
    JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
    readSet.forEach(
        (id, asset) -> {
          JsonObjectBuilder element = Json.createObjectBuilder().add("age", asset.age());
          jsonBuilder.add(id, element.build());
        });
    return jsonBuilder.build().toString();
  }

  @Test
  public void iterator_JsonFormattedStringGiven_ShouldIterateProperly() {
    // Arrange
    Map<String, InternalAsset> readSet =
        ImmutableMap.of(
            SOME_ASSET_ID1,
            createAsset(SOME_ASSET_ID1, SOME_ASSET_AGE1),
            SOME_ASSET_ID2,
            createAsset(SOME_ASSET_ID2, SOME_ASSET_AGE2));
    String input = new AssetInput(readSet).toString();

    // Act Assert
    AssetInput assetInput = new AssetInput(input);
    assetInput.forEach(
        asset -> {
          if (asset.id().equals(SOME_ASSET_ID1)) {
            assertThat(asset.age()).isEqualTo(SOME_ASSET_AGE1);
          } else if (asset.id().equals(SOME_ASSET_ID2)) {
            assertThat(asset.age()).isEqualTo(SOME_ASSET_AGE2);
          } else {
            throw new AssertionError();
          }
        });
  }

  @Test
  public void toString_ReadSetGiven_ShouldCreateCorrectInput() {
    // Arrange
    Map<String, InternalAsset> readSet =
        ImmutableMap.of(
            SOME_ASSET_ID1,
            createAsset(SOME_ASSET_ID1, SOME_ASSET_AGE1),
            SOME_ASSET_ID2,
            createAsset(SOME_ASSET_ID2, SOME_ASSET_AGE2));

    // Act
    String actual = new AssetInput(readSet).toString();

    // Assert
    String expected = toInputFrom(readSet);
    assertThat(actual).isEqualTo(expected);
  }
}
