package com.scalar.dl.client.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.scalar.dl.client.Constants;
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
import java.util.Map.Entry;
import java.util.stream.IntStream;
import javax.annotation.Nullable;

public class ValidateObject extends JacksonBasedContract {

  @Nullable
  @Override
  public JsonNode invoke(
      Ledger<JsonNode> ledger, JsonNode arguments, @Nullable JsonNode properties) {

    if (!arguments.has(Constants.OBJECT_ID) || !arguments.get(Constants.OBJECT_ID).isTextual()) {
      throw new ContractContextException(Constants.OBJECT_ID_IS_MISSING_OR_INVALID);
    }

    if (!arguments.has(Constants.HASH_VALUES)) {
      throw new ContractContextException(Constants.HASH_VALUES_ARE_MISSING);
    }

    List<Map.Entry<String, String>> hashValues =
        validateAndGetHashValues(arguments.get(Constants.HASH_VALUES));

    String assetId = arguments.get(Constants.OBJECT_ID).asText();
    AssetFilter filter =
        new AssetFilter(assetId).withLimit(hashValues.size()).withAgeOrder(AgeOrder.DESC);

    List<Asset<JsonNode>> history = ledger.scan(filter);

    List<String> faultyVersions = new ArrayList<>();
    IntStream.range(0, history.size())
        .forEach(
            i -> {
              Asset<JsonNode> asset = history.get(i);
              if (!asset.data().has(Constants.HASH_VALUE)
                  || !asset
                      .data()
                      .get(Constants.HASH_VALUE)
                      .asText()
                      .equals(hashValues.get(i).getValue())) {
                faultyVersions.add(hashValues.get(i).getKey());
              }
            });

    String status =
        history.size() == hashValues.size() && faultyVersions.isEmpty()
            ? Constants.STATUS_NORMAL
            : Constants.STATUS_FAULTY;
    ArrayNode jsonFaultyVersions = getObjectMapper().createArrayNode();
    faultyVersions.forEach(jsonFaultyVersions::add);

    return getObjectMapper()
        .createObjectNode()
        .put(Constants.STATUS, status)
        .set(Constants.FAULTY_VERSIONS, jsonFaultyVersions);
  }

  private List<Map.Entry<String, String>> validateAndGetHashValues(JsonNode hashValues) {
    if (!hashValues.isArray() || hashValues.isEmpty()) {
      throw new ContractContextException(Constants.INVALID_HASH_VALUES_FORMAT);
    }

    List<Map.Entry<String, String>> results = new ArrayList<>();
    for (JsonNode hashValue : hashValues) {
      if (!hashValue.isObject() || hashValue.size() != 1) {
        throw new ContractContextException(Constants.INVALID_HASH_VALUES_FORMAT);
      }

      Entry<String, JsonNode> entry = hashValue.fields().next();
      if (!entry.getValue().isTextual()) {
        throw new ContractContextException(Constants.INVALID_HASH_VALUES_FORMAT);
      }

      results.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().asText()));
    }

    return results;
  }
}
