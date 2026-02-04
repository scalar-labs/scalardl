package com.scalar.dl.ledger.model;

import static com.google.common.base.Preconditions.checkArgument;

import com.scalar.dl.ledger.crypto.SignatureValidator;
import com.scalar.dl.ledger.namespace.Namespaces;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import javax.annotation.Nullable;

public abstract class AbstractRequest {
  @Nullable private final String contextNamespace;
  private final String entityId;
  private final int keyVersion;

  public AbstractRequest(@Nullable String contextNamespace, String entityId, int keyVersion) {
    checkArgument(entityId != null && !entityId.isEmpty());
    checkArgument(keyVersion > 0);
    this.contextNamespace = contextNamespace;
    this.entityId = entityId;
    this.keyVersion = keyVersion;
  }

  /**
   * Returns the context namespace.
   *
   * @return the context namespace.
   */
  @Nullable
  public String getContextNamespace() {
    return contextNamespace;
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
   * Serializes the context namespace to a byte array for signing.
   *
   * @param contextNamespace the context namespace to serialize
   * @return the byte array representation of the context namespace
   */
  protected static byte[] serializeContextNamespace(@Nullable String contextNamespace) {
    assert contextNamespace == null || !contextNamespace.equals(Namespaces.DEFAULT)
        : "contextNamespace should be null instead of '" + Namespaces.DEFAULT + "'";
    return contextNamespace != null
        ? contextNamespace.getBytes(StandardCharsets.UTF_8)
        : new byte[0];
  }

  /**
   * Returns a hash code value for the object.
   *
   * @return a hash code value for this object.
   */
  @Override
  public int hashCode() {
    return Objects.hash(contextNamespace, entityId, keyVersion);
  }

  /**
   * Indicates whether some other object is "equal to" this object. The other object is considered
   * equal if it is the same instance or if:
   *
   * <ul>
   *   <li>it is also an {@code AbstractRequest} and
   *   <li>both instances have the same contextNamespace, entityId and keyVersion
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
    return Objects.equals(this.contextNamespace, other.contextNamespace)
        && this.entityId.equals(other.entityId)
        && this.keyVersion == other.keyVersion;
  }
}
