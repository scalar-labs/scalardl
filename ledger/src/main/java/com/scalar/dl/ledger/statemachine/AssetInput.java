package com.scalar.dl.ledger.statemachine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.scalar.dl.ledger.namespace.Namespaces;
import com.scalar.dl.ledger.statemachine.AssetInput.AssetInputEntry;
import com.scalar.dl.ledger.util.JacksonSerDe;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

public class AssetInput implements Iterable<AssetInputEntry> {
  @VisibleForTesting public static final String KEY_AGE = "age";
  @VisibleForTesting public static final String KEY_VERSION = "_version";
  @VisibleForTesting public static final int INPUT_FORMAT_VERSION = 2;
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final JacksonSerDe serde = new JacksonSerDe(mapper);
  private final ImmutableSet<AssetInputEntry> entries;

  public AssetInput(String input) {
    JsonNode node = serde.deserialize(input);
    if (node.has(KEY_VERSION) && node.get(KEY_VERSION).asInt() == INPUT_FORMAT_VERSION) {
      this.entries = convertFromV2(node);
    } else {
      this.entries = convertFromV1(node);
    }
  }

  public AssetInput(Map<AssetKey, InternalAsset> readSet) {
    this.entries = convertFrom(readSet);
  }

  private ImmutableSet<AssetInputEntry> convertFromV1(JsonNode node) {
    ImmutableSet.Builder<AssetInputEntry> builder = ImmutableSet.builder();
    node.properties()
        .forEach(
            entry -> {
              builder.add(
                  new AssetInputEntry(null, entry.getKey(), entry.getValue().get(KEY_AGE).asInt()));
            });
    return builder.build();
  }

  private ImmutableSet<AssetInputEntry> convertFromV2(JsonNode node) {
    ImmutableSet.Builder<AssetInputEntry> builder = ImmutableSet.builder();
    node.properties()
        .forEach(
            namespaceEntry -> {
              String fieldName = namespaceEntry.getKey();
              if (fieldName.equals(KEY_VERSION)) {
                // skip version field
                return;
              }
              namespaceEntry
                  .getValue()
                  .properties()
                  .forEach(
                      assetEntry -> {
                        builder.add(
                            new AssetInputEntry(
                                fieldName,
                                assetEntry.getKey(),
                                assetEntry.getValue().get(KEY_AGE).asInt()));
                      });
            });
    return builder.build();
  }

  private ImmutableSet<AssetInputEntry> convertFrom(Map<AssetKey, InternalAsset> readSet) {
    return readSet.entrySet().stream()
        .map(
            entry ->
                new AssetInputEntry(
                    entry.getKey().namespace(), entry.getKey().assetId(), entry.getValue().age()))
        .collect(ImmutableSet.toImmutableSet());
  }

  public boolean isEmpty() {
    return entries.isEmpty();
  }

  public int size() {
    return entries.size();
  }

  @Override
  @Nonnull
  public Iterator<AssetInputEntry> iterator() {
    return entries.iterator();
  }

  @Override
  public String toString() {
    // This method is called only when getting the string representation for putting an asset (lock)
    // record, and in that context, the AssetInput is always initialized with a read set that
    // includes namespace information. For backward compatibility with old Auditor versions that
    // don't support namespaces, we return V1 format when all entries are in the default namespace,
    // and V2 format otherwise.
    ImmutableSetMultimap<String, AssetInputEntry> entriesPerNamespace =
        entries.stream()
            .collect(
                ImmutableSetMultimap.toImmutableSetMultimap(
                    AssetInputEntry::namespace, entry -> entry));

    if (entriesPerNamespace.keySet().size() == 1
        && entriesPerNamespace.containsKey(Namespaces.DEFAULT)) {
      return toV1String(entriesPerNamespace.get(Namespaces.DEFAULT));
    }

    return toV2String(entriesPerNamespace);
  }

  private String toV1String(ImmutableSet<AssetInputEntry> assets) {
    ObjectNode objectNode = mapper.createObjectNode();
    assets.forEach(
        asset -> {
          ObjectNode element = mapper.createObjectNode();
          element.put(KEY_AGE, asset.age());
          objectNode.set(asset.id(), element);
        });
    return serde.serialize(objectNode);
  }

  private String toV2String(ImmutableSetMultimap<String, AssetInputEntry> entriesPerNamespace) {
    ObjectNode objectNode = mapper.createObjectNode();
    entriesPerNamespace
        .asMap()
        .forEach(
            (namespace, assets) -> {
              ObjectNode namespaceNode = mapper.createObjectNode();
              assets.forEach(
                  asset -> {
                    ObjectNode element = mapper.createObjectNode();
                    element.put(KEY_AGE, asset.age());
                    namespaceNode.set(asset.id(), element);
                  });
              objectNode.set(namespace, namespaceNode);
            });
    if (!entriesPerNamespace.isEmpty()) {
      objectNode.put(KEY_VERSION, INPUT_FORMAT_VERSION);
    }
    return serde.serialize(objectNode);
  }

  @Immutable
  public static class AssetInputEntry {
    private final String namespace;
    private final String id;
    private final int age;

    private AssetInputEntry(String namespace, String id, int age) {
      this.namespace = namespace;
      this.id = id;
      this.age = age;
    }

    public String namespace() {
      return namespace;
    }

    public String id() {
      return id;
    }

    public int age() {
      return age;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof AssetInputEntry)) {
        return false;
      }
      AssetInputEntry that = (AssetInputEntry) o;
      return age == that.age
          && Objects.equals(namespace, that.namespace)
          && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
      return Objects.hash(namespace, id, age);
    }
  }
}
