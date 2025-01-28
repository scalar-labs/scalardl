package com.scalar.dl.genericcontracts.collection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * `Create` creates a collection, which is a set of `String` values and is managed while
 * guaranteeing authenticity (for example, a set of object IDs to be audited or a set of users).
 *
 * <p>When adding or removing items in the collection, an asset that holds the differential data
 * will be added instead of managing and updating the whole set for storage efficiency. When getting
 * the current state of the set, the differential data in the past asset ages are merged. To avoid
 * merging all the ages every time, a snapshot for each checkpoint age is created. The interval
 * between each checkpoint age can be configured via the contract properties.
 *
 * <p>Concrete examples of the collection data in the checkpoint and non-checkpoint age are as
 * follows. Note that the checkpoint interval is 3 in this example (in other words, ages 0 and 3 are
 * the checkpoints).
 *
 * <pre>
 *   Age:0 {
 *     "collection_id":"test_set",
 *     "operation_type":"create",
 *     "object_ids":["object1","object2"],
 *     "snapshot":[],
 *     "checkpoint_interval":3
 *   }
 *   Age:1 {
 *     "collection_id":"test_set",
 *     "operation_type":"add",
 *     "object_ids":["object3"]
 *   }
 *   Age:2 {
 *     "collection_id":"test_set",
 *     "operation_type":"remove",
 *     "object_ids":["object1"]
 *   }
 *   Age:3 {
 *     "collection_id":"test_set",
 *     "operation_type":"add",
 *     "object_ids":["object4"],
 *     "snapshot":["object2","object3"],
 *     "checkpoint_interval":3
 *   }
 * </pre>
 *
 * <p>When `Get` is called at this moment, ["object2","object3","object4"] is returned as the latest
 * set.
 */
public class Create extends JacksonBasedContract {

  @Nullable
  @Override
  public JsonNode invoke(
      Ledger<JsonNode> ledger, JsonNode arguments, @Nullable JsonNode properties) {

    if (!arguments.has(Constants.COLLECTION_ID)
        || !arguments.get(Constants.COLLECTION_ID).isTextual()) {
      throw new ContractContextException(Constants.COLLECTION_ID_IS_MISSING_OR_INVALID);
    }

    if (!arguments.has(Constants.OBJECT_IDS)) {
      throw new ContractContextException(Constants.OBJECT_IDS_ARE_MISSING);
    }
    validateObjectIds(arguments.get(Constants.OBJECT_IDS));

    String assetId = getAssetIdForCollection(arguments.get(Constants.COLLECTION_ID).asText());
    Optional<Asset<JsonNode>> asset = ledger.get(assetId);
    if (asset.isPresent()) {
      throw new ContractContextException(Constants.COLLECTION_ALREADY_EXISTS);
    }

    ObjectNode node = getObjectMapper().createObjectNode();
    node.put(Constants.COLLECTION_ID, arguments.get(Constants.COLLECTION_ID).asText());
    node.put(Constants.OPERATION_TYPE, Constants.OPERATION_CREATE);
    node.set(Constants.OBJECT_IDS, arguments.get(Constants.OBJECT_IDS));
    node.set(Constants.COLLECTION_SNAPSHOT, getObjectMapper().createArrayNode());
    node.put(Constants.COLLECTION_CHECKPOINT_INTERVAL, getCheckpointInterval(ledger, arguments));
    ledger.put(assetId, node);

    return null;
  }

  private void validateObjectIds(JsonNode objectIds) {
    if (!objectIds.isArray()) {
      throw new ContractContextException(Constants.INVALID_OBJECT_IDS_FORMAT);
    }

    for (JsonNode objectId : objectIds) {
      if (!objectId.isTextual()) {
        throw new ContractContextException(Constants.INVALID_OBJECT_IDS_FORMAT);
      }
    }
  }

  private String getAssetIdForCollection(String collectionId) {
    return Constants.COLLECTION_ID_PREFIX + collectionId;
  }

  private int getCheckpointInterval(Ledger<JsonNode> ledger, JsonNode arguments) {
    JsonNode interval =
        invokeSubContract(Constants.CONTRACT_GET_CHECKPOINT_INTERVAL, ledger, arguments);
    assert interval != null;
    return interval.get(Constants.COLLECTION_CHECKPOINT_INTERVAL).asInt();
  }

  @VisibleForTesting
  JsonNode invokeSubContract(String contractId, Ledger<JsonNode> ledger, JsonNode arguments) {
    return invoke(contractId, ledger, arguments);
  }
}
