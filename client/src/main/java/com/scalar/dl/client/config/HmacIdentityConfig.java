package com.scalar.dl.client.config;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;
import javax.annotation.concurrent.Immutable;

@Immutable
public class HmacIdentityConfig {
  private final String entityId;
  private final int secretKeyVersion;
  private final String secretKey;

  private HmacIdentityConfig(HmacIdentityConfig.Builder builder) {
    this.entityId = builder.entityId;
    this.secretKeyVersion = builder.secretKeyVersion;
    this.secretKey = builder.secretKey;
  }

  public String getEntityId() {
    return entityId;
  }

  public int getSecretKeyVersion() {
    return secretKeyVersion;
  }

  public String getSecretKey() {
    return secretKey;
  }

  @Override
  public int hashCode() {
    return Objects.hash(entityId, secretKeyVersion, secretKey);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof HmacIdentityConfig)) {
      return false;
    }
    HmacIdentityConfig other = (HmacIdentityConfig) o;
    return getEntityId().equals(other.getEntityId())
        && getSecretKeyVersion() == other.getSecretKeyVersion()
        && getSecretKey().equals(other.getSecretKey());
  }

  public static HmacIdentityConfig.Builder newBuilder() {
    return new HmacIdentityConfig.Builder();
  }

  public static final class Builder {
    private String entityId;
    private int secretKeyVersion;
    private String secretKey;

    Builder() {
      this.entityId = null;
      this.secretKeyVersion = -1;
      this.secretKey = null;
    }

    public HmacIdentityConfig.Builder entityId(String entityId) {
      checkArgument(entityId != null);
      this.entityId = entityId;
      return this;
    }

    public HmacIdentityConfig.Builder secretKeyVersion(int secretKeyVersion) {
      checkArgument(secretKeyVersion >= 0);
      this.secretKeyVersion = secretKeyVersion;
      return this;
    }

    public HmacIdentityConfig.Builder secretKey(String secretKey) {
      checkArgument(secretKey != null);
      this.secretKey = secretKey;
      return this;
    }

    public HmacIdentityConfig build() {
      if (entityId == null || secretKeyVersion < 0 || secretKey == null) {
        throw new IllegalArgumentException("Required fields are not given.");
      }
      return new HmacIdentityConfig(this);
    }
  }
}
