package com.scalar.dl.ledger.service.contract;

import com.scalar.dl.ledger.contract.JsonpBasedContract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.service.Constants;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.json.JsonObject;

public class GetBalanceWithJsonp extends JsonpBasedContract {

  @Nullable
  @Override
  public JsonObject invoke(
      Ledger<JsonObject> ledger, JsonObject argument, @Nullable JsonObject properties) {
    if (properties == null
        || !properties
            .getString(Constants.CONTRACT_ID_ATTRIBUTE_NAME)
            .equals(Constants.GET_BALANCE_CONTRACT_ID2)) {
      throw new ContractContextException("properties is not set as expected.");
    }

    String assetId = argument.getString(Constants.ASSET_ATTRIBUTE_NAME);
    Optional<Asset<JsonObject>> asset = ledger.get(assetId);

    if (!asset.isPresent()) {
      throw new ContractContextException("asset not found");
    }

    return asset.get().data();
  }
}
