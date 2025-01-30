package com.scalar.dl.ledger.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.dl.ledger.config.LedgerConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AuditorKeyValidatorTest {
  private static final String SOME_ENTITY_ID = "entity_id";
  private static final int SOME_KEY_VERSION = 1;
  private static final String SOME_SECRET_KEY = "secret_key";
  @Mock private LedgerConfig config;
  @Mock private CertificateManager certificateManager;
  @Mock private SecretManager secretManager;
  @Mock private DigitalSignatureValidator digitalSignatureValidator;
  @Mock private HmacValidator hmacValidator;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void getValidator_HmacSecretKeyConfigured_ShouldReturnHmacValidator() {
    // Arrange
    when(config.getServersAuthenticationHmacSecretKey()).thenReturn(SOME_SECRET_KEY);
    AuditorKeyValidator auditorKeyValidator =
        new AuditorKeyValidator(config, certificateManager, secretManager);
    when(secretManager.getValidator(anyString())).thenReturn(hmacValidator);

    // Act
    SignatureValidator validator = auditorKeyValidator.getValidator();

    // Assert
    assertThat(validator).isEqualTo(hmacValidator);
    verify(secretManager).getValidator(SOME_SECRET_KEY);
    verify(certificateManager, never()).getValidator(any(CertificateEntry.Key.class));
  }

  @Test
  public void getValidator_HmacSecretKeyNotConfigured_ShouldReturnDigitalSignatureValidator() {
    // Arrange
    when(config.getServersAuthenticationHmacSecretKey()).thenReturn(null);
    when(config.getAuditorCertHolderId()).thenReturn(SOME_ENTITY_ID);
    when(config.getAuditorCertVersion()).thenReturn(SOME_KEY_VERSION);
    AuditorKeyValidator auditorKeyValidator =
        new AuditorKeyValidator(config, certificateManager, secretManager);
    when(certificateManager.getValidator(any())).thenReturn(digitalSignatureValidator);

    // Act
    SignatureValidator validator = auditorKeyValidator.getValidator();

    // Assert
    assertThat(validator).isEqualTo(digitalSignatureValidator);
    verify(certificateManager)
        .getValidator(
            new CertificateEntry.Key(
                config.getAuditorCertHolderId(), config.getAuditorCertVersion()));
    verify(secretManager, never()).getValidator(anyString());
  }
}
