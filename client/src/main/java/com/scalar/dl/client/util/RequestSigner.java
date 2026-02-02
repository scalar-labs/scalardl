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
import javax.annotation.concurrent.Immutable;

@Immutable
public class RequestSigner {
  private final SignatureSigner signer;

  @Inject
  public RequestSigner(SignatureSigner signer) {
    this.signer = signer;
  }

  public ContractRegistrationRequest.Builder sign(ContractRegistrationRequest.Builder builder) {
    byte[] bytes =
        com.scalar.dl.ledger.model.ContractRegistrationRequest.serialize(
            builder.getContractId(),
            builder.getContractBinaryName(),
            builder.getContractByteCode().toByteArray(),
            builder.getContractProperties(),
            builder.getEntityId(),
            builder.getKeyVersion());

    byte[] signature = signer.sign(bytes);
    return builder.setSignature(ByteString.copyFrom(signature));
  }

  public ContractsListingRequest.Builder sign(ContractsListingRequest.Builder builder) {
    byte[] bytes =
        com.scalar.dl.ledger.model.ContractsListingRequest.serialize(
            builder.getContractId(), builder.getEntityId(), builder.getKeyVersion());

    byte[] signature = signer.sign(bytes);
    return builder.setSignature(ByteString.copyFrom(signature));
  }

  public ContractExecutionRequest.Builder sign(ContractExecutionRequest.Builder builder) {
    byte[] bytes =
        com.scalar.dl.ledger.model.ContractExecutionRequest.serialize(
            builder.getContractId(),
            builder.getContractArgument(),
            builder.getEntityId(),
            builder.getKeyVersion());

    byte[] signature = signer.sign(bytes);
    return builder.setSignature(ByteString.copyFrom(signature));
  }

  public LedgerValidationRequest.Builder sign(LedgerValidationRequest.Builder builder) {
    byte[] bytes =
        com.scalar.dl.ledger.model.LedgerValidationRequest.serialize(
            builder.getNamespace(),
            builder.getAssetId(),
            builder.getStartAge(),
            builder.getEndAge(),
            builder.getEntityId(),
            builder.getKeyVersion());

    byte[] signature = signer.sign(bytes);
    return builder.setSignature(ByteString.copyFrom(signature));
  }

  public AssetProofRetrievalRequest.Builder sign(AssetProofRetrievalRequest.Builder builder) {
    byte[] bytes =
        com.scalar.dl.ledger.model.AssetProofRetrievalRequest.serialize(
            builder.getNamespace(),
            builder.getAssetId(),
            builder.getAge(),
            builder.getEntityId(),
            builder.getKeyVersion());

    byte[] signature = signer.sign(bytes);
    return builder.setSignature(ByteString.copyFrom(signature));
  }

  public ExecutionAbortRequest.Builder sign(ExecutionAbortRequest.Builder builder) {
    byte[] bytes =
        com.scalar.dl.ledger.model.ExecutionAbortRequest.serialize(
            builder.getNonce(), builder.getEntityId(), builder.getKeyVersion());

    byte[] signature = signer.sign(bytes);
    return builder.setSignature(ByteString.copyFrom(signature));
  }
}
