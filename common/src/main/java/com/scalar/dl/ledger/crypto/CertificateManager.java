package com.scalar.dl.ledger.crypto;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.scalar.dl.ledger.database.CertificateRegistry;
import com.scalar.dl.ledger.error.CommonError;
import com.scalar.dl.ledger.exception.DatabaseException;
import com.scalar.dl.ledger.exception.MissingCertificateException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.concurrent.Immutable;

/**
 * A manager used to store and retrieve {@link CertificateEntry}s from a {@link
 * CertificateRegistry}.
 *
 * <p>A {@code CertificateManager} will store {@link CertificateEntry}s in a {@link
 * CertificateRegistry}. It will also return {@link DigitalSignatureValidator}s for validating
 * signatures corresponding to the {@code CertificateEntry}s.
 */
@Immutable
public class CertificateManager {
  private static final int CACHE_SIZE = 131072;
  private final CertificateRegistry registry;
  private final ConcurrentMap<String, Cache<CertificateEntry.Key, DigitalSignatureValidator>>
      caches;

  /**
   * Constructs a {@code CertificateManager} with the specified {@link CertificateRegistry}.
   *
   * @param registry a {@link CertificateRegistry}
   */
  @Inject
  public CertificateManager(CertificateRegistry registry) {
    this.registry = checkNotNull(registry);
    this.caches = new ConcurrentHashMap<>();
  }

  @VisibleForTesting
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public CertificateManager(
      CertificateRegistry registry,
      ConcurrentMap<String, Cache<CertificateEntry.Key, DigitalSignatureValidator>> caches) {
    this.registry = registry;
    this.caches = caches;
  }

  /**
   * Stores the specified {@code CertificateEntry} in a {@link CertificateRegistry}. Will throw a
   * {@link DatabaseException} if the certificate has already been registered.
   *
   * @param namespace a namespace to register the certificate
   * @param entry a {@code CertificateEntry}
   */
  public void register(String namespace, CertificateEntry entry) {
    try {
      registry.lookup(namespace, entry.getKey());
      throw new DatabaseException(CommonError.CERTIFICATE_ALREADY_REGISTERED);
    } catch (MissingCertificateException e) {
      registry.bind(namespace, entry);
    }
  }

  /**
   * Returns a {@link DigitalSignatureValidator} for the given {@link CertificateEntry.Key}.
   *
   * @param namespace a namespace for the certificate
   * @param key {@link CertificateEntry.Key}
   * @return a {@link DigitalSignatureValidator}
   */
  public DigitalSignatureValidator getValidator(String namespace, CertificateEntry.Key key) {
    Cache<CertificateEntry.Key, DigitalSignatureValidator> cache =
        caches.computeIfAbsent(namespace, this::createCache);
    DigitalSignatureValidator validator = cache.getIfPresent(key);
    if (validator != null) {
      return validator;
    }

    CertificateEntry entry = registry.lookup(namespace, key);
    validator = new DigitalSignatureValidator(entry.getPem());
    cache.put(key, validator);

    return validator;
  }

  private Cache<CertificateEntry.Key, DigitalSignatureValidator> createCache(String namespace) {
    return CacheBuilder.newBuilder().maximumSize(CACHE_SIZE).build();
  }
}
