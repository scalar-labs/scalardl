package com.scalar.dl.ledger.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class CertificateRegistrationRequestTest {
  private static final String ENTITY_ID = "entity_id";
  private static final int VERSION = 1;
  private static final int ILLEGAL_VERSION = 0;
  private static final String PEM = "pem";

  @Test
  public void constructor_ProperValuesGiven_ShouldInstantiate() {
    // Act Assert
    assertThatCode(() -> new CertificateRegistrationRequest(ENTITY_ID, VERSION, PEM))
        .doesNotThrowAnyException();
  }

  @Test
  public void constructor_IllegalVersionGiven_ShouldThrowIllegalArgumentException() {
    // Act Assert
    assertThatThrownBy(() -> new CertificateRegistrationRequest(ENTITY_ID, ILLEGAL_VERSION, PEM))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void equals_OnTheSameObject_ShouldReturnTrue() {
    // Arrange
    CertificateRegistrationRequest request =
        new CertificateRegistrationRequest(ENTITY_ID, VERSION, PEM);
    CertificateRegistrationRequest other = request;

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  public void equals_OnTheSameData_ShouldReturnTrue() {
    // Arrange
    CertificateRegistrationRequest request =
        new CertificateRegistrationRequest(ENTITY_ID, VERSION, PEM);
    CertificateRegistrationRequest other =
        new CertificateRegistrationRequest(ENTITY_ID, VERSION, PEM);

    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isTrue();
  }

  @Test
  public void equals_OnAnArbitraryObject_ShouldReturnFalse() {
    // Arrange
    CertificateRegistrationRequest request =
        new CertificateRegistrationRequest(ENTITY_ID, VERSION, PEM);
    Object arbitraryObject = new Object();

    // Act
    boolean result = request.equals(arbitraryObject);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void equals_OnDifferentData_ShouldReturnFalse() {
    // Arrange
    CertificateRegistrationRequest request =
        new CertificateRegistrationRequest(ENTITY_ID, VERSION, PEM);
    String wrongPEM = "wrong PEM";
    CertificateRegistrationRequest other =
        new CertificateRegistrationRequest(ENTITY_ID, VERSION, wrongPEM);
    // Act
    boolean result = request.equals(other);

    // Assert
    assertThat(result).isFalse();
  }

  @Test
  public void getters_ProperValuesGiven_ShouldReturnProperValues() {
    // Act
    CertificateRegistrationRequest request =
        new CertificateRegistrationRequest(ENTITY_ID, VERSION, PEM);

    // Assert
    assertThat(request.getEntityId()).isEqualTo(ENTITY_ID);
    assertThat(request.getKeyVersion()).isEqualTo(VERSION);
    assertThat(request.getCertPem()).isEqualTo(PEM);
  }
}
