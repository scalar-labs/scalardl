package com.scalar.dl.ledger.database;

import com.scalar.dl.ledger.asset.Asset;
import java.util.List;
import java.util.Optional;
import javax.json.JsonObject;

/**
 * A tamper-evident ledger for storing histories of {@link Asset}s.
 *
 * @author Hiroyuki Yamada
 * @deprecated This class won't be available to users in release 5.0.0.
 */
@Deprecated
public interface Ledger {

  /**
   * Retrieves the latest {@link Asset} entry (an asset entry with the largest age) from the ledger
   * with the specified asset ID.
   *
   * @param assetId an asset ID
   * @return an {@code Optional} with the returned asset
   */
  Optional<Asset> get(String assetId);

  // TODO: separate scan ? since it is for read-only transaction
  /**
   * Retrieves a list of {@link Asset} entries from the ledger with the specified {@link
   * AssetFilter}.
   *
   * @param filter a condition to filter asset
   * @return a list of asset entries which passed the filtering
   */
  List<Asset> scan(AssetFilter filter);

  /**
   * Creates/Appends an {@link Asset} entry to the ledger. The initial entry is marked as age 0 and
   * new entry is appended with an incremented age.
   *
   * @param assetId an asset ID to create/append
   * @param data json-formatted asset data
   */
  void put(String assetId, JsonObject data);
}
