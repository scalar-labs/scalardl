package com.scalar.dl.ledger.validation;

import com.google.common.annotations.VisibleForTesting;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.AssetScanner;
import com.scalar.dl.ledger.error.LedgerError;
import com.scalar.dl.ledger.exception.ValidationException;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.AssetInput;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import com.scalar.dl.ledger.statemachine.MetadataComprisedAsset;
import com.scalar.dl.ledger.util.JsonpSerDe;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.json.JsonObject;

public class JsonpBasedLedgerTracer extends LedgerTracerBase<JsonObject> {
  private static final JsonpSerDe serde = new JsonpSerDe();
  private final AssetScanner scanner;

  public JsonpBasedLedgerTracer(AssetScanner scanner) {
    this.scanner = scanner;
  }

  @Override
  public void setInput(String input) {
    AssetInput assetInput = new AssetInput(input);
    assetInput.forEach(
        eachInput -> {
          InternalAsset asset = scanner.doGet(eachInput.id(), eachInput.age());
          if (asset == null) {
            throw new ValidationException(LedgerError.INCONSISTENT_INPUT_DEPENDENCIES);
          }
          inputs.put(eachInput.id(), new MetadataComprisedAsset<>(asset, serde::deserialize));
        });
  }

  @Override
  public void setInput(String assetId, InternalAsset asset) {
    if (asset == null) {
      return;
    }
    inputs.put(assetId, new MetadataComprisedAsset<>(asset, serde::deserialize));
  }

  @Override
  public String getOutput(String assetId) {
    return serde.serialize(outputs.get(assetId));
  }

  @Override
  public Map<String, String> getOutputs() {
    Map<String, String> result = new HashMap<>();
    outputs.forEach((assetId, output) -> result.put(assetId, serde.serialize(output)));
    return result;
  }

  @Override
  public List<Asset<JsonObject>> scan(AssetFilter filter) {
    return scanner.doScan(filter).stream()
        .map(asset -> new MetadataComprisedAsset<>(asset, serde::deserialize))
        .collect(Collectors.toList());
  }

  @VisibleForTesting
  Map<String, Asset<JsonObject>> getInputs() {
    return inputs;
  }
}
