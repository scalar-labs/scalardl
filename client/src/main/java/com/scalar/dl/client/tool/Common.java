package com.scalar.dl.client.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.scalar.dl.client.error.ClientError;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.ledger.service.StatusCode;
import javax.annotation.Nullable;

public class Common {
  private static final ObjectMapper mapper =
      new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
  static String STATUS_CODE_KEY = "status_code";
  static String OUTPUT_KEY = "output";
  static String ERROR_MESSAGE_KEY = "error_message";

  static void printOutput(@Nullable JsonNode value) {
    JsonNode json =
        mapper
            .createObjectNode()
            .put(Common.STATUS_CODE_KEY, StatusCode.OK.toString())
            .set(Common.OUTPUT_KEY, value);
    printJson(json);
  }

  static void printError(ClientException e) {
    JsonNode json =
        mapper
            .createObjectNode()
            .put(Common.STATUS_CODE_KEY, e.getStatusCode().toString())
            .put(Common.ERROR_MESSAGE_KEY, e.getMessage());
    printJson(json);
  }

  static void printJson(JsonNode json) {
    try {
      System.out.println(mapper.writeValueAsString(json));
    } catch (JsonProcessingException e) {
      throw new ClientException(ClientError.PROCESSING_JSON_FAILED, e, e.getMessage());
    }
  }
}
