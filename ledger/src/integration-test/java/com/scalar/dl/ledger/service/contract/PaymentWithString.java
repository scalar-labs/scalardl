package com.scalar.dl.ledger.service.contract;

import com.google.common.base.Splitter;
import com.scalar.dl.ledger.contract.StringBasedContract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.service.Constants;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.List;
import javax.annotation.Nullable;

public class PaymentWithString extends StringBasedContract {

  @Nullable
  @Override
  @SuppressWarnings("StringSplitter")
  public String invoke(Ledger<String> ledger, String argument, @Nullable String properties) {
    // <asset_id_from>,<asset_id_to>,<amount>
    List<String> elements = Splitter.on(',').splitToList(argument);
    if (elements.size() != 3) {
      throw new ContractContextException("invalid argument format");
    }
    String fromId = elements.get(0);
    String toId = elements.get(1);
    int amount = Integer.parseInt(elements.get(2));

    Asset<String> from = ledger.get(fromId).get();
    Asset<String> to = ledger.get(toId).get();
    String fromData = from.data();
    String toData = to.data();

    int fromBalance = Integer.parseInt(fromData.split(",")[1]);
    int toBalance = Integer.parseInt(toData.split(",")[1]);
    if (fromBalance - amount < 0) {
      throw new ContractContextException("not enough balance in from account");
    }

    fromData = Constants.BALANCE_ATTRIBUTE_NAME + "," + (fromBalance - amount);
    toData = Constants.BALANCE_ATTRIBUTE_NAME + "," + (toBalance + amount);

    ledger.put(fromId, fromData);
    ledger.put(toId, toData);

    return null;
  }
}
