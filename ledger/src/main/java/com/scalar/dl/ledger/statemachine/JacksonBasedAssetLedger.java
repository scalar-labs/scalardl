package com.scalar.dl.ledger.statemachine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.TamperEvidentAssetLedger;
import com.scalar.dl.ledger.util.JacksonSerDe;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class JacksonBasedAssetLedger implements Ledger<JsonNode> {
  private static final JacksonSerDe serde = new JacksonSerDe(new ObjectMapper());
  private final TamperEvidentAssetLedger ledger;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public JacksonBasedAssetLedger(TamperEvidentAssetLedger ledger) {
    this.ledger = ledger;
  }

  @Override
  public Optional<Asset<JsonNode>> get(String assetId) {
    return ledger.get(assetId).map(this::createAsset);
  }

  @Override
  public Optional<Asset<JsonNode>> get(String namespace, String assetId) {
    return ledger.get(namespace, assetId).map(this::createAsset);
  }

  @Override
  public List<Asset<JsonNode>> scan(AssetFilter filter) {
    return ledger.scan(filter).stream().map(this::createAsset).collect(Collectors.toList());
  }

  @Override
  public void put(String assetId, JsonNode data) {
    ledger.put(assetId, serde.serialize(data));
  }

  @Override
  public void put(String namespace, String assetId, JsonNode data) {
    ledger.put(namespace, assetId, serde.serialize(data));
  }

  private Asset<JsonNode> createAsset(InternalAsset asset) {
    return new MetadataComprisedAsset<>(asset, serde::deserialize);
  }
}
