package com.scalar.dl.ledger.crypto;

import com.scalar.dl.ledger.exception.SignatureException;
import com.scalar.dl.ledger.exception.UnloadableKeyException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;

/**
 * A validator used to validate that a given byte array was signed by the private key corresponding
 * to the specified certificate using the specified algorithm.
 */
@Immutable
public class DigitalSignatureValidator implements SignatureValidator {
  private static final String DEFAULT_ALGORITHM = "SHA256withECDSA";
  private final String algorithm;
  private final X509Certificate cert;

  public DigitalSignatureValidator(Path certPath) {
    this(certPath, DEFAULT_ALGORITHM);
  }

  public DigitalSignatureValidator(Path certPath, String algorithm) {
    this(readKeyFile(certPath), algorithm);
  }

  public DigitalSignatureValidator(String certPem) {
    this(certPem, DEFAULT_ALGORITHM);
  }

  public DigitalSignatureValidator(String certPem, String algorithm) {
    this.algorithm = algorithm;
    try {
      CertificateFactory factory = CertificateFactory.getInstance("X.509");
      InputStream input = new ByteArrayInputStream(certPem.getBytes(StandardCharsets.UTF_8));
      this.cert = (X509Certificate) factory.generateCertificate(input);
    } catch (CertificateException e) {
      throw new UnloadableKeyException("Failed in getting a validator from the certificate.", e);
    }
  }

  /**
   * SpotBugs detects Bug Type "CT_CONSTRUCTOR_THROW" saying that "The object under construction
   * remains partially initialized and may be vulnerable to Finalizer attacks."
   */
  @Override
  protected final void finalize() {}

  /**
   * Validates the signature corresponds to the certificate.
   *
   * @param toBeValidated a byte array whose signature will be validated
   * @param signatureBytes the bytes of the signature
   * @return true if the signature corresponds to the toBeValidated bytes; false otherwise
   * @throws SignatureException if it fails to validate
   */
  @Override
  public boolean validate(byte[] toBeValidated, byte[] signatureBytes) {
    try {
      Signature signature = Signature.getInstance(algorithm);
      signature.initVerify(cert.getPublicKey());
      signature.update(toBeValidated);
      return signature.verify(signatureBytes);
    } catch (java.security.SignatureException | NoSuchAlgorithmException | InvalidKeyException e) {
      throw new SignatureException(e.getMessage());
    }
  }

  /**
   * Returns a hash code value for the object.
   *
   * @return a hash code value for this object.
   */
  @Override
  public int hashCode() {
    return Objects.hash(algorithm, cert);
  }

  /**
   * Indicates whether some other object is "equal to" this object. The other object is considered
   * equal if it is also an {@code SignatureValidator} and both instances have the same
   *
   * <ul>
   *   <li>algorithm
   *   <li>cert
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
    if (!(o instanceof DigitalSignatureValidator)) {
      return false;
    }
    DigitalSignatureValidator another = (DigitalSignatureValidator) o;
    return this.algorithm.equals(another.algorithm) && this.cert.equals(another.cert);
  }

  private static String readKeyFile(Path file) {
    try {
      return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UnloadableKeyException(e.getMessage());
    }
  }
}
