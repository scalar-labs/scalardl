package com.scalar.dl.ledger.proof;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ComparisonChain;
import com.scalar.dl.ledger.util.Argument;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;

/** A proof stored in auditor to validate ledger states. */
@Immutable
public class RequestProof {
  private final String nonce;
  private final String contractId;
  private final String argument;
  private final String contractArgument;
  private final String entityId;
  private final int keyVersion;
  private final byte[] signature;
  private final Key key;

  /**
   * Constructs an {@code RequestProof} with the specified {@link RequestProof.Builder}.
   *
   * @param builder an {@code RequestProof.Builder} object
   */
  private RequestProof(RequestProof.Builder builder) {
    this.nonce = builder.nonce;
    this.contractId = builder.contractId;
    this.argument = builder.argument;
    this.contractArgument = Argument.getContractArgument(builder.argument);
    this.entityId = builder.entityId;
    this.keyVersion = builder.keyVersion;
    this.signature = builder.signature;
    this.key = new Key(nonce);
  }

  /**
   * Returns the {@code Key} of the {@code RequestProof}.
   *
   * @return the {@code Key} of the {@code RequestProof}
   */
  public Key getKey() {
    return key;
  }

  /**
   * Returns the nonce of the proof.
   *
   * @return a nonce created in the client and given from the server
   */
  public String getNonce() {
    return nonce;
  }

  public String getContractId() {
    return contractId;
  }

  public String getArgument() {
    return argument;
  }

  public String getContractArgument() {
    return contractArgument;
  }

  public String getEntityId() {
    return entityId;
  }

  public int getKeyVersion() {
    return keyVersion;
  }

  /**
   * Returns the signature of the proof.
   *
   * @return a signature calculated for the proof
   */
  @SuppressFBWarnings("EI_EXPOSE_REP")
  public byte[] getSignature() {
    return signature;
  }

  /**
   * Returns a hash code value for the object.
   *
   * @return a hash code value for this object.
   */
  @Override
  public int hashCode() {
    return Objects.hash(
        nonce, contractId, argument, entityId, keyVersion, Arrays.hashCode(signature));
  }

  /**
   * Indicates whether some other object is "equal to" this object. The other object is considered
   * equal if:
   *
   * <ul>
   *   <li>it is also an {@code RequestProof}, and
   *   <li>both instances have the same age, hash, nonce and signature.
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
    if (!(o instanceof RequestProof)) {
      return false;
    }
    RequestProof other = (RequestProof) o;
    return this.nonce.equals(other.nonce)
        && this.contractId.equals(other.contractId)
        && this.argument.equals(other.argument)
        && this.entityId.equals(other.entityId)
        && this.keyVersion == other.keyVersion
        && Arrays.equals(this.signature, other.signature);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("nonce", nonce)
        .add("contractId", contractId)
        .add("argument", argument)
        .add("entityId", entityId)
        .add("keyVersion", keyVersion)
        .add("signature", Base64.getEncoder().encodeToString(signature))
        .toString();
  }

  public static RequestProof.Builder newBuilder() {
    return new RequestProof.Builder();
  }

  public static final class Builder {
    private String nonce;
    private String contractId;
    private String argument;
    private String entityId;
    private int keyVersion;
    private byte[] signature;

    Builder() {
      this.nonce = null;
      this.contractId = null;
      this.argument = null;
      this.entityId = null;
      this.keyVersion = 1;
      this.signature = null;
    }

    public RequestProof.Builder nonce(String nonce) {
      checkArgument(nonce != null);
      this.nonce = nonce;
      return this;
    }

    public RequestProof.Builder contractId(String contractId) {
      checkArgument(contractId != null);
      this.contractId = contractId;
      return this;
    }

    public RequestProof.Builder argument(String argument) {
      checkArgument(argument != null);
      this.argument = argument;
      return this;
    }

    public RequestProof.Builder entityId(String entityId) {
      checkArgument(entityId != null);
      this.entityId = entityId;
      return this;
    }

    public RequestProof.Builder keyVersion(int keyVersion) {
      checkArgument(keyVersion >= 1);
      this.keyVersion = keyVersion;
      return this;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public RequestProof.Builder signature(byte[] signature) {
      checkArgument(signature != null);
      this.signature = signature;
      return this;
    }

    public RequestProof build() {
      if (nonce == null
          || contractId == null
          || argument == null
          || entityId == null
          || keyVersion < 1
          || signature == null) {
        throw new IllegalArgumentException("Required fields are not given.");
      }
      return new RequestProof(this);
    }
  }

  @Immutable
  public static class Key implements Comparable<Key> {
    private final String nonce;

    public Key(String nonce) {
      this.nonce = checkNotNull(nonce);
    }

    public String getNonce() {
      return nonce;
    }

    @Override
    public int hashCode() {
      return Objects.hash(nonce);
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (!(o instanceof RequestProof.Key)) {
        return false;
      }
      Key another = (Key) o;
      return this.nonce.equals(another.nonce);
    }

    @Override
    public int compareTo(Key o) {
      return ComparisonChain.start().compare(this.nonce, o.nonce).result();
    }
  }
}
