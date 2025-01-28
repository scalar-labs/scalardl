package com.scalar.dl.ledger.function;

import com.scalar.db.api.Delete;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.dl.ledger.database.Database;
import javax.annotation.Nullable;
import javax.json.JsonObject;

public class TestJsonpBasedFunction extends JsonpBasedFunction {

  @Nullable
  @Override
  public JsonObject invoke(
      Database<Get, Scan, Put, Delete, Result> database,
      @Nullable JsonObject functionArgument,
      JsonObject contractArgument,
      @Nullable JsonObject properties) {
    return null;
  }
}
