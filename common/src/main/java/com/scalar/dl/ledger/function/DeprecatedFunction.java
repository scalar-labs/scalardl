package com.scalar.dl.ledger.function;

import com.scalar.db.api.Delete;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.dl.ledger.database.Database;
import com.scalar.dl.ledger.util.JsonpSerDe;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.json.JsonObject;

class DeprecatedFunction extends FunctionBase<JsonObject, Get, Scan, Put, Delete, Result> {
  private static final JsonpSerDe serde = new JsonpSerDe();
  private final Function function;

  public DeprecatedFunction(Function function) {
    this.function = function;
  }

  @Override
  final void initialize(FunctionManager manager) {
    function.initialize(manager);
  }

  @Override
  final void setContractProperties(String contractProperties) {
    function.setProperties(
        contractProperties == null ? null : serde.deserialize(contractProperties));
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
            function.getProperties());
    return result == null ? null : serde.serialize(result);
  }

  @Override
  @Nullable
  public JsonObject invoke(
      Database<Get, Scan, Put, Delete, Result> database,
      @Nullable JsonObject functionArgument,
      JsonObject contractArgument,
      @Nullable JsonObject properties) {
    function.invoke(
        database,
        Optional.ofNullable(functionArgument),
        contractArgument,
        Optional.ofNullable(properties));

    // It returns null for forward compatibility
    return null;
  }
}
