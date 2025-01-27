package com.scalar.dl.ledger.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A helper class that formats and extracts from a contract argument. The contract argument passed
 * to Ledger and Auditor has the following format depending on the version:
 *
 * <p>{@literal V1 format: "{"nonce":"<nonce>","_function_ids_":['function_id1', ...],<contract
 * arguments in JSON>}"}
 *
 * <p>{@literal V2 format: "V2\u0001<nonce>\u0003<optional function IDs ('\u0002'
 * separated)>\u0003<contract arguments in a serialized JSON string format or a user-defined string
 * format>"}
 */
public class Argument {
  private static final Logger LOGGER = LoggerFactory.getLogger(Argument.class.getName());
  private static final JsonpSerDe jsonpSerDe = new JsonpSerDe();
  private static final JacksonSerDe jacksonSerDe = new JacksonSerDe(new ObjectMapper());
  private static final String ARGUMENT_VERSION_PREFIX = "V";
  private static final String ARGUMENT_FORMAT_VERSION = "2";
  private static final char NONCE_SEPARATOR = '\u0001';
  private static final char FUNCTION_SEPARATOR = '\u0002';
  private static final char ARGUMENT_SEPARATOR = '\u0003';
  public static final String NONCE_KEY_NAME = "nonce";
  public static final String FUNCTIONS_KEY = "_functions_";

  public static String format(Object argument, String nonce, List<String> functionIds) {
    String prefix =
        ARGUMENT_VERSION_PREFIX
            + ARGUMENT_FORMAT_VERSION
            + NONCE_SEPARATOR
            + nonce
            + ARGUMENT_SEPARATOR
            + Joiner.on(FUNCTION_SEPARATOR).skipNulls().join(functionIds)
            + ARGUMENT_SEPARATOR;
    if (argument instanceof String) {
      return prefix + argument;
    } else if (argument instanceof JsonObject) {
      return prefix + jsonpSerDe.serialize((JsonObject) argument);
    } else if (argument instanceof JsonNode) {
      return prefix + jacksonSerDe.serialize((JsonNode) argument);
    } else {
      throw new IllegalArgumentException("unsupported type " + argument.getClass() + " specified.");
    }
  }

  @Deprecated
  public static JsonObject format(JsonObject argument, String nonce) {
    if (!argument.containsKey(NONCE_KEY_NAME) || argument.getString(NONCE_KEY_NAME).isEmpty()) {
      return Json.createObjectBuilder(argument).add(NONCE_KEY_NAME, nonce).build();
    }
    return argument;
  }

  public static String getNonce(String argument) {
    if (argument.indexOf(ARGUMENT_VERSION_PREFIX) == 0) {
      List<String> elements = getElements(argument);
      List<String> versionNonce = Splitter.on(NONCE_SEPARATOR).splitToList(elements.get(0));
      if (versionNonce.size() != 2) {
        throw new IllegalArgumentException("the argument format looks illegal");
      }
      return versionNonce.get(1);
    } else {
      // for backward compatibility
      return jacksonSerDe.deserialize(argument).get(NONCE_KEY_NAME).asText();
    }
  }

  @Deprecated
  public static List<String> getFunctionIds(String argument) {
    if (!argument.contains(FUNCTIONS_KEY)) {
      return Collections.emptyList();
    }
    String contractArgument = getContractArgument(argument);
    return getFunctionIds(jsonpSerDe.deserialize(contractArgument));
  }

  @Deprecated
  public static List<String> getFunctionIds(JsonObject argument) {
    if (!argument.containsKey(FUNCTIONS_KEY)) {
      return Collections.emptyList();
    }
    JsonArray array = argument.getJsonArray(FUNCTIONS_KEY);
    List<String> functionIds =
        array.stream().map(id -> ((JsonString) id).getString()).collect(Collectors.toList());
    LOGGER.warn(
        "Specifying an array of function IDs in a contract argument is deprecated."
            + "The feature is removed in release 5.0.0.");
    return functionIds;
  }

  public static String getContractArgument(String argument) {
    if (argument.indexOf(ARGUMENT_VERSION_PREFIX) == 0) {
      List<String> elements = getElements(argument);
      return elements.get(elements.size() - 1);
    } else {
      // for backward compatibility
      return argument;
    }
  }

  private static List<String> getElements(String argument) {
    List<String> elements = Splitter.on(ARGUMENT_SEPARATOR).splitToList(argument);
    if (elements.size() != 2 && elements.size() != 3) {
      throw new IllegalArgumentException("the argument format looks illegal");
    }
    return elements;
  }
}
