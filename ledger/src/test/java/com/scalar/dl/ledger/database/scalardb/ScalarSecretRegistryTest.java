package com.scalar.dl.ledger.database.scalardb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.db.api.Consistency;
import com.scalar.db.api.Delete;
import com.scalar.db.api.DistributedStorage;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.exception.storage.ExecutionException;
import com.scalar.db.io.Key;
import com.scalar.dl.ledger.crypto.Cipher;
import com.scalar.dl.ledger.crypto.SecretEntry;
import com.scalar.dl.ledger.exception.DatabaseException;
import com.scalar.dl.ledger.exception.MissingSecretException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ScalarSecretRegistryTest {
  private static final String SOME_ENTITY_ID = "entity_id";
  private static final int SOME_KEY_VERSION = 1;
  private static final String SOME_SECRET_KEY = "secret_key";
  private static final long SOME_REGISTERED_AT = 1L;
  private static final byte[] SOME_ENCRYPTED = "encrypted".getBytes(StandardCharsets.UTF_8);
  @Mock private DistributedStorage storage;
  @Mock private Cipher cipher;
  @InjectMocks private ScalarSecretRegistry registry;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  private Optional<Result> configureResult(SecretEntry entry) {
    if (entry == null) {
      return Optional.empty();
    }
    Result result = mock(Result.class);
    when(result.getText(SecretEntry.ENTITY_ID)).thenReturn(entry.getEntityId());
    when(result.getInt(SecretEntry.KEY_VERSION)).thenReturn(entry.getKeyVersion());
    when(result.getBlobAsBytes(SecretEntry.SECRET_KEY)).thenReturn(SOME_ENCRYPTED);
    when(result.getBigInt(SecretEntry.REGISTERED_AT)).thenReturn(entry.getRegisteredAt());
    return Optional.of(result);
  }

  @Test
  public void bind_ValidArgumentGiven_ShouldBindProperly() throws ExecutionException {
    // Arrange
    SecretEntry entry =
        new SecretEntry(SOME_ENTITY_ID, SOME_KEY_VERSION, SOME_SECRET_KEY, SOME_REGISTERED_AT);
    doNothing().when(storage).put(any(Put.class));
    when(cipher.encrypt(any(), anyString())).thenReturn(SOME_ENCRYPTED);

    // Act Assert
    assertThatCode(() -> registry.bind(entry)).doesNotThrowAnyException();

    // Assert
    Put expected =
        new Put(
                new Key(SecretEntry.ENTITY_ID, SOME_ENTITY_ID),
                new Key(SecretEntry.KEY_VERSION, SOME_KEY_VERSION))
            .withValue(SecretEntry.SECRET_KEY, SOME_ENCRYPTED)
            .withValue(SecretEntry.REGISTERED_AT, SOME_REGISTERED_AT)
            .withConsistency(Consistency.SEQUENTIAL)
            .forTable(ScalarSecretRegistry.TABLE);
    verify(storage).put(expected);
  }

  @Test
  public void bind_ValidArgumentGivenAndStorageFailed_ShouldThrowDatabaseException()
      throws ExecutionException {
    // Arrange
    SecretEntry entry =
        new SecretEntry(SOME_ENTITY_ID, SOME_KEY_VERSION, SOME_SECRET_KEY, SOME_REGISTERED_AT);
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
    SecretEntry.Key key = new SecretEntry.Key(SOME_ENTITY_ID, SOME_KEY_VERSION);
    doNothing().when(storage).delete(any(Delete.class));

    // Act Assert
    assertThatCode(() -> registry.unbind(key)).doesNotThrowAnyException();

    // Assert
    Delete expected =
        new Delete(
                new Key(SecretEntry.ENTITY_ID, SOME_ENTITY_ID),
                new Key(SecretEntry.KEY_VERSION, SOME_KEY_VERSION))
            .withConsistency(Consistency.SEQUENTIAL)
            .forTable(ScalarSecretRegistry.TABLE);
    verify(storage).delete(expected);
  }

  @Test
  public void unbind_ValidArgumentGivenAndStorageFailed_ShouldThrowDatabaseException()
      throws ExecutionException {
    // Arrange
    SecretEntry.Key key = new SecretEntry.Key(SOME_ENTITY_ID, SOME_KEY_VERSION);
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
    SecretEntry entry =
        new SecretEntry(SOME_ENTITY_ID, SOME_KEY_VERSION, SOME_SECRET_KEY, SOME_REGISTERED_AT);
    Optional<Result> result = configureResult(entry);
    when(storage.get(any(Get.class))).thenReturn(result);
    when(cipher.decrypt(any(), anyString()))
        .thenReturn(SOME_SECRET_KEY.getBytes(StandardCharsets.UTF_8));

    // Act Assert
    SecretEntry actual = registry.lookup(entry.getKey());

    // Assert
    assertThat(actual).isEqualTo(entry);
    Get expected =
        new Get(
                new Key(SecretEntry.ENTITY_ID, SOME_ENTITY_ID),
                new Key(SecretEntry.KEY_VERSION, SOME_KEY_VERSION))
            .withConsistency(Consistency.SEQUENTIAL)
            .forTable(ScalarSecretRegistry.TABLE);
    verify(storage).get(expected);
  }

  @Test
  public void lookup_ValidArgumentGivenButEmptyResultReturned_ShouldThrowMissingSecretException()
      throws ExecutionException {
    // Arrange
    SecretEntry.Key key = new SecretEntry.Key(SOME_ENTITY_ID, SOME_KEY_VERSION);
    when(storage.get(any(Get.class))).thenReturn(Optional.empty());

    // Act Assert
    assertThatThrownBy(() -> registry.lookup(key)).isInstanceOf(MissingSecretException.class);

    // Assert
    verify(storage).get(any(Get.class));
  }

  @Test
  public void lookup_ValidArgumentGivenButStorageFailed_ShouldThrowDatabaseException()
      throws ExecutionException {
    // Arrange
    SecretEntry.Key key = new SecretEntry.Key(SOME_ENTITY_ID, SOME_KEY_VERSION);
    ExecutionException toThrow = mock(ExecutionException.class);
    when(storage.get(any(Get.class))).thenThrow(toThrow);

    // Act Assert
    assertThatThrownBy(() -> registry.lookup(key))
        .isInstanceOf(DatabaseException.class)
        .hasCause(toThrow);

    // Assert
    verify(storage).get(any(Get.class));
  }
}
