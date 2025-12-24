package com.scalar.dl.ledger.validation;

import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.AssetKey;
import com.scalar.dl.ledger.statemachine.AssetMetadata;
import com.scalar.dl.ledger.statemachine.Context;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.concurrent.ThreadSafe;

/** An implementation of {@link Ledger} that is used for validation. */
@ThreadSafe
public abstract class LedgerTracerBase<T> implements Ledger<T> {
  protected final Context context;
  protected final Map<AssetKey, Asset<T>> inputs;
  protected final Map<AssetKey, T> outputs;

  public LedgerTracerBase(Context context) {
    this.context = context;
    this.inputs = new ConcurrentHashMap<>();
    this.outputs = new ConcurrentHashMap<>();
  }

  public abstract void setInput(String input);

  public abstract void setInput(AssetKey key, InternalAsset asset);

  public abstract String getOutput(AssetKey key);

  public abstract Map<AssetKey, String> getOutputs();

  @Override
  public Optional<Asset<T>> get(String assetId) {
    return get(context.getNamespace(), assetId);
  }

  @Override
  public Optional<Asset<T>> get(String namespace, String assetId) {
    AssetKey key = AssetKey.of(namespace, assetId);
    if (outputs.containsKey(key)) {
      int age = 0;
      if (inputs.containsKey(key)) {
        age = inputs.get(key).age() + 1;
      }
      return Optional.of(createAsset(assetId, age, outputs.get(key)));
    } else if (inputs.containsKey(key)) {
      return Optional.of(inputs.get(key));
    }
    return Optional.empty();
  }

  @Override
  public void put(String assetId, T data) {
    put(context.getNamespace(), assetId, data);
  }

  @Override
  public void put(String namespace, String assetId, T data) {
    outputs.put(AssetKey.of(namespace, assetId), data);
  }

  private Asset<T> createAsset(String id, int age, T data) {
    return new Asset<T>() {
      @Override
      public String id() {
        return id;
      }

      @Override
      public int age() {
        return age;
      }

      @Override
      public T data() {
        return data;
      }

      @Override
      public AssetMetadata metadata() {
        return null;
      }
    };
  }
}
