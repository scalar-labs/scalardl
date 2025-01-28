package com.scalar.dl.ledger.database;

import com.scalar.dl.ledger.asset.MetadataComprisedAsset;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import com.scalar.dl.ledger.util.JsonpSerDe;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.concurrent.ThreadSafe;
import javax.json.JsonObject;

@ThreadSafe
public class AssetLedger implements Ledger {
  private static final JsonpSerDe serde = new JsonpSerDe();
  private final TamperEvidentAssetLedger ledger;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public AssetLedger(TamperEvidentAssetLedger ledger) {
    this.ledger = ledger;
  }

  @Override
  public Optional<com.scalar.dl.ledger.asset.Asset> get(String assetId) {
    return ledger.get(assetId).map(this::createAsset);
  }

  @Override
  public List<com.scalar.dl.ledger.asset.Asset> scan(AssetFilter filter) {
    return ledger.scan(filter).stream().map(this::createAsset).collect(Collectors.toList());
  }

  @Override
  public void put(String assetId, JsonObject data) {
    ledger.put(assetId, serde.serialize(data));
  }

  private com.scalar.dl.ledger.asset.Asset createAsset(InternalAsset asset) {
    return new MetadataComprisedAsset(asset, serde::deserialize);
  }
}
