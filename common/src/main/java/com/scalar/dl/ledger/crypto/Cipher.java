package com.scalar.dl.ledger.crypto;

import java.nio.charset.StandardCharsets;
import java.security.spec.AlgorithmParameterSpec;
import javax.annotation.concurrent.Immutable;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

@Immutable
public class Cipher {
  private static final int GCM_TAG_BIT_LENGTH = 128; // Must be one of {128, 120, 112, 104, 96}
  private static final String transformation = "AES/GCM/NoPadding";
  private final SecretKey secretKey;

  public Cipher(SecretKey secretKey) {
    this.secretKey = secretKey;
  }

  public byte[] encrypt(byte[] plainBytes, String nonce) {
    try {
      javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(transformation);
      cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey, getParameterSpec(nonce));
      return cipher.doFinal(plainBytes);
    } catch (Exception e) {
      throw new SecurityException(e);
    }
  }

  public byte[] decrypt(byte[] cipherBytes, String nonce) {
    try {
      javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(transformation);
      cipher.init(javax.crypto.Cipher.DECRYPT_MODE, secretKey, getParameterSpec(nonce));
      return cipher.doFinal(cipherBytes);
    } catch (Exception e) {
      throw new SecurityException(e);
    }
  }

  private AlgorithmParameterSpec getParameterSpec(String nonce) {
    return new GCMParameterSpec(GCM_TAG_BIT_LENGTH, nonce.getBytes(StandardCharsets.UTF_8));
  }
}
