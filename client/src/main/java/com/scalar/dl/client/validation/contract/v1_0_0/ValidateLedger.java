package com.scalar.dl.client.validation.contract.v1_0_0;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.AssetFilter.AgeOrder;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.List;

public class ValidateLedger extends JacksonBasedContract {
  public static final String NAMESPACE_KEY = "namespace";
  public static final String ASSET_ID_KEY = "asset_id";
  public static final String AGE_KEY = "age";
  public static final String START_AGE_KEY = "start_age";
  public static final String END_AGE_KEY = "end_age";
  static final String DATA_KEY = "data";

  @Override
  public JsonNode invoke(Ledger<JsonNode> ledger, JsonNode argument, JsonNode properties) {
    if (!argument.has(ASSET_ID_KEY)) {
      throw new ContractContextException("please set asset_id in the argument");
    }

    String assetId = argument.get(ASSET_ID_KEY).asText();

    int startAge = 0;
    int endAge = Integer.MAX_VALUE;
    if (argument.has(AGE_KEY)) {
      int age = argument.get(AGE_KEY).asInt();
      startAge = age;
      endAge = age;
    } else {
      if (argument.has(START_AGE_KEY)) {
        startAge = argument.get(START_AGE_KEY).asInt();
      }
      if (argument.has(END_AGE_KEY)) {
        endAge = argument.get(END_AGE_KEY).asInt();
      }
    }

    String namespace = argument.path(NAMESPACE_KEY).textValue();
    AssetFilter filter =
        (namespace != null ? new AssetFilter(namespace, assetId) : new AssetFilter(assetId))
            .withStartAge(startAge, true)
            .withEndAge(endAge, true)
            .withAgeOrder(AgeOrder.ASC);
    List<Asset<JsonNode>> assets = ledger.scan(filter);

    ObjectMapper mapper = getObjectMapper();
    ArrayNode array = mapper.createArrayNode();

    assets.forEach(
        a -> array.add(mapper.createObjectNode().put(AGE_KEY, a.age()).set(DATA_KEY, a.data())));

    return mapper.createObjectNode().set(assetId, array);
  }
}
