package com.scalar.dl.ledger.database;

import com.scalar.dl.ledger.function.FunctionEntry;
import java.util.Optional;

public interface FunctionRegistry {

  void bind(String namespace, FunctionEntry entry);

  void unbind(String namespace, String id);

  Optional<FunctionEntry> lookup(String namespace, String id);
}
