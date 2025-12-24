package com.scalar.dl.ledger.database.scalardb;

import com.google.inject.Inject;
import com.scalar.dl.ledger.config.ServerConfig;
import com.scalar.dl.ledger.namespace.NamespaceManager;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A resolver that converts logical namespace names to physical ScalarDB namespace names.
 *
 * <p>The conversion rule is:
 *
 * <ul>
 *   <li>Default namespace -&gt; base namespace from ServerConfig
 *   <li>Other namespaces -&gt; base namespace + "_" + logical namespace
 * </ul>
 */
@ThreadSafe
public class ScalarNamespaceResolver {
  private static final String NAMESPACE_NAME_SEPARATOR = "_";
  private final String baseNamespace;

  /**
   * Constructs a {@code ScalarNamespaceResolver} with the specified {@link ServerConfig}.
   *
   * @param config a {@link ServerConfig}
   */
  @Inject
  public ScalarNamespaceResolver(ServerConfig config) {
    this.baseNamespace = config.getNamespace();
  }

  /**
   * Resolves a logical namespace name to a physical ScalarDB namespace name. If it's the default
   * namespace, we use the base namespace in a special case, for backward compatibility.
   *
   * @param logicalNamespace the logical namespace name
   * @return the physical ScalarDB namespace name
   */
  public String resolve(String logicalNamespace) {
    if (logicalNamespace.equals(NamespaceManager.DEFAULT_NAMESPACE)) {
      return baseNamespace;
    }
    return baseNamespace + NAMESPACE_NAME_SEPARATOR + logicalNamespace;
  }
}
