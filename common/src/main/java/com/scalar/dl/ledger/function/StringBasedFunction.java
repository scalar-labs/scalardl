package com.scalar.dl.ledger.function;

import com.scalar.db.api.Delete;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.dl.ledger.database.Database;
import javax.annotation.Nullable;

/**
 * A base function using the internal String representation as it is for the invoke method
 * arguments, and invoke method return type. You can create your functions based on it. It is
 * recommended to use {@link JacksonBasedFunction} in most cases, but you can use this class to
 * achieve faster and more efficient function execution by avoiding JSON serialization and
 * deserialization.
 *
 * @author Hiroyuki Yamada
 */
public abstract class StringBasedFunction
    extends FunctionBase<String, Get, Scan, Put, Delete, Result> {

  @Override
  final void setContractProperties(@Nullable String contractProperties) {
    this.contractProperties = contractProperties;
  }

  @Override
  @Nullable
  final String invokeRoot(
      Database<Get, Scan, Put, Delete, Result> database,
      @Nullable String functionArgument,
      String contractArgument) {
    return invoke(database, functionArgument, contractArgument, getContractProperties());
  }
}
