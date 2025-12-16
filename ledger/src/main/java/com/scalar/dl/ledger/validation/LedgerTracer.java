package com.scalar.dl.ledger.validation;

import com.google.common.annotations.VisibleForTesting;
import com.scalar.dl.ledger.asset.Asset;
import com.scalar.dl.ledger.asset.AssetMetadata;
import com.scalar.dl.ledger.asset.MetadataComprisedAsset;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.AssetScanner;
import com.scalar.dl.ledger.database.Ledger;
import com.scalar.dl.ledger.error.CommonError;
import com.scalar.dl.ledger.error.LedgerError;
import com.scalar.dl.ledger.exception.ValidationException;
import com.scalar.dl.ledger.statemachine.AssetInput;
import com.scalar.dl.ledger.statemachine.AssetKey;
import com.scalar.dl.ledger.statemachine.Context;
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
  private final Context context;
  private final AssetScanner scanner;
  private final Map<AssetKey, Optional<Asset>> inputs;
  private final Map<AssetKey, JsonObject> outputs;

  public LedgerTracer(Context context, AssetScanner scanner) {
    this.context = context;
    this.scanner = scanner;
    this.inputs = new ConcurrentHashMap<>();
    this.outputs = new ConcurrentHashMap<>();
  }

  @VisibleForTesting
  LedgerTracer(
      Context context,
      AssetScanner scanner,
      Map<AssetKey, Optional<Asset>> inputs,
      Map<AssetKey, JsonObject> outputs) {
    this.context = context;
    this.scanner = scanner;
    this.inputs = inputs;
    this.outputs = outputs;
  }

  public void setInput(String input) {
    AssetInput assetInput = new AssetInput(input);
    assetInput.forEach(
        eachInput -> {
          String inputNamespace =
              eachInput.namespace() == null ? context.getNamespace() : eachInput.namespace();
          InternalAsset asset = scanner.doGet(inputNamespace, eachInput.id(), eachInput.age());
          if (asset == null) {
            throw new ValidationException(LedgerError.INCONSISTENT_INPUT_DEPENDENCIES);
          }
          inputs.put(
              AssetKey.of(inputNamespace, eachInput.id()),
              Optional.of(new MetadataComprisedAsset(asset, serde::deserialize)));
        });
  }

  public void setInput(AssetKey key, InternalAsset asset) {
    if (asset == null) {
      inputs.put(key, Optional.empty());
    } else {
      inputs.put(key, Optional.of(new MetadataComprisedAsset(asset, serde::deserialize)));
    }
  }

  public JsonObject getOutput(AssetKey key) {
    return outputs.get(key);
  }

  public Map<AssetKey, String> getOutputs() {
    Map<AssetKey, String> result = new HashMap<>();
    outputs.forEach((key, output) -> result.put(key, serde.serialize(output)));
    return result;
  }

  @Override
  public Optional<Asset> get(String assetId) {
    return get(context.getNamespace(), assetId);
  }

  public Optional<Asset> get(String namespace, String assetId) {
    AssetKey key = AssetKey.of(namespace, assetId);
    if (outputs.containsKey(key)) {
      int age = 0;
      if (inputs.containsKey(key) && inputs.get(key).isPresent()) {
        age = inputs.get(key).get().age() + 1;
      }
      return Optional.of(createAsset(assetId, age, outputs.get(key)));
    } else if (inputs.containsKey(key)) {
      return inputs.get(key);
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
    put(context.getNamespace(), assetId, data);
  }

  public void put(String namespace, String assetId, JsonObject data) {
    outputs.put(AssetKey.of(namespace, assetId), data);
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
        throw new IllegalStateException(CommonError.METADATA_NOT_AVAILABLE.buildMessage());
      }
    };
  }
}
