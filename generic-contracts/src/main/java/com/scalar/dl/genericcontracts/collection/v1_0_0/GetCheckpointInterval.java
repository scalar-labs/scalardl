package com.scalar.dl.genericcontracts.collection.v1_0_0;

import com.fasterxml.jackson.databind.JsonNode;
import com.scalar.dl.genericcontracts.collection.Constants;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Ledger;
import javax.annotation.Nullable;

public class GetCheckpointInterval extends JacksonBasedContract {

  @Nullable
  @Override
  public JsonNode invoke(
      Ledger<JsonNode> ledger, JsonNode arguments, @Nullable JsonNode properties) {
    int interval = Constants.DEFAULT_COLLECTION_CHECKPOINT_INTERVAL;

    if (properties != null && properties.has(Constants.COLLECTION_CHECKPOINT_INTERVAL)) {
      if (!properties.get(Constants.COLLECTION_CHECKPOINT_INTERVAL).isInt()
          || properties.get(Constants.COLLECTION_CHECKPOINT_INTERVAL).asInt()
              < Constants.MIN_COLLECTION_CHECKPOINT_INTERVAL) {
        throw new ContractContextException(Constants.INVALID_CONTRACT_PROPERTIES_FORMAT);
      }
      interval = properties.get(Constants.COLLECTION_CHECKPOINT_INTERVAL).asInt();
    }

    return getObjectMapper()
        .createObjectNode()
        .put(Constants.COLLECTION_CHECKPOINT_INTERVAL, interval);
  }
}
