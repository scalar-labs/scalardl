package com.scalar.dl.ledger.model;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.CharMatcher;
import com.scalar.dl.ledger.crypto.SignatureValidator;
import com.scalar.dl.ledger.error.CommonError;
import com.scalar.dl.ledger.error.CommonLedgerError;
import com.scalar.dl.ledger.exception.SignatureException;
import com.scalar.dl.ledger.util.Argument;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * A request for contract execution.
 *
 * @author Hiroyuki Yamada
 */
@Immutable
// non-final for mocking
public class ContractExecutionRequest extends AbstractRequest {
  private static final Pattern CANONICAL_UUID_PATTERN =
      Pattern.compile(
          "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
  private static final int MAX_NONCE_LENGTH_IN_MESSAGE = 64;
  private final String nonce;
  private final String contractId;
  private final String contractArgument;
  private final List<String> functionIds;
  @Nullable private final String functionArgument;
  private final byte[] signature;
  @Nullable private final byte[] auditorSignature;

  /**
   * Constructs a {@code ContractExecutionRequest} with the specified nonce, contract id, argument,
   * entity ID, key version, a list of {@code AssetProof} and signature of the request.
   *
   * @param nonce the unique id of a request in the canonical UUID format (e.g.,
   *     "550e8400-e29b-41d4-a716-446655440000")
   * @param contractId a contract id of a registered contract to execute
   * @param contractArgument an argument to a contract
   * @param functionIds a list of function ids
   * @param functionArgument an argument to a function
   * @param contextNamespace a context namespace
   * @param entityId an entity ID
   * @param keyVersion the version of a digital signature certificate or a HMAC secret key.
   * @param signature a signature of the request
   * @param auditorSignature a signature from an auditor
   */
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public ContractExecutionRequest(
      String nonce,
      String contractId,
      String contractArgument,
      List<String> functionIds,
      @Nullable String functionArgument,
      @Nullable String contextNamespace,
      String entityId,
      int keyVersion,
      byte[] signature,
      @Nullable byte[] auditorSignature) {
    super(contextNamespace, entityId, keyVersion);
    checkArgument(contractId != null && !contractId.isEmpty());
    checkArgument(contractArgument != null && !contractArgument.isEmpty());
    checkArgument(signature != null && signature.length > 0);
    this.nonce = (nonce == null || nonce.isEmpty()) ? Argument.getNonce(contractArgument) : nonce;
    validateNonceFormat(this.nonce);
    this.contractId = contractId;
    this.contractArgument = contractArgument;
    this.functionIds =
        (functionIds == null || functionIds.isEmpty())
            ? Argument.getFunctionIds(contractArgument)
            : functionIds;
    this.functionArgument = functionArgument;
    this.signature = signature;
    this.auditorSignature = auditorSignature;
  }

  /**
   * SpotBugs detects Bug Type "CT_CONSTRUCTOR_THROW" saying that "The object under construction
   * remains partially initialized and may be vulnerable to Finalizer attacks."
   */
  @Override
  protected final void finalize() {}

  // This throws IllegalArgumentException to stay consistent with the other argument checks in this
  // constructor and in Argument. Note that CommonService maps it to StatusCode.RUNTIME_ERROR even
  // though CommonError.INVALID_NONCE_FORMAT is an INVALID_ARGUMENT error. That mapping is a
  // pre-existing inconsistency shared by every IllegalArgumentException thrown on a request path,
  // and it should be revisited for all of them at once rather than only here.
  private static void validateNonceFormat(String nonce) {
    if (!CANONICAL_UUID_PATTERN.matcher(nonce).matches()) {
      throw new IllegalArgumentException(
          CommonError.INVALID_NONCE_FORMAT.buildMessage(sanitizeNonce(nonce)));
    }
  }

  private static String sanitizeNonce(String nonce) {
    String sanitized = CharMatcher.javaIsoControl().replaceFrom(nonce, '?');
    if (sanitized.length() > MAX_NONCE_LENGTH_IN_MESSAGE) {
      return sanitized.substring(0, MAX_NONCE_LENGTH_IN_MESSAGE)
          + "... ("
          + nonce.length()
          + " chars)";
    }
    return sanitized;
  }

  /**
   * Returns the nonce value of the request.
   *
   * @return the nonce value of the request.
   */
  public String getNonce() {
    return nonce;
  }

  /**
   * Returns the contract id of a contract.
   *
   * @return the contract id of a contract
   */
  public String getContractId() {
    return contractId;
  }

  /**
   * Returns the argument to a contract.
   *
   * @return the argument to a contract
   */
  public String getContractArgument() {
    return contractArgument;
  }

  /**
   * Returns a list of function ids.
   *
   * @return a list of function ids
   */
  @SuppressFBWarnings("EI_EXPOSE_REP")
  public List<String> getFunctionIds() {
    return functionIds;
  }

  /**
   * Returns the argument to user-defined functions
   *
   * @return an {@code Optional} with the argument to user-defined functions
   */
  public Optional<String> getFunctionArgument() {
    return Optional.ofNullable(functionArgument);
  }

  /**
   * Returns the signature of the request
   *
   * @return the signature of the request
   */
  @SuppressFBWarnings("EI_EXPOSE_REP")
  public byte[] getSignature() {
    return signature;
  }

  /**
   * Returns the signature from an auditor
   *
   * @return the signature of an auditor
   */
  @SuppressFBWarnings("EI_EXPOSE_REP")
  public byte[] getAuditorSignature() {
    return auditorSignature;
  }

  /**
   * Validates if the request is not tampered.
   *
   * @param validator a {@link SignatureValidator}
   * @throws SignatureException if the request is invalid.
   */
  @Override
  public void validateWith(SignatureValidator validator) {
    // proofs are not included in the signature because including it doesn't make much difference.
    // It assumes that nonce is appended to the argument by clients.
    byte[] bytes =
        serialize(
            contractId, contractArgument, getContextNamespace(), getEntityId(), getKeyVersion());

    if (!validator.validate(bytes, signature)) {
      throw new SignatureException(CommonError.REQUEST_SIGNATURE_VALIDATION_FAILED);
    }
  }

  /**
   * Validates if the signature from an auditor is not tampered
   *
   * @param validator a {@link SignatureValidator}
   * @throws SignatureException if the signature is invalid.
   */
  public void validateAuditorSignatureWith(SignatureValidator validator) {
    ByteBuffer buffer = ByteBuffer.allocate(nonce.getBytes(StandardCharsets.UTF_8).length);
    buffer.put(nonce.getBytes(StandardCharsets.UTF_8));
    byte[] bytes = buffer.array();

    if (!validator.validate(bytes, auditorSignature)) {
      throw new SignatureException(CommonLedgerError.AUDITOR_SIGNATURE_VALIDATION_FAILED);
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
        super.hashCode(),
        nonce,
        contractId,
        contractArgument,
        functionIds,
        functionArgument,
        Arrays.hashCode(signature),
        Arrays.hashCode(auditorSignature));
  }

  /**
   * Indicates whether some other object is "equal to" this object. The other object is considered
   * equal if it is the same instance or if:
   *
   * <ul>
   *   <li>both super class instances are equal and
   *   <li>it is also an {@code ContractExecutionRequest} and
   *   <li>both instances have the same items such as contract id, argument, and signatures.
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
    if (!(o instanceof ContractExecutionRequest)) {
      return false;
    }
    ContractExecutionRequest other = (ContractExecutionRequest) o;
    return (this.nonce.equals(other.nonce)
        && this.contractId.equals(other.contractId)
        && this.contractArgument.equals(other.contractArgument)
        && this.functionIds.equals(other.functionIds)
        && Objects.equals(this.functionArgument, other.functionArgument)
        && Arrays.equals(this.signature, other.signature)
        && Arrays.equals(this.auditorSignature, other.auditorSignature));
  }

  public static byte[] serialize(
      String contractId,
      String argument,
      @Nullable String contextNamespace,
      String entityId,
      int keyVersion) {
    byte[] contractIdBytes = contractId.getBytes(StandardCharsets.UTF_8);
    byte[] argumentBytes = argument.getBytes(StandardCharsets.UTF_8);
    byte[] contextNamespaceBytes = serializeContextNamespace(contextNamespace);
    byte[] entityIdBytes = entityId.getBytes(StandardCharsets.UTF_8);

    ByteBuffer buffer =
        ByteBuffer.allocate(
            contractIdBytes.length
                + argumentBytes.length
                + contextNamespaceBytes.length
                + entityIdBytes.length
                + Integer.BYTES);
    buffer.put(contractIdBytes);
    buffer.put(argumentBytes);
    buffer.put(contextNamespaceBytes);
    buffer.put(entityIdBytes);
    buffer.putInt(keyVersion);
    buffer.rewind();
    return buffer.array();
  }
}
