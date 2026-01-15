package com.scalar.dl.ledger.statemachine;

/**
 * Asset metadata, which corresponds to the proof of a corresponding asset, that is internally
 * utilized to verify the integrity of a corresponding asset. {@code AssetMetadata} might be useful
 * for some applications that, for example, create an application-specific hash graph using the
 * {@code hash} method.
 */
public interface AssetMetadata {

  /**
   * Returns the nonce of the request that created the asset.
   *
   * @return the nonce of the request that created the asset
   */
  String nonce();

  /**
   * Returns the json-formatted input dependencies of the asset.
   *
   * @return the input dependencies of the asset
   */
  String input();

  /**
   * Returns the hash of the asset record.
   *
   * @return the hash of the asset record
   */
  byte[] hash();

  /**
   * Returns the previous hash of the asset record.
   *
   * @return the previous hash of the asset record
   */
  byte[] prevHash();
}
