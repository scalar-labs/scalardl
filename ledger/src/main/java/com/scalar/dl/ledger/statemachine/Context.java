package com.scalar.dl.ledger.statemachine;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import java.util.Objects;

public class Context {
  private final String namespace;

  private Context(String namespace) {
    this.namespace = checkNotNull(namespace);
  }

  public static Context withNamespace(String namespace) {
    return new Context(namespace);
  }

  public String getNamespace() {
    return namespace;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Context)) {
      return false;
    }
    Context context = (Context) o;
    return Objects.equals(namespace, context.namespace);
  }

  @Override
  public int hashCode() {
    return Objects.hash(namespace);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("namespace", namespace).toString();
  }
}
