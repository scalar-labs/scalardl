package com.scalar.application.bankaccount.contract;

import com.scalar.dl.ledger.asset.Asset;
import com.scalar.dl.ledger.contract.Contract;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.database.Ledger;
import java.util.List;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

public class AccountHistory extends Contract {
  @Override
  public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> property) {
    if (!argument.containsKey("id")) {
      throw new ContractContextException("a required key is missing: id");
    }

    AssetFilter filter = new AssetFilter(argument.getString("id"));
    if (argument.containsKey("start")) {
      filter.withStartVersion(argument.getInt("start"), true);
    }
    if (argument.containsKey("end")) {
      filter.withEndVersion(argument.getInt("end"), false);
    }
    if (argument.containsKey("limit")) {
      filter.withLimit(argument.getInt("limit"));
    }
    if (argument.containsKey("order") && argument.getString("order").equals("asc")) {
      filter.withVersionOrder(AssetFilter.VersionOrder.ASC);
    }

    List<Asset> history = ledger.scan(filter);

    JsonArrayBuilder result = Json.createArrayBuilder();
    history.forEach(
        asset -> {
          JsonObject json =
              Json.createObjectBuilder()
                  .add("id", asset.id())
                  .add("data", asset.data())
                  .add("age", asset.age())
                  .build();
          result.add(json);
        });

    return Json.createObjectBuilder()
        .add("status", "succeeded")
        .add("history", result.build())
        .build();
  }
}
