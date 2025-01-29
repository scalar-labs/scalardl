package com.scalar.dl.ledger.function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.db.api.Delete;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.dl.ledger.database.Database;
import com.scalar.dl.ledger.util.JacksonSerDe;
import javax.annotation.Nullable;

/**
 * A base function using Jackson for the invoke method arguments and invoke method return type. You
 * can create your functions based on it. It is recommended to use this class in most cases to
 * balance development productivity and performance well.
 *
 * @author Hiroyuki Yamada
 */
public abstract class JacksonBasedFunction
    extends FunctionBase<JsonNode, Get, Scan, Put, Delete, Result> {
  private static final JacksonSerDe serde = new JacksonSerDe(new ObjectMapper());

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
    JsonNode jsonFunctionArgument =
        functionArgument == null ? null : serde.deserialize(functionArgument);
    JsonNode result =
        invoke(
            database,
            jsonFunctionArgument,
            serde.deserialize(contractArgument),
            getContractProperties());
    return result == null ? null : serde.serialize(result);
  }

  protected ObjectMapper getObjectMapper() {
    return serde.getObjectMapper();
  }
}
