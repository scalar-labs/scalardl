package com.scalar.dl.ledger.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.dl.ledger.error.CommonError;
import com.scalar.dl.ledger.exception.LedgerException;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class NamespaceRestrictedAssetLedgerTest {
  private static final String CONTEXT_NAMESPACE = "context_ns";
  private static final String SAME_NAMESPACE = CONTEXT_NAMESPACE;
  private static final String DIFFERENT_NAMESPACE = "different_ns";
  private static final String ASSET_ID = "asset_id";
  private static final String DATA = "data";

  @Mock private TamperEvidentAssetLedger delegate;

  private NamespaceRestrictedAssetLedger ledger;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    ledger = new NamespaceRestrictedAssetLedger(delegate, CONTEXT_NAMESPACE);
  }

  @Test
  public void get_SameNamespaceGiven_ShouldDelegateSuccessfully() {
    // Arrange
    InternalAsset asset = mock(InternalAsset.class);
    when(delegate.get(SAME_NAMESPACE, ASSET_ID)).thenReturn(Optional.of(asset));

    // Act
    Optional<InternalAsset> result = ledger.get(SAME_NAMESPACE, ASSET_ID);

    // Assert
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(asset);
    verify(delegate).get(SAME_NAMESPACE, ASSET_ID);
  }

  @Test
  public void get_DifferentNamespaceGiven_ShouldThrowLedgerException() {
    // Act & Assert
    assertThatThrownBy(() -> ledger.get(DIFFERENT_NAMESPACE, ASSET_ID))
        .isInstanceOf(LedgerException.class)
        .hasMessage(
            CommonError.ACCESSING_NAMESPACE_NOT_ALLOWED.buildMessage(
                DIFFERENT_NAMESPACE, CONTEXT_NAMESPACE))
        .extracting("code")
        .isEqualTo(StatusCode.INVALID_REQUEST);
  }

  @Test
  public void put_SameNamespaceGiven_ShouldDelegateSuccessfully() {
    // Act
    ledger.put(SAME_NAMESPACE, ASSET_ID, DATA);

    // Assert
    verify(delegate).put(SAME_NAMESPACE, ASSET_ID, DATA);
  }

  @Test
  public void put_DifferentNamespaceGiven_ShouldThrowLedgerException() {
    // Act & Assert
    assertThatThrownBy(() -> ledger.put(DIFFERENT_NAMESPACE, ASSET_ID, DATA))
        .isInstanceOf(LedgerException.class)
        .hasMessage(
            CommonError.ACCESSING_NAMESPACE_NOT_ALLOWED.buildMessage(
                DIFFERENT_NAMESPACE, CONTEXT_NAMESPACE))
        .extracting("code")
        .isEqualTo(StatusCode.INVALID_REQUEST);
  }

  @Test
  public void scan_SameNamespaceInFilterGiven_ShouldDelegateSuccessfully() {
    // Arrange
    AssetFilter filter = new AssetFilter(SAME_NAMESPACE, ASSET_ID);
    InternalAsset asset = mock(InternalAsset.class);
    when(delegate.scan(filter)).thenReturn(Collections.singletonList(asset));

    // Act
    List<InternalAsset> result = ledger.scan(filter);

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(asset);
    verify(delegate).scan(filter);
  }

  @Test
  public void scan_DifferentNamespaceInFilterGiven_ShouldThrowLedgerException() {
    // Arrange
    AssetFilter filter = new AssetFilter(DIFFERENT_NAMESPACE, ASSET_ID);

    // Act & Assert
    assertThatThrownBy(() -> ledger.scan(filter))
        .isInstanceOf(LedgerException.class)
        .hasMessage(
            CommonError.ACCESSING_NAMESPACE_NOT_ALLOWED.buildMessage(
                DIFFERENT_NAMESPACE, CONTEXT_NAMESPACE))
        .extracting("code")
        .isEqualTo(StatusCode.INVALID_REQUEST);
  }

  @Test
  public void getWithoutNamespace_ShouldDelegateSuccessfully() {
    // Arrange
    InternalAsset asset = mock(InternalAsset.class);
    when(delegate.get(ASSET_ID)).thenReturn(Optional.of(asset));

    // Act
    Optional<InternalAsset> result = ledger.get(ASSET_ID);

    // Assert
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(asset);
    verify(delegate).get(ASSET_ID);
  }

  @Test
  public void putWithoutNamespace_ShouldDelegateSuccessfully() {
    // Act
    ledger.put(ASSET_ID, DATA);

    // Assert
    verify(delegate).put(ASSET_ID, DATA);
  }

  @Test
  public void scan_FilterWithoutNamespaceGiven_ShouldDelegateSuccessfully() {
    // Arrange
    AssetFilter filter = new AssetFilter(ASSET_ID); // no namespace
    InternalAsset asset = mock(InternalAsset.class);
    when(delegate.scan(filter)).thenReturn(Collections.singletonList(asset));

    // Act
    List<InternalAsset> result = ledger.scan(filter);

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(asset);
    verify(delegate).scan(filter);
  }
}
