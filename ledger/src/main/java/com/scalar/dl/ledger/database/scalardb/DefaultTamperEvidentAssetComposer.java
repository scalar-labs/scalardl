package com.scalar.dl.ledger.database.scalardb;

import com.google.common.annotations.VisibleForTesting;
import com.scalar.db.api.Put;
import com.scalar.db.io.Key;
import com.scalar.dl.ledger.asset.AssetHasher;
import com.scalar.dl.ledger.contract.ContractEntry;
import com.scalar.dl.ledger.crypto.CertificateEntry;
import com.scalar.dl.ledger.database.Snapshot;
import com.scalar.dl.ledger.model.ContractExecutionRequest;
import com.scalar.dl.ledger.statemachine.AssetInput;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.concurrent.Immutable;

@Immutable
public class DefaultTamperEvidentAssetComposer implements TamperEvidentAssetComposer {

  @Override
  public List<Put> compose(Snapshot snapshot, ContractExecutionRequest request) {
    List<Put> puts = new ArrayList<>();
    Map<String, InternalAsset> readSet = snapshot.getReadSet();
    Map<String, InternalAsset> writeSet = snapshot.getWriteSet();

    writeSet.forEach(
        (id, uncommitted) -> {
          InternalAsset committed = readSet.get(id);
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
                      new Key(AssetAttribute.toIdValue(id)),
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
                              id,
                              age,
                              input,
                              output,
                              contractKey.serialize(),
                              argument,
                              signature,
                              prevHash)))
                  .forTable(ScalarTamperEvidentAssetLedger.TABLE);
          puts.add(put);
        });
    return puts;
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
