package com.scalar.dl.ledger.util;

import java.io.StringReader;
import javax.annotation.concurrent.Immutable;
import javax.json.Json;
import javax.json.JsonObject;

@Immutable
public final class JsonpSerDe implements JsonSerDe<JsonObject> {

  @Override
  public String serialize(JsonObject json) {
    return json.toString();
  }

  @Override
  public JsonObject deserialize(String jsonString) {
    return Json.createReader(new StringReader(jsonString)).readObject();
  }
}
