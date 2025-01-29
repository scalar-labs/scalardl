package com.scalar.dl.ledger.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.scalar.dl.ledger.database.SecretRegistry;
import com.scalar.dl.ledger.exception.DatabaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SecretManagerTest {
  private static final String SOME_ENTITY_ID = "entity_id";
  private static final int SOME_KEY_VERSION = 1;
  private static final String SOME_SECRET_KEY = "secret_key";
  private static final long SOME_REGISTERED_AT = 1L;
  private SecretEntry entry;
  @Mock private SecretRegistry registry;
  @Mock private HmacValidator validator;
  private Cache<SecretEntry.Key, HmacValidator> cacheForKey;
  private Cache<String, HmacValidator> cacheForSecret;
  private SecretManager manager;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    cacheForKey = CacheBuilder.newBuilder().maximumSize(128).build();
    cacheForSecret = CacheBuilder.newBuilder().maximumSize(128).build();
    manager = new SecretManager(registry, cacheForKey, cacheForSecret);
    entry = new SecretEntry(SOME_ENTITY_ID, SOME_KEY_VERSION, SOME_SECRET_KEY, SOME_REGISTERED_AT);
  }

  @Test
  public void register_ProperSecretEntryGiven_ShouldCallBind() {
    // Arrange
    when(registry.lookup(entry.getKey())).thenReturn(null);

    // Act
    manager.register(entry);

    // Assert
    verify(registry).lookup(entry.getKey());
    verify(registry).bind(entry);
  }

  @Test
  public void register_AlreadyRegisteredSecretGiven_ShouldThrowDatabaseException() {
    // Arrange
    when(registry.lookup(entry.getKey())).thenReturn(mock(SecretEntry.class));

    // Act
    assertThatThrownBy(() -> manager.register(entry)).isInstanceOf(DatabaseException.class);

    // Assert
    verify(registry, never()).bind(entry);
  }

  @Test
  public void getValidator_EntityIdAndVersionGivenAndValidatorNotCached_ShouldGetFromRegistry() {
    // Arrange
    when(registry.lookup(entry.getKey())).thenReturn(entry);

    // Act
    HmacValidator actual = manager.getValidator(entry.getKey());

    // Assert
    verify(registry).lookup(entry.getKey());
    assertThat(actual).isEqualTo(new HmacValidator(SOME_SECRET_KEY));
    assertThat(cacheForKey.asMap()).containsOnly(entry(entry.getKey(), actual));
  }

  @Test
  public void getValidator_EntityIdAndVersionGivenAndValidatorCached_ShouldGetFromCache() {
    // Arrange
    cacheForKey.put(entry.getKey(), validator);

    // Act
    HmacValidator actual = manager.getValidator(entry.getKey());

    // Assert
    verify(registry, never()).lookup(entry.getKey());
    assertThat(actual).isEqualTo(validator);
    assertThat(cacheForKey.asMap()).containsOnly(entry(entry.getKey(), actual));
  }

  @Test
  public void getValidator_SecretKeyGivenAndValidatorNotCached_ShouldGetFromRegistry() {
    // Arrange
    when(registry.lookup(entry.getKey())).thenReturn(entry);

    // Act
    HmacValidator actual = manager.getValidator(SOME_SECRET_KEY);

    // Assert
    verify(registry, never()).lookup(any());
    assertThat(actual).isEqualTo(new HmacValidator(SOME_SECRET_KEY));
    assertThat(cacheForSecret.asMap()).containsOnly(entry(SOME_SECRET_KEY, actual));
  }

  @Test
  public void getValidator_SecretKeyGivenAndValidatorCached_ShouldGetFromCache() {
    // Arrange
    cacheForSecret.put(SOME_SECRET_KEY, validator);

    // Act
    HmacValidator actual = manager.getValidator(SOME_SECRET_KEY);

    // Assert
    verify(registry, never()).lookup(entry.getKey());
    assertThat(actual).isEqualTo(validator);
    assertThat(cacheForSecret.asMap()).containsOnly(entry(SOME_SECRET_KEY, actual));
  }
}
