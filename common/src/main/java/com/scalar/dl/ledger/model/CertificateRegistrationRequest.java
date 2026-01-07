package com.scalar.dl.ledger.model;

import static com.google.common.base.Preconditions.checkNotNull;

import com.scalar.dl.ledger.crypto.SignatureValidator;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;

/**
 * A request for certification registration.
 *
 * @author Hiroyuki Yamada
 */
@Immutable
public class CertificateRegistrationRequest extends AbstractRequest {
  private final String certPem;

  /**
   * Constructs a {@code CertificateRegistrationRequest} with the specified entity id, the version
   * of a certificate and the certificate in PEM format.
   *
   * @param entityId an entity id that holds a certificate
   * @param certVersion the version of the certificate
   * @param certPem the certificate in PEM format
   */
  public CertificateRegistrationRequest(String entityId, int certVersion, String certPem) {
    super(entityId, certVersion);
    this.certPem = checkNotNull(certPem);
  }

  /**
   * Returns the certificate in PEM format.
   *
   * @return the certificate in PEM format
   */
  public String getCertPem() {
    return certPem;
  }

  @Override
  public void validateWith(SignatureValidator validator) {
    // This request can not be validated since it is the one for registering a certificate.
  }

  /**
   * Returns a hash code value for the object.
   *
   * @return a hash code value for this object.
   */
  @Override
  public int hashCode() {
    return Objects.hash(certPem);
  }

  /**
   * Indicates whether some other object is "equal to" this object. The other object is considered
   * equal if it is the same instance or if:
   *
   * <ul>
   *   <li>both super class instances are equal and
   *   <li>it is also an {@code CertificateRegistrationRequest} and
   *   <li>both instances have the same pem
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
    if (!(o instanceof CertificateRegistrationRequest)) {
      return false;
    }
    CertificateRegistrationRequest other = (CertificateRegistrationRequest) o;
    return this.certPem.equals(other.certPem);
  }
}
