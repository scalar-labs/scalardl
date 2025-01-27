package com.scalar.dl.ledger.crypto;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.Before;
import org.junit.Test;

public class HmacValidatorTest {
  private static final String SECRET_KEY_A = "secret_key_a";
  private static final String SECRET_KEY_B = "secret_key_b";
  private HmacSigner signer;
  private HmacValidator validator;

  @Before
  public void setUp() {
    signer = new HmacSigner(SECRET_KEY_A);
    validator = new HmacValidator(SECRET_KEY_A);
  }

  @Test
  public void validate_CorrectBytesAndSignatureGiven_ShouldReturnTrue() {
    // Arrange
    byte[] bytes = "any_bytes".getBytes(StandardCharsets.UTF_8);
    byte[] signature = signer.sign(bytes);

    // Act
    boolean actual = validator.validate(bytes, signature);

    // Assert
    assertThat(actual).isTrue();
  }

  @Test
  public void validate_IncorrectBytesGiven_ShouldReturnFalse() {
    // Arrange
    byte[] bytes = "any_bytes".getBytes(StandardCharsets.UTF_8);
    byte[] signature = signer.sign(bytes);

    // Act
    boolean actual = validator.validate("incorrect".getBytes(StandardCharsets.UTF_8), signature);

    // Assert
    assertThat(actual).isFalse();
  }

  @Test
  public void validate_IncorrectSignatureGiven_ShouldReturnFalse() {
    // Arrange
    byte[] bytes = "any_bytes".getBytes(StandardCharsets.UTF_8);
    byte[] signature = signer.sign("another_bytes".getBytes(StandardCharsets.UTF_8));

    // Act
    boolean actual = validator.validate(bytes, signature);

    // Assert
    assertThat(actual).isFalse();
  }

  @Test
  public void validate_SignatureSignedByIncompatiblePrivateKeyGiven_ShouldReturnFalse() {
    // Arrange
    signer = new HmacSigner(SECRET_KEY_B);
    byte[] bytes = "any_bytes".getBytes(StandardCharsets.UTF_8);
    byte[] signature = signer.sign(bytes);

    // Act
    boolean actual = validator.validate(bytes, signature);

    // Assert
    assertThat(actual).isFalse();
  }

  @Test
  public void validate_ValidatorCreatedWithIncompatibleCertificateGiven_ShouldReturnFalse() {
    // Arrange
    byte[] bytes = "any_bytes".getBytes(StandardCharsets.UTF_8);
    byte[] signature = signer.sign(bytes);
    validator = new HmacValidator(SECRET_KEY_B);

    // Act
    boolean actual = validator.validate(bytes, signature);

    // Assert
    assertThat(actual).isFalse();
  }
}
