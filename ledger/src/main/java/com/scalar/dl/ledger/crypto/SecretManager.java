package com.scalar.dl.ledger.crypto;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.scalar.dl.ledger.database.SecretRegistry;
import com.scalar.dl.ledger.error.CommonError;
import com.scalar.dl.ledger.exception.DatabaseException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javax.annotation.concurrent.Immutable;

/**
 * A manager used to store and retrieve {@link SecretEntry}s from a {@link SecretRegistry}.
 *
 * <p>A {@code SecretManager} will store {@link SecretEntry}s in a {@link SecretRegistry}. It will
 * also return {@link HmacValidator}s corresponding to the {@code SecretEntry}s for validating
 * hashes (signatures) .
 */
@Immutable
public class SecretManager {
  private static final int CACHE_SIZE = 131072;
  private final SecretRegistry registry;
  private final Cache<SecretEntry.Key, HmacValidator> cacheForKey;
  private final Cache<String, HmacValidator> cacheForSecret;

  /**
   * Constructs a {@code SecretManager} with the specified {@link SecretRegistry}.
   *
   * @param registry a {@link SecretRegistry}
   */
  @Inject
  public SecretManager(SecretRegistry registry) {
    this.registry = checkNotNull(registry);
    this.cacheForKey = CacheBuilder.newBuilder().maximumSize(CACHE_SIZE).build();
    this.cacheForSecret = CacheBuilder.newBuilder().maximumSize(CACHE_SIZE).build();
  }

  @VisibleForTesting
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public SecretManager(
      SecretRegistry registry,
      Cache<SecretEntry.Key, HmacValidator> cacheForKey,
      Cache<String, HmacValidator> cacheForSecret) {
    this.registry = registry;
    this.cacheForKey = cacheForKey;
    this.cacheForSecret = cacheForSecret;
  }

  /**
   * Stores the specified {@code SecretEntry} in a {@link SecretRegistry}. Will throw a {@link
   * DatabaseException} if the secret has already been registered.
   *
   * @param entry a {@code SecretEntry}
   */
  public void register(SecretEntry entry) {
    SecretEntry existing = registry.lookup(entry.getKey());
    if (existing != null) {
      throw new DatabaseException(CommonError.SECRET_ALREADY_REGISTERED);
    }
    registry.bind(entry);
  }

  /**
   * Returns a {@link HmacValidator} for the given {@link SecretEntry.Key}.
   *
   * @param key {@link SecretEntry.Key}
   * @return a {@link HmacValidator}
   */
  public HmacValidator getValidator(SecretEntry.Key key) {
    HmacValidator validator = cacheForKey.getIfPresent(key);
    if (validator != null) {
      return validator;
    }

    SecretEntry entry = registry.lookup(key);
    validator = new HmacValidator(entry.getSecretKey());
    cacheForKey.put(key, validator);

    return validator;
  }

  /**
   * Returns a {@link HmacValidator} for the given secret.
   *
   * @param secretKey secret key
   * @return a {@link HmacValidator}
   */
  public HmacValidator getValidator(String secretKey) {
    HmacValidator validator = cacheForSecret.getIfPresent(secretKey);
    if (validator != null) {
      return validator;
    }

    validator = new HmacValidator(secretKey);
    cacheForSecret.put(secretKey, validator);

    return validator;
  }
}
