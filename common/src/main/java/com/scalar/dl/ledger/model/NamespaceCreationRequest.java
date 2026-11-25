package com.scalar.dl.ledger.model;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

@Immutable
public class NamespaceCreationRequest {
  @Nonnull private final String namespace;
  private final boolean imported;

  /**
   * Constructs a {@code NamespaceCreationRequest} with the specified namespace.
   *
   * @param namespace a namespace name
   */
  public NamespaceCreationRequest(String namespace) {
    this(namespace, false);
  }

  /**
   * Constructs a {@code NamespaceCreationRequest} with the specified namespace and whether the
   * namespace is created to hold data imported from another ScalarDL deployment.
   *
   * @param namespace a namespace name
   * @param imported whether the namespace is created to hold imported data
   */
  public NamespaceCreationRequest(String namespace, boolean imported) {
    this.namespace = checkNotNull(namespace);
    this.imported = imported;
  }

  /**
   * Returns the namespace name.
   *
   * @return the namespace name
   */
  public String getNamespace() {
    return namespace;
  }

  /**
   * Returns whether the namespace is created to hold data imported from another ScalarDL
   * deployment.
   *
   * @return whether the namespace is created to hold imported data
   */
  public boolean isImported() {
    return imported;
  }

  /**
   * Returns a hash code value for the object.
   *
   * @return a hash code value for this object.
   */
  @Override
  public int hashCode() {
    return Objects.hash(namespace, imported);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof NamespaceCreationRequest)) {
      return false;
    }
    NamespaceCreationRequest other = (NamespaceCreationRequest) o;
    return this.namespace.equals(other.namespace) && this.imported == other.imported;
  }
}
