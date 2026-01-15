package com.scalar.dl.ledger.database;

import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A condition used to scan and filter asset entries.
 *
 * @author Hiroyuki Yamada
 */
@ThreadSafe
public class AssetFilter {
  @Nullable private final String namespace;
  private final String id;
  private Optional<Integer> startAge;
  private boolean startInclusive;
  private Optional<Integer> endAge;
  private boolean endInclusive;
  private Optional<AgeOrder> ageOrder;
  private int limit;

  /**
   * Constructs a {@code AssetFilter} with the specified ID.
   *
   * @param id an asset ID
   */
  public AssetFilter(String id) {
    this(null, id);
  }

  /**
   * Constructs a {@code AssetFilter} with the specified namespace and ID.
   *
   * @param namespace a namespace
   * @param id an asset ID
   */
  public AssetFilter(String namespace, String id) {
    this.namespace = namespace;
    this.id = id;
    startAge = Optional.empty();
    endAge = Optional.empty();
    ageOrder = Optional.empty();
    limit = 0;
  }

  /**
   * Returns the namespace.
   *
   * @return the namespace
   */
  public Optional<String> getNamespace() {
    return Optional.ofNullable(namespace);
  }

  /**
   * Returns the asset ID.
   *
   * @return the asset ID
   */
  public String getId() {
    return id;
  }

  /**
   * Sets a start age of scan with the specified age and the boundary condition.
   *
   * @param age a start age
   * @param inclusive a boundary condition
   * @return this object
   */
  public synchronized AssetFilter withStartAge(int age, boolean inclusive) {
    startAge = Optional.of(age);
    startInclusive = inclusive;
    return this;
  }

  /**
   * Returns the start age of scan.
   *
   * @return the start age of scan
   */
  public Optional<Integer> getStartAge() {
    return startAge;
  }

  /**
   * Returns the boundary condition of the start age.
   *
   * @return the boundary condition of the start age
   */
  public boolean isStartInclusive() {
    return startInclusive;
  }

  /**
   * Sets an end age of scan with the specified age and the boundary condition.
   *
   * @param age an end age
   * @param inclusive a boundary condition
   * @return this object
   */
  public synchronized AssetFilter withEndAge(int age, boolean inclusive) {
    endAge = Optional.of(age);
    endInclusive = inclusive;
    return this;
  }

  /**
   * Returns the end age of scan.
   *
   * @return the end age of scan
   */
  public Optional<Integer> getEndAge() {
    return endAge;
  }

  /**
   * Returns the boundary condition of the end age.
   *
   * @return the boundary condition of the end age
   */
  public boolean isEndInclusive() {
    return endInclusive;
  }

  /**
   * Sets an ordering of filtered results.
   *
   * @param ageOrder an ordering of age
   * @return this object
   */
  public AssetFilter withAgeOrder(AgeOrder ageOrder) {
    this.ageOrder = Optional.of(ageOrder);
    return this;
  }

  /**
   * Returns the ordering used to sort results.
   *
   * @return the ordering used to sort results
   */
  public Optional<AgeOrder> getAgeOrder() {
    return ageOrder;
  }

  /**
   * Sets the maximum number of results to be returned.
   *
   * @param limit the maximum number of results to be returned
   * @return this object
   */
  public AssetFilter withLimit(int limit) {
    this.limit = limit;
    return this;
  }

  /**
   * Returns the maximum number of results to be returned.
   *
   * @return the maximum number of results to be returned
   */
  public int getLimit() {
    return limit;
  }

  /** An order used to specify ordering of results. */
  public enum AgeOrder {
    /** Ascending order */
    ASC,
    /** Descending order */
    DESC,
  }

  /**
   * Returns a hash code value for the object.
   *
   * @return a hash code value for this object.
   */
  @Override
  public int hashCode() {
    return Objects.hash(
        namespace, id, startAge, startInclusive, endAge, endInclusive, ageOrder, limit);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof AssetFilter)) {
      return false;
    }
    AssetFilter other = (AssetFilter) o;
    return Objects.equals(this.namespace, other.namespace)
        && this.id.equals(other.id)
        && this.startAge.equals(other.startAge)
        && this.startInclusive == other.startInclusive
        && this.endAge.equals(other.endAge)
        && this.endInclusive == other.endInclusive
        && this.ageOrder.equals(other.ageOrder)
        && this.limit == other.limit;
  }
}
