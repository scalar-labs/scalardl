package com.scalar.dl.ledger.statemachine;

import com.scalar.dl.ledger.database.AssetFilter;
import java.util.List;
import java.util.Optional;

/**
 * A tamper-evident ledger for storing histories of {@link Asset}s.
 *
 * @author Hiroyuki Yamada
 */
public interface Ledger<T> {

  /**
   * Retrieves the latest {@link Asset} entry (an asset entry with the largest age) from the ledger
   * with the specified asset ID.
   *
   * @param assetId an asset ID
   * @return an {@code Optional} with the returned asset
   */
  Optional<Asset<T>> get(String assetId);

  /**
   * Retrieves a list of {@link Asset} entries from the ledger with the specified {@link
   * AssetFilter}.
   *
   * @param filter a condition to filter asset
   * @return a list of asset entries which passed the filtering
   */
  List<Asset<T>> scan(AssetFilter filter);

  /**
   * Creates/Appends an {@link Asset} entry to the ledger. The initial entry is marked as age 0 and
   * new entry is appended with an incremented age.
   *
   * @param assetId an asset ID to create/append
   * @param data asset data
   */
  void put(String assetId, T data);
}
