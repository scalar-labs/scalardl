package com.scalar.dl.ledger.service.contract;

import com.scalar.dl.ledger.asset.Asset;
import com.scalar.dl.ledger.contract.Contract;
import com.scalar.dl.ledger.database.Ledger;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.service.Constants;
import java.util.Optional;
import javax.json.JsonObject;

public class GetBalance extends Contract {

  @Override
  public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> properties) {
    if (!properties.isPresent()
        || !properties
            .get()
            .getString(Constants.CONTRACT_ID_ATTRIBUTE_NAME)
            .equals(Constants.GET_BALANCE_CONTRACT_ID1)) {
      throw new ContractContextException("properties is not set as expected.");
    }

    String assetId = argument.getString(Constants.ASSET_ATTRIBUTE_NAME);
    Optional<Asset> asset = ledger.get(assetId);

    if (!asset.isPresent()) {
      throw new ContractContextException("asset not found");
    }

    return asset.get().data();
  }
}
