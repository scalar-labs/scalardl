package com.scalar.dl.ledger.crypto;

import static com.google.common.base.Preconditions.checkNotNull;

import com.scalar.dl.ledger.model.CertificateRegistrationRequest;
import com.scalar.dl.ledger.util.Time;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;

/**
 * Container for a registered certificate and data about that certificate. In addition to the
 * certificate, a {@code CertificateEntry} will keep track of:
 *
 * <ul>
 *   <li>an entity id that holds the certificate
 *   <li>the certificate version
 *   <li>the time in which the certificate was registered
 * </ul>
 */
@Immutable
public class CertificateEntry {
  public static final String ENTITY_ID = "holder_id";
  public static final String VERSION = "version";
  public static final String PEM = "pem";
  public static final String REGISTERED_AT = "registered_at";
  private final String entityId;
  private final int version;
  private final String pem;
  private final long registeredAt;
  private final Key key;

  /**
   * Constructs a {@code CertificateEntry} with the specified entity id, certificate version,
   * certificate, and time the certificate was registered.
   *
   * @param entityId an entity id that holds a certificate
   * @param version the certificate version
   * @param pem the certificate
   * @param registeredAt the time the contract was registered
   */
  public CertificateEntry(String entityId, int version, String pem, long registeredAt) {
    this.entityId = checkNotNull(entityId);
    this.version = version;
    this.pem = checkNotNull(pem);
    this.registeredAt = registeredAt;
    this.key = new Key(entityId, version);
  }

  /**
   * Returns the {@code Key} of the {@code CertificateEntry}. A Key is made up of the entity id and
   * version.
   *
   * @return the {@code Key} of the {@code CertificateEntry}
   */
  public Key getKey() {
    return key;
  }

  /**
   * Returns the entity ID that holds the certificate.
   *
   * @return the entity ID that holds the certificate
   */
  public String getEntityId() {
    return entityId;
  }

  /**
   * Returns the certificate version.
   *
   * @return the certificate version
   */
  public int getVersion() {
    return version;
  }

  /**
   * Returns the certificate.
   *
   * @return the certificate
   */
  public String getPem() {
    return pem;
  }

  /**
   * Returns the time the certificate was registered at.
   *
   * @return the time the certificate was registered at
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
    return Objects.hash(entityId, version, pem, registeredAt, key);
  }

  /**
   * Indicates whether some other object is "equal to" this object. The other object is considered
   * equal if it is also an {@code CertificateEntry} and both instances have the same
   *
   * <ul>
   *   <li>entity id
   *   <li>version
   *   <li>certificate
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
    if (!(o instanceof CertificateEntry)) {
      return false;
    }
    CertificateEntry another = (CertificateEntry) o;
    return this.entityId.equals(another.entityId)
        && this.version == another.version
        && this.pem.equals(another.pem)
        && this.registeredAt == another.registeredAt;
  }

  /**
   * Constructs a {@code CertificateEntry} from a {@link CertificateRegistrationRequest} by adding
   * the current time as the registered at time.
   *
   * @param request a {@link CertificateRegistrationRequest}
   * @return a {@code CertificateEntry}
   */
  public static CertificateEntry from(CertificateRegistrationRequest request) {
    return new CertificateEntry(
        request.getEntityId(),
        request.getKeyVersion(),
        request.getCertPem(),
        Time.getCurrentUtcTimeInMillis());
  }

  @Immutable
  public static class Key implements ClientIdentityKey {
    private final String entityId;
    private final int version;

    public Key(String entityId, int version) {
      this.entityId = checkNotNull(entityId);
      this.version = version;
    }

    /** This method will be removed in 5.0.0. */
    @Deprecated
    public String getHolderId() {
      return entityId;
    }

    @Override
    public String getEntityId() {
      return entityId;
    }

    @Override
    public int getKeyVersion() {
      return version;
    }

    @Override
    public int hashCode() {
      return Objects.hash(entityId, version);
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (!(o instanceof CertificateEntry.Key)) {
        return false;
      }
      Key another = (Key) o;
      return this.entityId.equals(another.entityId) && this.version == another.version;
    }
  }
}
