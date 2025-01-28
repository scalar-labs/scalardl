package com.scalar.dl.ledger.service.contract;

import com.google.common.base.Splitter;
import com.scalar.dl.ledger.contract.StringBasedContract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.service.Constants;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.List;
import javax.annotation.Nullable;

public class CreateWithString extends StringBasedContract {

  @Nullable
  @Override
  public String invoke(Ledger<String> ledger, String argument, @Nullable String properties) {
    // <asset_id>,<amount>
    List<String> elements = Splitter.on(',').splitToList(argument);

    if (elements.size() < 2) {
      throw new ContractContextException("invalid argument format");
    }

    String data = Constants.BALANCE_ATTRIBUTE_NAME + "," + elements.get(1);

    String assetId = elements.get(0);
    ledger.put(assetId, data);

    if (properties != null && elements.size() == 3 && Boolean.parseBoolean(elements.get(2))) {
      if (!properties.equals(Constants.CREATE_CONTRACT_ID4)) {
        throw new ContractContextException("properties is not set properly.");
      }
      return invoke(Constants.GET_BALANCE_CONTRACT_ID4, ledger, argument);
    } else {
      return null;
    }
  }
}
