package com.scalar.dl.ledger.database;

import com.google.inject.Inject;
import com.scalar.dl.ledger.crypto.SignatureSigner;
import com.scalar.dl.ledger.proof.AssetProof;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import com.scalar.dl.ledger.util.Argument;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
public class AssetProofComposer {
  private final SignatureSigner signer;

  @Inject
  public AssetProofComposer(@Nullable SignatureSigner signer) {
    this.signer = signer;
  }

  public AssetProof create(String namespace, InternalAsset asset) {
    return create(namespace, asset, null);
  }

  public AssetProof create(String namespace, InternalAsset asset, String nonce) {
    if (signer == null) {
      return null;
    }
    if (nonce == null) {
      nonce = Argument.getNonce(asset.argument());
    }
    return create(
        namespace, asset.id(), asset.age(), nonce, asset.input(), asset.hash(), asset.prevHash());
  }

  public AssetProof create(
      String namespace,
      String id,
      int age,
      String nonce,
      String input,
      byte[] hash,
      byte[] prevHash) {
    if (signer == null) {
      return null;
    }
    byte[] signature =
        signer.sign(AssetProof.serialize(namespace, id, age, nonce, input, hash, prevHash));
    return AssetProof.newBuilder()
        .namespace(namespace)
        .id(id)
        .age(age)
        .nonce(nonce)
        .input(input)
        .hash(hash)
        .prevHash(prevHash)
        .signature(signature)
        .build();
  }
}
