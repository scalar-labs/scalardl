package com.scalar.dl.ledger.validation;

import com.google.common.annotations.VisibleForTesting;
import com.scalar.dl.ledger.asset.Asset;
import com.scalar.dl.ledger.asset.AssetMetadata;
import com.scalar.dl.ledger.asset.MetadataComprisedAsset;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.AssetScanner;
import com.scalar.dl.ledger.database.Ledger;
import com.scalar.dl.ledger.exception.ValidationException;
import com.scalar.dl.ledger.service.StatusCode;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import com.scalar.dl.ledger.util.JsonpSerDe;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.annotation.concurrent.ThreadSafe;
import javax.json.JsonObject;

/** An implementation of {@link Ledger} that is used for validation. */
@ThreadSafe
public class LedgerTracer implements Ledger {
  private static final JsonpSerDe serde = new JsonpSerDe();
  private final AssetScanner scanner;
  private final Map<String, Optional<Asset>> inputs;
  private final Map<String, JsonObject> outputs;

  public LedgerTracer(AssetScanner scanner) {
    this.scanner = scanner;
    this.inputs = new ConcurrentHashMap<>();
    this.outputs = new ConcurrentHashMap<>();
  }

  @VisibleForTesting
  LedgerTracer(
      AssetScanner scanner, Map<String, Optional<Asset>> inputs, Map<String, JsonObject> outputs) {
    this.scanner = scanner;
    this.inputs = inputs;
    this.outputs = outputs;
  }

  public void setInput(JsonObject input) {
    input.forEach(
        (id, value) -> {
          JsonObject json = value.asJsonObject();
          InternalAsset asset = scanner.doGet(id, json.getInt("age"));
          if (asset == null) {
            throw new ValidationException(
                "the asset specified by input dependencies is not found. ",
                StatusCode.INCONSISTENT_STATES);
          }
          inputs.put(id, Optional.of(new MetadataComprisedAsset(asset, serde::deserialize)));
        });
  }

  public void setInput(String assetId, InternalAsset asset) {
    if (asset == null) {
      inputs.put(assetId, Optional.empty());
    } else {
      inputs.put(assetId, Optional.of(new MetadataComprisedAsset(asset, serde::deserialize)));
    }
  }

  public JsonObject getOutput(String assetId) {
    return outputs.get(assetId);
  }

  public Map<String, String> getOutputs() {
    Map<String, String> result = new HashMap<>();
    outputs.forEach((assetId, output) -> result.put(assetId, serde.serialize(output)));
    return result;
  }

  @Override
  public Optional<Asset> get(String assetId) {
    if (outputs.containsKey(assetId)) {
      int age = 0;
      if (inputs.containsKey(assetId) && inputs.get(assetId).isPresent()) {
        age = inputs.get(assetId).get().age() + 1;
      }
      return Optional.of(createAsset(assetId, age, outputs.get(assetId)));
    } else if (inputs.containsKey(assetId)) {
      return inputs.get(assetId);
    }
    return Optional.empty();
  }

  @Override
  public List<Asset> scan(AssetFilter filter) {
    return scanner.doScan(filter).stream()
        .map(asset -> new MetadataComprisedAsset(asset, serde::deserialize))
        .collect(Collectors.toList());
  }

  @Override
  public void put(String assetId, JsonObject data) {
    outputs.put(assetId, data);
  }

  private Asset createAsset(String id, int age, JsonObject data) {
    return new Asset() {
      @Override
      public String id() {
        return id;
      }

      @Override
      public int age() {
        return age;
      }

      @Override
      public JsonObject data() {
        return data;
      }

      @Override
      public AssetMetadata metadata() {
        throw new IllegalStateException(
            "The metadata is not available since the asset has not been committed yet.");
      }
    };
  }
}
