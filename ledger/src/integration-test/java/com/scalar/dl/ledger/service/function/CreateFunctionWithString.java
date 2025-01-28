package com.scalar.dl.ledger.service.function;

import com.google.common.base.Splitter;
import com.scalar.db.api.Delete;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.db.io.Key;
import com.scalar.dl.ledger.database.Database;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.function.StringBasedFunction;
import com.scalar.dl.ledger.service.Constants;
import java.util.List;
import javax.annotation.Nullable;

public class CreateFunctionWithString extends StringBasedFunction {

  @Nullable
  @Override
  public String invoke(
      Database<Get, Scan, Put, Delete, Result> database,
      @Nullable String functionArgument,
      String contractArgument,
      @Nullable String contractProperties) {
    if (functionArgument == null) {
      throw new ContractContextException("improper function argument");
    }
    // <id>,<balance>
    List<String> elements = Splitter.on(',').splitToList(functionArgument);
    if (elements.size() != 2 && elements.size() != 3) {
      throw new ContractContextException("invalid argument format");
    }
    String id = elements.get(0);
    int balance = Integer.parseInt(elements.get(1));
    String namespace = elements.size() == 3 ? elements.get(2) : Constants.FUNCTION_NAMESPACE;

    Put put =
        new Put(new Key(Constants.ID_ATTRIBUTE_NAME, id))
            .withValue(Constants.BALANCE_ATTRIBUTE_NAME, balance)
            .forNamespace(namespace)
            .forTable(Constants.FUNCTION_TABLE);

    database.put(put);

    return null;
  }
}
