package com.scalar.dl.ledger.statemachine;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ComparisonChain;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;

@Immutable
public final class AssetKey implements Comparable<AssetKey> {
  private final String namespace;
  private final String assetId;

  private AssetKey(String namespace, String assetId) {
    this.namespace = namespace;
    this.assetId = assetId;
  }

  public static AssetKey of(String namespace, String assetId) {
    return new AssetKey(namespace, assetId);
  }

  public String namespace() {
    return namespace;
  }

  public String assetId() {
    return assetId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(namespace, assetId);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof AssetKey)) {
      return false;
    }
    AssetKey another = (AssetKey) o;
    return this.namespace.equals(another.namespace) && this.assetId.equals(another.assetId);
  }

  @Override
  public int compareTo(AssetKey o) {
    return ComparisonChain.start()
        .compare(this.namespace, o.namespace)
        .compare(this.assetId, o.assetId)
        .result();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("namespace", namespace)
        .add("assetId", assetId)
        .toString();
  }
}
