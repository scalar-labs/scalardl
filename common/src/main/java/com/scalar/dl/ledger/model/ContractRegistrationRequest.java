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

/**
 * A request for contract registration.
 *
 * @author Hiroyuki Yamada
 */
@Immutable
// non-final for mocking
public class ContractRegistrationRequest extends AbstractRequest {
  private final String contractId;
  private final String contractBinaryName;
  private final byte[] contractByteCode;
  @Nullable private final String contractProperties;
  private final byte[] signature;

  /**
   * Constructs a {@code ContractRegistrationRequest} with the specified contract id, contract
   * binary name, contract itself in byte-code format, entity ID, key version and signature of the
   * request.
   *
   * @param contractId an id of a registered contract to execute
   * @param contractBinaryName a binary name of a registered contract to execute
   * @param contractByteCode a contract itself in byte-code format
   * @param contractProperties properties in json format
   * @param entityId an entity ID
   * @param keyVersion the version of a digital signature certificate or a HMAC secret key
   * @param signature a signature of the request
   */
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public ContractRegistrationRequest(
      String contractId,
      String contractBinaryName,
      byte[] contractByteCode,
      @Nullable String contractProperties,
      String entityId,
      int keyVersion,
      byte[] signature) {
    super(entityId, keyVersion);
    this.contractId = checkNotNull(contractId);
    this.contractBinaryName = checkNotNull(contractBinaryName);
    this.contractByteCode = checkNotNull(contractByteCode);
    this.contractProperties = contractProperties;
    this.signature = checkNotNull(signature);
  }

  /**
   * Returns the id of the contract.
   *
   * @return the id of the contract
   */
  public String getContractId() {
    return contractId;
  }

  /**
   * Returns the binary name of the contract.
   *
   * @return the binary name of the contract
   */
  public String getContractBinaryName() {
    return contractBinaryName;
  }

  /**
   * Returns the contract in byte-code format.
   *
   * @return the contract in byte-code format
   */
  @SuppressFBWarnings("EI_EXPOSE_REP")
  public byte[] getContractByteCode() {
    return contractByteCode;
  }

  /**
   * Returns the properties of the contract.
   *
   * @return an {@code Optional} with the properties of the contract
   */
  public Optional<String> getContractProperties() {
    return Optional.ofNullable(contractProperties);
  }

  /**
   * Returns the signature of the request. The signature is either a HMAC signature or a digital
   * signature.
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
    return Objects.hash(
        contractId,
        contractBinaryName,
        Arrays.hashCode(contractByteCode),
        contractProperties,
        Arrays.hashCode(signature));
  }

  /**
   * Indicates whether some other object is "equal to" this object. The other object is considered
   * equal if it is the same instance or if:
   *
   * <ul>
   *   <li>both super class instances are equal and
   *   <li>it is also an {@code ContractRegistrationRequest} and
   *   <li>both instances have the same items such as contract id, contract name, contract, and
   *       signatures.
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
    if (!(o instanceof ContractRegistrationRequest)) {
      return false;
    }
    ContractRegistrationRequest other = (ContractRegistrationRequest) o;
    return (this.contractId.equals(other.contractId)
        && this.contractBinaryName.equals(other.contractBinaryName)
        && Arrays.equals(this.contractByteCode, other.contractByteCode)
        && Objects.equals(this.contractProperties, other.contractProperties)
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
    byte[] bytes =
        serialize(
            contractId,
            contractBinaryName,
            contractByteCode,
            contractProperties,
            getEntityId(),
            getKeyVersion());

    if (!validator.validate(bytes, signature)) {
      throw new SignatureException(CommonError.REQUEST_SIGNATURE_VALIDATION_FAILED);
    }
  }

  public static byte[] serialize(
      String contractId,
      String contractBinaryName,
      byte[] contractByteCode,
      @Nullable String contractProperties,
      String entityId,
      int keyVersion) {
    String propertiesString = contractProperties != null ? contractProperties : "";
    ByteBuffer buffer =
        ByteBuffer.allocate(
            contractId.getBytes(StandardCharsets.UTF_8).length
                + contractBinaryName.getBytes(StandardCharsets.UTF_8).length
                + contractByteCode.length
                + propertiesString.getBytes(StandardCharsets.UTF_8).length
                + entityId.getBytes(StandardCharsets.UTF_8).length
                + Integer.BYTES);
    buffer.put(contractId.getBytes(StandardCharsets.UTF_8));
    buffer.put(contractBinaryName.getBytes(StandardCharsets.UTF_8));
    buffer.put(contractByteCode);
    buffer.put(propertiesString.getBytes(StandardCharsets.UTF_8));
    buffer.put(entityId.getBytes(StandardCharsets.UTF_8));
    buffer.putInt(keyVersion);
    buffer.rewind();
    return buffer.array();
  }
}
