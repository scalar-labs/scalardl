package com.scalar.dl.ledger.statemachine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scalar.dl.ledger.statemachine.AssetInput.AssetIdAge;
import com.scalar.dl.ledger.util.JacksonSerDe;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class AssetInput implements Iterable<AssetIdAge> {
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final JacksonSerDe serde = new JacksonSerDe(mapper);
  private final JsonNode node;

  public AssetInput(String input) {
    node = serde.deserialize(input);
  }

  public AssetInput(Map<String, InternalAsset> readSet) {
    ObjectNode objectNode = mapper.createObjectNode();
    readSet.forEach(
        (id, asset) -> {
          ObjectNode element = mapper.createObjectNode();
          element.put("age", asset.age());
          objectNode.set(id, element);
        });
    node = objectNode;
  }

  public boolean isEmpty() {
    return node.size() == 0;
  }

  public int size() {
    return node.size();
  }

  @Override
  public Iterator<AssetIdAge> iterator() {
    Iterator<Entry<String, JsonNode>> iterator = node.fields();
    return new Iterator<AssetIdAge>() {
      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public AssetIdAge next() {
        Entry<String, JsonNode> entry = iterator.next();
        return new AssetIdAge() {
          @Override
          public String id() {
            return entry.getKey();
          }

          @Override
          public int age() {
            return entry.getValue().get("age").asInt();
          }
        };
      }
    };
  }

  @Override
  public String toString() {
    return serde.serialize(node);
  }

  public interface AssetIdAge {

    String id();

    int age();
  }
}
