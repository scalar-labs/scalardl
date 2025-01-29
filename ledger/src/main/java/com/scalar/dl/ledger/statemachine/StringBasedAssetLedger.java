package com.scalar.dl.ledger.statemachine;

import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.TamperEvidentAssetLedger;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class StringBasedAssetLedger implements Ledger<String> {
  private final TamperEvidentAssetLedger ledger;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public StringBasedAssetLedger(TamperEvidentAssetLedger ledger) {
    this.ledger = ledger;
  }

  @Override
  public Optional<Asset<String>> get(String assetId) {
    return ledger.get(assetId).map(this::createAsset);
  }

  @Override
  public List<Asset<String>> scan(AssetFilter filter) {
    return ledger.scan(filter).stream().map(this::createAsset).collect(Collectors.toList());
  }

  @Override
  public void put(String assetId, String data) {
    ledger.put(assetId, data);
  }

  private Asset<String> createAsset(InternalAsset asset) {
    return new MetadataComprisedAsset<>(asset, data -> data);
  }
}
