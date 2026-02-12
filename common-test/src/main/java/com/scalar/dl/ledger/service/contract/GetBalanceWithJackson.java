package com.scalar.dl.ledger.service.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.service.Constants;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.Optional;
import javax.annotation.Nullable;

public class GetBalanceWithJackson extends JacksonBasedContract {

  @Nullable
  @Override
  public JsonNode invoke(
      Ledger<JsonNode> ledger, JsonNode argument, @Nullable JsonNode properties) {
    if (properties == null
        || !properties
            .get(Constants.CONTRACT_ID_ATTRIBUTE_NAME)
            .asText()
            .equals(Constants.GET_BALANCE_CONTRACT_ID3)) {
      throw new ContractContextException("properties is not set as expected.");
    }

    String assetId = argument.get(Constants.ASSET_ATTRIBUTE_NAME).asText();
    Optional<Asset<JsonNode>> asset = ledger.get(assetId);

    if (!asset.isPresent()) {
      throw new ContractContextException("asset not found");
    }

    return asset.get().data();
  }
}
