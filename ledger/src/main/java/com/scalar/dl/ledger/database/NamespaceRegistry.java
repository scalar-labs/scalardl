package com.scalar.dl.ledger.database;

import java.util.List;

public interface NamespaceRegistry {

  void create(String namespace);

  void drop(String namespace);

  List<String> scan(String pattern);
}
