package com.scalar.dl.ledger.database.scalardb;

import com.scalar.db.api.Delete;
import com.scalar.db.api.Get;
import com.scalar.db.api.Operation;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.dl.ledger.database.MutableDatabase;
import com.scalar.dl.ledger.error.LedgerError;
import com.scalar.dl.ledger.exception.InvalidFunctionException;
import com.scalar.dl.ledger.namespace.Namespaces;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A decorator for {@link MutableDatabase} that enforces namespace-based access control on the
 * ScalarDB operations issued by functions.
 *
 * <p>This is the single place that governs which ScalarDB namespaces a function may access:
 *
 * <ul>
 *   <li>When the context namespace is the default namespace, only system namespaces and namespaces
 *       with reserved prefixes are disallowed (the historical behavior, kept for backward
 *       compatibility). Operations without a namespace are allowed.
 *   <li>When the context namespace is a non-default namespace, the function may only access the
 *       ScalarDB namespace with the same name as its context namespace. Any operation targeting a
 *       different namespace, or an operation without an explicit namespace, is rejected. Since
 *       system namespaces never match a user context namespace, they are implicitly disallowed too.
 * </ul>
 *
 * <p>This mirrors {@link com.scalar.dl.ledger.database.NamespaceRestrictedAssetLedger}, which
 * applies the equivalent restriction to the Asset Ledger accessed by contracts. Two differences are
 * intentional:
 *
 * <ul>
 *   <li>This decorator is always applied (even for the default context namespace) because it must
 *       also enforce the system-namespace deny list, which the Asset Ledger does not need. The
 *       Asset Ledger decorator is only applied for non-default context namespaces.
 *   <li>In a non-default context, this decorator rejects operations without an explicit namespace,
 *       whereas the Asset Ledger decorator delegates them. An unspecified namespace here would
 *       otherwise fall back to ScalarDB's configured default namespace and allow implicit
 *       cross-namespace access.
 * </ul>
 */
@ThreadSafe
public class NamespaceRestrictedMutableDatabase
    implements MutableDatabase<Get, Scan, Put, Delete, Result> {
  private static final List<String> DISALLOWED_NAMESPACES =
      Arrays.asList(
          "system",
          "system_schema",
          "system_auth",
          "system_distributed",
          "system_traces",
          "coordinator");
  // Note: "auditor" is included as a safeguard for development and PoC environments. In
  // production, the Auditor namespace is not accessible from Ledger's transaction manager because
  // Ledger and Auditor belong to different administrative domains.
  private static final List<String> DISALLOWED_NAMESPACE_PREFIXES =
      Arrays.asList("scalar", "auditor");

  private final MutableDatabase<Get, Scan, Put, Delete, Result> delegate;
  private final String contextNamespace;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public NamespaceRestrictedMutableDatabase(
      MutableDatabase<Get, Scan, Put, Delete, Result> delegate, String contextNamespace) {
    this.delegate = delegate;
    this.contextNamespace = contextNamespace;
  }

  @Override
  public Optional<Result> get(Get get) {
    validateNamespaceAccess(get);
    return delegate.get(get);
  }

  @Override
  public List<Result> scan(Scan scan) {
    validateNamespaceAccess(scan);
    return delegate.scan(scan);
  }

  @Override
  public void put(Put put) {
    validateNamespaceAccess(put);
    delegate.put(put);
  }

  @Override
  public void delete(Delete delete) {
    validateNamespaceAccess(delete);
    delegate.delete(delete);
  }

  private void validateNamespaceAccess(Operation operation) {
    if (Namespaces.DEFAULT.equals(contextNamespace)) {
      validateAgainstDisallowedNamespaces(operation);
      return;
    }

    // For a non-default context namespace, a function may only access the ScalarDB namespace with
    // the same name as its context namespace. An operation without an explicit namespace is also
    // rejected to prevent implicit access to the configured default namespace.
    String namespace = operation.forNamespace().orElse(null);
    if (!contextNamespace.equals(namespace)) {
      throw new InvalidFunctionException(
          LedgerError.FUNCTION_IS_NOT_ALLOWED_TO_ACCESS_DIFFERENT_NAMESPACE,
          namespace,
          contextNamespace);
    }
  }

  private void validateAgainstDisallowedNamespaces(Operation operation) {
    if (operation.forNamespace().isEmpty()) {
      return;
    }
    String lowercaseNamespace = operation.forNamespace().get().toLowerCase();
    if (DISALLOWED_NAMESPACES.contains(lowercaseNamespace)
        || DISALLOWED_NAMESPACE_PREFIXES.stream().anyMatch(lowercaseNamespace::startsWith)) {
      throw new InvalidFunctionException(
          LedgerError.FUNCTION_IS_NOT_ALLOWED_TO_ACCESS_SPECIFIED_NAMESPACE);
    }
  }
}
