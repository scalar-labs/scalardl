package com.scalar.dl.ledger.model;

import com.scalar.dl.ledger.crypto.SignatureValidator;
import com.scalar.dl.ledger.exception.SignatureException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;

@Immutable
public class ExecutionAbortRequest extends AbstractRequest {
  private final String nonce;
  private final byte[] signature;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public ExecutionAbortRequest(String nonce, String entityId, int keyVersion, byte[] signature) {
    super(entityId, keyVersion);
    this.nonce = nonce;
    this.signature = signature;
  }

  public String getNonce() {
    return nonce;
  }

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
    return Objects.hash(nonce, Arrays.hashCode(signature));
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof ExecutionAbortRequest)) {
      return false;
    }
    ExecutionAbortRequest other = (ExecutionAbortRequest) o;
    return this.nonce.equals(other.nonce);
  }

  /**
   * Validates if the request is not tampered.
   *
   * @param validator a {@link SignatureValidator}
   * @throws SignatureException if the request is invalid.
   */
  @Override
  public void validateWith(SignatureValidator validator) {
    byte[] bytes = serialize(nonce, getEntityId(), getKeyVersion());

    if (!validator.validate(bytes, signature)) {
      throw new SignatureException("The request signature can't be validated with the validator.");
    }
  }

  public static byte[] serialize(String nonce, String entityId, int keyVersion) {
    ByteBuffer buffer =
        ByteBuffer.allocate(
            nonce.getBytes(StandardCharsets.UTF_8).length
                + entityId.getBytes(StandardCharsets.UTF_8).length
                + Integer.BYTES);
    buffer.put(nonce.getBytes(StandardCharsets.UTF_8));
    buffer.put(entityId.getBytes(StandardCharsets.UTF_8));
    buffer.putInt(keyVersion);
    buffer.rewind();
    return buffer.array();
  }
}
