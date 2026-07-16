package com.scalar.dl.ledger.database.scalardb;

import com.scalar.db.exception.storage.ExecutionException;
import com.scalar.db.exception.transaction.CrudException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/**
 * Runs a ScalarDB CRUD operation in a privileged block, so that a contract's restricted {@code
 * ProtectionDomain} on the current stack is excluded from JVM-level permission checks (properties,
 * ServiceLoader, sockets) performed by the underlying database driver and connection pool during
 * connection setup.
 *
 * <p>Without this, a database access issued from within a contract (or a function) fails under the
 * SecurityManager whenever it needs to establish a new physical connection (e.g., after a
 * server-side idle timeout, a database restart, or a network interruption), because the driver's
 * connection housekeeping is checked against the contract's restricted {@code ProtectionDomain} on
 * the stack. Wrapping only the ScalarDB call in a privileged block cuts the permission-check stack
 * walk at the framework frame while the contract code itself remains restricted.
 *
 * <p>Only ScalarDB CRUD operations may be passed here — never contract-derived code or callbacks.
 * The functional interfaces below are intentionally narrow (tied to ScalarDB's exception
 * signatures) so contract lambdas cannot be smuggled in. Also, keep the construction of operation
 * objects (e.g., {@code Get}, {@code Scan}) and the exception translation outside the privileged
 * block so that contract-derived data is not processed on a privileged frame.
 *
 * <p>Every ScalarDB call that is synchronously reachable from {@code Contract.invoke()} or {@code
 * Function.invoke()} must go through here. That includes the obvious direct reads and writes on the
 * ledger, and also the less obvious reads that fire on cache miss during nested contract invocation
 * (looking up the callee contract, its signing certificate, or its HMAC secret) and the read that
 * fires on every function invocation (since {@code FunctionManager} has no cache). Framework paths
 * that run before {@code invoke()} or after it returns (registration, commit/abort, validation,
 * background workers) do not need wrapping.
 */
public final class Privileged {
  private Privileged() {}

  /** A CRUD operation on a {@code DistributedTransaction}. */
  @FunctionalInterface
  public interface TransactionCrud<T> {
    T call() throws CrudException;
  }

  /**
   * A CRUD operation on a {@code DistributedStorage}, including {@code Scanner} iteration. An
   * {@code IOException} from closing a {@code Scanner} should be handled inside the operation
   * (e.g., logged), following the existing convention of the call sites.
   */
  @FunctionalInterface
  public interface StorageCrud<T> {
    T call() throws ExecutionException;
  }

  /**
   * Runs the given {@code DistributedTransaction} CRUD operation in a privileged block.
   *
   * @param operation a CRUD operation on a {@code DistributedTransaction}
   * @param <T> the result type
   * @return the result of the operation
   * @throws CrudException if the operation fails
   */
  public static <T> T transactionCrud(TransactionCrud<T> operation) throws CrudException {
    try {
      return AccessController.doPrivileged((PrivilegedExceptionAction<T>) operation::call);
    } catch (PrivilegedActionException e) {
      // TransactionCrud declares only CrudException, so the cause is always one.
      throw (CrudException) e.getCause();
    }
  }

  /**
   * Runs the given {@code DistributedStorage} CRUD operation in a privileged block.
   *
   * @param operation a CRUD operation on a {@code DistributedStorage}
   * @param <T> the result type
   * @return the result of the operation
   * @throws ExecutionException if the operation fails
   */
  public static <T> T storageCrud(StorageCrud<T> operation) throws ExecutionException {
    try {
      return AccessController.doPrivileged((PrivilegedExceptionAction<T>) operation::call);
    } catch (PrivilegedActionException e) {
      // StorageCrud declares only ExecutionException, so the cause is always one.
      throw (ExecutionException) e.getCause();
    }
  }
}
