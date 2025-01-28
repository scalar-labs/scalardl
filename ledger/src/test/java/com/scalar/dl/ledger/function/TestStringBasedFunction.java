package com.scalar.dl.ledger.function;

import com.scalar.db.api.Delete;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.dl.ledger.database.Database;
import javax.annotation.Nullable;

public class TestStringBasedFunction extends StringBasedFunction {

  @Nullable
  @Override
  public String invoke(
      Database<Get, Scan, Put, Delete, Result> database,
      @Nullable String functionArgument,
      String contractArgument,
      @Nullable String properties) {
    return null;
  }
}
