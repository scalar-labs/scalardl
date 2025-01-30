package com.scalar.dl.ledger.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.dl.ledger.statemachine.InternalAsset;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SnapshotTest {
  private static final String ANY_ASSET_ID = "asset_id";
  @Mock private Map<String, InternalAsset> readSet;
  @Mock private Map<String, InternalAsset> writeSet;
  private Snapshot snapshot;
  @Mock private InternalAsset assetRecord;
  @Mock private InternalAsset asset;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    snapshot = new Snapshot(readSet, writeSet);
  }

  @Test
  public void put_InternalAssetGiven_ShouldPutInReadSet() {
    // Arrange
    when(readSet.put(ANY_ASSET_ID, assetRecord)).thenReturn(null);

    // Act
    snapshot.put(ANY_ASSET_ID, assetRecord);

    // Assert
    verify(readSet).put(ANY_ASSET_ID, assetRecord);
    verify(writeSet, never()).put(anyString(), any(InternalAsset.class));
  }

  @Test
  public void put_DataGiven_ShouldPutInWriteSet() {
    // Arrange
    when(writeSet.put(anyString(), any(InternalAsset.class))).thenReturn(null);
    String data = "expected";

    // Act
    snapshot.put(ANY_ASSET_ID, data);

    // Assert
    AssetRecord asset = AssetRecord.newBuilder().id(ANY_ASSET_ID).age(0).data(data).build();
    verify(writeSet).put(ANY_ASSET_ID, asset);
    verify(readSet, never()).put(anyString(), any(InternalAsset.class));
  }

  @Test
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
  public void get_AssetIdGivenAndItsInReadSetAndItsNotInWriteSet_ShouldReturnFromReadSet() {
    // Arrange
    when(writeSet.containsKey(ANY_ASSET_ID)).thenReturn(false);
    when(readSet.containsKey(ANY_ASSET_ID)).thenReturn(true);
    when(readSet.get(ANY_ASSET_ID)).thenReturn(assetRecord);

    // Act
    Optional<InternalAsset> actual = snapshot.get(ANY_ASSET_ID);

    // Assert
    assertThat(actual).isEqualTo(Optional.of(assetRecord));
    verify(readSet).get(ANY_ASSET_ID);
    verify(writeSet, never()).get(ANY_ASSET_ID);
  }

  @Test
  public void get_AssetIdGivenAndItsNotInSnapshot_ShouldReturnEmpty() {
    // Arrange
    when(writeSet.containsKey(ANY_ASSET_ID)).thenReturn(false);
    when(readSet.containsKey(ANY_ASSET_ID)).thenReturn(false);

    // Act
    Optional<InternalAsset> actual = snapshot.get(ANY_ASSET_ID);

    // Assert
    assertThat(actual.isPresent()).isFalse();
  }

  @Test
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
  public void get_AssetIdGivenAndItsInWriteSet_ShouldReturnAssetFromWriteSet() {
    // Arrange
    when(writeSet.containsKey(ANY_ASSET_ID)).thenReturn(true);
    when(readSet.containsKey(ANY_ASSET_ID)).thenReturn(true);
    when(writeSet.get(ANY_ASSET_ID)).thenReturn(asset);

    // Act
    Optional<InternalAsset> actual = snapshot.get(ANY_ASSET_ID);

    // Assert
    assertThat(actual.get()).isEqualTo(asset);
    verify(writeSet).get(ANY_ASSET_ID);
    verify(readSet, never()).get(ANY_ASSET_ID);
  }

  @Test
  public void isEmpty_SomeEntriesGivenInBothSet_ShouldReturnFalse() {
    // Arrange
    when(readSet.isEmpty()).thenReturn(false);
    when(writeSet.isEmpty()).thenReturn(false);

    // Act
    boolean actual = snapshot.isEmpty();

    // Assert
    assertThat(actual).isFalse();
  }

  @Test
  public void isEmpty_NoEntryGivenInBothSet_ShouldReturnTrue() {
    // Arrange
    when(readSet.isEmpty()).thenReturn(true);
    when(writeSet.isEmpty()).thenReturn(true);

    // Act
    boolean actual = snapshot.isEmpty();

    // Assert
    assertThat(actual).isTrue();
  }

  @Test
  public void hasWriteSet_SomeEntriesGivenInWriteSet_ShouldReturnTrue() {
    // Arrange
    when(writeSet.isEmpty()).thenReturn(false);

    // Act
    boolean actual = snapshot.hasWriteSet();

    // Assert
    assertThat(actual).isTrue();
  }

  @Test
  public void hasWriteSet_NoEntryGivenInWriteSet_ShouldReturnFalse() {
    // Arrange
    when(writeSet.isEmpty()).thenReturn(true);

    // Act
    boolean actual = snapshot.hasWriteSet();

    // Assert
    assertThat(actual).isFalse();
  }
}
