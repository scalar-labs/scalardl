package com.scalar.dl.testing.contract;

import com.scalar.dl.ledger.contract.Contract;
import com.scalar.dl.ledger.database.Ledger;
import java.util.Optional;
import javax.json.JsonObject;

/**
 * A contract that only invokes the contract specified in the argument (nested invocation) and
 * returns its result. It deliberately performs no ledger access of its own, so that the nested
 * contract's registry lookup is the first registry-side database access of the execution when this
 * contract's own entry is already cached.
 */
public class NestedInvoker extends Contract {

  @Override
  public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> properties) {
    String contractId = argument.getString(Constants.CONTRACT_ID_ATTRIBUTE_NAME);
    return invoke(contractId, ledger, argument);
  }
}
