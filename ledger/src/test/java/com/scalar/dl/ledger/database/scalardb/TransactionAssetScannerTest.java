package com.scalar.dl.ledger.database.scalardb;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.TamperEvidentAssetLedger;
import com.scalar.dl.ledger.database.Transaction;
import com.scalar.dl.ledger.database.TransactionManager;
import com.scalar.dl.ledger.exception.ConflictException;
import com.scalar.dl.ledger.exception.DatabaseException;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TransactionAssetScannerTest {
  private static final String SOME_ID = "id";
  @Mock private TransactionManager manager;
  @InjectMocks private TransactionAssetScanner assetScanner;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void doScan_AssetFilterGiven_ShouldScanProperly() {
    // Arrange
    AssetFilter filter = new AssetFilter(SOME_ID);
    InternalAsset asset = mock(InternalAsset.class);
    List<InternalAsset> assets = Collections.singletonList(asset);
    Transaction transaction = mock(Transaction.class);
    TamperEvidentAssetLedger ledger = mock(TamperEvidentAssetLedger.class);
    when(manager.startWith()).thenReturn(transaction);
    when(transaction.getLedger()).thenReturn(ledger);
    when(ledger.scan(any())).thenReturn(assets);

    // Act
    List<InternalAsset> actual = assetScanner.doScan(filter);

    // Assert
    assertThat(actual.size()).isEqualTo(1);
    assertThat(actual.get(0)).isEqualTo(asset);
    verify(transaction).commit();
    verify(transaction, never()).abort();
  }

  @Test
  public void doScan_ConflictExceptionThrown_ShouldAbortAndThrowDatabaseException() {
    // Arrange
    AssetFilter filter = new AssetFilter(SOME_ID);
    Transaction transaction = mock(Transaction.class);
    TamperEvidentAssetLedger ledger = mock(TamperEvidentAssetLedger.class);
    when(manager.startWith()).thenReturn(transaction);
    when(transaction.getLedger()).thenReturn(ledger);
    ConflictException toThrow = mock(ConflictException.class);
    when(ledger.scan(any())).thenThrow(toThrow);

    // Act
    Throwable thrown = catchThrowable(() -> assetScanner.doScan(filter));

    // Assert
    assertThat(thrown).isExactlyInstanceOf(DatabaseException.class);
    assertThat(thrown.getCause()).isEqualTo(toThrow);
    verify(transaction, never()).commit();
    verify(transaction).abort();
  }
}
