package com.scalar.dl.ledger.crypto;

import static com.google.common.base.Preconditions.checkArgument;

import com.scalar.dl.ledger.model.SecretRegistrationRequest;
import com.scalar.dl.ledger.util.Time;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;

/**
 * Container for a registered HMAC secret key and metadata about the secret key. In addition to the
 * secret key, a {@code SecretEntry} has:
 *
 * <ul>
 *   <li>the entity ID that has the secret key
 *   <li>the version of the secret key
 *   <li>the time at which the secret key was registered
 * </ul>
 */
@Immutable
public class SecretEntry {
  public static final String ENTITY_ID = "entity_id";
  public static final String KEY_VERSION = "key_version";
  public static final String SECRET_KEY = "secret_key";
  public static final String REGISTERED_AT = "registered_at";
  private final String entityId;
  private final int keyVersion;
  private final String secretKey;
  private final long registeredAt;
  private final Key key;

  /**
   * Constructs a {@code SecretEntry} with the specified entity ID, secret key version, secret key,
   * and time the secret was registered at.
   *
   * @param entityId the ID of an entity
   * @param keyVersion the version of a secret
   * @param secretKey a secret key
   * @param registeredAt the time the contract was registered at
   */
  public SecretEntry(String entityId, int keyVersion, String secretKey, long registeredAt) {
    checkArgument(entityId != null);
    checkArgument(keyVersion >= 1);
    checkArgument(secretKey != null);
    checkArgument(registeredAt >= 0);
    this.entityId = entityId;
    this.keyVersion = keyVersion;
    this.secretKey = secretKey;
    this.registeredAt = registeredAt;
    this.key = new Key(entityId, keyVersion);
  }

  /**
   * Returns the {@code Key} of the {@code SecretEntry}. A Key is made up of the entity ID and the
   * secret key version.
   *
   * @return the {@code Key} of the {@code SecretEntry}
   */
  public Key getKey() {
    return key;
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
   * Returns the secret key version.
   *
   * @return the secret key version
   */
  public int getKeyVersion() {
    return keyVersion;
  }

  /**
   * Returns the secret key.
   *
   * @return the secret key
   */
  public String getSecretKey() {
    return secretKey;
  }

  /**
   * Returns the time the secret key was registered at.
   *
   * @return the time the secret key was registered at
   */
  public long getRegisteredAt() {
    return registeredAt;
  }

  /**
   * Returns a hash code value for the object.
   *
   * @return a hash code value for this object.
   */
  @Override
  public int hashCode() {
    return Objects.hash(entityId, keyVersion, secretKey, registeredAt);
  }

  /**
   * Indicates whether some other object is "equal to" this object. The other object is considered
   * equal if it is also an {@code SecretEntry} and both instances have the same
   *
   * <ul>
   *   <li>entity ID
   *   <li>key version
   *   <li>secret key
   *   <li>registered at time
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
    if (!(o instanceof SecretEntry)) {
      return false;
    }
    SecretEntry another = (SecretEntry) o;
    return this.entityId.equals(another.entityId)
        && this.keyVersion == another.keyVersion
        && this.secretKey.equals(another.secretKey)
        && this.registeredAt == another.registeredAt;
  }

  /**
   * Creates a {@code SecretEntry} from the specified {@code SecretRegistrationRequest}.
   *
   * @param request a {@code SecretRegistrationRequest}
   * @return a {@code SecretEntry}
   */
  public static SecretEntry from(SecretRegistrationRequest request) {
    return new SecretEntry(
        request.getEntityId(),
        request.getKeyVersion(),
        request.getSecretKey(),
        Time.getCurrentUtcTimeInMillis());
  }

  @Immutable
  public static class Key implements ClientIdentityKey {
    private final String entityId;
    private final int keyVersion;

    public Key(String entityId, int keyVersion) {
      checkArgument(entityId != null);
      checkArgument(keyVersion >= 1);
      this.entityId = entityId;
      this.keyVersion = keyVersion;
    }

    @Override
    public String getEntityId() {
      return entityId;
    }

    @Override
    public int getKeyVersion() {
      return keyVersion;
    }

    @Override
    public int hashCode() {
      return Objects.hash(entityId, keyVersion);
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (!(o instanceof SecretEntry.Key)) {
        return false;
      }
      Key another = (Key) o;
      return this.entityId.equals(another.entityId) && this.keyVersion == another.keyVersion;
    }
  }
}
