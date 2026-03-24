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
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
public class SignedFunctionRegistrationRequest extends AbstractRequest {
  private final String functionId;
  private final String functionBinaryName;
  private final byte[] functionByteCode;
  private final byte[] signature;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public SignedFunctionRegistrationRequest(
      String functionId,
      String functionBinaryName,
      byte[] functionByteCode,
      @Nullable String contextNamespace,
      String entityId,
      int keyVersion,
      byte[] signature) {
    super(contextNamespace, entityId, keyVersion);
    this.functionId = checkNotNull(functionId);
    this.functionBinaryName = checkNotNull(functionBinaryName);
    this.functionByteCode = checkNotNull(functionByteCode);
    this.signature = checkNotNull(signature);
  }

  public String getFunctionId() {
    return functionId;
  }

  public String getFunctionBinaryName() {
    return functionBinaryName;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public byte[] getFunctionByteCode() {
    return functionByteCode;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public byte[] getSignature() {
    return signature;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        super.hashCode(),
        functionId,
        functionBinaryName,
        Arrays.hashCode(functionByteCode),
        Arrays.hashCode(signature));
  }

  @Override
  public boolean equals(Object o) {
    if (!super.equals(o)) {
      return false;
    }
    if (o == this) {
      return true;
    }
    if (!(o instanceof SignedFunctionRegistrationRequest)) {
      return false;
    }
    SignedFunctionRegistrationRequest other = (SignedFunctionRegistrationRequest) o;
    return (this.functionId.equals(other.functionId)
        && this.functionBinaryName.equals(other.functionBinaryName)
        && Arrays.equals(this.functionByteCode, other.functionByteCode)
        && Arrays.equals(this.signature, other.signature));
  }

  @Override
  public void validateWith(SignatureValidator validator) {
    byte[] bytes =
        serialize(
            functionId,
            functionBinaryName,
            functionByteCode,
            getContextNamespace(),
            getEntityId(),
            getKeyVersion());

    if (!validator.validate(bytes, signature)) {
      throw new SignatureException(CommonError.REQUEST_SIGNATURE_VALIDATION_FAILED);
    }
  }

  public static byte[] serialize(
      String functionId,
      String functionBinaryName,
      byte[] functionByteCode,
      @Nullable String contextNamespace,
      String entityId,
      int keyVersion) {
    byte[] functionIdBytes = functionId.getBytes(StandardCharsets.UTF_8);
    byte[] functionBinaryNameBytes = functionBinaryName.getBytes(StandardCharsets.UTF_8);
    byte[] contextNamespaceBytes = serializeContextNamespace(contextNamespace);
    byte[] entityIdBytes = entityId.getBytes(StandardCharsets.UTF_8);

    ByteBuffer buffer =
        ByteBuffer.allocate(
            functionIdBytes.length
                + functionBinaryNameBytes.length
                + functionByteCode.length
                + contextNamespaceBytes.length
                + entityIdBytes.length
                + Integer.BYTES);
    buffer.put(functionIdBytes);
    buffer.put(functionBinaryNameBytes);
    buffer.put(functionByteCode);
    buffer.put(contextNamespaceBytes);
    buffer.put(entityIdBytes);
    buffer.putInt(keyVersion);
    buffer.rewind();
    return buffer.array();
  }
}
