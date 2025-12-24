package com.scalar.dl.ledger.statemachine;

import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.TamperEvidentAssetLedger;
import com.scalar.dl.ledger.util.JsonpSerDe;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.concurrent.ThreadSafe;
import javax.json.JsonObject;

@ThreadSafe
public class JsonpBasedAssetLedger implements Ledger<JsonObject> {
  private static final JsonpSerDe serde = new JsonpSerDe();
  private final TamperEvidentAssetLedger ledger;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public JsonpBasedAssetLedger(TamperEvidentAssetLedger ledger) {
    this.ledger = ledger;
  }

  @Override
  public Optional<Asset<JsonObject>> get(String assetId) {
    return ledger.get(assetId).map(this::createAsset);
  }

  @Override
  public Optional<Asset<JsonObject>> get(String namespace, String assetId) {
    return ledger.get(namespace, assetId).map(this::createAsset);
  }

  @Override
  public List<Asset<JsonObject>> scan(AssetFilter filter) {
    return ledger.scan(filter).stream().map(this::createAsset).collect(Collectors.toList());
  }

  @Override
  public void put(String assetId, JsonObject data) {
    ledger.put(assetId, serde.serialize(data));
  }

  @Override
  public void put(String namespace, String assetId, JsonObject data) {
    ledger.put(namespace, assetId, serde.serialize(data));
  }

  private Asset<JsonObject> createAsset(InternalAsset asset) {
    return new MetadataComprisedAsset<>(asset, serde::deserialize);
  }
}
