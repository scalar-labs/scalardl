package com.scalar.dl.ledger.config;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
public class TargetConfig {
  private final String host;
  private final int port;
  private final int privilegedPort;
  private final boolean tlsEnabled;
  private final String tlsCaRootCert;
  private final String tlsOverrideAuthority;
  private final String authorizationCredential;

  private TargetConfig(TargetConfig.Builder builder) {
    this.host = builder.host;
    this.port = builder.port;
    this.privilegedPort = builder.privilegedPort;
    this.tlsEnabled = builder.tlsEnabled;
    this.tlsCaRootCert = builder.tlsCaRootCert;
    this.tlsOverrideAuthority = builder.tlsOverrideAuthority;
    this.authorizationCredential = builder.authorizationCredential;
  }

  public String getTargetHost() {
    return host;
  }

  public int getTargetPort() {
    return port;
  }

  public int getTargetPrivilegedPort() {
    return privilegedPort;
  }

  public boolean isTargetTlsEnabled() {
    return tlsEnabled;
  }

  @Nullable
  public String getTargetTlsCaRootCert() {
    return tlsCaRootCert;
  }

  @Nullable
  public String getTargetTlsOverrideAuthority() {
    return tlsOverrideAuthority;
  }

  @Nullable
  public String getTargetAuthorizationCredential() {
    return authorizationCredential;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        host, port, privilegedPort, tlsEnabled, tlsCaRootCert, authorizationCredential);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof TargetConfig)) {
      return false;
    }
    TargetConfig other = (TargetConfig) o;
    return getTargetHost().equals(other.getTargetHost())
        && getTargetPort() == other.getTargetPort()
        && getTargetPrivilegedPort() == other.getTargetPrivilegedPort()
        && isTargetTlsEnabled() == other.isTargetTlsEnabled()
        && Objects.equals(getTargetTlsCaRootCert(), other.getTargetTlsCaRootCert())
        && Objects.equals(getTargetTlsOverrideAuthority(), other.getTargetTlsOverrideAuthority())
        && Objects.equals(
            getTargetAuthorizationCredential(), other.getTargetAuthorizationCredential());
  }

  public static TargetConfig.Builder newBuilder() {
    return new TargetConfig.Builder();
  }

  public static final class Builder {
    private String host;
    private int port;
    private int privilegedPort;
    private boolean tlsEnabled;
    private String tlsCaRootCert;
    private String tlsOverrideAuthority;
    private String authorizationCredential;

    Builder() {
      this.host = null;
      this.port = -1;
      this.privilegedPort = -1;
      this.tlsEnabled = false;
      this.tlsCaRootCert = null;
      this.authorizationCredential = null;
    }

    public TargetConfig.Builder host(String host) {
      checkArgument(host != null);
      this.host = host;
      return this;
    }

    public TargetConfig.Builder port(int port) {
      checkArgument(port >= 0);
      this.port = port;
      return this;
    }

    public TargetConfig.Builder privilegedPort(int privilegedPort) {
      checkArgument(privilegedPort >= 0);
      this.privilegedPort = privilegedPort;
      return this;
    }

    public TargetConfig.Builder tlsEnabled(boolean tlsEnabled) {
      this.tlsEnabled = tlsEnabled;
      return this;
    }

    public TargetConfig.Builder tlsCaRootCert(@Nullable String tlsCaRootCert) {
      this.tlsCaRootCert = tlsCaRootCert;
      return this;
    }

    public TargetConfig.Builder tlsOverrideAuthority(@Nullable String tlsOverrideAuthority) {
      this.tlsOverrideAuthority = tlsOverrideAuthority;
      return this;
    }

    public TargetConfig.Builder authorizationCredential(@Nullable String authorizationCredential) {
      this.authorizationCredential = authorizationCredential;
      return this;
    }

    public TargetConfig build() {
      if (host == null || port < 0 || privilegedPort < 0) {
        throw new IllegalArgumentException("Required fields are not given.");
      }
      return new TargetConfig(this);
    }
  }
}
