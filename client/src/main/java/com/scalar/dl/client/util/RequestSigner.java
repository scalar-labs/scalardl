package com.scalar.dl.client.util;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.scalar.dl.ledger.crypto.SignatureSigner;
import com.scalar.dl.rpc.AssetProofRetrievalRequest;
import com.scalar.dl.rpc.ContractExecutionRequest;
import com.scalar.dl.rpc.ContractRegistrationRequest;
import com.scalar.dl.rpc.ContractsListingRequest;
import com.scalar.dl.rpc.ExecutionAbortRequest;
import com.scalar.dl.rpc.LedgerValidationRequest;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import javax.annotation.concurrent.Immutable;

@Immutable
public class RequestSigner {
  private final SignatureSigner signer;

  @Inject
  public RequestSigner(SignatureSigner signer) {
    this.signer = signer;
  }

  public ContractRegistrationRequest.Builder sign(ContractRegistrationRequest.Builder builder) {
    ByteBuffer buffer =
        ByteBuffer.allocate(
            builder.getContractId().getBytes(StandardCharsets.UTF_8).length
                + builder.getContractBinaryName().getBytes(StandardCharsets.UTF_8).length
                + builder.getContractByteCode().size()
                + builder.getContractProperties().getBytes(StandardCharsets.UTF_8).length
                + builder.getEntityId().getBytes(StandardCharsets.UTF_8).length
                + Integer.BYTES);

    buffer.put(builder.getContractId().getBytes(StandardCharsets.UTF_8));
    buffer.put(builder.getContractBinaryName().getBytes(StandardCharsets.UTF_8));
    buffer.put(builder.getContractByteCode().toByteArray());
    buffer.put(builder.getContractProperties().getBytes(StandardCharsets.UTF_8));
    buffer.put(builder.getEntityId().getBytes(StandardCharsets.UTF_8));
    buffer.putInt(builder.getKeyVersion());
    buffer.rewind();

    byte[] signature = signer.sign(buffer.array());
    return builder.setSignature(ByteString.copyFrom(signature));
  }

  public ContractsListingRequest.Builder sign(ContractsListingRequest.Builder builder) {
    ByteBuffer buffer =
        ByteBuffer.allocate(
            builder.getContractId().getBytes(StandardCharsets.UTF_8).length
                + builder.getEntityId().getBytes(StandardCharsets.UTF_8).length
                + Integer.BYTES);

    buffer.put(builder.getContractId().getBytes(StandardCharsets.UTF_8));
    buffer.put(builder.getEntityId().getBytes(StandardCharsets.UTF_8));
    buffer.putInt(builder.getKeyVersion());
    buffer.rewind();

    byte[] signature = signer.sign(buffer.array());
    return builder.setSignature(ByteString.copyFrom(signature));
  }

  public ContractExecutionRequest.Builder sign(ContractExecutionRequest.Builder builder) {
    ByteBuffer buffer =
        ByteBuffer.allocate(
            builder.getContractId().getBytes(StandardCharsets.UTF_8).length
                + builder.getContractArgument().getBytes(StandardCharsets.UTF_8).length
                + builder.getEntityId().getBytes(StandardCharsets.UTF_8).length
                + Integer.BYTES);

    buffer.put(builder.getContractId().getBytes(StandardCharsets.UTF_8));
    buffer.put(builder.getContractArgument().getBytes(StandardCharsets.UTF_8));
    buffer.put(builder.getEntityId().getBytes(StandardCharsets.UTF_8));
    buffer.putInt(builder.getKeyVersion());
    buffer.rewind();

    byte[] signature = signer.sign(buffer.array());
    return builder.setSignature(ByteString.copyFrom(signature));
  }

  public LedgerValidationRequest.Builder sign(LedgerValidationRequest.Builder builder) {
    ByteBuffer buffer =
        ByteBuffer.allocate(
            builder.getAssetId().getBytes(StandardCharsets.UTF_8).length
                + Integer.BYTES
                + Integer.BYTES
                + builder.getEntityId().getBytes(StandardCharsets.UTF_8).length
                + Integer.BYTES);

    buffer.put(builder.getAssetId().getBytes(StandardCharsets.UTF_8));
    buffer.putInt(builder.getStartAge());
    buffer.putInt(builder.getEndAge());
    buffer.put(builder.getEntityId().getBytes(StandardCharsets.UTF_8));
    buffer.putInt(builder.getKeyVersion());
    buffer.rewind();

    byte[] signature = signer.sign(buffer.array());
    return builder.setSignature(ByteString.copyFrom(signature));
  }

  public AssetProofRetrievalRequest.Builder sign(AssetProofRetrievalRequest.Builder builder) {
    ByteBuffer buffer =
        ByteBuffer.allocate(
            builder.getAssetId().getBytes(StandardCharsets.UTF_8).length
                + Integer.BYTES
                + builder.getEntityId().getBytes(StandardCharsets.UTF_8).length
                + Integer.BYTES);

    buffer.put(builder.getAssetId().getBytes(StandardCharsets.UTF_8));
    buffer.putInt(builder.getAge());
    buffer.put(builder.getEntityId().getBytes(StandardCharsets.UTF_8));
    buffer.putInt(builder.getKeyVersion());
    buffer.rewind();

    byte[] signature = signer.sign(buffer.array());
    return builder.setSignature(ByteString.copyFrom(signature));
  }

  public ExecutionAbortRequest.Builder sign(ExecutionAbortRequest.Builder builder) {
    ByteBuffer buffer =
        ByteBuffer.allocate(
            builder.getNonce().getBytes(StandardCharsets.UTF_8).length
                + builder.getEntityId().getBytes(StandardCharsets.UTF_8).length
                + Integer.BYTES);

    buffer.put(builder.getNonce().getBytes(StandardCharsets.UTF_8));
    buffer.put(builder.getEntityId().getBytes(StandardCharsets.UTF_8));
    buffer.putInt(builder.getKeyVersion());
    buffer.rewind();

    byte[] signature = signer.sign(buffer.array());
    return builder.setSignature(ByteString.copyFrom(signature));
  }
}
