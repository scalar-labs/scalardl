package com.scalar.dl.genericcontracts.object.v1_0_0;

import com.fasterxml.jackson.databind.JsonNode;
import com.scalar.dl.genericcontracts.object.Constants;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.Optional;
import javax.annotation.Nullable;

public class Get extends JacksonBasedContract {

  @Nullable
  @Override
  public JsonNode invoke(
      Ledger<JsonNode> ledger, JsonNode arguments, @Nullable JsonNode properties) {

    if (!arguments.has(Constants.OBJECT_ID) || !arguments.get(Constants.OBJECT_ID).isTextual()) {
      throw new ContractContextException(Constants.OBJECT_ID_IS_MISSING_OR_INVALID);
    }

    String assetId = getAssetIdForObject(arguments.get(Constants.OBJECT_ID).asText());
    Optional<Asset<JsonNode>> asset = ledger.get(assetId);

    return asset.map(Asset::data).orElse(null);
  }

  private String getAssetIdForObject(String objectId) {
    return Constants.OBJECT_ID_PREFIX + objectId;
  }
}
