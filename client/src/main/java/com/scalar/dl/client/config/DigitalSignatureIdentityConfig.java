package com.scalar.dl.client.config;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;
import javax.annotation.concurrent.Immutable;

@Immutable
public class DigitalSignatureIdentityConfig {
  private final String entityId;
  private final int certVersion;
  private final String cert;
  private final String privateKey;

  private DigitalSignatureIdentityConfig(DigitalSignatureIdentityConfig.Builder builder) {
    this.entityId = builder.entityId;
    this.certVersion = builder.certVersion;
    this.cert = builder.cert;
    this.privateKey = builder.privateKey;
  }

  public String getEntityId() {
    return entityId;
  }

  public int getCertVersion() {
    return certVersion;
  }

  public String getCert() {
    return cert;
  }

  public String getPrivateKey() {
    return privateKey;
  }

  @Override
  public int hashCode() {
    return Objects.hash(entityId, certVersion, cert, privateKey);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof DigitalSignatureIdentityConfig)) {
      return false;
    }
    DigitalSignatureIdentityConfig other = (DigitalSignatureIdentityConfig) o;
    return getEntityId().equals(other.getEntityId())
        && getCertVersion() == other.getCertVersion()
        && getCert().equals(other.getCert())
        && getPrivateKey().equals(other.getPrivateKey());
  }

  public static DigitalSignatureIdentityConfig.Builder newBuilder() {
    return new DigitalSignatureIdentityConfig.Builder();
  }

  public static final class Builder {
    private String entityId;
    private int certVersion;
    private String cert;
    private String privateKey;

    Builder() {
      this.entityId = null;
      this.certVersion = -1;
      this.cert = null;
      this.privateKey = null;
    }

    public DigitalSignatureIdentityConfig.Builder entityId(String entityId) {
      checkArgument(entityId != null);
      this.entityId = entityId;
      return this;
    }

    public DigitalSignatureIdentityConfig.Builder certVersion(int certVersion) {
      checkArgument(certVersion >= 0);
      this.certVersion = certVersion;
      return this;
    }

    public DigitalSignatureIdentityConfig.Builder cert(String cert) {
      checkArgument(cert != null);
      this.cert = cert;
      return this;
    }

    public DigitalSignatureIdentityConfig.Builder privateKey(String privateKey) {
      checkArgument(privateKey != null);
      this.privateKey = privateKey;
      return this;
    }

    public DigitalSignatureIdentityConfig build() {
      if (entityId == null || certVersion < 0 || cert == null || privateKey == null) {
        throw new IllegalArgumentException("Required fields are not given.");
      }
      return new DigitalSignatureIdentityConfig(this);
    }
  }
}
