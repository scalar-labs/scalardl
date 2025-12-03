package com.scalar.dl.ledger.database.scalardb;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.scalar.db.api.Consistency;
import com.scalar.db.api.Delete;
import com.scalar.db.api.DistributedStorage;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.TableMetadata;
import com.scalar.db.exception.storage.ExecutionException;
import com.scalar.db.io.DataType;
import com.scalar.db.io.Key;
import com.scalar.dl.ledger.database.FunctionRegistry;
import com.scalar.dl.ledger.error.CommonError;
import com.scalar.dl.ledger.error.LedgerError;
import com.scalar.dl.ledger.exception.DatabaseException;
import com.scalar.dl.ledger.exception.UnexpectedValueException;
import com.scalar.dl.ledger.function.FunctionEntry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Optional;

public class ScalarFunctionRegistry implements FunctionRegistry, TableMetadataProvider {
  static final String FUNCTION_TABLE = "function";
  private static final TableMetadata FUNCTION_TABLE_METADATA =
      TableMetadata.newBuilder()
          .addColumn(FunctionEntry.ID, DataType.TEXT)
          .addColumn(FunctionEntry.BINARY_NAME, DataType.TEXT)
          .addColumn(FunctionEntry.BYTE_CODE, DataType.BLOB)
          .addColumn(FunctionEntry.REGISTERED_AT, DataType.BIGINT)
          .addPartitionKey(FunctionEntry.ID)
          .build();
  private final DistributedStorage storage;

  @Inject
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public ScalarFunctionRegistry(DistributedStorage storage) {
    this.storage = storage;
  }

  @Override
  public Map<String, TableMetadata> getStorageTables() {
    return ImmutableMap.of(FUNCTION_TABLE, FUNCTION_TABLE_METADATA);
  }

  @Override
  public void bind(FunctionEntry entry) {
    long currentTime = System.currentTimeMillis();

    ScalarFunctionEntry wrapped = new ScalarFunctionEntry(entry);
    Put put =
        new Put(new Key(wrapped.getIdValue()))
            .withValue(wrapped.getBinaryNameValue())
            .withValue(wrapped.getByteCodeValue())
            .withValue(FunctionEntry.REGISTERED_AT, currentTime)
            .withConsistency(Consistency.SEQUENTIAL)
            .forTable(FUNCTION_TABLE);

    try {
      storage.put(put);
    } catch (ExecutionException e) {
      throw new DatabaseException(LedgerError.BINDING_FUNCTION_FAILED, e, e.getMessage());
    }
  }

  @Override
  public void unbind(String id) {
    Delete delete =
        new Delete(new Key(FunctionEntry.ID, id))
            .withConsistency(Consistency.SEQUENTIAL)
            .forTable(FUNCTION_TABLE);

    try {
      storage.delete(delete);
    } catch (ExecutionException e) {
      throw new DatabaseException(LedgerError.UNBINDING_FUNCTION_FAILED, e, e.getMessage());
    }
  }

  @Override
  public Optional<FunctionEntry> lookup(String id) {
    Get get =
        new Get(new Key(FunctionEntry.ID, id))
            .withConsistency(Consistency.SEQUENTIAL)
            .forTable(FUNCTION_TABLE);

    try {
      return storage.get(get).map(this::toFunctionEntry);
    } catch (ExecutionException e) {
      throw new DatabaseException(LedgerError.GETTING_FUNCTION_FAILED, e, e.getMessage());
    }
  }

  private String getIdFrom(Result result) {
    return result.getValue(FunctionEntry.ID).get().getAsString().get();
  }

  private String getBinaryNameFrom(Result result) {
    return result.getValue(FunctionEntry.BINARY_NAME).get().getAsString().get();
  }

  private byte[] getBytesFrom(Result result) {
    return result.getValue(FunctionEntry.BYTE_CODE).get().getAsBytes().get();
  }

  private long getRegisteredAtFrom(Result result) {
    return result.getValue(FunctionEntry.REGISTERED_AT).get().getAsLong();
  }

  private FunctionEntry toFunctionEntry(Result result) {
    try {
      return new FunctionEntry(
          getIdFrom(result),
          getBinaryNameFrom(result),
          getBytesFrom(result),
          getRegisteredAtFrom(result));
    } catch (Exception e) {
      throw new UnexpectedValueException(
          CommonError.UNEXPECTED_RECORD_VALUE_OBSERVED, e, e.getMessage());
    }
  }
}
