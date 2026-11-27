package com.scalar.dl.ledger.namespace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Ticker;
import com.scalar.dl.ledger.error.CommonError;
import com.scalar.dl.ledger.exception.DatabaseException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DefaultNamespaceContractExecutionCheckerTest {
  private static final long SOME_EXPIRATION_TIME_MILLIS = 1000;
  @Mock private NamespaceManager namespaceManager;
  private final AtomicLong currentTimeNanos = new AtomicLong();
  private final Ticker ticker =
      new Ticker() {
        @Override
        public long read() {
          return currentTimeNanos.get();
        }
      };

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  private DefaultNamespaceContractExecutionChecker createChecker(boolean enabled) {
    return new DefaultNamespaceContractExecutionChecker(
        enabled, namespaceManager, SOME_EXPIRATION_TIME_MILLIS, ticker);
  }

  private void advanceTime(long millis) {
    currentTimeNanos.addAndGet(TimeUnit.MILLISECONDS.toNanos(millis));
  }

  @Test
  public void isAllowed_DisabledGiven_ShouldReturnFalseWithoutCheckingImportedNamespace() {
    // Arrange
    DefaultNamespaceContractExecutionChecker checker = createChecker(false);

    // Act
    boolean actual = checker.isAllowed();

    // Assert
    assertThat(actual).isFalse();
    verify(namespaceManager, never()).existsImported();
  }

  @Test
  public void isAllowed_EnabledAndImportedNamespaceNotExists_ShouldReturnTrue() {
    // Arrange
    when(namespaceManager.existsImported()).thenReturn(false);
    DefaultNamespaceContractExecutionChecker checker = createChecker(true);

    // Act
    boolean actual = checker.isAllowed();

    // Assert
    assertThat(actual).isTrue();
  }

  @Test
  public void isAllowed_EnabledAndImportedNamespaceExists_ShouldReturnFalse() {
    // Arrange
    when(namespaceManager.existsImported()).thenReturn(true);
    DefaultNamespaceContractExecutionChecker checker = createChecker(true);

    // Act
    boolean actual = checker.isAllowed();

    // Assert
    assertThat(actual).isFalse();
  }

  @Test
  public void isAllowed_CalledAgainBeforeExpiration_ShouldUseCachedResult() {
    // Arrange
    when(namespaceManager.existsImported()).thenReturn(false, true);
    DefaultNamespaceContractExecutionChecker checker = createChecker(true);
    checker.isAllowed();
    advanceTime(SOME_EXPIRATION_TIME_MILLIS - 1);

    // Act
    boolean actual = checker.isAllowed();

    // Assert
    assertThat(actual).isTrue();
    verify(namespaceManager).existsImported();
  }

  @Test
  public void isAllowed_CalledAgainAfterExpiration_ShouldCheckImportedNamespaceAgain() {
    // Arrange
    when(namespaceManager.existsImported()).thenReturn(false, true);
    DefaultNamespaceContractExecutionChecker checker = createChecker(true);
    checker.isAllowed();
    advanceTime(SOME_EXPIRATION_TIME_MILLIS);

    // Act
    boolean actual = checker.isAllowed();

    // Assert
    assertThat(actual).isFalse();
    verify(namespaceManager, times(2)).existsImported();
  }

  @Test
  public void isAllowed_FirstCheckFailed_ShouldThrowExceptionAndCheckAgainNextTime() {
    // Arrange
    DatabaseException toThrow =
        new DatabaseException(CommonError.SCANNING_NAMESPACES_FAILED, "details");
    when(namespaceManager.existsImported()).thenThrow(toThrow).thenReturn(false);
    DefaultNamespaceContractExecutionChecker checker = createChecker(true);

    // Act Assert
    assertThatThrownBy(checker::isAllowed).isEqualTo(toThrow);
    assertThat(checker.isAllowed()).isTrue();
    verify(namespaceManager, times(2)).existsImported();
  }

  @Test
  public void isAllowed_CheckFailedAfterExpiration_ShouldUseLastResultUntilNextExpiration() {
    // Arrange
    DatabaseException toThrow =
        new DatabaseException(CommonError.SCANNING_NAMESPACES_FAILED, "details");
    when(namespaceManager.existsImported()).thenReturn(true).thenThrow(toThrow).thenReturn(false);
    DefaultNamespaceContractExecutionChecker checker = createChecker(true);
    checker.isAllowed();
    advanceTime(SOME_EXPIRATION_TIME_MILLIS);

    // Act
    boolean actualOnFailure = checker.isAllowed();
    boolean actualBeforeNextExpiration = checker.isAllowed();
    advanceTime(SOME_EXPIRATION_TIME_MILLIS);
    boolean actualAfterNextExpiration = checker.isAllowed();

    // Assert
    assertThat(actualOnFailure).isFalse();
    assertThat(actualBeforeNextExpiration).isFalse();
    assertThat(actualAfterNextExpiration).isTrue();
    verify(namespaceManager, times(3)).existsImported();
  }
}
