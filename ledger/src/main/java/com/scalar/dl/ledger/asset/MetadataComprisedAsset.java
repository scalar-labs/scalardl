package com.scalar.dl.ledger.asset;

import com.scalar.dl.ledger.error.CommonError;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import com.scalar.dl.ledger.util.Argument;
import java.util.function.Function;
import javax.json.JsonObject;

@Deprecated
public class MetadataComprisedAsset implements Asset {
  private final InternalAsset asset;
  private final Function<String, JsonObject> deserializer;
  private JsonObject data;
  private String nonce;

  public MetadataComprisedAsset(InternalAsset asset, Function<String, JsonObject> deserializer) {
    this.asset = asset;
    this.deserializer = deserializer;
  }

  @Override
  public String id() {
    return asset.id();
  }

  @Override
  public int age() {
    return asset.age();
  }

  @Override
  public JsonObject data() {
    if (data != null) {
      return data;
    }
    data = deserializer.apply(asset.data());
    return data;
  }

  @Override
  public AssetMetadata metadata() {
    if (asset.hash() == null) {
      // if one of the metadata is null, we can assume the other metadata is also null.
      throw new IllegalStateException(CommonError.METADATA_NOT_AVAILABLE.buildMessage());
    }
    return new AssetMetadata() {
      @Override
      public String nonce() {
        if (nonce != null) {
          return nonce;
        }
        nonce = Argument.getNonce(asset.argument());
        return nonce;
      }

      @Override
      public String input() {
        return asset.input();
      }

      @Override
      public byte[] hash() {
        return asset.hash();
      }

      @Override
      public byte[] prevHash() {
        return asset.prevHash();
      }
    };
  }
}
