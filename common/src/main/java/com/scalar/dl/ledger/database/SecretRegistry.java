package com.scalar.dl.ledger.database;

import com.scalar.dl.ledger.crypto.SecretEntry;

public interface SecretRegistry {

  void bind(String namespace, SecretEntry entry);

  void unbind(String namespace, SecretEntry.Key key);

  SecretEntry lookup(String namespace, SecretEntry.Key key);
}
