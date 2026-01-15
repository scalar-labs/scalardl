package com.scalar.dl.ledger.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.dl.ledger.statemachine.Ledger;
import com.scalar.dl.ledger.util.JacksonSerDe;
import javax.annotation.Nullable;

/**
 * A base contract using Jackson for the Ledger data, invoke method arguments, and invoke method
 * return type. You can create your contracts based on it. It is recommended to use this class in
 * most cases to balance development productivity and performance well.
 *
 * @author Hiroyuki Yamada
 */
public abstract class JacksonBasedContract extends ContractBase<JsonNode> {
  private static final JacksonSerDe serde = new JacksonSerDe(new ObjectMapper());

  @Override
  JsonNode deserialize(String string) {
    return serde.deserialize(string);
  }

  @Nullable
  @Override
  String invokeRoot(Ledger<JsonNode> ledger, String argument, String properties) {
    JsonNode jsonProperties = properties == null ? null : serde.deserialize(properties);
    JsonNode result = invoke(ledger, serde.deserialize(argument), jsonProperties);
    return result == null ? null : serde.serialize(result);
  }

  protected ObjectMapper getObjectMapper() {
    return serde.getObjectMapper();
  }
}
