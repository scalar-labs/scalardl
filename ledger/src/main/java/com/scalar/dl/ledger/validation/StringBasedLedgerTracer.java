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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StringBasedLedgerTracer extends LedgerTracerBase<String> {
  private final AssetScanner scanner;

  public StringBasedLedgerTracer(AssetScanner scanner) {
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
          inputs.put(eachInput.id(), new MetadataComprisedAsset<>(asset, data -> data));
        });
  }

  @Override
  public void setInput(String assetId, InternalAsset asset) {
    if (asset == null) {
      return;
    }
    inputs.put(assetId, new MetadataComprisedAsset<>(asset, data -> data));
  }

  @Override
  public String getOutput(String assetId) {
    return outputs.get(assetId);
  }

  @Override
  @SuppressFBWarnings("EI_EXPOSE_REP")
  public Map<String, String> getOutputs() {
    return outputs;
  }

  @Override
  public List<Asset<String>> scan(AssetFilter filter) {
    return scanner.doScan(filter).stream()
        .map(asset -> new MetadataComprisedAsset<>(asset, data -> data))
        .collect(Collectors.toList());
  }

  @VisibleForTesting
  Map<String, Asset<String>> getInputs() {
    return inputs;
  }
}
