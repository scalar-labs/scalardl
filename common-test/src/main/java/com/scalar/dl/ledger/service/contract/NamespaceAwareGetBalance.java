package com.scalar.dl.ledger.service.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.service.Constants;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.Optional;
import javax.annotation.Nullable;

@SuppressWarnings("StringSplitter")
public class NamespaceAwareGetBalance extends JacksonBasedContract {

  @Nullable
  @Override
  public JsonNode invoke(
      Ledger<JsonNode> ledger, JsonNode argument, @Nullable JsonNode properties) {
    String[] assetKey =
        argument.get(Constants.ASSET_ATTRIBUTE_NAME).asText().split(Constants.ASSET_ID_SEPARATOR);
    String namespace = assetKey[0];
    String assetId = assetKey[1];
    Optional<Asset<JsonNode>> asset = ledger.get(namespace, assetId);

    if (!asset.isPresent()) {
      throw new ContractContextException("asset not found");
    }

    return asset.get().data();
  }
}
