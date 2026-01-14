package com.scalar.dl.ledger.model;

import static com.google.common.base.Preconditions.checkArgument;

import com.scalar.dl.ledger.crypto.SignatureValidator;
import com.scalar.dl.ledger.error.CommonLedgerError;
import com.scalar.dl.ledger.exception.SignatureException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
public class AssetProofRetrievalRequest extends AbstractRequest {
  @Nullable private final String namespace;
  private final String assetId;
  private final int age;
  private final byte[] signature;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public AssetProofRetrievalRequest(
      @Nullable String namespace,
      String assetId,
      int age,
      String entityId,
      int keyVersion,
      byte[] signature) {
    super(entityId, keyVersion);
    this.namespace = namespace;
    checkArgument(assetId != null);
    this.assetId = assetId;
    this.age = age;
    this.signature = signature;
  }

  /**
   * Returns the namespace of the asset.
   *
   * @return the namespace of the asset
   */
  @Nullable
  public String getNamespace() {
    return namespace;
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
    return Objects.hash(namespace, assetId, age, Arrays.hashCode(signature));
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
    return Objects.equals(namespace, other.namespace)
        && this.assetId.equals(other.assetId)
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
    byte[] bytes = serialize(namespace, assetId, age, getEntityId(), getKeyVersion());

    if (!validator.validate(bytes, signature)) {
      throw new SignatureException(CommonLedgerError.REQUEST_SIGNATURE_VALIDATION_FAILED);
    }
  }

  public static byte[] serialize(
      @Nullable String namespace, String assetId, int age, String entityId, int keyVersion) {
    byte[] namespaceBytes =
        namespace != null ? namespace.getBytes(StandardCharsets.UTF_8) : new byte[0];
    byte[] assetIdBytes = assetId.getBytes(StandardCharsets.UTF_8);
    byte[] entityIdBytes = entityId.getBytes(StandardCharsets.UTF_8);

    ByteBuffer buffer =
        ByteBuffer.allocate(
            namespaceBytes.length
                + assetIdBytes.length
                + Integer.BYTES
                + entityIdBytes.length
                + Integer.BYTES);
    buffer.put(namespaceBytes);
    buffer.put(assetIdBytes);
    buffer.putInt(age);
    buffer.put(entityIdBytes);
    buffer.putInt(keyVersion);
    buffer.rewind();
    return buffer.array();
  }
}
