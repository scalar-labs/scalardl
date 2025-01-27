package com.scalar.dl.ledger.crypto;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.scalar.dl.ledger.database.CertificateRegistry;
import com.scalar.dl.ledger.exception.DatabaseException;
import com.scalar.dl.ledger.exception.MissingCertificateException;
import com.scalar.dl.ledger.service.StatusCode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
  private final Cache<CertificateEntry.Key, DigitalSignatureValidator> cache;

  /**
   * Constructs a {@code CertificateManager} with the specified {@link CertificateRegistry}.
   *
   * @param registry a {@link CertificateRegistry}
   */
  @Inject
  public CertificateManager(CertificateRegistry registry) {
    this.registry = checkNotNull(registry);
    this.cache = CacheBuilder.newBuilder().maximumSize(CACHE_SIZE).build();
  }

  @VisibleForTesting
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public CertificateManager(
      CertificateRegistry registry, Cache<CertificateEntry.Key, DigitalSignatureValidator> cache) {
    this.registry = registry;
    this.cache = cache;
  }

  /**
   * Stores the specified {@code CertificateEntry} in a {@link CertificateRegistry}. Will throw a
   * {@link DatabaseException} if the certificate has already been registered.
   *
   * @param entry a {@code CertificateEntry}
   */
  public void register(CertificateEntry entry) {
    try {
      registry.lookup(entry.getKey());
      throw new DatabaseException(
          "The specified certificate is already registered",
          StatusCode.CERTIFICATE_ALREADY_REGISTERED);
    } catch (MissingCertificateException e) {
      registry.bind(entry);
    }
  }

  /**
   * Returns a {@link DigitalSignatureValidator} for the given {@link CertificateEntry.Key}.
   *
   * @param key {@link CertificateEntry.Key}
   * @return a {@link DigitalSignatureValidator}
   */
  public DigitalSignatureValidator getValidator(CertificateEntry.Key key) {
    DigitalSignatureValidator validator = cache.getIfPresent(key);
    if (validator != null) {
      return validator;
    }

    CertificateEntry entry = registry.lookup(key);
    validator = new DigitalSignatureValidator(entry.getPem());
    cache.put(key, validator);

    return validator;
  }
}
