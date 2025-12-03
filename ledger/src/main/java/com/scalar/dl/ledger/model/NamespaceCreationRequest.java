package com.scalar.dl.ledger.model;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

@Immutable
public class NamespaceCreationRequest {
  @Nonnull private final String namespace;

  /**
   * Constructs a {@code NamespaceCreationRequest} with the specified namespace.
   *
   * @param namespace a namespace name
   */
  public NamespaceCreationRequest(String namespace) {
    this.namespace = checkNotNull(namespace);
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
   * Returns a hash code value for the object.
   *
   * @return a hash code value for this object.
   */
  @Override
  public int hashCode() {
    return Objects.hash(namespace);
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
    return this.namespace.equals(other.namespace);
  }
}
