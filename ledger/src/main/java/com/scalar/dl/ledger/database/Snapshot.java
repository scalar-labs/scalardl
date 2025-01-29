package com.scalar.dl.ledger.database;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class Snapshot {
  private final Map<String, InternalAsset> readSet;
  private final Map<String, InternalAsset> writeSet;

  public Snapshot() {
    this.readSet = new ConcurrentHashMap<>();
    this.writeSet = new ConcurrentHashMap<>();
  }

  @VisibleForTesting
  Snapshot(Map<String, InternalAsset> readSet, Map<String, InternalAsset> writeSet) {
    this.readSet = readSet;
    this.writeSet = writeSet;
  }

  public void put(String assetId, InternalAsset record) {
    readSet.put(assetId, record);
  }

  public void put(String assetId, String data) {
    int age = readSet.containsKey(assetId) ? readSet.get(assetId).age() + 1 : 0;
    InternalAsset asset = createAsset(assetId, age, data);
    writeSet.put(assetId, asset);
  }

  public Optional<InternalAsset> get(String assetId) {
    if (writeSet.containsKey(assetId)) {
      return Optional.of(writeSet.get(assetId));
    } else if (readSet.containsKey(assetId)) {
      return Optional.of(readSet.get(assetId));
    }
    return Optional.empty();
  }

  public Map<String, InternalAsset> getReadSet() {
    return ImmutableMap.copyOf(readSet);
  }

  public Map<String, InternalAsset> getWriteSet() {
    return ImmutableMap.copyOf(writeSet);
  }

  public boolean isEmpty() {
    return (readSet.isEmpty() && writeSet.isEmpty());
  }

  public boolean hasWriteSet() {
    return !writeSet.isEmpty();
  }

  private AssetRecord createAsset(String assetId, int age, String data) {
    return AssetRecord.newBuilder().id(assetId).age(age).data(data).build();
  }
}
