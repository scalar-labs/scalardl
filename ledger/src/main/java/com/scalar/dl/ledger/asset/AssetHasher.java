package com.scalar.dl.ledger.asset;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.scalar.dl.ledger.error.CommonError;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javax.annotation.concurrent.Immutable;

/** A sha256 hasher for assets */
@Immutable
public class AssetHasher {
  private final HashCode hashCode;

  private AssetHasher(HashCode hashCode) {
    this.hashCode = hashCode;
  }

  public byte[] get() {
    return hashCode.asBytes();
  }

  public static class Builder {
    private String id;
    private int age;
    private String input;
    private String output;
    private String contractId;
    private String argument;
    private byte[] signature;
    private byte[] prevHash;

    public Builder() {
      age = -1;
    }

    public Builder id(String id) {
      this.id = checkNotNull(id);
      return this;
    }

    public Builder age(int age) {
      this.age = age;
      return this;
    }

    public Builder input(String input) {
      this.input = input;
      return this;
    }

    public Builder output(String output) {
      this.output = output;
      return this;
    }

    public Builder contractId(String contractId) {
      this.contractId = contractId;
      return this;
    }

    public Builder argument(String argument) {
      this.argument = argument;
      return this;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public Builder signature(byte[] signature) {
      this.signature = signature;
      return this;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public Builder prevHash(byte[] prevHash) {
      this.prevHash = prevHash;
      return this;
    }

    /**
     * Compute the hash of the built asset.
     *
     * @return an {@code AssetHasher} containing the hash of an asset
     */
    public AssetHasher build() {
      if (id == null
          || age < 0
          || input == null
          || output == null
          || contractId == null
          || argument == null
          || signature == null
          || (age > 0 && prevHash == null)) {
        throw new IllegalArgumentException(
            CommonError.REQUIRED_FIELDS_ARE_NOT_GIVEN.buildMessage());
      }

      // TODO: generalize
      Hasher hasher =
          Hashing.sha256()
              .newHasher()
              .putString(id, Charsets.UTF_8)
              .putInt(age)
              .putString(input, Charsets.UTF_8)
              .putString(output, Charsets.UTF_8)
              .putString(contractId, Charsets.UTF_8)
              .putString(argument, Charsets.UTF_8)
              .putBytes(signature);

      if (prevHash != null) {
        hasher.putBytes(prevHash);
      }
      return new AssetHasher(hasher.hash());
    }
  }
}
