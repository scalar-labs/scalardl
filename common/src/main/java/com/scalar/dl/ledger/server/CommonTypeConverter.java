package com.scalar.dl.ledger.server;

import com.google.protobuf.ByteString;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.rpc.AssetProof;
import com.scalar.dl.rpc.ContractExecutionResponse;
import com.scalar.dl.rpc.LedgerValidationResponse;

public class CommonTypeConverter {

  public static LedgerValidationResponse convert(LedgerValidationResult result) {
    LedgerValidationResponse.Builder builder = LedgerValidationResponse.newBuilder();
    builder.setStatusCode(result.getCode().get());
    // expect either ledger proof or auditor proof is set
    result.getLedgerProof().ifPresent(proof -> builder.setProof(convert(proof)));
    result.getAuditorProof().ifPresent(proof -> builder.setProof(convert(proof)));
    return builder.build();
  }

  public static ContractExecutionResponse convert(ContractExecutionResult result) {
    ContractExecutionResponse.Builder builder = ContractExecutionResponse.newBuilder();
    result.getContractResult().ifPresent(builder::setContractResult);
    result.getFunctionResult().ifPresent(builder::setFunctionResult);
    result.getLedgerProofs().forEach(proof -> builder.addProofs(convert(proof)));
    if (builder.getProofsList().isEmpty()) {
      result.getAuditorProofs().forEach(proof -> builder.addProofs(convert(proof)));
    }
    return builder.build();
  }

  public static AssetProof convert(com.scalar.dl.ledger.proof.AssetProof proof) {
    AssetProof.Builder builder =
        AssetProof.newBuilder()
            .setAssetId(proof.getId())
            .setAge(proof.getAge())
            .setNonce(proof.getNonce())
            .setInput(proof.getInput())
            .setHash(ByteString.copyFrom(proof.getHash()))
            .setSignature(ByteString.copyFrom(proof.getSignature()));
    if (proof.getPrevHash() != null) {
      builder.setPrevHash(ByteString.copyFrom(proof.getPrevHash()));
    }
    return builder.build();
  }

  public static com.scalar.dl.ledger.proof.AssetProof convert(AssetProof proof) {
    return com.scalar.dl.ledger.proof.AssetProof.newBuilder()
        .id(proof.getAssetId())
        .age(proof.getAge())
        .nonce(proof.getNonce())
        .input(proof.getInput())
        .hash(proof.getHash().toByteArray())
        .prevHash(proof.getPrevHash().toByteArray())
        .signature(proof.getSignature().toByteArray())
        .build();
  }
}
