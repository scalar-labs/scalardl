package com.scalar.dl.ledger.database.scalardb;

import com.scalar.db.api.Delete;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.Get;
import com.scalar.db.api.Operation;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.db.exception.transaction.CrudException;
import com.scalar.dl.ledger.database.MutableDatabase;
import com.scalar.dl.ledger.exception.DatabaseException;
import com.scalar.dl.ledger.exception.InvalidFunctionException;
import com.scalar.dl.ledger.service.StatusCode;
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
    } catch (CrudException e) {
      throw new DatabaseException(e.getMessage(), e, StatusCode.DATABASE_ERROR);
    }
  }

  @Override
  public List<Result> scan(Scan scan) {
    validateNamespace(scan);
    try {
      return transaction.scan(scan);
    } catch (CrudException e) {
      throw new DatabaseException(e.getMessage(), e, StatusCode.DATABASE_ERROR);
    }
  }

  @Override
  public void put(Put put) {
    validateNamespace(put);
    try {
      transaction.put(put);
    } catch (CrudException e) {
      throw new DatabaseException("put failed", e, StatusCode.DATABASE_ERROR);
    }
  }

  @Override
  public void delete(Delete delete) {
    validateNamespace(delete);
    try {
      transaction.delete(delete);
    } catch (CrudException e) {
      throw new DatabaseException("delete failed", e, StatusCode.DATABASE_ERROR);
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
            throw new InvalidFunctionException("Function is not allowed to access the namespace.");
          }
        });
  }
}
