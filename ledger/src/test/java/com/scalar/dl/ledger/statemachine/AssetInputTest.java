package com.scalar.dl.ledger.statemachine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

public class AssetInputTest {
  private static final String SOME_ASSET_ID1 = "asset_id1";
  private static final String SOME_ASSET_ID2 = "asset_id2";
  private static final String SOME_NAMESPACE1 = "namespace1";
  private static final String SOME_NAMESPACE2 = "namespace2";
  private static final int SOME_ASSET_AGE1 = 1;
  private static final int SOME_ASSET_AGE2 = 2;

  @BeforeEach
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

  @Test
  public void iterator_JsonFormattedStringGiven_ShouldIterateProperly() {
    // Arrange
    Map<AssetKey, InternalAsset> readSet =
        ImmutableMap.of(
            AssetKey.of(SOME_NAMESPACE1, SOME_ASSET_ID1),
            createAsset(SOME_ASSET_ID1, SOME_ASSET_AGE1),
            AssetKey.of(SOME_NAMESPACE1, SOME_ASSET_ID2),
            createAsset(SOME_ASSET_ID2, SOME_ASSET_AGE2));
    String input = new AssetInput(readSet).toString();

    // Act Assert
    AssetInput assetInput = new AssetInput(input);
    assetInput.forEach(
        asset -> {
          if (asset.id().equals(SOME_ASSET_ID1)) {
            assertThat(asset.age()).isEqualTo(SOME_ASSET_AGE1);
            assertThat(asset.namespace()).isEqualTo(SOME_NAMESPACE1);
          } else if (asset.id().equals(SOME_ASSET_ID2)) {
            assertThat(asset.age()).isEqualTo(SOME_ASSET_AGE2);
            assertThat(asset.namespace()).isEqualTo(SOME_NAMESPACE1);
          } else {
            throw new AssertionError();
          }
        });
  }

  @Test
  public void toString_ReadSetGiven_ShouldCreateCorrectInput() {
    // Arrange
    Map<AssetKey, InternalAsset> readSet =
        ImmutableMap.of(
            AssetKey.of(SOME_NAMESPACE1, SOME_ASSET_ID1),
            createAsset(SOME_ASSET_ID1, SOME_ASSET_AGE1),
            AssetKey.of(SOME_NAMESPACE1, SOME_ASSET_ID2),
            createAsset(SOME_ASSET_ID2, SOME_ASSET_AGE2));

    // Act
    String actual = new AssetInput(readSet).toString();

    // Assert
    // V2 format should contain version field
    assertThat(actual).contains("\"_version\":2");
    assertThat(actual).contains(SOME_NAMESPACE1);

    // Verify by parsing the output
    AssetInput parsed = new AssetInput(actual);
    assertThat(parsed.size()).isEqualTo(2);
  }

  @Test
  public void constructor_V1FormatStringGiven_ShouldParseCorrectly() {
    // Arrange - V1 format without namespace
    String v1Input = "{\"asset1\":{\"age\":1},\"asset2\":{\"age\":2}}";

    // Act
    AssetInput assetInput = new AssetInput(v1Input);

    // Assert
    assertThat(assetInput.size()).isEqualTo(2);
    List<AssetInput.AssetInputEntry> entries = new ArrayList<>();
    assetInput.forEach(entries::add);

    assertThat(entries.stream().anyMatch(e -> e.id().equals("asset1") && e.age() == 1)).isTrue();
    assertThat(entries.stream().anyMatch(e -> e.id().equals("asset2") && e.age() == 2)).isTrue();
    assertThat(entries.stream().allMatch(e -> e.namespace() == null)).isTrue();
  }

  @Test
  public void constructor_V2FormatStringGiven_ShouldParseCorrectly() {
    // Arrange - V2 format with namespace
    String v2Input =
        "{\"_version\":2,\"namespace1\":{\"asset1\":{\"age\":1},\"asset2\":{\"age\":2}}}";

    // Act
    AssetInput assetInput = new AssetInput(v2Input);

    // Assert
    assertThat(assetInput.size()).isEqualTo(2);
    List<AssetInput.AssetInputEntry> entries = new ArrayList<>();
    assetInput.forEach(entries::add);
    assertThat(
            entries.stream()
                .anyMatch(
                    e ->
                        e.namespace().equals("namespace1")
                            && e.id().equals("asset1")
                            && e.age() == 1))
        .isTrue();
    assertThat(
            entries.stream()
                .anyMatch(
                    e ->
                        e.namespace().equals("namespace1")
                            && e.id().equals("asset2")
                            && e.age() == 2))
        .isTrue();
  }

  @Test
  public void constructor_V2FormatWithMultipleNamespacesGiven_ShouldParseCorrectly() {
    // Arrange - V2 format with multiple namespaces
    String v2Input =
        "{\"_version\":2,\"namespace1\":{\"asset1\":{\"age\":1}},\"namespace2\":{\"asset2\":{\"age\":2}}}";

    // Act
    AssetInput assetInput = new AssetInput(v2Input);

    // Assert
    assertThat(assetInput.size()).isEqualTo(2);
    List<AssetInput.AssetInputEntry> entries = new ArrayList<>();
    assetInput.forEach(entries::add);

    assertThat(
            entries.stream()
                .anyMatch(
                    e ->
                        e.namespace().equals("namespace1")
                            && e.id().equals("asset1")
                            && e.age() == 1))
        .isTrue();
    assertThat(
            entries.stream()
                .anyMatch(
                    e ->
                        e.namespace().equals("namespace2")
                            && e.id().equals("asset2")
                            && e.age() == 2))
        .isTrue();
  }

  @Test
  public void constructor_ReadSetGiven_ShouldCreateCorrectly() {
    // Arrange
    Map<AssetKey, InternalAsset> readSet =
        ImmutableMap.of(
            AssetKey.of(SOME_NAMESPACE1, SOME_ASSET_ID1),
            createAsset(SOME_ASSET_ID1, SOME_ASSET_AGE1),
            AssetKey.of(SOME_NAMESPACE2, SOME_ASSET_ID2),
            createAsset(SOME_ASSET_ID2, SOME_ASSET_AGE2));

    // Act
    AssetInput assetInput = new AssetInput(readSet);

    // Assert
    assertThat(assetInput.size()).isEqualTo(2);
    List<AssetInput.AssetInputEntry> entries = new ArrayList<>();
    assetInput.forEach(entries::add);

    assertThat(
            entries.stream()
                .anyMatch(
                    e ->
                        e.namespace().equals(SOME_NAMESPACE1)
                            && e.id().equals(SOME_ASSET_ID1)
                            && e.age() == SOME_ASSET_AGE1))
        .isTrue();
    assertThat(
            entries.stream()
                .anyMatch(
                    e ->
                        e.namespace().equals(SOME_NAMESPACE2)
                            && e.id().equals(SOME_ASSET_ID2)
                            && e.age() == SOME_ASSET_AGE2))
        .isTrue();
  }

  @Test
  public void isEmpty_EmptyReadSetGiven_ShouldReturnTrue() {
    // Arrange
    Map<AssetKey, InternalAsset> readSet = ImmutableMap.of();

    // Act
    AssetInput assetInput = new AssetInput(readSet);

    // Assert
    assertThat(assetInput.isEmpty()).isTrue();
  }

  @Test
  public void isEmpty_NonEmptyReadSetGiven_ShouldReturnFalse() {
    // Arrange
    Map<AssetKey, InternalAsset> readSet =
        ImmutableMap.of(
            AssetKey.of(SOME_NAMESPACE1, SOME_ASSET_ID1),
            createAsset(SOME_ASSET_ID1, SOME_ASSET_AGE1));

    // Act
    AssetInput assetInput = new AssetInput(readSet);

    // Assert
    assertThat(assetInput.isEmpty()).isFalse();
  }

  @Test
  public void size_ShouldReturnCorrectSize() {
    // Arrange
    Map<AssetKey, InternalAsset> readSet =
        ImmutableMap.of(
            AssetKey.of(SOME_NAMESPACE1, SOME_ASSET_ID1),
            createAsset(SOME_ASSET_ID1, SOME_ASSET_AGE1),
            AssetKey.of(SOME_NAMESPACE2, SOME_ASSET_ID2),
            createAsset(SOME_ASSET_ID2, SOME_ASSET_AGE2));

    // Act
    AssetInput assetInput = new AssetInput(readSet);

    // Assert
    assertThat(assetInput.size()).isEqualTo(2);
  }
}
