package com.scalar.dl.ledger.crypto;

import static org.assertj.core.api.Java6Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.Before;
import org.junit.Test;

public class CipherTest {
  private static final String NONCE = "nonce";
  private Cipher cipher;

  @Before
  public void setUp() throws InvalidKeySpecException, NoSuchAlgorithmException {
    SecretKey secretKey = getSecretKey();
    cipher = new Cipher(secretKey);
  }

  @Test
  public void encryptDecrypt_PlainTextGiven_ShouldReturnSameText() {
    // Arrange
    String plainText = "plainText";

    // Act
    byte[] encrypted = cipher.encrypt(plainText.getBytes(StandardCharsets.UTF_8), NONCE);
    byte[] decrypted = cipher.decrypt(encrypted, NONCE);

    // Assert
    assertThat(new String(decrypted, StandardCharsets.UTF_8)).isEqualTo(plainText);
  }

  @Test
  public void encryptDecrypt_ExecutedInParallel_ShouldWorkProperly() {
    // Arrange
    String plainText = "plainText";
    List<String> plainTexts = Arrays.asList(plainText, plainText, plainText, plainText, plainText);

    // Act
    List<byte[]> encrypted =
        plainTexts.parallelStream()
            .map(t -> cipher.encrypt(t.getBytes(StandardCharsets.UTF_8), NONCE))
            .collect(Collectors.toList());
    List<byte[]> decrypted =
        encrypted.parallelStream().map(e -> cipher.decrypt(e, NONCE)).collect(Collectors.toList());

    // Assert
    decrypted.forEach(d -> assertThat(new String(d, StandardCharsets.UTF_8)).isEqualTo(plainText));
  }

  private SecretKey getSecretKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    KeySpec spec =
        new PBEKeySpec(
            "password".toCharArray(), "salt".getBytes(StandardCharsets.UTF_8), 65536, 256);
    return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
  }
}
