package com.scalar.dl.ledger.crypto;

public interface SignatureSigner {

  byte[] sign(byte[] bytes);
}
