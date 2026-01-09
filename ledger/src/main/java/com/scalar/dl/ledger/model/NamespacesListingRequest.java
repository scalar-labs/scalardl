package com.scalar.dl.ledger.model;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

@Immutable
public class NamespacesListingRequest {
  @Nonnull private final String pattern;

  /**
   * Constructs a {@code NamespacesListingRequest} with the specified namespace filter.
   *
   * @param pattern a namespace name to filter (exact match). If empty, returns all namespaces.
   */
  public NamespacesListingRequest(String pattern) {
    this.pattern = checkNotNull(pattern);
  }

  /**
   * Returns the pattern string to filter.
   *
   * @return the pattern string to filter, or empty string if no filter is applied
   */
  @Nonnull
  public String getPattern() {
    return pattern;
  }

  /**
   * Returns a hash code value for the object.
   *
   * @return a hash code value for this object.
   */
  @Override
  public int hashCode() {
    return Objects.hash(pattern);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof NamespacesListingRequest)) {
      return false;
    }
    NamespacesListingRequest other = (NamespacesListingRequest) o;
    return this.pattern.equals(other.pattern);
  }
}
