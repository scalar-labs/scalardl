package com.scalar.dl.ledger.database;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.scalar.dl.ledger.statemachine.AssetKey;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class Snapshot {
  private final Map<AssetKey, InternalAsset> readSet;
  private final Map<AssetKey, InternalAsset> writeSet;

  public Snapshot() {
    this.readSet = new ConcurrentHashMap<>();
    this.writeSet = new ConcurrentHashMap<>();
  }

  @VisibleForTesting
  Snapshot(Map<AssetKey, InternalAsset> readSet, Map<AssetKey, InternalAsset> writeSet) {
    this.readSet = readSet;
    this.writeSet = writeSet;
  }

  public void put(AssetKey key, InternalAsset record) {
    readSet.put(key, record);
  }

  public void put(AssetKey key, String data) {
    int age = readSet.containsKey(key) ? readSet.get(key).age() + 1 : 0;
    InternalAsset asset = createAsset(key, age, data);
    writeSet.put(key, asset);
  }

  public Optional<InternalAsset> get(AssetKey key) {
    if (writeSet.containsKey(key)) {
      return Optional.of(writeSet.get(key));
    } else if (readSet.containsKey(key)) {
      return Optional.of(readSet.get(key));
    }
    return Optional.empty();
  }

  public Map<AssetKey, InternalAsset> getReadSet() {
    return ImmutableMap.copyOf(readSet);
  }

  public Map<AssetKey, InternalAsset> getWriteSet() {
    return ImmutableMap.copyOf(writeSet);
  }

  public boolean isEmpty() {
    return (readSet.isEmpty() && writeSet.isEmpty());
  }

  public boolean hasWriteSet() {
    return !writeSet.isEmpty();
  }

  private AssetRecord createAsset(AssetKey key, int age, String data) {
    return AssetRecord.newBuilder().id(key.assetId()).age(age).data(data).build();
  }
}
