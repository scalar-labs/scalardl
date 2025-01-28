package com.scalar.dl.ledger.crypto;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.scalar.dl.ledger.config.AuthenticationMethod;
import com.scalar.dl.ledger.config.ServersHmacAuthenticatable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.concurrent.Immutable;

@Immutable
public class ClientKeyValidator {
  public static final String AUDITOR_ENTITY_ID = "_AUDITOR_";
  private final ServersHmacAuthenticatable hmacAuthenticatable;
  private final AuthenticationMethod authMethod;
  private final CertificateManager certManager;
  private final SecretManager secretManager;
  private final Map<String, SignatureValidator> validators;

  @Inject
  public ClientKeyValidator(
      ServersHmacAuthenticatable hmacAuthenticatable,
      AuthenticationMethod authMethod,
      CertificateManager certManager,
      SecretManager secretManager) {
    this.hmacAuthenticatable = hmacAuthenticatable;
    this.authMethod = authMethod;
    this.certManager = certManager;
    this.secretManager = secretManager;
    this.validators = new ConcurrentHashMap<>();
  }

  public SignatureValidator getValidator(String entityId, int keyVersion) {
    if (authMethod == AuthenticationMethod.DIGITAL_SIGNATURE) {
      return certManager.getValidator(new CertificateEntry.Key(entityId, keyVersion));
    } else {
      if (entityId.equals(AUDITOR_ENTITY_ID)) { // from Auditor
        assert hmacAuthenticatable.getServersAuthenticationHmacSecretKey() != null;
        return validators.computeIfAbsent(
            hmacAuthenticatable.getServersAuthenticationHmacSecretKey(), this::createHmacValidator);
      } else {
        return secretManager.getValidator(new SecretEntry.Key(entityId, keyVersion));
      }
    }
  }

  @VisibleForTesting
  HmacValidator createHmacValidator(String secretKey) {
    return new HmacValidator(secretKey);
  }
}
