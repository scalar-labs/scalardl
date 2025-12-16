package com.scalar.dl.ledger.database.scalardb;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.scalar.db.api.Put;
import com.scalar.db.io.Key;
import com.scalar.dl.ledger.asset.AssetHasher;
import com.scalar.dl.ledger.contract.ContractEntry;
import com.scalar.dl.ledger.crypto.CertificateEntry;
import com.scalar.dl.ledger.database.Snapshot;
import com.scalar.dl.ledger.model.ContractExecutionRequest;
import com.scalar.dl.ledger.statemachine.AssetInput;
import com.scalar.dl.ledger.statemachine.AssetKey;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import java.util.Map;
import javax.annotation.concurrent.Immutable;

@Immutable
public class DefaultTamperEvidentAssetComposer implements TamperEvidentAssetComposer {
  private final ScalarNamespaceResolver namespaceResolver;

  @Inject
  public DefaultTamperEvidentAssetComposer(ScalarNamespaceResolver namespaceResolver) {
    this.namespaceResolver = namespaceResolver;
  }

  @Override
  public Map<AssetKey, Put> compose(Snapshot snapshot, ContractExecutionRequest request) {
    ImmutableMap.Builder<AssetKey, Put> builder = ImmutableMap.builder();
    Map<AssetKey, InternalAsset> readSet = snapshot.getReadSet();
    Map<AssetKey, InternalAsset> writeSet = snapshot.getWriteSet();

    writeSet.forEach(
        (key, uncommitted) -> {
          InternalAsset committed = readSet.get(key);
          int age = uncommitted.age();

          String input = new AssetInput(readSet).toString();
          String output = uncommitted.data();
          String argument = request.getContractArgument();
          byte[] signature = request.getSignature();
          byte[] prevHash = committed == null ? null : committed.hash();
          ContractEntry.Key contractKey =
              new ContractEntry.Key(
                  request.getContractId(),
                  new CertificateEntry.Key(request.getEntityId(), request.getKeyVersion()));

          Put put =
              new Put(
                      new Key(AssetAttribute.toIdValue(key.assetId())),
                      new Key(AssetAttribute.toAgeValue(age)))
                  .withValue(AssetAttribute.toInputValue(input))
                  .withValue(AssetAttribute.toOutputValue(output))
                  .withValue(AssetAttribute.toContactIdValue(contractKey.serialize()))
                  .withValue(AssetAttribute.toArgumentValue(argument))
                  .withValue(AssetAttribute.toSignatureValue(signature))
                  .withValue(AssetAttribute.toPrevHashValue(prevHash))
                  .withValue(
                      AssetAttribute.toHashValue(
                          hashWith(
                              key.assetId(),
                              age,
                              input,
                              output,
                              contractKey.serialize(),
                              argument,
                              signature,
                              prevHash)))
                  .forNamespace(namespaceResolver.resolve(key.namespace()))
                  .forTable(ScalarTamperEvidentAssetLedger.TABLE);
          builder.put(key, put);
        });
    return builder.build();
  }

  @VisibleForTesting
  byte[] hashWith(
      String id,
      int age,
      String input,
      String output,
      String contractId,
      String argument,
      byte[] signature,
      byte[] prevHash) {
    return new AssetHasher.Builder()
        .id(id)
        .age(age)
        .input(input)
        .output(output)
        .contractId(contractId)
        .argument(argument)
        .signature(signature)
        .prevHash(prevHash)
        .build()
        .get();
  }
}
