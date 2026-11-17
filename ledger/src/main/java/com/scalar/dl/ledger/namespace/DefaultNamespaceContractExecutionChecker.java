package com.scalar.dl.ledger.namespace;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Ticker;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A checker to decide whether contract execution in the default namespace is allowed.
 *
 * <p>Contract execution in the default namespace is not allowed when it is disabled by the
 * configuration, or when at least one namespace created for import exists. The latter cannot be
 * turned off by the configuration: once the data of another deployment is imported, executing a
 * contract in the default namespace, which can access any namespace, could create assets that are
 * indistinguishable from the imported ones.
 *
 * <p>Whether a namespace created for import exists is read from the namespace table and cached for
 * a certain period, so creating or dropping such a namespace is reflected after the period at the
 * latest. If reading it fails, the last result is kept being used until the next period.
 */
@ThreadSafe
public class DefaultNamespaceContractExecutionChecker {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(DefaultNamespaceContractExecutionChecker.class.getName());
  @VisibleForTesting static final long CACHE_EXPIRATION_TIME_MILLIS = 60000;
  private final boolean enabled;
  private final NamespaceManager namespaceManager;
  private final long cacheExpirationTimeNanos;
  private final Ticker ticker;
  @Nullable private volatile ImportedNamespaceExistence importedNamespaceExistence;

  /**
   * Constructs a {@code DefaultNamespaceContractExecutionChecker}.
   *
   * @param enabled whether contract execution in the default namespace is enabled by the
   *     configuration
   * @param namespaceManager a {@link NamespaceManager}
   */
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public DefaultNamespaceContractExecutionChecker(
      boolean enabled, NamespaceManager namespaceManager) {
    this(enabled, namespaceManager, CACHE_EXPIRATION_TIME_MILLIS, Ticker.systemTicker());
  }

  @VisibleForTesting
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  DefaultNamespaceContractExecutionChecker(
      boolean enabled,
      NamespaceManager namespaceManager,
      long cacheExpirationTimeMillis,
      Ticker ticker) {
    this.enabled = enabled;
    this.namespaceManager = namespaceManager;
    this.cacheExpirationTimeNanos = TimeUnit.MILLISECONDS.toNanos(cacheExpirationTimeMillis);
    this.ticker = ticker;
  }

  /**
   * Returns whether contract execution in the default namespace is allowed.
   *
   * @return true if contract execution in the default namespace is allowed
   */
  public boolean isAllowed() {
    if (!enabled) {
      return false;
    }
    return !importedNamespaceExists();
  }

  private boolean importedNamespaceExists() {
    ImportedNamespaceExistence existence = importedNamespaceExistence;
    if (existence != null && !isExpired(existence)) {
      return existence.exists;
    }

    synchronized (this) {
      existence = importedNamespaceExistence;
      if (existence != null && !isExpired(existence)) {
        return existence.exists;
      }

      boolean exists;
      try {
        exists = namespaceManager.existsImported();
      } catch (RuntimeException e) {
        if (existence == null) {
          throw e;
        }
        LOGGER.warn(
            "Failed to check if a namespace created for import exists. "
                + "The last result is used until the next check.",
            e);
        exists = existence.exists;
      }
      importedNamespaceExistence = new ImportedNamespaceExistence(exists, ticker.read());
      return exists;
    }
  }

  private boolean isExpired(ImportedNamespaceExistence existence) {
    return ticker.read() - existence.checkedAtNanos >= cacheExpirationTimeNanos;
  }

  @Immutable
  private static final class ImportedNamespaceExistence {
    private final boolean exists;
    private final long checkedAtNanos;

    private ImportedNamespaceExistence(boolean exists, long checkedAtNanos) {
      this.exists = exists;
      this.checkedAtNanos = checkedAtNanos;
    }
  }
}
