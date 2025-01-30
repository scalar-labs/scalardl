package com.scalar.dl.ledger.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.dl.ledger.config.AuthenticationMethod;
import com.scalar.dl.ledger.config.ServersHmacAuthenticatable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ClientKeyValidatorTest {
  private static final String SOME_ENTITY_ID = "entity_id";
  private static final int SOME_KEY_VERSION = 1;
  private static final String SOME_SECRET_KEY = "secret_key";
  @Mock private CertificateManager certificateManager;
  @Mock private SecretManager secretManager;
  @Mock private DigitalSignatureValidator digitalSignatureValidator;
  @Mock private HmacValidator hmacValidator;
  @Mock private ServersHmacAuthenticatable serversHmacAuthenticatable;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void getValidator_DigitalSignatureConfigured_ShouldReturnDigitalSignatureValidator() {
    // Arrange
    ClientKeyValidator clientKeyValidator =
        new ClientKeyValidator(
            serversHmacAuthenticatable,
            AuthenticationMethod.DIGITAL_SIGNATURE,
            certificateManager,
            secretManager);
    when(certificateManager.getValidator(any(CertificateEntry.Key.class)))
        .thenReturn(digitalSignatureValidator);

    // Act
    SignatureValidator validator =
        clientKeyValidator.getValidator(SOME_ENTITY_ID, SOME_KEY_VERSION);

    // Assert
    assertThat(validator).isEqualTo(digitalSignatureValidator);
    verify(certificateManager)
        .getValidator(new CertificateEntry.Key(SOME_ENTITY_ID, SOME_KEY_VERSION));
    verify(secretManager, never()).getValidator(anyString());
  }

  @Test
  public void getValidator_HmacConfigured_ShouldReturnHmacValidator() {
    // Arrange
    ClientKeyValidator clientKeyValidator =
        new ClientKeyValidator(
            serversHmacAuthenticatable,
            AuthenticationMethod.HMAC,
            certificateManager,
            secretManager);
    when(secretManager.getValidator(any(SecretEntry.Key.class))).thenReturn(hmacValidator);

    // Act
    SignatureValidator validator =
        clientKeyValidator.getValidator(SOME_ENTITY_ID, SOME_KEY_VERSION);

    // Assert
    assertThat(validator).isEqualTo(hmacValidator);
    verify(secretManager).getValidator(new SecretEntry.Key(SOME_ENTITY_ID, SOME_KEY_VERSION));
    verify(certificateManager, never()).getValidator(any(CertificateEntry.Key.class));
  }

  @Test
  public void
      getValidator_HmacConfiguredAndRequestGivenFromAuditor_ShouldReturnHmacValidatorWithServersSecretKey() {
    // Arrange
    ClientKeyValidator clientKeyValidator =
        spy(
            new ClientKeyValidator(
                serversHmacAuthenticatable,
                AuthenticationMethod.HMAC,
                certificateManager,
                secretManager));
    when(serversHmacAuthenticatable.getServersAuthenticationHmacSecretKey())
        .thenReturn(SOME_SECRET_KEY);
    doReturn(hmacValidator).when(clientKeyValidator).createHmacValidator(anyString());

    // Act
    SignatureValidator validator =
        clientKeyValidator.getValidator(ClientKeyValidator.AUDITOR_ENTITY_ID, SOME_KEY_VERSION);

    // Assert
    assertThat(validator).isEqualTo(hmacValidator);
    verify(secretManager, never()).getValidator(any(SecretEntry.Key.class));
    verify(certificateManager, never()).getValidator(any(CertificateEntry.Key.class));
    verify(clientKeyValidator).createHmacValidator(SOME_SECRET_KEY);
  }

  @Test
  public void
      getValidator_HmacConfiguredAndRequestGivenFromAuditorTwice_ShouldReturnCachedHmacValidatorWithServersSecretKey() {
    // Arrange
    ClientKeyValidator clientKeyValidator =
        spy(
            new ClientKeyValidator(
                serversHmacAuthenticatable,
                AuthenticationMethod.HMAC,
                certificateManager,
                secretManager));
    when(serversHmacAuthenticatable.getServersAuthenticationHmacSecretKey())
        .thenReturn(SOME_SECRET_KEY);
    doReturn(hmacValidator).when(clientKeyValidator).createHmacValidator(anyString());
    clientKeyValidator.getValidator(ClientKeyValidator.AUDITOR_ENTITY_ID, SOME_KEY_VERSION);

    // Act
    SignatureValidator validator2 =
        clientKeyValidator.getValidator(ClientKeyValidator.AUDITOR_ENTITY_ID, SOME_KEY_VERSION);

    // Assert
    assertThat(validator2).isEqualTo(hmacValidator);
    verify(secretManager, never()).getValidator(any(SecretEntry.Key.class));
    verify(certificateManager, never()).getValidator(any(CertificateEntry.Key.class));
    verify(clientKeyValidator).createHmacValidator(SOME_SECRET_KEY);
  }
}
