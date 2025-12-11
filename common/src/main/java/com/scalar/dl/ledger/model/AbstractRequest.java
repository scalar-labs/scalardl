package com.scalar.dl.ledger.model;

import static com.google.common.base.Preconditions.checkArgument;

import com.scalar.dl.ledger.crypto.SignatureValidator;
import java.util.Objects;

public abstract class AbstractRequest {
  private final String entityId;
  private final int keyVersion;

  public AbstractRequest(String entityId, int keyVersion) {
    checkArgument(entityId != null && !entityId.isEmpty());
    checkArgument(keyVersion > 0);
    this.entityId = entityId;
    this.keyVersion = keyVersion;
  }

  /**
   * Returns the entity ID.
   *
   * @return the entity ID
   */
  public String getEntityId() {
    return entityId;
  }

  /**
   * Returns the version of a digital signature certificate or a HMAC secret key.
   *
   * @return the version of a digital signature certificate or a HMAC secret key.
   */
  public int getKeyVersion() {
    return keyVersion;
  }

  public abstract void validateWith(SignatureValidator validator);

  /**
   * Returns a hash code value for the object.
   *
   * @return a hash code value for this object.
   */
  @Override
  public int hashCode() {
    return Objects.hash(entityId, keyVersion);
  }

  /**
   * Indicates whether some other object is "equal to" this object. The other object is considered
   * equal if it is the same instance or if:
   *
   * <ul>
   *   <li>it is also an {@code AbstractRequest} and
   *   <li>both instances have the same entityId and keyVersion
   * </ul>
   *
   * @param o an object to be tested for equality
   * @return {@code true} if the other object is "equal to" this object otherwise {@code false}
   */
  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof AbstractRequest)) {
      return false;
    }
    AbstractRequest other = (AbstractRequest) o;
    return this.entityId.equals(other.entityId) && this.keyVersion == other.keyVersion;
  }
}
