package com.scalar.dl.ledger.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.scalar.dl.ledger.error.CommonError;
import com.scalar.dl.ledger.namespace.Namespaces;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
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
 *
 * <p>{@literal V3 format: "V3\u0001<nonce>\u0003<context_namespace>\u0003<optional function IDs
 * ('\u0002' separated)>\u0003<contract arguments in a serialized JSON string format or a
 * user-defined string format>"}
 */
public class Argument {
  private static final Logger LOGGER = LoggerFactory.getLogger(Argument.class.getName());
  private static final JsonpSerDe jsonpSerDe = new JsonpSerDe();
  private static final JacksonSerDe jacksonSerDe = new JacksonSerDe(new ObjectMapper());
  private static final String ARGUMENT_VERSION_PREFIX = "V";
  private static final char NONCE_SEPARATOR = '\u0001';
  private static final char FUNCTION_SEPARATOR = '\u0002';
  private static final char ARGUMENT_SEPARATOR = '\u0003';
  public static final String NONCE_KEY_NAME = "nonce";
  public static final String FUNCTIONS_KEY = "_functions_";

  /**
   * Argument format version.
   *
   * <ul>
   *   <li>V1: JSON embedded format (legacy)
   *   <li>V2: V2 + NONCE_SEP + nonce + ARG_SEP + funcIds + ARG_SEP + arg
   *   <li>V3: V3 + NONCE_SEP + nonce + ARG_SEP + namespace + ARG_SEP + funcIds + ARG_SEP + arg
   * </ul>
   */
  private enum Version {
    V1,
    V2,
    V3
  }

  /**
   * Returns the version of the argument format.
   *
   * @param argument the formatted argument string
   * @return the version
   * @throws IllegalArgumentException if the argument format is invalid
   */
  private static Version getVersion(String argument) {
    if (!argument.startsWith(ARGUMENT_VERSION_PREFIX)) {
      return Version.V1;
    }
    int separatorIndex = argument.indexOf(NONCE_SEPARATOR);
    if (separatorIndex < 2) {
      throw new IllegalArgumentException(CommonError.ILLEGAL_ARGUMENT_FORMAT.buildMessage());
    }
    String versionStr = argument.substring(1, separatorIndex);
    switch (versionStr) {
      case "2":
        return Version.V2;
      case "3":
        return Version.V3;
      default:
        throw new IllegalArgumentException(CommonError.ILLEGAL_ARGUMENT_FORMAT.buildMessage());
    }
  }

  /**
   * Splits the argument into elements and validates the element count based on the version.
   *
   * @param argument the formatted argument string
   * @return the list of elements
   * @throws IllegalArgumentException if the element count is invalid for the version
   */
  private static List<String> getElements(String argument) {
    Version version = getVersion(argument);
    List<String> elements = Splitter.on(ARGUMENT_SEPARATOR).splitToList(argument);
    int expectedSize = (version == Version.V3) ? 4 : 3;
    if (elements.size() != expectedSize) {
      throw new IllegalArgumentException(CommonError.ILLEGAL_ARGUMENT_FORMAT.buildMessage());
    }
    return elements;
  }

  /**
   * Formats the argument in V3 format with context namespace.
   *
   * @param argument the contract argument
   * @param nonce a unique ID of the execution request
   * @param contextNamespace the context namespace (uses {@link Namespaces#DEFAULT} if null)
   * @param functionIds the list of function IDs
   * @return the formatted argument string
   */
  public static String format(
      Object argument, String nonce, @Nullable String contextNamespace, List<String> functionIds) {
    String namespace = contextNamespace == null ? Namespaces.DEFAULT : contextNamespace;
    String prefix =
        ARGUMENT_VERSION_PREFIX
            + "3"
            + NONCE_SEPARATOR
            + nonce
            + ARGUMENT_SEPARATOR
            + namespace
            + ARGUMENT_SEPARATOR
            + Joiner.on(FUNCTION_SEPARATOR).skipNulls().join(functionIds)
            + ARGUMENT_SEPARATOR;
    return formatWithPrefix(prefix, argument);
  }

  private static String formatWithPrefix(String prefix, Object argument) {
    if (argument instanceof String) {
      return prefix + argument;
    } else if (argument instanceof JsonObject) {
      return prefix + jsonpSerDe.serialize((JsonObject) argument);
    } else if (argument instanceof JsonNode) {
      return prefix + jacksonSerDe.serialize((JsonNode) argument);
    } else {
      throw new IllegalArgumentException(
          CommonError.UNSUPPORTED_DESERIALIZATION_TYPE.buildMessage(argument.getClass()));
    }
  }

  /**
   * Formats the argument in V3 format with {@link Namespaces#DEFAULT} as the context namespace.
   * This method is kept for testing purposes only.
   *
   * @param argument the contract argument
   * @param nonce a unique ID of the execution request
   * @param functionIds the list of function IDs
   * @return the formatted argument string
   * @deprecated Use {@link #format(Object, String, String, List)} instead. This method is kept for
   *     testing purposes only.
   */
  @Deprecated
  public static String format(Object argument, String nonce, List<String> functionIds) {
    return format(argument, nonce, null, functionIds);
  }

  /**
   * Formats the argument in V1 format by adding nonce to the JSON object. This method is kept for
   * testing purposes only.
   *
   * @param argument the contract argument as a JSON object
   * @param nonce a unique ID of the execution request
   * @return the JSON object with nonce added
   * @deprecated This method produces V1 format which is legacy. This method is kept for testing
   *     purposes only.
   */
  @Deprecated
  public static JsonObject format(JsonObject argument, String nonce) {
    if (!argument.containsKey(NONCE_KEY_NAME) || argument.getString(NONCE_KEY_NAME).isEmpty()) {
      return Json.createObjectBuilder(argument).add(NONCE_KEY_NAME, nonce).build();
    }
    return argument;
  }

  /**
   * Returns the nonce from the argument.
   *
   * @param argument the formatted argument string
   * @return the nonce
   * @throws IllegalArgumentException if the argument format is invalid
   */
  public static String getNonce(String argument) {
    switch (getVersion(argument)) {
      case V1:
        return jacksonSerDe.deserialize(argument).get(NONCE_KEY_NAME).asText();
      case V2:
      case V3:
        List<String> elements = getElements(argument);
        List<String> versionNonce = Splitter.on(NONCE_SEPARATOR).splitToList(elements.get(0));
        if (versionNonce.size() != 2) {
          throw new IllegalArgumentException(CommonError.ILLEGAL_ARGUMENT_FORMAT.buildMessage());
        }
        return versionNonce.get(1);
      default:
        throw new IllegalArgumentException(CommonError.ILLEGAL_ARGUMENT_FORMAT.buildMessage());
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

  /**
   * Returns the contract argument from the formatted argument.
   *
   * @param argument the formatted argument string
   * @return the contract argument (the last element)
   */
  public static String getContractArgument(String argument) {
    switch (getVersion(argument)) {
      case V1:
        return argument;
      case V2:
      case V3:
        List<String> elements = getElements(argument);
        return elements.get(elements.size() - 1);
      default:
        throw new IllegalArgumentException(CommonError.ILLEGAL_ARGUMENT_FORMAT.buildMessage());
    }
  }

  /**
   * Returns the context namespace from the argument. For V1 and V2 formats, returns {@link
   * Namespaces#DEFAULT}.
   *
   * @param argument the formatted argument string
   * @return the context namespace (never null)
   */
  public static String getContextNamespace(String argument) {
    switch (getVersion(argument)) {
      case V1:
      case V2:
        return Namespaces.DEFAULT;
      case V3:
        List<String> elements = getElements(argument);
        return elements.get(1);
      default:
        throw new IllegalArgumentException(CommonError.ILLEGAL_ARGUMENT_FORMAT.buildMessage());
    }
  }
}
