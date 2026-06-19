package com.scalar.dl.ledger.database.scalardb;

import com.scalar.db.api.Delete;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.db.exception.transaction.CrudConflictException;
import com.scalar.db.exception.transaction.CrudException;
import com.scalar.dl.ledger.database.MutableDatabase;
import com.scalar.dl.ledger.error.LedgerError;
import com.scalar.dl.ledger.exception.ConflictException;
import com.scalar.dl.ledger.exception.DatabaseException;
import com.scalar.dl.ledger.exception.InvalidFunctionException;
import java.util.List;
import java.util.Optional;

/**
 * A {@link MutableDatabase} implementation backed by a ScalarDB {@link DistributedTransaction}.
 *
 * <p>Namespace-based access control is intentionally not handled here; it is enforced by {@link
 * NamespaceRestrictedMutableDatabase}, which wraps this class. This class only delegates operations
 * to the underlying transaction and translates ScalarDB exceptions.
 *
 * <p>Instances must always be wrapped by {@link NamespaceRestrictedMutableDatabase}. The
 * constructor is intentionally package-private so that only {@link ScalarTransactionManager} can
 * create instances, preventing an unwrapped instance (with no access control) from being used.
 */
public class ScalarMutableDatabase implements MutableDatabase<Get, Scan, Put, Delete, Result> {
  private final DistributedTransaction transaction;

  ScalarMutableDatabase(DistributedTransaction transaction) {
    this.transaction = transaction;
  }

  @Override
  public Optional<Result> get(Get get) {
    try {
      return transaction.get(get);
    } catch (IllegalArgumentException e) {
      throw new InvalidFunctionException(
          LedgerError.OPERATION_FAILED_DUE_TO_ILLEGAL_ARGUMENT, e, e.getMessage());
    } catch (CrudConflictException e) {
      throw new ConflictException(LedgerError.OPERATION_FAILED_DUE_TO_CONFLICT, e, e.getMessage());
    } catch (CrudException e) {
      throw new DatabaseException(
          LedgerError.OPERATION_FAILED_DUE_TO_DATABASE_ERROR, e, e.getMessage());
    }
  }

  @Override
  public List<Result> scan(Scan scan) {
    try {
      return transaction.scan(scan);
    } catch (IllegalArgumentException e) {
      throw new InvalidFunctionException(
          LedgerError.OPERATION_FAILED_DUE_TO_ILLEGAL_ARGUMENT, e, e.getMessage());
    } catch (CrudConflictException e) {
      throw new ConflictException(LedgerError.OPERATION_FAILED_DUE_TO_CONFLICT, e, e.getMessage());
    } catch (CrudException e) {
      throw new DatabaseException(
          LedgerError.OPERATION_FAILED_DUE_TO_DATABASE_ERROR, e, e.getMessage());
    }
  }

  @Override
  public void put(Put put) {
    try {
      // Make put() consistent with Ledger's put(), which always pre-read implicitly.
      transaction.put(Put.newBuilder(put).implicitPreReadEnabled(true).build());
    } catch (IllegalArgumentException e) {
      throw new InvalidFunctionException(
          LedgerError.OPERATION_FAILED_DUE_TO_ILLEGAL_ARGUMENT, e, e.getMessage());
    } catch (CrudConflictException e) {
      throw new ConflictException(LedgerError.OPERATION_FAILED_DUE_TO_CONFLICT, e, e.getMessage());
    } catch (CrudException e) {
      throw new DatabaseException(
          LedgerError.OPERATION_FAILED_DUE_TO_DATABASE_ERROR, e, e.getMessage());
    }
  }

  @Override
  public void delete(Delete delete) {
    try {
      transaction.delete(delete);
    } catch (IllegalArgumentException e) {
      throw new InvalidFunctionException(
          LedgerError.OPERATION_FAILED_DUE_TO_ILLEGAL_ARGUMENT, e, e.getMessage());
    } catch (CrudConflictException e) {
      throw new ConflictException(LedgerError.OPERATION_FAILED_DUE_TO_CONFLICT, e, e.getMessage());
    } catch (CrudException e) {
      throw new DatabaseException(
          LedgerError.OPERATION_FAILED_DUE_TO_DATABASE_ERROR, e, e.getMessage());
    }
  }
}
