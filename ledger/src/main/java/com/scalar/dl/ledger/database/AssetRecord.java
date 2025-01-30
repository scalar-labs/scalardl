package com.scalar.dl.ledger.database;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ComparisonChain;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;

public class AssetRecord implements InternalAsset {
  public static final String ID = "id";
  public static final String AGE = "age";
  public static final String NONCE = "nonce";
  public static final String ARGUMENT = "argument";
  public static final String CONTRACT_ID = "contract_id";
  public static final String INPUT = "input";
  public static final String OUTPUT = "output";
  public static final String SIGNATURE = "signature";
  public static final String PREV_HASH = "prev_hash";
  public static final String HASH = "hash";
  private final String id;
  private final int age;
  private final String nonce;
  private final String data;
  private final String input;
  private final byte[] signature;
  private final String contractId;
  private final String argument;
  private final byte[] hash;
  private final byte[] prevHash;
  private final Key key;

  private AssetRecord(Builder builder) {
    this.id = builder.id;
    this.age = builder.age;
    this.nonce = builder.nonce;
    this.argument = builder.argument;
    this.contractId = builder.contractId;
    this.input = builder.input;
    this.data = builder.data;
    this.signature = builder.signature;
    this.prevHash = builder.prevHash;
    this.hash = builder.hash;
    this.key = new Key(id, age);
  }

  @Override
  public String id() {
    return id;
  }

  @Override
  public int age() {
    return age;
  }

  public String nonce() {
    return nonce;
  }

  @Override
  public String argument() {
    return argument;
  }

  @Override
  public String contractId() {
    return contractId;
  }

  @Override
  public String input() {
    return input;
  }

  @Override
  public String data() {
    return data;
  }

  @Override
  @SuppressFBWarnings("EI_EXPOSE_REP")
  public byte[] signature() {
    return signature;
  }

  @Override
  @SuppressFBWarnings("EI_EXPOSE_REP")
  public byte[] prevHash() {
    return prevHash;
  }

  @Override
  @SuppressFBWarnings("EI_EXPOSE_REP")
  public byte[] hash() {
    return hash;
  }

  public Key getKey() {
    return key;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        id,
        age,
        nonce,
        data,
        input,
        Arrays.hashCode(signature),
        contractId,
        argument,
        Arrays.hashCode(hash),
        Arrays.hashCode(prevHash));
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof AssetRecord)) {
      return false;
    }
    AssetRecord another = (AssetRecord) o;
    return this.id.equals(another.id)
        && this.age == another.age
        && Objects.equals(nonce, another.nonce)
        && Objects.equals(data, another.data)
        && Objects.equals(input, another.input)
        && Arrays.equals(signature, another.signature)
        && Objects.equals(contractId, another.contractId)
        && Objects.equals(argument, another.argument)
        && Arrays.equals(hash, another.hash)
        && Arrays.equals(prevHash, another.prevHash);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("id", id)
        .add("age", age)
        .add("nonce", nonce)
        .add("input", input)
        .add("output", data)
        .add("contractId", contractId)
        .add("argument", argument)
        .add("signature", signature)
        .add("hash", hash)
        .add("prevHash", prevHash)
        .toString();
  }

  public static AssetRecord.Builder newBuilder() {
    return new AssetRecord.Builder();
  }

  public static AssetRecord.Builder newBuilder(AssetRecord prototype) {
    return new AssetRecord.Builder(prototype);
  }

  public static final class Builder {
    private String id;
    private int age;
    private String nonce;
    private String data;
    private String input;
    private byte[] signature;
    private String contractId;
    private String argument;
    private byte[] hash;
    private byte[] prevHash;

    Builder() {
      this.id = null;
      this.age = -1;
      this.nonce = null;
      this.data = null;
      this.input = null;
      this.signature = null;
      this.contractId = null;
      this.argument = null;
      this.hash = null;
      this.prevHash = null;
    }

    Builder(AssetRecord prototype) {
      this.id = prototype.id;
      this.age = prototype.age;
      this.nonce = prototype.nonce;
      this.data = prototype.data;
      this.input = prototype.input;
      this.signature = prototype.signature;
      this.contractId = prototype.contractId;
      this.argument = prototype.argument;
      this.hash = prototype.hash;
      this.prevHash = prototype.prevHash;
    }

    public AssetRecord.Builder id(String id) {
      checkArgument(id != null);
      this.id = id;
      return this;
    }

    public AssetRecord.Builder age(int age) {
      checkArgument(age >= 0);
      this.age = age;
      return this;
    }

    public AssetRecord.Builder nonce(String nonce) {
      checkArgument(nonce != null);
      this.nonce = nonce;
      return this;
    }

    public AssetRecord.Builder data(String data) {
      checkArgument(data != null);
      this.data = data;
      return this;
    }

    public AssetRecord.Builder input(String input) {
      checkArgument(input != null);
      this.input = input;
      return this;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AssetRecord.Builder signature(byte[] signature) {
      checkArgument(signature != null);
      this.signature = signature;
      return this;
    }

    public AssetRecord.Builder contractId(String contractId) {
      checkArgument(contractId != null);
      this.contractId = contractId;
      return this;
    }

    public AssetRecord.Builder argument(String argument) {
      checkArgument(argument != null);
      this.argument = argument;
      return this;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AssetRecord.Builder hash(byte[] hash) {
      checkArgument(hash != null);
      this.hash = hash;
      return this;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AssetRecord.Builder prevHash(byte[] prevHash) {
      // prevHash can be null in an age-0 record
      this.prevHash = prevHash;
      return this;
    }

    public AssetRecord build() {
      if (id == null || age < 0) {
        throw new IllegalArgumentException("required values are not set properly.");
      }
      return new AssetRecord(this);
    }
  }

  @Immutable
  public static class Key implements Comparable<AssetRecord.Key> {
    private final String id;
    private final int age;

    public Key(String id, int age) {
      this.id = checkNotNull(id);
      checkArgument(age >= 0, "age must be bigger or equal to 0.");
      this.age = age;
    }

    public String getId() {
      return id;
    }

    public int getAge() {
      return age;
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, age);
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (!(o instanceof AssetRecord.Key)) {
        return false;
      }
      AssetRecord.Key another = (AssetRecord.Key) o;
      return this.id.equals(another.id) && this.age == another.age;
    }

    @Override
    public int compareTo(AssetRecord.Key o) {
      return ComparisonChain.start().compare(this.id, o.id).compare(this.age, o.age).result();
    }
  }
}
