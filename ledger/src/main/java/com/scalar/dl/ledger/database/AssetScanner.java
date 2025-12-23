package com.scalar.dl.ledger.database;

import com.scalar.dl.ledger.statemachine.InternalAsset;
import java.util.List;

public interface AssetScanner {

  List<InternalAsset> doScan(AssetFilter filter);

  default InternalAsset doGet(String namespace, String assetId, int age) {
    AssetFilter filter =
        new AssetFilter(namespace, assetId).withStartAge(age, true).withEndAge(age, true);
    List<InternalAsset> assets = doScan(filter);
    if (assets.isEmpty()) {
      return null;
    }
    return assets.get(0);
  }
}
