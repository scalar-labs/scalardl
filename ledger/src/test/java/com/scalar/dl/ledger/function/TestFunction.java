package com.scalar.dl.ledger.function;

import com.scalar.dl.ledger.database.Database;
import java.util.Optional;
import javax.json.JsonObject;

public class TestFunction extends Function {

  @Override
  public void invoke(
      Database database,
      Optional<JsonObject> functionArgument,
      JsonObject contractArgument,
      Optional<JsonObject> contractProperties) {}
}
