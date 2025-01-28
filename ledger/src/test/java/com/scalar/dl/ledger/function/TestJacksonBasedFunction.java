package com.scalar.dl.ledger.function;

import com.fasterxml.jackson.databind.JsonNode;
import com.scalar.db.api.Delete;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.dl.ledger.database.Database;
import javax.annotation.Nullable;

public class TestJacksonBasedFunction extends JacksonBasedFunction {

  @Nullable
  @Override
  public JsonNode invoke(
      Database<Get, Scan, Put, Delete, Result> database,
      @Nullable JsonNode functionArgument,
      JsonNode contractArgument,
      @Nullable JsonNode properties) {
    return null;
  }
}
