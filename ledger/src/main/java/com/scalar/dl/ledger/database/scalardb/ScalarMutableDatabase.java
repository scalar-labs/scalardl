package com.scalar.dl.ledger.database.scalardb;

import com.scalar.db.api.Delete;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.Get;
import com.scalar.db.api.Operation;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.db.exception.transaction.CrudConflictException;
import com.scalar.db.exception.transaction.CrudException;
import com.scalar.dl.ledger.database.MutableDatabase;
import com.scalar.dl.ledger.error.CommonLedgerError;
import com.scalar.dl.ledger.error.LedgerError;
import com.scalar.dl.ledger.exception.ConflictException;
import com.scalar.dl.ledger.exception.DatabaseException;
import com.scalar.dl.ledger.exception.InvalidFunctionException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ScalarMutableDatabase implements MutableDatabase<Get, Scan, Put, Delete, Result> {
  private final DistributedTransaction transaction;
  private static final List<String> DISALLOWED_NAMESPACES =
      Arrays.asList(
          "system",
          "system_schema",
          "system_auth",
          "system_distributed",
          "system_traces",
          "scalar",
          "coordinator");

  public ScalarMutableDatabase(DistributedTransaction transaction) {
    this.transaction = transaction;
  }

  @Override
  public Optional<Result> get(Get get) {
    validateNamespace(get);
    try {
      return transaction.get(get);
    } catch (IllegalArgumentException e) {
      throw new InvalidFunctionException(
          CommonLedgerError.OPERATION_FAILED_DUE_TO_ILLEGAL_ARGUMENT, e, e.getMessage());
    } catch (CrudConflictException e) {
      throw new ConflictException(
          CommonLedgerError.OPERATION_FAILED_DUE_TO_CONFLICT, e, e.getMessage());
    } catch (CrudException e) {
      throw new DatabaseException(
          LedgerError.OPERATION_FAILED_DUE_TO_DATABASE_ERROR, e, e.getMessage());
    }
  }

  @Override
  public List<Result> scan(Scan scan) {
    validateNamespace(scan);
    try {
      return transaction.scan(scan);
    } catch (IllegalArgumentException e) {
      throw new InvalidFunctionException(
          CommonLedgerError.OPERATION_FAILED_DUE_TO_ILLEGAL_ARGUMENT, e, e.getMessage());
    } catch (CrudConflictException e) {
      throw new ConflictException(
          CommonLedgerError.OPERATION_FAILED_DUE_TO_CONFLICT, e, e.getMessage());
    } catch (CrudException e) {
      throw new DatabaseException(
          LedgerError.OPERATION_FAILED_DUE_TO_DATABASE_ERROR, e, e.getMessage());
    }
  }

  @Override
  public void put(Put put) {
    validateNamespace(put);
    try {
      // Make put() consistent with Ledger's put(), which always pre-read implicitly.
      transaction.put(Put.newBuilder(put).implicitPreReadEnabled(true).build());
    } catch (IllegalArgumentException e) {
      throw new InvalidFunctionException(
          CommonLedgerError.OPERATION_FAILED_DUE_TO_ILLEGAL_ARGUMENT, e, e.getMessage());
    } catch (CrudConflictException e) {
      throw new ConflictException(
          CommonLedgerError.OPERATION_FAILED_DUE_TO_CONFLICT, e, e.getMessage());
    } catch (CrudException e) {
      throw new DatabaseException(
          LedgerError.OPERATION_FAILED_DUE_TO_DATABASE_ERROR, e, e.getMessage());
    }
  }

  @Override
  public void delete(Delete delete) {
    validateNamespace(delete);
    try {
      transaction.delete(delete);
    } catch (IllegalArgumentException e) {
      throw new InvalidFunctionException(
          CommonLedgerError.OPERATION_FAILED_DUE_TO_ILLEGAL_ARGUMENT, e, e.getMessage());
    } catch (CrudConflictException e) {
      throw new ConflictException(
          CommonLedgerError.OPERATION_FAILED_DUE_TO_CONFLICT, e, e.getMessage());
    } catch (CrudException e) {
      throw new DatabaseException(
          LedgerError.OPERATION_FAILED_DUE_TO_DATABASE_ERROR, e, e.getMessage());
    }
  }

  private void validateNamespace(Operation operation) {
    if (!operation.forNamespace().isPresent()) {
      return;
    }
    String namespace = operation.forNamespace().get();

    DISALLOWED_NAMESPACES.forEach(
        n -> {
          if (n.equalsIgnoreCase(namespace)) {
            throw new InvalidFunctionException(
                LedgerError.FUNCTION_IS_NOT_ALLOWED_TO_ACCESS_SPECIFIED_NAMESPACE);
          }
        });
  }
}
