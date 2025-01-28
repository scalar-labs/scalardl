package com.scalar.dl.ledger.crypto;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;

/** A HMAC signer for a byte array with a specified secret key. */
@SuppressWarnings("UnstableApiUsage")
@Immutable
public class HmacSigner implements SignatureSigner {
  private final String secret;
  private final HashFunction hash;

  public HmacSigner(String secret) {
    this.secret = secret;
    hash = Hashing.hmacSha256(secret.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Creates a HMAC hash (signature) for the given byte array.
   *
   * @param bytes the byte array
   * @return a HMAC hash
   */
  @Override
  public byte[] sign(byte[] bytes) {
    return hash.newHasher().putBytes(bytes).hash().asBytes();
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
   * equal if it is also an {@link HmacSigner} and both instances have the same
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
    if (!(o instanceof HmacSigner)) {
      return false;
    }
    HmacSigner another = (HmacSigner) o;
    return this.secret.equals(another.secret);
  }
}
