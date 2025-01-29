package com.scalar.dl.ledger.service.contract;

import com.google.common.base.Splitter;
import com.scalar.dl.ledger.contract.StringBasedContract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.service.Constants;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

public class GetBalanceWithString extends StringBasedContract {

  @Nullable
  @Override
  public String invoke(Ledger<String> ledger, String argument, @Nullable String properties) {
    if (properties == null || !properties.equals(Constants.GET_BALANCE_CONTRACT_ID4)) {
      throw new ContractContextException("properties is not set as expected.");
    }

    // <asset_id>,<amount>
    List<String> elements = Splitter.on(',').splitToList(argument);
    Optional<Asset<String>> asset = ledger.get(elements.get(0));

    if (!asset.isPresent()) {
      throw new ContractContextException("asset not found");
    }

    return asset.get().data();
  }
}
