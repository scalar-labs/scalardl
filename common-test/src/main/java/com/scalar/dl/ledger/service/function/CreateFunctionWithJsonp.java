package com.scalar.dl.ledger.service.function;

import com.scalar.db.api.Delete;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.db.io.Key;
import com.scalar.dl.ledger.database.Database;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.function.JsonpBasedFunction;
import com.scalar.dl.ledger.service.Constants;
import javax.annotation.Nullable;
import javax.json.JsonObject;

public class CreateFunctionWithJsonp extends JsonpBasedFunction {

  @Nullable
  @Override
  public JsonObject invoke(
      Database<Get, Scan, Put, Delete, Result> database,
      @Nullable JsonObject functionArgument,
      JsonObject contractArgument,
      @Nullable JsonObject contractProperties) {
    if (functionArgument == null
        || !functionArgument.containsKey(Constants.ID_ATTRIBUTE_NAME)
        || !functionArgument.containsKey(Constants.BALANCE_ATTRIBUTE_NAME)) {
      throw new ContractContextException("improper function argument");
    }

    String id = functionArgument.getString(Constants.ID_ATTRIBUTE_NAME);
    int balance = functionArgument.getInt(Constants.BALANCE_ATTRIBUTE_NAME);
    String namespace =
        functionArgument.containsKey(Constants.NAMESPACE_ATTRIBUTE_NAME)
            ? functionArgument.getString(Constants.NAMESPACE_ATTRIBUTE_NAME)
            : Constants.FUNCTION_NAMESPACE;

    Put put =
        new Put(new Key(Constants.ID_ATTRIBUTE_NAME, id))
            .withValue(Constants.BALANCE_ATTRIBUTE_NAME, balance)
            .forNamespace(namespace)
            .forTable(Constants.FUNCTION_TABLE);

    database.put(put);

    return null;
  }
}
