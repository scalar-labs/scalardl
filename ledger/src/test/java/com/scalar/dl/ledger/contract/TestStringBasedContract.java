package com.scalar.dl.ledger.contract;

import com.scalar.dl.ledger.statemachine.Ledger;
import javax.annotation.Nullable;

public class TestStringBasedContract extends StringBasedContract {

  @Override
  public String invoke(Ledger<String> ledger, String argument, @Nullable String properties) {
    return null;
  }
}
