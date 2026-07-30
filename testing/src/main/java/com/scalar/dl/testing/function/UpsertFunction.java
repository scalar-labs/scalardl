package com.scalar.dl.testing.function;

import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.io.Key;
import com.scalar.dl.ledger.database.Database;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.function.Function;
import com.scalar.dl.testing.contract.Constants;
import com.scalar.dl.testing.schema.SchemaConstants;
import java.util.Optional;
import javax.json.JsonObject;

/**
 * A function that reads the current balance of the specified ID and writes the sum of it and the
 * given balance. Unlike {@link CreateFunction}, it issues a read; the JDBC reconnection test relies
 * on this because a put alone is buffered until commit under Consensus Commit and does not access
 * the database from the function's context.
 */
public class UpsertFunction extends Function {

  @Override
  @SuppressWarnings("unchecked")
  public void invoke(
      Database database,
      Optional<JsonObject> functionArgument,
      JsonObject contractArgument,
      Optional<JsonObject> contractProperties) {
    if (!functionArgument.isPresent()
        || !functionArgument.get().containsKey(Constants.ID_ATTRIBUTE_NAME)
        || !functionArgument.get().containsKey(Constants.BALANCE_ATTRIBUTE_NAME)) {
      throw new ContractContextException("improper function argument");
    }

    String id = functionArgument.get().getString(Constants.ID_ATTRIBUTE_NAME);
    int balance = functionArgument.get().getInt(Constants.BALANCE_ATTRIBUTE_NAME);
    String namespace =
        functionArgument.get().containsKey(Constants.NAMESPACE_ATTRIBUTE_NAME)
            ? functionArgument.get().getString(Constants.NAMESPACE_ATTRIBUTE_NAME)
            : SchemaConstants.FUNCTION_NAMESPACE;

    Get get =
        new Get(new Key(Constants.ID_ATTRIBUTE_NAME, id))
            .forNamespace(namespace)
            .forTable(SchemaConstants.FUNCTION_TABLE);
    Optional<Result> existing = database.get(get);
    int newBalance =
        existing.map(r -> r.getInt(Constants.BALANCE_ATTRIBUTE_NAME)).orElse(0) + balance;

    Put put =
        new Put(new Key(Constants.ID_ATTRIBUTE_NAME, id))
            .withValue(Constants.BALANCE_ATTRIBUTE_NAME, newBalance)
            .forNamespace(namespace)
            .forTable(SchemaConstants.FUNCTION_TABLE);
    database.put(put);
  }
}
