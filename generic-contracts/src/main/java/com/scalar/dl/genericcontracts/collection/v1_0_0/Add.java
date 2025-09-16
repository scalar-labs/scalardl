package com.scalar.dl.genericcontracts.collection.v1_0_0;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.scalar.dl.genericcontracts.collection.Constants;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * `Add` adds a set of `String` values to the specified collection. See the comment in `Create` for
 * details about the internal data structure of the collection.
 */
public class Add extends JacksonBasedContract {

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
    if (!asset.isPresent()) {
      throw new ContractContextException(Constants.COLLECTION_NOT_FOUND);
    }

    JsonNode collection = getCollection(ledger, arguments);
    if (!isForcedOptionSpecified(arguments)) {
      validateDuplicates(arguments, collection);
    }

    ObjectNode data =
        getObjectMapper()
            .createObjectNode()
            .put(Constants.COLLECTION_ID, arguments.get(Constants.COLLECTION_ID).asText())
            .put(Constants.OPERATION_TYPE, Constants.OPERATION_ADD)
            .set(Constants.OBJECT_IDS, arguments.get(Constants.OBJECT_IDS));

    int age = asset.get().age();
    int checkpointInterval = getCheckpointInterval(ledger, arguments);
    if (isCheckpointAge(age + 1, checkpointInterval)) {
      // If the next age is checkpoint age, append the current set of object IDs as a snapshot.
      data.set(Constants.COLLECTION_SNAPSHOT, collection.get(Constants.OBJECT_IDS));
      data.put(Constants.COLLECTION_CHECKPOINT_INTERVAL, checkpointInterval);
    }
    ledger.put(assetId, data);

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

  private boolean isForcedOptionSpecified(JsonNode arguments) {
    return arguments.has(Constants.OPTIONS)
        && arguments.get(Constants.OPTIONS).isObject()
        && arguments.get(Constants.OPTIONS).has(Constants.OPTION_FORCE)
        && arguments.get(Constants.OPTIONS).get(Constants.OPTION_FORCE).asBoolean();
  }

  private boolean isCheckpointAge(int age, int checkpointInterval) {
    return age % checkpointInterval == 0;
  }

  private void validateDuplicates(JsonNode arguments, JsonNode collection) {
    Set<String> objectIds = new HashSet<>();
    for (JsonNode objectId : collection.get(Constants.OBJECT_IDS)) {
      objectIds.add(objectId.asText());
    }

    for (JsonNode objectId : arguments.get(Constants.OBJECT_IDS)) {
      if (objectIds.contains(objectId.asText())) {
        throw new ContractContextException(Constants.OBJECT_ALREADY_EXISTS_IN_COLLECTION);
      }
    }
  }

  private int getCheckpointInterval(Ledger<JsonNode> ledger, JsonNode arguments) {
    JsonNode interval =
        invokeSubContract(Constants.CONTRACT_GET_CHECKPOINT_INTERVAL, ledger, arguments);
    assert interval != null;
    return interval.get(Constants.COLLECTION_CHECKPOINT_INTERVAL).asInt();
  }

  private JsonNode getCollection(Ledger<JsonNode> ledger, JsonNode arguments) {
    JsonNode collection = invokeSubContract(Constants.CONTRACT_GET, ledger, arguments);
    if (collection == null) {
      throw new ContractContextException(Constants.COLLECTION_NOT_FOUND);
    }
    return collection;
  }

  @VisibleForTesting
  JsonNode invokeSubContract(String contractId, Ledger<JsonNode> ledger, JsonNode arguments) {
    return invoke(contractId, ledger, arguments);
  }
}
