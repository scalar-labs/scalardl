package com.scalar.dl.ledger.crypto;

public interface SignatureValidator {

  boolean validate(byte[] toBeValidated, byte[] signatureBytes);
}
