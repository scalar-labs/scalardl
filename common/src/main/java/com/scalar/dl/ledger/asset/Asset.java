package com.scalar.dl.ledger.asset;

import javax.json.JsonObject;

/**
 * An asset entry abstraction. An asset is a set of historical data and it's data is identified by
 * it's ID and age as conceptually described below.
 *
 * <p>{@code asset-ID => {<age-1, data-1>, <age-2, data-2>, ..., <age-N, data-N>}}
 *
 * <p>The largest age specifies the latest asset entry.
 *
 * @author Hiroyuki Yamada
 * @deprecated This class won't be available to users in release 5.0.0.
 */
@Deprecated
public interface Asset {

  /**
   * Returns an ID of the asset. This ID is the same in all aged.
   *
   * @return an ID of the asset
   */
  String id();

  /**
   * Returns an age of the asset.
   *
   * @return an age of the asset
   */
  int age();

  /**
   * Returns a data of the asset.
   *
   * @return a data of the asset
   */
  JsonObject data();

  /**
   * Returns the metadata with {@link AssetMetadata} of the asset. {@link AssetMetadata} is
   * available for committed assets and null for uncommitted assets. You get uncommitted assets only
   * when you put a new asset and get the asset you put in a contract because the contract is not
   * successfully finished yet.
   *
   * @return the metadata of the asset
   */
  AssetMetadata metadata();
}
