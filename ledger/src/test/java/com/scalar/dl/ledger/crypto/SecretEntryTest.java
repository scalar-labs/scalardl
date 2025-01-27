package com.scalar.dl.ledger.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.Test;

public class SecretEntryTest {
  private static final String SOME_ENTITY_ID = "entity_id";
  private static final int SOME_KEY_VERSION = 1;
  private static final String SOME_SECRET_KEY = "secret_key";
  private static final long SOME_REGISTERED_AT = 10L;

  @Test
  public void constructor_EntriesProperlyGiven_ShouldInstantiateAndHoldEntries() {
    // Arrange

    // Act
    SecretEntry entry =
        new SecretEntry(SOME_ENTITY_ID, SOME_KEY_VERSION, SOME_SECRET_KEY, SOME_REGISTERED_AT);

    // Assert
    assertThat(entry.getEntityId()).isEqualTo(SOME_ENTITY_ID);
    assertThat(entry.getKeyVersion()).isEqualTo(SOME_KEY_VERSION);
    assertThat(entry.getSecretKey()).isEqualTo(SOME_SECRET_KEY);
    assertThat(entry.getRegisteredAt()).isEqualTo(SOME_REGISTERED_AT);
    assertThat(entry.getKey().getEntityId()).isEqualTo(SOME_ENTITY_ID);
    assertThat(entry.getKey().getKeyVersion()).isEqualTo(SOME_KEY_VERSION);
  }

  @Test
  public void constructor_InvalidEntityIdGiven_ShouldThrowIllegalArgumentException() {
    // Arrange

    // Act
    Throwable thrown =
        catchThrowable(
            () -> new SecretEntry(null, SOME_KEY_VERSION, SOME_SECRET_KEY, SOME_REGISTERED_AT));

    // Assert
    assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void constructor_InvalidKeyVersionGiven_ShouldThrowIllegalArgumentException() {
    // Arrange

    // Act
    Throwable thrown =
        catchThrowable(
            () -> new SecretEntry(SOME_ENTITY_ID, 0, SOME_SECRET_KEY, SOME_REGISTERED_AT));

    // Assert
    assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void constructor_InvalidSecretKeyGiven_ShouldThrowIllegalArgumentException() {
    // Arrange

    // Act
    Throwable thrown =
        catchThrowable(
            () -> new SecretEntry(SOME_ENTITY_ID, SOME_KEY_VERSION, null, SOME_REGISTERED_AT));

    // Assert
    assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void constructor_InvalidRegisteredAtGiven_ShouldThrowIllegalArgumentException() {
    // Arrange

    // Act
    Throwable thrown =
        catchThrowable(
            () -> new SecretEntry(SOME_ENTITY_ID, SOME_KEY_VERSION, SOME_SECRET_KEY, -1));

    // Assert
    assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void equals_SameEntriesGiven_ShouldReturnTrue() {
    // Arrange
    SecretEntry entry1 =
        new SecretEntry(SOME_ENTITY_ID, SOME_KEY_VERSION, SOME_SECRET_KEY, SOME_REGISTERED_AT);
    SecretEntry entry2 =
        new SecretEntry(SOME_ENTITY_ID, SOME_KEY_VERSION, SOME_SECRET_KEY, SOME_REGISTERED_AT);

    // Act
    boolean actual = entry1.equals(entry2);

    // Assert
    assertThat(actual).isTrue();
    assertThat(entry1.hashCode()).isEqualTo(entry2.hashCode());
  }

  @Test
  public void equals_DifferentEntriesGiven_ShouldReturnFalse() {
    // Arrange
    SecretEntry entry1 =
        new SecretEntry(SOME_ENTITY_ID, SOME_KEY_VERSION, SOME_SECRET_KEY, SOME_REGISTERED_AT);
    SecretEntry entry2 =
        new SecretEntry(SOME_ENTITY_ID, SOME_KEY_VERSION + 1, SOME_SECRET_KEY, SOME_REGISTERED_AT);

    // Act
    boolean actual = entry1.equals(entry2);

    // Assert
    assertThat(actual).isFalse();
    assertThat(entry1.hashCode()).isNotEqualTo(entry2.hashCode());
  }

  @Test
  public void KeyConstructor_InvalidEntriesGiven_ShouldThrowIllegalArgumentException() {
    // Arrange

    // Act
    Throwable thrown1 = catchThrowable(() -> new SecretEntry.Key(null, SOME_KEY_VERSION));
    Throwable thrown2 = catchThrowable(() -> new SecretEntry.Key(SOME_ENTITY_ID, 0));

    // Assert
    assertThat(thrown1).isInstanceOf(IllegalArgumentException.class);
    assertThat(thrown2).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void KeyEquals_SameEntriesGiven_ShouldReturnTrue() {
    // Arrange
    SecretEntry.Key key1 =
        new SecretEntry(SOME_ENTITY_ID, SOME_KEY_VERSION, SOME_SECRET_KEY, SOME_REGISTERED_AT)
            .getKey();
    SecretEntry.Key key2 =
        new SecretEntry(SOME_ENTITY_ID, SOME_KEY_VERSION, SOME_SECRET_KEY, SOME_REGISTERED_AT)
            .getKey();

    // Act
    boolean actual = key1.equals(key2);

    // Assert
    assertThat(actual).isTrue();
    assertThat(key1.hashCode()).isEqualTo(key2.hashCode());
  }

  @Test
  public void KeyEquals_DifferentEntriesGiven_ShouldReturnFalse() {
    // Arrange
    SecretEntry.Key key1 =
        new SecretEntry(SOME_ENTITY_ID, SOME_KEY_VERSION, SOME_SECRET_KEY, SOME_REGISTERED_AT)
            .getKey();
    SecretEntry.Key key2 =
        new SecretEntry(SOME_ENTITY_ID, SOME_KEY_VERSION + 1, SOME_SECRET_KEY, SOME_REGISTERED_AT)
            .getKey();

    // Act
    boolean actual = key1.equals(key2);

    // Assert
    assertThat(actual).isFalse();
    assertThat(key1.hashCode()).isNotEqualTo(key2.hashCode());
  }
}
