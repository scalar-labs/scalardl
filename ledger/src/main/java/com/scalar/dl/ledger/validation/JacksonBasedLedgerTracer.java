package com.scalar.dl.ledger.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.database.AssetScanner;
import com.scalar.dl.ledger.error.LedgerError;
import com.scalar.dl.ledger.exception.ValidationException;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.AssetInput;
import com.scalar.dl.ledger.statemachine.AssetKey;
import com.scalar.dl.ledger.statemachine.Context;
import com.scalar.dl.ledger.statemachine.InternalAsset;
import com.scalar.dl.ledger.statemachine.MetadataComprisedAsset;
import com.scalar.dl.ledger.util.JacksonSerDe;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JacksonBasedLedgerTracer extends LedgerTracerBase<JsonNode> {
  private static final JacksonSerDe serde = new JacksonSerDe(new ObjectMapper());
  private final AssetScanner scanner;

  public JacksonBasedLedgerTracer(Context context, AssetScanner scanner) {
    super(context);
    this.scanner = scanner;
  }

  @Override
  public void setInput(String input) {
    AssetInput assetInput = new AssetInput(input);
    assetInput.forEach(
        eachInput -> {
          String inputNamespace =
              eachInput.namespace() == null ? context.getNamespace() : eachInput.namespace();
          InternalAsset asset = scanner.doGet(inputNamespace, eachInput.id(), eachInput.age());
          if (asset == null) {
            throw new ValidationException(LedgerError.INCONSISTENT_INPUT_DEPENDENCIES);
          }
          inputs.put(
              AssetKey.of(inputNamespace, eachInput.id()),
              new MetadataComprisedAsset<>(asset, serde::deserialize));
        });
  }

  @Override
  public void setInput(AssetKey key, InternalAsset asset) {
    if (asset == null) {
      return;
    }
    inputs.put(key, new MetadataComprisedAsset<>(asset, serde::deserialize));
  }

  @Override
  public String getOutput(AssetKey key) {
    return serde.serialize(outputs.get(key));
  }

  @Override
  public Map<AssetKey, String> getOutputs() {
    Map<AssetKey, String> result = new HashMap<>();
    outputs.forEach((key, output) -> result.put(key, serde.serialize(output)));
    return result;
  }

  @Override
  public List<Asset<JsonNode>> scan(AssetFilter filter) {
    return scanner.doScan(filter).stream()
        .map(asset -> new MetadataComprisedAsset<>(asset, serde::deserialize))
        .collect(Collectors.toList());
  }

  @VisibleForTesting
  Map<AssetKey, Asset<JsonNode>> getInputs() {
    return inputs;
  }
}
