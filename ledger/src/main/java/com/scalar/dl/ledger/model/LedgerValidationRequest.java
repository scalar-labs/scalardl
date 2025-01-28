package com.scalar.dl.ledger.model;

import com.scalar.dl.ledger.crypto.SignatureValidator;
import com.scalar.dl.ledger.error.LedgerError;
import com.scalar.dl.ledger.exception.SignatureException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

public class LedgerValidationRequest extends AbstractRequest {
  private final String assetId;
  private final int startAge;
  private final int endAge;
  private final byte[] signature;

  /**
   * Constructs a {@code LedgerValidationRequest} with the specified asset id, entity ID, key
   * version, signature of the request, and client-side proof.
   *
   * @param assetId an id of an asset
   * @param startAge an age to be validated from
   * @param endAge an age to be validated to
   * @param entityId an entity ID
   * @param keyVersion the version of a digital signature certificate or a HMAC secret key.
   * @param signature a signature of the request
   */
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public LedgerValidationRequest(
      String assetId, int startAge, int endAge, String entityId, int keyVersion, byte[] signature) {
    super(entityId, keyVersion);
    this.assetId = assetId;
    this.startAge = startAge;
    this.endAge = endAge;
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
   * Returns the age of the asset to be validated from.
   *
   * @return the age of the asset to be validated from
   */
  public int getStartAge() {
    return startAge;
  }

  /**
   * Returns the age of the asset to be validated to.
   *
   * @return the age of the asset to be validated to
   */
  public int getEndAge() {
    return endAge;
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
    return Objects.hash(assetId, startAge, endAge, Arrays.hashCode(signature));
  }

  /**
   * Indicates whether some other object is "equal to" this object. The other object is considered
   * equal if it is the same instance or if:
   *
   * <ul>
   *   <li>both super class instances are equal and
   *   <li>it is also an {@code LedgerValidationRequest} and
   *   <li>both instances have the same asset id, signature and proof.
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
    if (!(o instanceof LedgerValidationRequest)) {
      return false;
    }
    LedgerValidationRequest other = (LedgerValidationRequest) o;
    return (this.assetId.equals(other.assetId)
        && this.startAge == other.startAge
        && this.endAge == other.endAge
        && Arrays.equals(this.signature, other.signature));
  }

  /**
   * Validates if the request is not tampered.
   *
   * @param validator a {@link SignatureValidator}
   * @throws SignatureException if the request is invalid.
   */
  @Override
  public void validateWith(SignatureValidator validator) {
    byte[] bytes = serialize(assetId, startAge, endAge, getEntityId(), getKeyVersion());

    if (!validator.validate(bytes, signature)) {
      throw new SignatureException(LedgerError.REQUEST_SIGNATURE_VALIDATION_FAILED);
    }
  }

  public static byte[] serialize(
      String assetId, int startAge, int endAge, String entityId, int keyVersion) {
    ByteBuffer buffer =
        ByteBuffer.allocate(
            assetId.getBytes(StandardCharsets.UTF_8).length
                + Integer.BYTES
                + Integer.BYTES
                + entityId.getBytes(StandardCharsets.UTF_8).length
                + Integer.BYTES);
    buffer.put(assetId.getBytes(StandardCharsets.UTF_8));
    buffer.putInt(startAge);
    buffer.putInt(endAge);
    buffer.put(entityId.getBytes(StandardCharsets.UTF_8));
    buffer.putInt(keyVersion);
    buffer.rewind();
    return buffer.array();
  }
}
