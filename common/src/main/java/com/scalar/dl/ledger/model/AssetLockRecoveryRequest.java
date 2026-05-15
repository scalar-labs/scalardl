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
public class AssetLockRecoveryRequest extends AbstractRequest {
  @Nullable private final String namespace;
  private final String assetId;
  private final byte[] signature;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public AssetLockRecoveryRequest(
      @Nullable String namespace,
      String assetId,
      String entityId,
      int keyVersion,
      byte[] signature) {
    super(null, entityId, keyVersion);
    checkArgument(assetId != null);
    this.namespace = namespace;
    this.assetId = assetId;
    this.signature = signature;
  }

  @Nullable
  public String getNamespace() {
    return namespace;
  }

  public String getAssetId() {
    return assetId;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public byte[] getSignature() {
    return signature;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), namespace, assetId, Arrays.hashCode(signature));
  }

  @Override
  public boolean equals(Object o) {
    if (!super.equals(o)) {
      return false;
    }
    if (o == this) {
      return true;
    }
    if (!(o instanceof AssetLockRecoveryRequest)) {
      return false;
    }
    AssetLockRecoveryRequest other = (AssetLockRecoveryRequest) o;
    return Objects.equals(this.namespace, other.namespace)
        && this.assetId.equals(other.assetId)
        && Arrays.equals(this.signature, other.signature);
  }

  @Override
  public void validateWith(SignatureValidator validator) {
    byte[] bytes = serialize(namespace, assetId, getEntityId(), getKeyVersion());

    if (!validator.validate(bytes, signature)) {
      throw new SignatureException(CommonLedgerError.REQUEST_SIGNATURE_VALIDATION_FAILED);
    }
  }

  public static byte[] serialize(
      @Nullable String namespace, String assetId, String entityId, int keyVersion) {
    byte[] namespaceBytes =
        namespace != null ? namespace.getBytes(StandardCharsets.UTF_8) : new byte[0];
    byte[] assetIdBytes = assetId.getBytes(StandardCharsets.UTF_8);
    byte[] entityIdBytes = entityId.getBytes(StandardCharsets.UTF_8);

    ByteBuffer buffer =
        ByteBuffer.allocate(
            namespaceBytes.length + assetIdBytes.length + entityIdBytes.length + Integer.BYTES);
    buffer.put(namespaceBytes);
    buffer.put(assetIdBytes);
    buffer.put(entityIdBytes);
    buffer.putInt(keyVersion);
    buffer.rewind();
    return buffer.array();
  }
}
