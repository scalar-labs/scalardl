package com.scalar.dl.ledger.crypto;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import org.bouncycastle.util.Arrays;

/** A HMAC signer for a byte array with a specified secret key. */
@SuppressWarnings("UnstableApiUsage")
@Immutable
public class HmacValidator implements SignatureValidator {
  private final String secret;
  private final HashFunction hash;

  public HmacValidator(String secret) {
    this.secret = secret;
    hash = Hashing.hmacSha256(secret.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Validates a given byte array with HMAC.
   *
   * @param toBeValidated a byte array to be validated.
   * @param signatureBytes a hash (signature) derived by HMAC hashing.
   * @return true if the signature corresponds to the toBeValidated bytes; false otherwise
   */
  @Override
  public boolean validate(byte[] toBeValidated, byte[] signatureBytes) {
    byte[] signature = hash.newHasher().putBytes(toBeValidated).hash().asBytes();
    return Arrays.areEqual(signature, signatureBytes);
  }

  /**
   * Returns a hash code value for the object.
   *
   * @return a hash code value for this object.
   */
  @Override
  public int hashCode() {
    return Objects.hash(secret);
  }

  /**
   * Indicates whether some other object is "equal to" this object. The other object is considered
   * equal if it is also an {@link HmacValidator} and both instances have the same
   *
   * <ul>
   *   <li>secret
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
    if (!(o instanceof HmacValidator)) {
      return false;
    }
    HmacValidator another = (HmacValidator) o;
    return this.secret.equals(another.secret);
  }
}
