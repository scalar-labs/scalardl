package com.scalar.application.bankaccount.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.List;
import javax.annotation.Nullable;

public class AccountHistory extends JacksonBasedContract {
  @Override
  public JsonNode invoke(Ledger<JsonNode> ledger, JsonNode argument, @Nullable JsonNode property) {
    if (!argument.has("id")) {
      throw new ContractContextException("a required key is missing: id");
    }

    AssetFilter filter = new AssetFilter(argument.get("id").asText());
    if (argument.has("start")) {
      filter.withStartAge(argument.get("start").asInt(), true);
    }
    if (argument.has("end")) {
      filter.withEndAge(argument.get("end").asInt(), false);
    }
    if (argument.has("limit")) {
      filter.withLimit(argument.get("limit").asInt());
    }
    if (argument.has("order") && argument.get("order").asText().equals("asc")) {
      filter.withAgeOrder(AssetFilter.AgeOrder.ASC);
    }

    List<Asset<JsonNode>> history = ledger.scan(filter);

    ArrayNode result = JsonNodeFactory.instance.arrayNode();
    history.forEach(
        asset -> {
          JsonNode json =
              getObjectMapper()
                  .createObjectNode()
                  .put("id", asset.id())
                  .put("age", asset.age())
                  .set("data", asset.data());
          result.add(json);
        });

    return getObjectMapper().createObjectNode().put("status", "succeeded").set("history", result);
  }
}
