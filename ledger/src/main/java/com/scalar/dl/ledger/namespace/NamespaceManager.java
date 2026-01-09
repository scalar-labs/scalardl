package com.scalar.dl.ledger.namespace;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.Inject;
import com.scalar.dl.ledger.database.NamespaceRegistry;
import com.scalar.dl.ledger.error.CommonError;
import com.scalar.dl.ledger.exception.LedgerException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;

/**
 * A manager used to manage namespaces in a {@link NamespaceRegistry}.
 *
 * <p>A {@code NamespaceManager} manages namespaces in a {@link NamespaceRegistry}.
 */
public class NamespaceManager {
  public static final String DEFAULT_NAMESPACE = "default";
  public static final Pattern NAMESPACE_NAME_PATTERN = Pattern.compile("[a-zA-Z][a-zA-Z0-9_]*");
  private final NamespaceRegistry registry;

  /**
   * Constructs a {@code NamespaceManager} with the specified {@link NamespaceRegistry}.
   *
   * @param registry a {@link NamespaceRegistry}
   */
  @Inject
  public NamespaceManager(NamespaceRegistry registry) {
    this.registry = checkNotNull(registry);
  }

  /**
   * Creates a namespace with the specified name. The namespace name must start with an alphabetic
   * character and can only contain alphanumeric characters and underscores.
   *
   * @param namespace a namespace name to create
   * @throws LedgerException if the namespace name is invalid
   */
  public void create(@Nonnull String namespace) {
    if (!isValidNamespaceName(namespace)) {
      throw new LedgerException(CommonError.INVALID_NAMESPACE_NAME, namespace);
    }
    if (namespace.equals(DEFAULT_NAMESPACE)) {
      throw new LedgerException(CommonError.RESERVED_NAMESPACE, namespace);
    }
    registry.create(namespace);
  }

  private boolean isValidNamespaceName(@Nonnull String namespace) {
    return NAMESPACE_NAME_PATTERN.matcher(namespace).matches();
  }

  /**
   * Scans namespaces. If the pattern is empty, returns all namespaces. If the pattern is specified,
   * only returns namespaces that contain the pattern.
   *
   * @param pattern a string pattern. If empty, returns all namespaces.
   * @return a list of namespace names
   */
  public List<String> scan(@Nonnull String pattern) {
    List<String> namespaces = new ArrayList<>(registry.scan(pattern));
    if (DEFAULT_NAMESPACE.contains(pattern)) {
      int index = Collections.binarySearch(namespaces, DEFAULT_NAMESPACE);
      if (index < 0) {
        namespaces.add(-index - 1, DEFAULT_NAMESPACE);
      }
    }
    return namespaces;
  }
}
