package com.scalar.dl.ledger.model;

import static com.google.common.base.Preconditions.checkNotNull;

import com.scalar.dl.ledger.crypto.SignatureValidator;
import com.scalar.dl.ledger.error.CommonError;
import com.scalar.dl.ledger.exception.SignatureException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
// non-final for mocking
public class ContractsListingRequest extends AbstractRequest {
  @Nullable private final String contractId;
  private final byte[] signature;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public ContractsListingRequest(
      @Nullable String contractId, String entityId, int keyVersion, byte[] signature) {
    super(entityId, keyVersion);
    this.contractId = contractId;
    this.signature = checkNotNull(signature);
  }

  /**
   * Returns the contract id of the request.
   *
   * @return the contract id of the request
   */
  public Optional<String> getContractId() {
    return Optional.ofNullable(contractId);
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
    return Objects.hash(contractId, Arrays.hashCode(signature));
  }

  /**
   * Indicates whether some other object is "equal to" this object. The other object is considered
   * equal if it is the same instance or if:
   *
   * <ul>
   *   <li>both super class instances are equal and
   *   <li>it is also an {@code ContractsListingRequest} and
   *   <li>both instances have the same contract id and signature.
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
    if (!(o instanceof ContractsListingRequest)) {
      return false;
    }
    ContractsListingRequest other = (ContractsListingRequest) o;
    return Objects.equals(this.contractId, other.contractId)
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
    byte[] bytes = serialize(contractId, getEntityId(), getKeyVersion());

    if (!validator.validate(bytes, signature)) {
      throw new SignatureException(CommonError.REQUEST_SIGNATURE_VALIDATION_FAILED);
    }
  }

  public static byte[] serialize(@Nullable String contractId, String entityId, int keyVersion) {
    byte[] contractIdBytes =
        contractId != null ? contractId.getBytes(StandardCharsets.UTF_8) : new byte[0];
    byte[] entityIdBytes = entityId.getBytes(StandardCharsets.UTF_8);

    ByteBuffer buffer =
        ByteBuffer.allocate(contractIdBytes.length + entityIdBytes.length + Integer.BYTES);
    buffer.put(contractIdBytes);
    buffer.put(entityIdBytes);
    buffer.putInt(keyVersion);
    buffer.rewind();
    return buffer.array();
  }
}
