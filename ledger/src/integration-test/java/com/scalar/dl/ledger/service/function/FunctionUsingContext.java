package com.scalar.dl.ledger.service.function;

import com.fasterxml.jackson.databind.JsonNode;
import com.scalar.db.api.Delete;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.dl.ledger.database.Database;
import com.scalar.dl.ledger.function.JacksonBasedFunction;
import javax.annotation.Nullable;

public class FunctionUsingContext extends JacksonBasedFunction {

  @Nullable
  @Override
  public JsonNode invoke(
      Database<Get, Scan, Put, Delete, Result> database,
      @Nullable JsonNode functionArgument,
      JsonNode contractArgument,
      @Nullable JsonNode contractProperties) {

    return getContractContext();
  }
}
