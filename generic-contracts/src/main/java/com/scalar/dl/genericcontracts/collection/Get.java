package com.scalar.dl.genericcontracts.collection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.annotations.VisibleForTesting;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.AssetFilter.AgeOrder;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * `Get` gets a set of `String` values in the specified collection. See the comment in `Create` for
 * details about the internal data structure of the collection.
 */
public class Get extends JacksonBasedContract {

  @Nullable
  @Override
  public JsonNode invoke(
      Ledger<JsonNode> ledger, JsonNode arguments, @Nullable JsonNode properties) {

    if (!arguments.has(Constants.COLLECTION_ID)
        || !arguments.get(Constants.COLLECTION_ID).isTextual()) {
      throw new ContractContextException(Constants.COLLECTION_ID_IS_MISSING_OR_INVALID);
    }

    String assetId = getAssetIdForCollection(arguments.get(Constants.COLLECTION_ID).asText());
    Optional<Asset<JsonNode>> asset = ledger.get(assetId);

    if (!asset.isPresent()) {
      return null;
    }

    int age = asset.get().age();
    int checkpointInterval = getCheckpointInterval(ledger, arguments);
    if (isCheckpointAge(age, checkpointInterval)) {
      validateCheckpoint(asset.get().data(), checkpointInterval);
      return mergeObjectIds(asset.get().data());
    } else {
      return mergeObjectIdsByScan(ledger, assetId, age, checkpointInterval);
    }
  }

  private String getAssetIdForCollection(String collectionId) {
    return Constants.COLLECTION_ID_PREFIX + collectionId;
  }

  private boolean isCheckpointAge(int age, int checkpointInterval) {
    return age % checkpointInterval == 0;
  }

  private JsonNode mergeObjectIds(JsonNode asset) {
    Set<String> objectIds = new HashSet<>();

    // Add object IDs in the baseline snapshot
    JsonNode snapshot = asset.get(Constants.COLLECTION_SNAPSHOT);
    snapshot.forEach(id -> objectIds.add(id.asText()));

    // Add or remove object IDs, which are added or removed in this asset
    addOrRemoveObjectIds(objectIds, asset);

    ArrayNode jsonObjectIds = getObjectMapper().createArrayNode();
    objectIds.forEach(jsonObjectIds::add);

    return getObjectMapper().createObjectNode().set(Constants.OBJECT_IDS, jsonObjectIds);
  }

  private JsonNode mergeObjectIdsByScan(
      Ledger<JsonNode> ledger, String assetId, int age, int checkpointInterval) {
    int checkpointAge = age - age % checkpointInterval;
    AssetFilter filter =
        new AssetFilter(assetId)
            .withStartAge(checkpointAge, true)
            .withEndAge(age, true)
            .withAgeOrder(AgeOrder.ASC);

    List<Asset<JsonNode>> history = ledger.scan(filter);

    Set<String> objectIds = new HashSet<>();
    for (int i = 0; i < history.size(); i++) {
      Asset<JsonNode> asset = history.get(i);

      // Add object IDs in the baseline snapshot if the age of the asset is the checkpoint age.
      if (i == 0) {
        validateCheckpoint(asset.data(), checkpointInterval);
        JsonNode snapshot = asset.data().get(Constants.COLLECTION_SNAPSHOT);
        snapshot.forEach(id -> objectIds.add(id.asText()));
      }

      // Add or remove object IDs, which are added or removed in this asset
      addOrRemoveObjectIds(objectIds, asset.data());
    }

    ArrayNode jsonObjectIds = getObjectMapper().createArrayNode();
    objectIds.forEach(jsonObjectIds::add);

    return getObjectMapper().createObjectNode().set(Constants.OBJECT_IDS, jsonObjectIds);
  }

  private void validateCheckpoint(JsonNode checkpoint, int checkpointInterval) {
    if (!checkpoint.has(Constants.COLLECTION_CHECKPOINT_INTERVAL)
        || !checkpoint.get(Constants.COLLECTION_CHECKPOINT_INTERVAL).isInt()
        || checkpoint.get(Constants.COLLECTION_CHECKPOINT_INTERVAL).asInt() != checkpointInterval) {
      throw new ContractContextException(Constants.INVALID_CHECKPOINT);
    }

    if (!checkpoint.has(Constants.COLLECTION_SNAPSHOT)
        || !checkpoint.get(Constants.COLLECTION_SNAPSHOT).isArray()) {
      throw new ContractContextException(Constants.ILLEGAL_ASSET_STATE);
    }
  }

  private void addOrRemoveObjectIds(Set<String> objectIds, JsonNode asset) {
    String operationType = asset.get(Constants.OPERATION_TYPE).asText();
    JsonNode objectIdsNode = asset.get(Constants.OBJECT_IDS);
    switch (operationType) {
      case Constants.OPERATION_CREATE:
      case Constants.OPERATION_ADD:
        objectIdsNode.forEach(id -> objectIds.add(id.asText()));
        break;
      case Constants.OPERATION_REMOVE:
        objectIdsNode.forEach(id -> objectIds.remove(id.asText()));
        break;
      default:
        throw new ContractContextException(Constants.ILLEGAL_ASSET_STATE);
    }
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
