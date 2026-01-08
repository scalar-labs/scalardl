package com.scalar.dl.ledger.database;

import com.scalar.dl.ledger.proof.AssetProof;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import java.util.List;
import java.util.Optional;

public interface TamperEvidentAssetLedger {

  Optional<InternalAsset> get(String assetId);

  Optional<InternalAsset> get(String namespace, String assetId);

  List<InternalAsset> scan(AssetFilter filter);

  void put(String assetId, String data);

  void put(String namespace, String assetId, String data);

  List<AssetProof> commit();

  void abort();
}
