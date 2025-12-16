package com.scalar.dl.ledger.proof;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ComparisonChain;
import com.scalar.dl.ledger.crypto.SignatureValidator;
import com.scalar.dl.ledger.error.CommonError;
import com.scalar.dl.ledger.exception.SignatureException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/** A proof stored in client-side to validate the server ledger states. */
@Immutable
public class AssetProof {
  private final String namespace;
  private final String id;
  private final int age;
  private final String nonce;
  private final String input;
  private final byte[] hash;
  private final byte[] prevHash;
  private final byte[] signature;
  private final AssetProof.Key key;

  /**
   * Constructs an {@code AssetProof} with the specified {@link AssetProof.Builder}.
   *
   * @param builder an {@code AssetProof.Builder} object
   */
  private AssetProof(Builder builder) {
    this.namespace = builder.namespace;
    this.id = builder.id;
    this.age = builder.age;
    this.nonce = builder.nonce;
    this.input = builder.input;
    this.hash = builder.hash;
    this.prevHash = builder.prevHash;
    this.signature = builder.signature;
    this.key = new AssetProof.Key(namespace, id, age);
  }

  /**
   * Constructs an {@code AssetProof} with the specified {@link com.scalar.dl.rpc.AssetProof}
   *
   * @param proof a {@code com.scalar.rpc.AssetProof} object
   */
  public AssetProof(com.scalar.dl.rpc.AssetProof proof) {
    this.namespace = proof.getNamespace();
    this.id = proof.getAssetId();
    this.age = proof.getAge();
    this.nonce = proof.getNonce();
    this.input = proof.getInput();
    this.hash = proof.getHash().toByteArray();
    this.prevHash = proof.getPrevHash().toByteArray();
    this.signature = proof.getSignature().toByteArray();
    this.key = new AssetProof.Key(namespace, id, age);
  }

  /**
   * Returns the {@code Key} of the {@code AssetProof}.
   *
   * @return the {@code Key} of the {@code AssetProof}
   */
  public AssetProof.Key getKey() {
    return key;
  }

  /**
   * Returns the asset's namespace.
   *
   * @return the asset's namespace
   */
  public String getNamespace() {
    return namespace;
  }

  /**
   * Returns the asset's id.
   *
   * @return the asset's id
   */
  public String getId() {
    return id;
  }

  /**
   * Returns the asset entry's age assigned by the server.
   *
   * @return the asset entry's age assigned by the server
   */
  public int getAge() {
    return age;
  }

  /**
   * Returns the nonce of the asset entry.
   *
   * @return a nonce created in the client and given from the server
   */
  public String getNonce() {
    return nonce;
  }

  public String getInput() {
    return input;
  }

  /**
   * Returns the hash of the asset entry.
   *
   * @return a hash value calculated for the asset in the server
   */
  public byte[] getHash() {
    return Arrays.copyOf(hash, hash.length);
  }

  /**
   * Returns the prev_hash of the asset entry.
   *
   * @return a prev_hash value calculated for the asset in the server
   */
  @Nullable
  public byte[] getPrevHash() {
    if (prevHash == null) {
      return null;
    }
    return Arrays.copyOf(prevHash, prevHash.length);
  }

  /**
   * Returns the signature of the proof entry.
   *
   * @return a signature calculated for the proof in the server
   */
  public byte[] getSignature() {
    return Arrays.copyOf(signature, signature.length);
  }

  /**
   * Validates if the proof is not tampered.
   *
   * @param validator a {@link SignatureValidator}
   * @throws SignatureException if the proof is invalid.
   */
  public void validateWith(SignatureValidator validator) {
    byte[] bytes = serialize(namespace, id, age, nonce, input, hash, prevHash);

    if (!validator.validate(bytes, signature)) {
      throw new SignatureException(CommonError.PROOF_SIGNATURE_VALIDATION_FAILED);
    }
  }

  /**
   * Returns a hash code value for the object.
   *
   * @return a hash code value for this object.
   */
  @Override
  public int hashCode() {
    return Objects.hash(
        namespace,
        id,
        age,
        nonce,
        input,
        Arrays.hashCode(hash),
        Arrays.hashCode(prevHash),
        Arrays.hashCode(signature));
  }

  /**
   * Indicates whether some other object is "equal to" this object. The other object is considered
   * equal if:
   *
   * <ul>
   *   <li>it is also an {@code AssetProof}, and
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
    if (!(o instanceof AssetProof)) {
      return false;
    }
    AssetProof other = (AssetProof) o;
    return this.namespace.equals(other.namespace)
        && this.id.equals(other.id)
        && this.age == other.age
        && this.nonce.equals(other.nonce)
        && this.input.equals(other.input)
        && Arrays.equals(this.hash, other.hash)
        && Arrays.equals(this.prevHash, other.prevHash)
        && Arrays.equals(this.signature, other.signature);
  }

  /**
   * Indicates whether some other object is "equal to" this object except for signature. The other
   * object is considered equal if:
   *
   * <ul>
   *   <li>it is also an {@code AssetProof}, and
   *   <li>both instances have the same age, hash and nonce.
   * </ul>
   *
   * @param o an object to be tested for equality
   * @return {@code true} if the other object is "equal to" this object otherwise {@code false}
   */
  public boolean valueEquals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof AssetProof)) {
      return false;
    }
    AssetProof other = (AssetProof) o;
    return this.namespace.equals(other.namespace)
        && this.id.equals(other.id)
        && this.age == other.age
        && this.nonce.equals(other.nonce)
        && this.input.equals(other.input)
        && Arrays.equals(this.hash, other.hash)
        && Arrays.equals(this.prevHash, other.prevHash);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("namespace", namespace)
        .add("id", id)
        .add("age", age)
        .add("nonce", nonce)
        .add("input", input)
        .add("hash", Base64.getEncoder().encodeToString(hash))
        .add("prev_hash", Base64.getEncoder().encodeToString(prevHash))
        .add("signature", Base64.getEncoder().encodeToString(signature))
        .toString();
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static byte[] serialize(
      String namespace,
      String id,
      int age,
      String nonce,
      String input,
      byte[] hash,
      byte[] prevHash) {
    int prevHashLength = 0;
    if (prevHash != null) {
      prevHashLength = prevHash.length;
    }
    ByteBuffer buffer =
        ByteBuffer.allocate(
            namespace.getBytes(StandardCharsets.UTF_8).length
                + id.getBytes(StandardCharsets.UTF_8).length
                + Integer.BYTES
                + nonce.getBytes(StandardCharsets.UTF_8).length
                + input.getBytes(StandardCharsets.UTF_8).length
                + hash.length
                + prevHashLength);
    buffer.put(namespace.getBytes(StandardCharsets.UTF_8));
    buffer.put(id.getBytes(StandardCharsets.UTF_8));
    buffer.putInt(age);
    buffer.put(nonce.getBytes(StandardCharsets.UTF_8));
    buffer.put(input.getBytes(StandardCharsets.UTF_8));
    buffer.put(hash);
    if (prevHash != null) {
      buffer.put(prevHash);
    }
    buffer.rewind();
    return buffer.array();
  }

  public static final class Builder {
    private String namespace;
    private String id;
    private int age;
    private String nonce;
    private String input;
    private byte[] hash;
    private byte[] prevHash;
    private byte[] signature;

    Builder() {
      this.namespace = null;
      this.id = null;
      this.age = -1;
      this.nonce = null;
      this.input = null;
      this.hash = null;
      this.prevHash = null;
      this.signature = null;
    }

    public Builder namespace(String namespace) {
      checkArgument(namespace != null);
      this.namespace = namespace;
      return this;
    }

    public Builder id(String id) {
      checkArgument(id != null);
      this.id = id;
      return this;
    }

    public Builder age(int age) {
      checkArgument(age >= 0);
      this.age = age;
      return this;
    }

    public Builder nonce(String nonce) {
      checkArgument(nonce != null);
      this.nonce = nonce;
      return this;
    }

    public Builder input(String input) {
      checkArgument(input != null);
      this.input = input;
      return this;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public Builder hash(byte[] hash) {
      checkArgument(hash != null);
      this.hash = hash;
      return this;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public Builder prevHash(byte[] prevHash) {
      this.prevHash = prevHash;
      return this;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public Builder signature(byte[] signature) {
      checkArgument(signature != null);
      this.signature = signature;
      return this;
    }

    public AssetProof build() {
      if (namespace == null
          || id == null
          || age < 0
          || nonce == null
          || input == null
          || hash == null
          || signature == null
          || (age >= 1 && prevHash == null)) {
        throw new IllegalArgumentException(
            CommonError.REQUIRED_FIELDS_ARE_NOT_GIVEN.buildMessage());
      }
      return new AssetProof(this);
    }
  }

  @Immutable
  public static class Key implements Comparable<AssetProof.Key> {
    private final String namespace;
    private final String id;
    private final int age;

    public Key(String namespace, String id, int age) {
      this.namespace = checkNotNull(namespace);
      this.id = checkNotNull(id);
      this.age = age;
    }

    public String getNamespace() {
      return namespace;
    }

    public String getId() {
      return id;
    }

    public int getAge() {
      return age;
    }

    @Override
    public int hashCode() {
      return Objects.hash(namespace, id, age);
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (!(o instanceof AssetProof.Key)) {
        return false;
      }
      AssetProof.Key another = (AssetProof.Key) o;
      return this.namespace.equals(another.namespace)
          && this.id.equals(another.id)
          && this.age == another.age;
    }

    @Override
    public int compareTo(AssetProof.Key o) {
      return ComparisonChain.start()
          .compare(this.namespace, o.namespace)
          .compare(this.id, o.id)
          .compare(this.age, o.age)
          .result();
    }
  }

  @Immutable
  public static class Range implements Comparable<AssetProof.Range> {
    private final String namespace;
    private final String id;
    private final int startAge;
    private final int endAge;
    private final boolean startInclusive;
    private final boolean endInclusive;
    private final Order order;
    private final int limit;

    private Range(Builder builder) {
      this.namespace = builder.namespace;
      this.id = builder.id;
      this.startAge = builder.startAge;
      this.startInclusive = builder.startInclusive;
      this.endAge = builder.endAge;
      this.endInclusive = builder.endInclusive;
      this.order = builder.order;
      this.limit = builder.limit;
    }

    public String getNamespace() {
      return namespace;
    }

    public String getId() {
      return id;
    }

    public int getStartAge() {
      return startAge;
    }

    public boolean isStartInclusive() {
      return startInclusive;
    }

    public int getEndAge() {
      return endAge;
    }

    public boolean isEndInclusive() {
      return endInclusive;
    }

    public Order getOrder() {
      return order;
    }

    public int getLimit() {
      return limit;
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          namespace, id, startAge, startInclusive, endAge, endInclusive, order, limit);
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (!(o instanceof AssetProof.Range)) {
        return false;
      }
      AssetProof.Range another = (AssetProof.Range) o;
      return this.namespace.equals(another.namespace)
          && this.id.equals(another.id)
          && this.startAge == another.startAge
          && this.startInclusive == another.startInclusive
          && this.endAge == another.endAge
          && this.endInclusive == another.endInclusive
          && this.order == another.order
          && this.limit == another.limit;
    }

    @Override
    public int compareTo(Range o) {
      return ComparisonChain.start()
          .compare(this.namespace, o.namespace)
          .compare(this.id, o.id)
          .compare(this.startAge, o.startAge)
          .compare(this.endAge, o.endAge)
          .compareTrueFirst(this.startInclusive, o.startInclusive)
          .compareTrueFirst(this.endInclusive, o.endInclusive)
          .compare(this.order, o.order)
          .compare(this.limit, o.limit)
          .result();
    }

    public static AssetProof.Range.Builder newBuilder() {
      return new AssetProof.Range.Builder();
    }

    public static final class Builder {
      private String namespace;
      private String id;
      private int startAge;
      private boolean startInclusive;
      private int endAge;
      private boolean endInclusive;
      private Order order;
      private int limit;

      Builder() {
        this.namespace = null;
        this.id = null;
        this.startAge = 0;
        this.startInclusive = true;
        this.endAge = Integer.MAX_VALUE;
        this.endInclusive = true;
        this.order = Order.ASC;
        this.limit = -1;
      }

      public Range.Builder namespace(String namespace) {
        checkArgument(namespace != null);
        this.namespace = namespace;
        return this;
      }

      public Range.Builder id(String id) {
        checkArgument(id != null);
        this.id = id;
        return this;
      }

      public Range.Builder startAge(int startAge) {
        checkArgument(startAge >= 0);
        this.startAge = startAge;
        return this;
      }

      public Range.Builder startInclusive(boolean startInclusive) {
        this.startInclusive = startInclusive;
        return this;
      }

      public Range.Builder endAge(int endAge) {
        checkArgument(endAge >= 0);
        this.endAge = endAge;
        return this;
      }

      public Range.Builder endInclusive(boolean endInclusive) {
        this.endInclusive = endInclusive;
        return this;
      }

      public Range.Builder order(Order order) {
        this.order = order;
        return this;
      }

      public Range.Builder limit(int limit) {
        this.limit = limit;
        return this;
      }

      public Range build() {
        return new Range(this);
      }
    }

    public enum Order {
      ASC,
      DESC
    }
  }
}
