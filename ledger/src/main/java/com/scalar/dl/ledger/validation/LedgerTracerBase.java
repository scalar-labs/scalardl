package com.scalar.dl.ledger.validation;

import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.AssetMetadata;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.concurrent.ThreadSafe;

/** An implementation of {@link Ledger} that is used for validation. */
@ThreadSafe
public abstract class LedgerTracerBase<T> implements Ledger<T> {
  protected final Map<String, Asset<T>> inputs;
  protected final Map<String, T> outputs;

  public LedgerTracerBase() {
    this.inputs = new ConcurrentHashMap<>();
    this.outputs = new ConcurrentHashMap<>();
  }

  public abstract void setInput(String input);

  public abstract void setInput(String assetId, InternalAsset asset);

  public abstract String getOutput(String assetId);

  public abstract Map<String, String> getOutputs();

  @Override
  public Optional<Asset<T>> get(String assetId) {
    if (outputs.containsKey(assetId)) {
      int age = 0;
      if (inputs.containsKey(assetId)) {
        age = inputs.get(assetId).age() + 1;
      }
      return Optional.of(createAsset(assetId, age, outputs.get(assetId)));
    } else if (inputs.containsKey(assetId)) {
      return Optional.of(inputs.get(assetId));
    }
    return Optional.empty();
  }

  @Override
  public void put(String assetId, T data) {
    outputs.put(assetId, data);
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
