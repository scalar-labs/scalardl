package com.scalar.dl.ledger.database;

import com.scalar.dl.ledger.crypto.CertificateEntry;

public interface CertificateRegistry {

  void bind(String namespace, CertificateEntry entry);

  void unbind(String namespace, CertificateEntry.Key key);

  CertificateEntry lookup(String namespace, CertificateEntry.Key key);
}
