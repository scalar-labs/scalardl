package com.scalar.dl.ledger.crypto;

import com.scalar.dl.ledger.error.CommonError;
import com.scalar.dl.ledger.exception.SecurityException;
import java.security.spec.KeySpec;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class CipherFactory {
  private static final String PKCS5_ALGORITHM = "PBKDF2WithHmacSHA256";
  private static final String ENCRYPTION_ALGORITHM = "AES";
  private static final int ITERATION_COUNT = 65536;
  private static final int KEY_LENGTH = 256;

  public static Cipher create(String cipherKey, byte[] salt) {
    try {
      SecretKeyFactory factory = SecretKeyFactory.getInstance(PKCS5_ALGORITHM);
      KeySpec spec = new PBEKeySpec(cipherKey.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
      return new Cipher(
          new SecretKeySpec(factory.generateSecret(spec).getEncoded(), ENCRYPTION_ALGORITHM));
    } catch (Exception e) {
      throw new SecurityException(CommonError.CREATING_CIPHER_KEY_FAILED, e, e.getMessage());
    }
  }
}
