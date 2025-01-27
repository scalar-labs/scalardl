package com.scalar.dl.ledger.database;

import com.scalar.dl.ledger.crypto.SecretEntry;

public interface SecretRegistry {

  void bind(SecretEntry entry);

  void unbind(SecretEntry.Key key);

  SecretEntry lookup(SecretEntry.Key key);
}
