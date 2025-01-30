package com.scalar.dl.ledger.service.function;

import com.fasterxml.jackson.databind.JsonNode;
import com.scalar.db.api.Delete;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.db.io.Key;
import com.scalar.dl.ledger.database.Database;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.function.JacksonBasedFunction;
import com.scalar.dl.ledger.service.Constants;
import javax.annotation.Nullable;

public class CreateFunctionWithJackson extends JacksonBasedFunction {

  @Nullable
  @Override
  public JsonNode invoke(
      Database<Get, Scan, Put, Delete, Result> database,
      @Nullable JsonNode functionArgument,
      JsonNode contractArgument,
      @Nullable JsonNode contractProperties) {
    if (functionArgument == null
        || !functionArgument.has(Constants.ID_ATTRIBUTE_NAME)
        || !functionArgument.has(Constants.BALANCE_ATTRIBUTE_NAME)) {
      throw new ContractContextException("improper function argument");
    }

    String id = functionArgument.get(Constants.ID_ATTRIBUTE_NAME).asText();
    int balance = functionArgument.get(Constants.BALANCE_ATTRIBUTE_NAME).asInt();

    Put put =
        new Put(new Key(Constants.ID_ATTRIBUTE_NAME, id))
            .withValue(Constants.BALANCE_ATTRIBUTE_NAME, balance)
            .forNamespace(Constants.FUNCTION_NAMESPACE)
            .forTable(Constants.FUNCTION_TABLE);

    database.put(put);

    return null;
  }
}
