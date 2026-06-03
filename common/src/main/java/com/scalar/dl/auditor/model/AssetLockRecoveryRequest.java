package com.scalar.dl.auditor.model;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

@Immutable
public class AssetLockRecoveryRequest {
  @Nonnull private final String namespace;
  @Nonnull private final String assetId;

  /**
   * Constructs a {@code AssetLockRecoveryRequest} with the specified namespace and asset ID.
   *
   * @param namespace a namespace of an asset
   * @param assetId an id of an asset
   */
  public AssetLockRecoveryRequest(String namespace, String assetId) {
    this.namespace = checkNotNull(namespace);
    this.assetId = checkNotNull(assetId);
  }

  /**
   * Returns the namespace to recover.
   *
   * @return the string of the namespace
   */
  @Nonnull
  public String getNamespace() {
    return namespace;
  }

  /**
   * Returns the asset ID to recover.
   *
   * @return the string of the asset ID
   */
  @Nonnull
  public String getAssetId() {
    return assetId;
  }

  /**
   * Returns a hash code value for the object.
   *
   * @return a hash code value for this object.
   */
  @Override
  public int hashCode() {
    return Objects.hash(namespace, assetId);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof AssetLockRecoveryRequest)) {
      return false;
    }
    AssetLockRecoveryRequest other = (AssetLockRecoveryRequest) o;
    return this.namespace.equals(other.namespace) && this.assetId.equals(other.assetId);
  }
}
