package com.scalar.dl.ledger.crypto;

import com.google.inject.Inject;
import com.scalar.dl.ledger.config.LedgerConfig;
import javax.annotation.concurrent.Immutable;

@Immutable
public class AuditorKeyValidator {
  private final LedgerConfig config;
  private final CertificateManager certManager;
  private final SecretManager secretManager;

  @Inject
  public AuditorKeyValidator(
      LedgerConfig config, CertificateManager certManager, SecretManager secretManager) {
    this.config = config;
    this.certManager = certManager;
    this.secretManager = secretManager;
  }

  public SignatureValidator getValidator() {
    if (config.getServersAuthenticationHmacSecretKey() == null) { // use digital signatures
      return certManager.getValidator(
          new CertificateEntry.Key(
              config.getAuditorCertHolderId(), config.getAuditorCertVersion()));
    } else { // use HMAC
      return secretManager.getValidator(config.getServersAuthenticationHmacSecretKey());
    }
  }
}
