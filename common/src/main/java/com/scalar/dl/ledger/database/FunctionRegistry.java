package com.scalar.dl.ledger.database;

import com.scalar.dl.ledger.function.FunctionEntry;
import java.util.Optional;

public interface FunctionRegistry {

  void bind(FunctionEntry entry);

  void unbind(String id);

  Optional<FunctionEntry> lookup(String id);
}
