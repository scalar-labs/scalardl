package com.scalar.dl.client.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scalar.dl.client.error.ClientError;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.ledger.proof.AssetProof;
import com.scalar.dl.ledger.service.StatusCode;
import java.util.Base64;
import javax.annotation.Nullable;

public class Common {
  private static final ObjectMapper mapper =
      new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
  static String STATUS_CODE_KEY = "status_code";
  static String OUTPUT_KEY = "output";
  static String ERROR_MESSAGE_KEY = "error_message";

  static final String SCALARDL_GC_COMMAND_NAME = "scalardl-gc";
  static final String SCALARDL_GC_SUBCOMMAND_NAME = "generic-contracts";
  static final String SCALARDL_GC_ALIAS = "gc";

  public static JsonNode getValidationResult(LedgerValidationResult result) {
    ObjectNode json =
        mapper.createObjectNode().put(Common.STATUS_CODE_KEY, result.getCode().toString());
    json.set("Ledger", getProof(result.getLedgerProof().orElse(null)));
    json.set("Auditor", getProof(result.getAuditorProof().orElse(null)));
    return json;
  }

  private static JsonNode getProof(@Nullable AssetProof proof) {
    if (proof == null) {
      return null;
    }

    return mapper
        .createObjectNode()
        .put("namespace", proof.getNamespace())
        .put("id", proof.getId())
        .put("age", proof.getAge())
        .put("nonce", proof.getNonce())
        .put("hash", Base64.getEncoder().encodeToString(proof.getHash()))
        .put("signature", Base64.getEncoder().encodeToString(proof.getSignature()));
  }

  public static void printOutput(@Nullable JsonNode value) {
    JsonNode json =
        mapper
            .createObjectNode()
            .put(Common.STATUS_CODE_KEY, StatusCode.OK.toString())
            .set(Common.OUTPUT_KEY, value);
    printJson(json);
  }

  public static void printError(ClientException e) {
    JsonNode json =
        mapper
            .createObjectNode()
            .put(Common.STATUS_CODE_KEY, e.getStatusCode().toString())
            .put(Common.ERROR_MESSAGE_KEY, e.getMessage());
    printJson(json);
  }

  public static void printJson(JsonNode json) {
    try {
      System.out.println(mapper.writeValueAsString(json));
    } catch (JsonProcessingException e) {
      throw new ClientException(ClientError.PROCESSING_JSON_FAILED, e, e.getMessage());
    }
  }
}
