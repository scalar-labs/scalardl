package com.scalar.dl.ledger.database.scalardb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.db.api.Consistency;
import com.scalar.db.api.Delete;
import com.scalar.db.api.DistributedStorage;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.db.exception.storage.ExecutionException;
import com.scalar.db.io.BigIntValue;
import com.scalar.db.io.IntValue;
import com.scalar.db.io.Key;
import com.scalar.db.io.TextValue;
import com.scalar.dl.ledger.crypto.CertificateEntry;
import com.scalar.dl.ledger.exception.DatabaseException;
import com.scalar.dl.ledger.exception.MissingCertificateException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ScalarCertificateRegistryTest {
  private static final String ANY_ENTITY_ID = "entity_id";
  private static final int ANY_VERSION = 1;
  private static final String ANY_PEM = "pem";
  private static final long ANY_TIME = 1L;
  @Mock private DistributedStorage storage;
  @InjectMocks private ScalarCertificateRegistry registry;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  private Optional<Result> configureResult(CertificateEntry entry) {
    if (entry == null) {
      return Optional.empty();
    }
    Result result = mock(Result.class);
    when(result.getValue(CertificateEntry.ENTITY_ID))
        .thenReturn(Optional.of(new TextValue(CertificateEntry.ENTITY_ID, entry.getEntityId())));
    when(result.getValue(CertificateEntry.VERSION))
        .thenReturn(Optional.of(new IntValue(CertificateEntry.VERSION, entry.getVersion())));
    when(result.getValue(CertificateEntry.PEM))
        .thenReturn(Optional.of(new TextValue(CertificateEntry.PEM, entry.getPem())));
    when(result.getValue(CertificateEntry.REGISTERED_AT))
        .thenReturn(
            Optional.of(new BigIntValue(CertificateEntry.REGISTERED_AT, entry.getRegisteredAt())));
    return Optional.of(result);
  }

  @Test
  public void bind_ValidArgumentGiven_ShouldBindProperly() throws ExecutionException {
    // Arrange
    CertificateEntry entry = new CertificateEntry(ANY_ENTITY_ID, ANY_VERSION, ANY_PEM, ANY_TIME);
    doNothing().when(storage).put(any(Put.class));

    // Act Assert
    assertThatCode(() -> registry.bind(entry)).doesNotThrowAnyException();

    // Assert
    Put expected =
        new Put(
                new Key(CertificateEntry.ENTITY_ID, ANY_ENTITY_ID),
                new Key(CertificateEntry.VERSION, ANY_VERSION))
            .withValue(CertificateEntry.PEM, ANY_PEM)
            .withValue(CertificateEntry.REGISTERED_AT, ANY_TIME)
            .withConsistency(Consistency.SEQUENTIAL)
            .forTable(ScalarCertificateRegistry.TABLE);
    verify(storage).put(expected);
  }

  @Test
  public void bind_InvalidArgumentGiven_ShouldThrowIllegalArgumentException()
      throws ExecutionException {
    // Arrange

    // Act Assert
    assertThatThrownBy(
            () -> registry.bind(new CertificateEntry(null, ANY_VERSION, ANY_PEM, ANY_TIME)))
        .isInstanceOf(NullPointerException.class);

    // Assert
    verify(storage, never()).put(any(Put.class));
  }

  @Test
  public void bind_ValidArgumentGivenAndStorageFailed_ShouldThrowDatabaseException()
      throws ExecutionException {
    // Arrange
    CertificateEntry entry = new CertificateEntry(ANY_ENTITY_ID, ANY_VERSION, ANY_PEM, ANY_TIME);
    ExecutionException toThrow = mock(ExecutionException.class);
    doThrow(toThrow).when(storage).put(any(Put.class));

    // Act Assert
    assertThatThrownBy(() -> registry.bind(entry))
        .isInstanceOf(DatabaseException.class)
        .hasCause(toThrow);
  }

  @Test
  public void unbind_ValidArgumentGiven_ShouldUnbindProperly() throws ExecutionException {
    // Arrange
    CertificateEntry.Key key = new CertificateEntry.Key(ANY_ENTITY_ID, ANY_VERSION);
    doNothing().when(storage).delete(any(Delete.class));

    // Act Assert
    assertThatCode(() -> registry.unbind(key)).doesNotThrowAnyException();

    // Assert
    Delete expected =
        new Delete(
                new Key(CertificateEntry.ENTITY_ID, ANY_ENTITY_ID),
                new Key(CertificateEntry.VERSION, ANY_VERSION))
            .withConsistency(Consistency.SEQUENTIAL)
            .forTable(ScalarCertificateRegistry.TABLE);
    verify(storage).delete(expected);
  }

  @Test
  public void unbind_InvalidArgumentGiven_ShouldThrowIllegalArgumentException()
      throws ExecutionException {
    // Arrange

    // Act Assert
    assertThatThrownBy(() -> registry.unbind(new CertificateEntry.Key(null, ANY_VERSION)))
        .isInstanceOf(NullPointerException.class);

    // Assert
    verify(storage, never()).delete(any(Delete.class));
  }

  @Test
  public void unbind_ValidArgumentGivenAndStorageFailed_ShouldThrowDatabaseException()
      throws ExecutionException {
    // Arrange
    CertificateEntry.Key key = new CertificateEntry.Key(ANY_ENTITY_ID, ANY_VERSION);
    ExecutionException toThrow = mock(ExecutionException.class);
    doThrow(toThrow).when(storage).delete(any(Delete.class));

    // Act Assert
    assertThatThrownBy(() -> registry.unbind(key))
        .isInstanceOf(DatabaseException.class)
        .hasCause(toThrow);
  }

  @Test
  public void lookup_ValidArgumentGiven_ShouldLookupProperly() throws ExecutionException {
    // Arrange
    CertificateEntry entry = new CertificateEntry(ANY_ENTITY_ID, ANY_VERSION, ANY_PEM, ANY_TIME);
    Optional<Result> result = configureResult(entry);
    when(storage.get(any(Get.class))).thenReturn(result);

    // Act Assert
    CertificateEntry actual = registry.lookup(entry.getKey());

    // Assert
    assertThat(actual).isEqualTo(entry);
    Get expected =
        new Get(
                new Key(CertificateEntry.ENTITY_ID, ANY_ENTITY_ID),
                new Key(CertificateEntry.VERSION, ANY_VERSION))
            .withConsistency(Consistency.SEQUENTIAL)
            .forTable(ScalarCertificateRegistry.TABLE);
    verify(storage).get(expected);
  }

  @Test
  public void lookup_InvalidArgumentGiven_ShouldThrowIllegalArgumentException()
      throws ExecutionException {
    // Arrange

    // Act Assert
    assertThatThrownBy(() -> registry.lookup(new CertificateEntry.Key(null, ANY_VERSION)))
        .isInstanceOf(NullPointerException.class);

    // Assert
    verify(storage, never()).scan(any(Scan.class));
  }

  @Test
  public void
      lookup_ValidArgumentGivenButEmptyResultReturned_ShouldThrowMissingCertificateException()
          throws ExecutionException {
    // Arrange
    CertificateEntry.Key key = new CertificateEntry.Key(ANY_ENTITY_ID, ANY_VERSION);
    when(storage.get(any(Get.class))).thenReturn(Optional.empty());

    // Act Assert
    assertThatThrownBy(() -> registry.lookup(key)).isInstanceOf(MissingCertificateException.class);

    // Assert
    verify(storage).get(any(Get.class));
  }

  @Test
  public void lookup_ValidArgumentGivenButStorageFailed_ShouldThrowDatabaseException()
      throws ExecutionException {
    // Arrange
    CertificateEntry.Key key = new CertificateEntry.Key(ANY_ENTITY_ID, ANY_VERSION);
    ExecutionException toThrow = mock(ExecutionException.class);
    when(storage.get(any(Get.class))).thenThrow(toThrow);

    // Act Assert
    assertThatThrownBy(() -> registry.lookup(key))
        .isInstanceOf(DatabaseException.class)
        .hasCause(toThrow);

    // Assert
    verify(storage).get(any(Get.class));
  }

  @Test
  public void lookup_ValidArgumentGivenButEmptyPemReturned_ShouldThrowMissingCertificateException()
      throws ExecutionException {
    // Arrange
    CertificateEntry.Key key = new CertificateEntry.Key(ANY_ENTITY_ID, ANY_VERSION);
    Optional<Result> result = configureResult(null);
    when(storage.get(any(Get.class))).thenReturn(result);

    // Act Assert
    assertThatThrownBy(() -> registry.lookup(key)).isInstanceOf(MissingCertificateException.class);

    // Assert
    verify(storage).get(any(Get.class));
  }
}
