package com.scalar.dl.genericcontracts.object.v1_0_0;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scalar.dl.genericcontracts.object.Constants;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.AssetFilter.AgeOrder;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import javax.annotation.Nullable;

public class Validate extends JacksonBasedContract {

  @Nullable
  @Override
  public JsonNode invoke(
      Ledger<JsonNode> ledger, JsonNode arguments, @Nullable JsonNode properties) {

    if (!arguments.has(Constants.OBJECT_ID) || !arguments.get(Constants.OBJECT_ID).isTextual()) {
      throw new ContractContextException(Constants.OBJECT_ID_IS_MISSING_OR_INVALID);
    }

    if (!arguments.has(Constants.VERSIONS)) {
      throw new ContractContextException(Constants.VERSIONS_ARE_MISSING);
    }

    List<Map.Entry<String, JsonNode>> versions =
        validateAndGetVersions(arguments.get(Constants.VERSIONS));

    String assetId = getAssetIdForObject(arguments.get(Constants.OBJECT_ID).asText());
    AssetFilter filter;
    if (isAllOptionSpecified(arguments)) {
      filter = new AssetFilter(assetId).withAgeOrder(AgeOrder.DESC);
    } else {
      filter = new AssetFilter(assetId).withLimit(versions.size()).withAgeOrder(AgeOrder.DESC);
    }

    List<Asset<JsonNode>> history = ledger.scan(filter);

    if (history.size() != versions.size()) {
      return getObjectMapper()
          .createObjectNode()
          .put(Constants.STATUS, Constants.STATUS_FAULTY)
          .put(Constants.DETAILS, Constants.DETAILS_NUMBER_OF_VERSIONS_MISMATCH)
          .set(Constants.FAULTY_VERSIONS, getObjectMapper().createArrayNode());
    }

    boolean verbose = isVerboseOptionSpecified(arguments);
    ArrayNode faultyVersions = getObjectMapper().createArrayNode();
    ArrayNode givenVersions = getObjectMapper().createArrayNode();
    IntStream.range(0, history.size())
        .forEach(
            i -> {
              String versionId = versions.get(i).getKey();
              JsonNode storedVersion = history.get(i).data();
              JsonNode givenVersion = versions.get(i).getValue();
              if (isFaultyVersion(storedVersion, givenVersion)) {
                if (verbose) {
                  faultyVersions.add(createFaultyVersion(versionId, storedVersion));
                  givenVersions.add(givenVersion);
                } else {
                  faultyVersions.add(versionId);
                }
              }
            });

    if (!faultyVersions.isEmpty()) {
      ObjectNode result =
          getObjectMapper()
              .createObjectNode()
              .put(Constants.STATUS, Constants.STATUS_FAULTY)
              .put(Constants.DETAILS, Constants.DETAILS_FAULTY_VERSIONS_EXIST)
              .set(Constants.FAULTY_VERSIONS, faultyVersions);
      if (verbose) {
        result.set(Constants.GIVEN_VERSIONS, givenVersions);
      }
      return result;
    }

    return getObjectMapper()
        .createObjectNode()
        .put(Constants.STATUS, Constants.STATUS_CORRECT)
        .put(Constants.DETAILS, Constants.DETAILS_CORRECT_STATUS)
        .set(Constants.FAULTY_VERSIONS, getObjectMapper().createArrayNode());
  }

  private String getAssetIdForObject(String objectId) {
    return Constants.OBJECT_ID_PREFIX + objectId;
  }

  private boolean isAllOptionSpecified(JsonNode arguments) {
    return arguments.has(Constants.OPTIONS)
        && arguments.get(Constants.OPTIONS).isObject()
        && arguments.get(Constants.OPTIONS).has(Constants.OPTION_ALL)
        && arguments.get(Constants.OPTIONS).get(Constants.OPTION_ALL).asBoolean();
  }

  private boolean isVerboseOptionSpecified(JsonNode arguments) {
    return arguments.has(Constants.OPTIONS)
        && arguments.get(Constants.OPTIONS).isObject()
        && arguments.get(Constants.OPTIONS).has(Constants.OPTION_VERBOSE)
        && arguments.get(Constants.OPTIONS).get(Constants.OPTION_VERBOSE).asBoolean();
  }

  private List<Map.Entry<String, JsonNode>> validateAndGetVersions(JsonNode versions) {
    if (!versions.isArray() || versions.isEmpty()) {
      throw new ContractContextException(Constants.INVALID_VERSIONS_FORMAT);
    }

    List<Map.Entry<String, JsonNode>> results = new ArrayList<>();
    for (JsonNode version : versions) {
      if (!version.isObject()) {
        throw new ContractContextException(Constants.INVALID_VERSIONS_FORMAT);
      }

      if (!version.has(Constants.VERSION_ID)
          || !version.get(Constants.VERSION_ID).isTextual()
          || !version.has(Constants.HASH_VALUE)
          || !version.get(Constants.HASH_VALUE).isTextual()) {
        throw new ContractContextException(Constants.INVALID_VERSIONS_FORMAT);
      }

      if (version.has(Constants.METADATA) && !version.get(Constants.METADATA).isObject()) {
        throw new ContractContextException(Constants.INVALID_VERSIONS_FORMAT);
      }

      results.add(
          new AbstractMap.SimpleEntry<>(version.get(Constants.VERSION_ID).asText(), version));
    }

    return results;
  }

  private boolean isFaultyVersion(JsonNode storedVersion, JsonNode givenVersion) {
    if (!storedVersion.has(Constants.HASH_VALUE)) {
      return true;
    }

    String hash = storedVersion.get(Constants.HASH_VALUE).asText();
    String hashToCompare = givenVersion.get(Constants.HASH_VALUE).asText();
    if (!hash.equals(hashToCompare)) {
      return true;
    }

    if (givenVersion.has(Constants.METADATA)) {
      if (!storedVersion.has(Constants.METADATA)) {
        return true;
      }
      JsonNode metadata = storedVersion.get(Constants.METADATA);
      JsonNode metadataToCompare = givenVersion.get(Constants.METADATA);
      return !metadata.equals(metadataToCompare);
    }

    return false;
  }

  private JsonNode createFaultyVersion(String versionId, JsonNode storedVersion) {
    ObjectNode faultyVersion =
        getObjectMapper()
            .createObjectNode()
            .put(Constants.VERSION_ID, versionId)
            .put(Constants.HASH_VALUE, storedVersion.get(Constants.HASH_VALUE).asText());
    if (storedVersion.has(Constants.METADATA)) {
      faultyVersion.set(Constants.METADATA, storedVersion.get(Constants.METADATA));
    }
    return faultyVersion;
  }
}
