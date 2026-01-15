package com.scalar.dl.ledger.contract;

import com.scalar.dl.ledger.statemachine.Ledger;
import com.scalar.dl.ledger.util.JsonpSerDe;
import javax.annotation.Nullable;
import javax.json.JsonObject;

/**
 * A base contract using JSONP for the Ledger data, invoke method arguments, and invoke method
 * return type. You can create your contracts based on it. Unless you have a special reason to use
 * this class like migrating from the old {@link Contract} without changing the code much, it is
 * recommended to use {@link JacksonBasedContract} since it is faster.
 *
 * @author Hiroyuki Yamada
 */
public abstract class JsonpBasedContract extends ContractBase<JsonObject> {
  private static final JsonpSerDe serde = new JsonpSerDe();

  @Override
  JsonObject deserialize(String string) {
    return serde.deserialize(string);
  }

  @Override
  @Nullable
  final String invokeRoot(Ledger<JsonObject> ledger, String argument, String properties) {
    JsonObject jsonProperties = properties == null ? null : serde.deserialize(properties);
    JsonObject result = invoke(ledger, serde.deserialize(argument), jsonProperties);
    return result == null ? null : serde.serialize(result);
  }
}
