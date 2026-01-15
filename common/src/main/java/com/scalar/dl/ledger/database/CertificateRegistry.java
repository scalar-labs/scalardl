package com.scalar.dl.ledger.database;

import com.scalar.dl.ledger.crypto.CertificateEntry;

public interface CertificateRegistry {

  void bind(CertificateEntry entry);

  void unbind(CertificateEntry.Key key);

  CertificateEntry lookup(CertificateEntry.Key key);
}
