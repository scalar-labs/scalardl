package com.scalar.dl.ledger.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.dl.ledger.exception.InvalidJsonException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javax.annotation.concurrent.Immutable;

@Immutable
public class JacksonSerDe implements JsonSerDe<JsonNode> {
  private final ObjectMapper mapper;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public JacksonSerDe(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public String serialize(JsonNode json) {
    try {
      return mapper.writeValueAsString(json);
    } catch (JsonProcessingException e) {
      throw new InvalidJsonException("can't serialize the specified json", e);
    }
  }

  @Override
  public JsonNode deserialize(String jsonString) {
    try {
      return mapper.readTree(jsonString);
    } catch (JsonProcessingException e) {
      throw new InvalidJsonException("can't deserialize the specified json string", e);
    }
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public ObjectMapper getObjectMapper() {
    return mapper;
  }
}
