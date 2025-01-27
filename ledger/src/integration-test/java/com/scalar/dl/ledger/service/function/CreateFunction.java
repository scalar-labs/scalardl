package com.scalar.dl.ledger.service.function;

import com.scalar.db.api.Put;
import com.scalar.db.io.Key;
import com.scalar.dl.ledger.database.Database;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.function.Function;
import com.scalar.dl.ledger.service.Constants;
import java.util.Optional;
import javax.json.JsonObject;

public class CreateFunction extends Function {

  @Override
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

    Put put =
        new Put(new Key(Constants.ID_ATTRIBUTE_NAME, id))
            .withValue(Constants.BALANCE_ATTRIBUTE_NAME, balance)
            .forNamespace(Constants.FUNCTION_NAMESPACE)
            .forTable(Constants.FUNCTION_TABLE);

    database.put(put);
  }
}
