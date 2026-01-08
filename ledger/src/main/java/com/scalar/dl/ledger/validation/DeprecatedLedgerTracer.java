package com.scalar.dl.ledger.validation;

import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.Ledger;
import com.scalar.dl.ledger.error.CommonError;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.AssetKey;
import com.scalar.dl.ledger.statemachine.AssetMetadata;
import com.scalar.dl.ledger.statemachine.DeprecatedLedgerReturnable;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import com.scalar.dl.ledger.util.JsonpSerDe;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.json.JsonObject;

public class DeprecatedLedgerTracer extends LedgerTracerBase<JsonObject>
    implements DeprecatedLedgerReturnable {
  private static final JsonpSerDe serde = new JsonpSerDe();
  private final LedgerTracer tracer;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public DeprecatedLedgerTracer(LedgerTracer tracer) {
    super(null);
    this.tracer = tracer;
  }

  @Override
  public void setInput(String input) {
    tracer.setInput(input);
  }

  @Override
  public void setInput(AssetKey key, InternalAsset asset) {
    tracer.setInput(key, asset);
  }

  @Override
  public String getOutput(AssetKey key) {
    return serde.serialize(tracer.getOutput(key));
  }

  @Override
  public Map<AssetKey, String> getOutputs() {
    return tracer.getOutputs();
  }

  @Override
  public Optional<Asset<JsonObject>> get(String assetId) {
    return tracer.get(assetId).map(this::createAsset);
  }

  @Override
  public List<Asset<JsonObject>> scan(AssetFilter filter) {
    return tracer.scan(filter).stream().map(this::createAsset).collect(Collectors.toList());
  }

  @Override
  public void put(String assetId, JsonObject data) {
    tracer.put(assetId, data);
  }

  @Nonnull
  @Override
  @SuppressFBWarnings("EI_EXPOSE_REP")
  public Ledger getDeprecatedLedger() {
    return tracer;
  }

  private Asset<JsonObject> createAsset(com.scalar.dl.ledger.asset.Asset asset) {
    return new Asset<JsonObject>() {
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
        return asset.data();
      }

      @Override
      public AssetMetadata metadata() {
        com.scalar.dl.ledger.asset.AssetMetadata metadata = asset.metadata();
        if (metadata == null) {
          throw new IllegalStateException(CommonError.METADATA_NOT_AVAILABLE.buildMessage());
        }
        return new AssetMetadata() {
          @Override
          public String nonce() {
            return metadata.nonce();
          }

          @Override
          public String input() {
            return metadata.input();
          }

          @Override
          public byte[] hash() {
            return metadata.hash();
          }

          @Override
          public byte[] prevHash() {
            return metadata.prevHash();
          }
        };
      }
    };
  }
}
