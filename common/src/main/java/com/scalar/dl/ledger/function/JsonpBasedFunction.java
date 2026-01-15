package com.scalar.dl.ledger.function;

import com.scalar.db.api.Delete;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.dl.ledger.database.Database;
import com.scalar.dl.ledger.util.JsonpSerDe;
import javax.annotation.Nullable;
import javax.json.JsonObject;

/**
 * A base function using JSONP for the invoke method arguments and invoke method return type. You
 * can create your functions based on it. Unless you have a special reason to use this class like
 * migrating from the old {@link Function} without changing the code much, it is recommended to use
 * {@link JacksonBasedFunction} since it is faster.
 *
 * @author Hiroyuki Yamada
 */
public abstract class JsonpBasedFunction
    extends FunctionBase<JsonObject, Get, Scan, Put, Delete, Result> {
  private static final JsonpSerDe serde = new JsonpSerDe();

  @Override
  final void setContractProperties(@Nullable String contractProperties) {
    this.contractProperties =
        contractProperties == null ? null : serde.deserialize(contractProperties);
  }

  @Override
  @Nullable
  final String invokeRoot(
      Database<Get, Scan, Put, Delete, Result> database,
      @Nullable String functionArgument,
      String contractArgument) {
    JsonObject jsonFunctionArgument =
        functionArgument == null ? null : serde.deserialize(functionArgument);
    JsonObject result =
        invoke(
            database,
            jsonFunctionArgument,
            serde.deserialize(contractArgument),
            getContractProperties());
    return result == null ? null : serde.serialize(result);
  }
}
