package com.scalar.dl.hashstore.client.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scalar.dl.ledger.util.JacksonSerDe;
import com.scalar.dl.ledger.util.JsonpSerDe;
import javax.json.JsonObject;

public class HashStoreClientUtils {
  private static final JsonpSerDe jsonpSerDe = new JsonpSerDe();
  private static final JacksonSerDe jacksonSerDe = new JacksonSerDe(new ObjectMapper());

  private HashStoreClientUtils() {}

  public static ObjectNode createObjectNode() {
    return jacksonSerDe.getObjectMapper().createObjectNode();
  }

  public static ArrayNode createArrayNode() {
    return jacksonSerDe.getObjectMapper().createArrayNode();
  }

  public static JsonNode convertToJsonNode(JsonObject json) {
    return jacksonSerDe.deserialize(jsonpSerDe.serialize(json));
  }

  public static JsonNode convertToJsonNode(String jsonString) {
    return jacksonSerDe.deserialize(jsonString);
  }
}
