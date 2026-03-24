package com.scalar.dl.ledger.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.scalar.dl.ledger.database.SecretRegistry;
import com.scalar.dl.ledger.exception.DatabaseException;
import com.scalar.dl.ledger.exception.MissingSecretException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SecretManagerTest {
  private static final String SOME_NAMESPACE = "some_namespace";
  private static final String SOME_OTHER_NAMESPACE = "some_other_namespace";
  private static final String SOME_ENTITY_ID = "entity_id";
  private static final int SOME_KEY_VERSION = 1;
  private static final String SOME_SECRET_KEY = "secret_key";
  private static final long SOME_REGISTERED_AT = 1L;
  private SecretEntry entry;
  @Mock private SecretRegistry registry;
  @Mock private HmacValidator validator;
  private ConcurrentMap<String, Cache<SecretEntry.Key, HmacValidator>> cachesForKey;
  private Cache<String, HmacValidator> cacheForSecret;
  private SecretManager manager;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    cachesForKey = new ConcurrentHashMap<>();
    cacheForSecret = CacheBuilder.newBuilder().maximumSize(128).build();
    manager = new SecretManager(registry, cachesForKey, cacheForSecret);
    entry = new SecretEntry(SOME_ENTITY_ID, SOME_KEY_VERSION, SOME_SECRET_KEY, SOME_REGISTERED_AT);
  }

  @Test
  public void register_ProperSecretEntryGiven_ShouldCallBind() {
    // Arrange
    MissingSecretException toThrow = mock(MissingSecretException.class);
    when(registry.lookup(SOME_NAMESPACE, entry.getKey())).thenThrow(toThrow);

    // Act
    manager.register(SOME_NAMESPACE, entry);

    // Assert
    verify(registry).lookup(SOME_NAMESPACE, entry.getKey());
    verify(registry).bind(SOME_NAMESPACE, entry);
  }

  @Test
  public void register_AlreadyRegisteredSecretGiven_ShouldThrowDatabaseException() {
    // Arrange
    when(registry.lookup(SOME_NAMESPACE, entry.getKey())).thenReturn(mock(SecretEntry.class));

    // Act
    assertThatThrownBy(() -> manager.register(SOME_NAMESPACE, entry))
        .isInstanceOf(DatabaseException.class);

    // Assert
    verify(registry, never()).bind(eq(SOME_NAMESPACE), eq(entry));
  }

  @Test
  public void getValidator_EntityIdAndVersionGivenAndValidatorNotCached_ShouldGetFromRegistry() {
    // Arrange
    when(registry.lookup(SOME_NAMESPACE, entry.getKey())).thenReturn(entry);

    // Act
    HmacValidator actual = manager.getValidator(SOME_NAMESPACE, entry.getKey());

    // Assert
    verify(registry).lookup(SOME_NAMESPACE, entry.getKey());
    assertThat(actual).isEqualTo(new HmacValidator(SOME_SECRET_KEY));
    assertThat(cachesForKey.get(SOME_NAMESPACE).asMap())
        .containsOnly(entry(entry.getKey(), actual));
  }

  @Test
  public void getValidator_EntityIdAndVersionGivenAndValidatorCached_ShouldGetFromCache() {
    // Arrange
    Cache<SecretEntry.Key, HmacValidator> cache =
        CacheBuilder.newBuilder().maximumSize(128).build();
    cache.put(entry.getKey(), validator);
    cachesForKey.put(SOME_NAMESPACE, cache);

    // Act
    HmacValidator actual = manager.getValidator(SOME_NAMESPACE, entry.getKey());

    // Assert
    verify(registry, never()).lookup(SOME_NAMESPACE, entry.getKey());
    assertThat(actual).isEqualTo(validator);
    assertThat(cachesForKey.get(SOME_NAMESPACE).asMap())
        .containsOnly(entry(entry.getKey(), actual));
  }

  @Test
  public void getValidator_DifferentNamespacesGiven_ShouldUseSeparateCaches() {
    // Arrange
    SecretEntry otherEntry =
        new SecretEntry(SOME_ENTITY_ID, SOME_KEY_VERSION, SOME_SECRET_KEY, SOME_REGISTERED_AT);
    when(registry.lookup(SOME_NAMESPACE, entry.getKey())).thenReturn(entry);
    when(registry.lookup(SOME_OTHER_NAMESPACE, otherEntry.getKey())).thenReturn(otherEntry);

    // Act
    HmacValidator validator1 = manager.getValidator(SOME_NAMESPACE, entry.getKey());
    HmacValidator validator2 = manager.getValidator(SOME_OTHER_NAMESPACE, otherEntry.getKey());

    // Assert
    verify(registry).lookup(SOME_NAMESPACE, entry.getKey());
    verify(registry).lookup(SOME_OTHER_NAMESPACE, otherEntry.getKey());
    assertThat(cachesForKey).hasSize(2);
    assertThat(cachesForKey.get(SOME_NAMESPACE).asMap())
        .containsOnly(entry(entry.getKey(), validator1));
    assertThat(cachesForKey.get(SOME_OTHER_NAMESPACE).asMap())
        .containsOnly(entry(otherEntry.getKey(), validator2));
  }

  @Test
  public void getValidator_SecretKeyGivenAndValidatorNotCached_ShouldGetFromRegistry() {
    // Arrange
    when(registry.lookup(SOME_NAMESPACE, entry.getKey())).thenReturn(entry);

    // Act
    HmacValidator actual = manager.getValidator(SOME_SECRET_KEY);

    // Assert
    verify(registry, never()).lookup(any(), any());
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
    verify(registry, never()).lookup(SOME_NAMESPACE, entry.getKey());
    assertThat(actual).isEqualTo(validator);
    assertThat(cacheForSecret.asMap()).containsOnly(entry(SOME_SECRET_KEY, actual));
  }
}
