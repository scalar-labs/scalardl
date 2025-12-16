package com.scalar.dl.ledger.database;

import java.util.Objects;
import javax.annotation.concurrent.ThreadSafe;

/** A condition used to scan and filter asset entries in a specified namespace. */
@ThreadSafe
public class NamespaceAwareAssetFilter extends AssetFilter {
  private final String namespace;

  /**
   * Constructs a {@code NamespaceAwareAssetFilter} with the specified namespace and asset ID.
   *
   * @param namespace a namespace
   * @param id an asset ID
   */
  public NamespaceAwareAssetFilter(String namespace, String id) {
    super(id);
    this.namespace = namespace;
  }

  /**
   * Returns the namespace.
   *
   * @return the namespace
   */
  public String getNamespace() {
    return namespace;
  }

  /**
   * Returns a hash code value for the object.
   *
   * @return a hash code value for this object.
   */
  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), namespace);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof NamespaceAwareAssetFilter)) {
      return false;
    }
    NamespaceAwareAssetFilter other = (NamespaceAwareAssetFilter) o;
    return super.equals(other) && this.namespace.equals(other.namespace);
  }
}
