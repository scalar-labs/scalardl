package com.scalar.dl.ledger.model;

import static com.google.common.base.Preconditions.checkNotNull;

import com.scalar.dl.ledger.crypto.SignatureValidator;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/** A request for secret key registration. */
@Immutable
public class SecretRegistrationRequest extends AbstractRequest {
  private final String secretKey;

  /**
   * Constructs a {@code SecretRegistrationRequest} with the specified context namespace, entity id,
   * the version of a secret key and the secret key.
   *
   * @param contextNamespace a namespace to register the secret key
   * @param entityId an entity id that holds a secret key
   * @param keyVersion the version of the secret key
   * @param secretKey the secret key
   */
  public SecretRegistrationRequest(
      @Nullable String contextNamespace, String entityId, int keyVersion, String secretKey) {
    super(contextNamespace, entityId, keyVersion);
    this.secretKey = checkNotNull(secretKey);
  }

  /**
   * Returns the secret key.
   *
   * @return the secret key
   */
  public String getSecretKey() {
    return secretKey;
  }

  @Override
  public void validateWith(SignatureValidator validator) {
    // This request can not be validated since it is the one for registering a secret key.
  }

  /**
   * Returns a hash code value for the object.
   *
   * @return a hash code value for this object.
   */
  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), secretKey);
  }

  /**
   * Indicates whether some other object is "equal to" this object. The other object is considered
   * equal if it is the same instance or if:
   *
   * <ul>
   *   <li>both super class instances are equal and
   *   <li>it is also an {@code SecretRegistrationRequest} and
   *   <li>both instances have the same secret key
   * </ul>
   *
   * @param o an object to be tested for equality
   * @return {@code true} if the other object is "equal to" this object otherwise {@code false}
   */
  @Override
  public boolean equals(Object o) {
    if (!super.equals(o)) {
      return false;
    }
    if (o == this) {
      return true;
    }
    if (!(o instanceof SecretRegistrationRequest)) {
      return false;
    }
    SecretRegistrationRequest other = (SecretRegistrationRequest) o;
    return this.secretKey.equals(other.secretKey);
  }
}
