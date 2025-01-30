package com.scalar.dl.ledger.model;

import static com.google.common.base.Preconditions.checkArgument;

import com.scalar.dl.ledger.crypto.SignatureValidator;
import com.scalar.dl.ledger.exception.SignatureException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;

@Immutable
public class AssetProofRetrievalRequest extends AbstractRequest {
  private final String assetId;
  private final int age;
  private final byte[] signature;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public AssetProofRetrievalRequest(
      String assetId, int age, String entityId, int keyVersion, byte[] signature) {
    super(entityId, keyVersion);
    checkArgument(assetId != null);
    this.assetId = assetId;
    this.age = age;
    this.signature = signature;
  }

  /**
   * Returns the id of the asset.
   *
   * @return the id of the asset
   */
  public String getAssetId() {
    return assetId;
  }

  /**
   * Returns the age of the asset.
   *
   * @return the age of the asset
   */
  public int getAge() {
    return age;
  }

  /**
   * Returns the signature of the request.
   *
   * @return the signature of the request
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
    return Objects.hash(assetId, age, Arrays.hashCode(signature));
  }

  @Override
  public boolean equals(Object o) {
    if (!super.equals(o)) {
      return false;
    }
    if (o == this) {
      return true;
    }
    if (!(o instanceof AssetProofRetrievalRequest)) {
      return false;
    }
    AssetProofRetrievalRequest other = (AssetProofRetrievalRequest) o;
    return this.assetId.equals(other.assetId)
        && this.age == other.age
        && Arrays.equals(this.signature, other.signature);
  }

  /**
   * Validates if the request is not tampered.
   *
   * @param validator a {@link SignatureValidator}
   * @throws SignatureException if the request is invalid.
   */
  @Override
  public void validateWith(SignatureValidator validator) {
    // age is not used for creating a signature on purpose
    byte[] bytes = serialize(assetId, age, getEntityId(), getKeyVersion());

    if (!validator.validate(bytes, signature)) {
      throw new SignatureException("The request signature can't be validated with the validator.");
    }
  }

  public static byte[] serialize(String assetId, int age, String entityId, int keyVersion) {
    ByteBuffer buffer =
        ByteBuffer.allocate(
            assetId.getBytes(StandardCharsets.UTF_8).length
                + Integer.BYTES
                + entityId.getBytes(StandardCharsets.UTF_8).length
                + Integer.BYTES);
    buffer.put(assetId.getBytes(StandardCharsets.UTF_8));
    buffer.putInt(age);
    buffer.put(entityId.getBytes(StandardCharsets.UTF_8));
    buffer.putInt(keyVersion);
    buffer.rewind();
    return buffer.array();
  }
}
