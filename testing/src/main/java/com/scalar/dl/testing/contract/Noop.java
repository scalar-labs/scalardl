package com.scalar.dl.testing.contract;

import com.scalar.dl.ledger.contract.Contract;
import com.scalar.dl.ledger.database.Ledger;
import java.util.Optional;
import javax.json.JsonObject;

/**
 * A contract that performs no ledger access. Pairing it with a function allows a test to make the
 * function's database access the first transaction-side database access of the execution.
 */
public class Noop extends Contract {

  @Override
  public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> properties) {
    return null;
  }
}
