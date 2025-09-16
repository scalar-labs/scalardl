package com.scalar.dl.hashstore.client.model;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.scalar.dl.genericcontracts.object.Constants.HASH_VALUE;
import static com.scalar.dl.genericcontracts.object.Constants.METADATA;
import static com.scalar.dl.genericcontracts.object.Constants.VERSION_ID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scalar.dl.hashstore.client.util.HashStoreClientUtils;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.JsonObject;

/**
 * Represents a version of an object, which is used to compare with the corresponding version in the
 * hash store.
 *
 * <p>This class encapsulates version information including a version ID, hash value, and optional
 * metadata. It provides factory methods to create instances with different metadata formats
 * (JsonObject, ObjectNode, String, or no metadata).
 *
 * <p>The version ID is only used for identifying which version is faulty in the output, so any
 * string values can be used. If there is no version management for your objects, use an empty
 * string for the version ID.
 */
public class Version {
  private final String versionId;
  private final String hash;
  private final JsonNode metadata;

  private Version(String versionId, String hash, @Nullable JsonNode metadata) {
    this.versionId = versionId;
    this.hash = hash;
    this.metadata = metadata;
  }

  public ObjectNode toObjectNode() {
    ObjectNode version =
        HashStoreClientUtils.createObjectNode().put(VERSION_ID, versionId).put(HASH_VALUE, hash);
    return metadata == null ? version : version.set(METADATA, metadata);
  }

  /**
   * Creates a Version instance with a version ID and hash value without metadata.
   *
   * @param versionId the version identifier
   * @param hash the hash value of the version
   * @return a new Version instance
   */
  public static Version of(String versionId, String hash) {
    return new Version(versionId, hash, null);
  }

  /**
   * Creates a Version instance with a version ID, hash value, and metadata as JsonObject.
   *
   * @param versionId the version identifier
   * @param hash the hash value of the version
   * @param metadata the metadata associated with the version as JsonObject
   * @return a new Version instance
   */
  public static Version of(String versionId, String hash, @Nonnull JsonObject metadata) {
    checkNotNull(metadata);
    return new Version(versionId, hash, HashStoreClientUtils.convertToJsonNode(metadata));
  }

  /**
   * Creates a Version instance with a version ID, hash value, and metadata as ObjectNode.
   *
   * @param versionId the version identifier
   * @param hash the hash value of the version
   * @param metadata the metadata associated with the version as ObjectNode
   * @return a new Version instance
   */
  public static Version of(String versionId, String hash, @Nonnull ObjectNode metadata) {
    checkNotNull(metadata);
    return new Version(versionId, hash, metadata);
  }

  /**
   * Creates a Version instance with a version ID, hash value, and metadata as String.
   *
   * @param versionId the version identifier
   * @param hash the hash value of the version
   * @param metadata the metadata associated with the version as JSON string
   * @return a new Version instance
   */
  public static Version of(String versionId, String hash, @Nonnull String metadata) {
    checkNotNull(metadata);
    return new Version(versionId, hash, HashStoreClientUtils.convertToJsonNode(metadata));
  }
}
