package com.scalar.dl.client.config;

import com.google.common.annotations.VisibleForTesting;
import com.scalar.dl.ledger.config.ConfigUtils;
import com.scalar.dl.ledger.config.GrpcClientConfig;
import com.scalar.dl.ledger.config.TargetConfig;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.annotation.concurrent.Immutable;

@Immutable
@SuppressFBWarnings("JCIP_FIELD_ISNT_FINAL_IN_IMMUTABLE_CLASS")
public class GatewayClientConfig {
  @VisibleForTesting static final String DEFAULT_GATEWAY_HOST = "localhost";
  @VisibleForTesting static final int DEFAULT_GATEWAY_PORT = 30051;
  @VisibleForTesting static final int DEFAULT_GATEWAY_PRIVILEGED_PORT = 30052;
  @VisibleForTesting static final boolean DEFAULT_GATEWAY_TLS_ENABLED = false;

  private static final String PREFIX = "scalar.dl.client.gateway.";

  /**
   * <code>scalar.dl.client.gateway.host</code> (Optional)<br>
   * A hostname or an IP address of the gateway ("localhost" by default). It assumes that there is a
   * single endpoint that is given by DNS or a load balancer.
   */
  public static final String GATEWAY_HOST = PREFIX + "host";

  /**
   * <code>scalar.dl.client.gateway.port</code> (Optional)<br>
   * A port number of the gateway (30051 by default).
   */
  public static final String GATEWAY_PORT = PREFIX + "port";

  /**
   * <code>scalar.dl.client.gateway.privileged_port</code> (Optional)<br>
   * A port number of the gateway for privileged services (30052 by default).
   */
  public static final String GATEWAY_PRIVILEGED_PORT = PREFIX + "privileged_port";

  /**
   * <code>scalar.dl.client.gateway.tls.enabled</code> (Optional)<br>
   * A flag to enable TLS communication for the gateway (false by default).
   */
  public static final String GATEWAY_TLS_ENABLED = PREFIX + "tls.enabled";

  /**
   * <code>scalar.dl.client.gateway.tls.ca_root_cert_path</code> (Optional)<br>
   * A custom CA root certificate (file path) for TLS communication for the gateway. If the issuing
   * certificate authority is known to the client, it can be empty.
   */
  public static final String GATEWAY_TLS_CA_ROOT_CERT_PATH = PREFIX + "tls.ca_root_cert_path";

  /**
   * <code>scalar.dl.client.gateway.tls.ca_root_cert_pem</code> (Optional)<br>
   * A custom CA root certificate (PEM data) for TLS communication for the gateway. If the issuing
   * certificate authority is known to the client, it can be empty.
   */
  public static final String GATEWAY_TLS_CA_ROOT_CERT_PEM = PREFIX + "tls.ca_root_cert_pem";

  /**
   * <code>scalar.dl.client.gateway.tls.override_authority</code> (Optional)<br>
   * A custom authority for TLS communication for Auditor. This doesn't change what the host is
   * actually connected to. This is intended for testing, but may safely be used outside of tests as
   * an alternative to DNS overrides. For example, you can specify the hostname presented in the
   * certificate chain file that you set for `scalar.dl.gateway.server.tls.cert_chain_path`.
   */
  public static final String GATEWAY_TLS_OVERRIDE_AUTHORITY = PREFIX + "tls.override_authority";

  /**
   * <code>scalar.dl.client.gateway.authorization.credential</code> (Optional)<br>
   * An authorization credential for the gateway. (e.g. authorization: Bearer token) If this is
   * given, clients will add "authorization: [credential]" http/2 header.
   */
  public static final String GATEWAY_AUTHORIZATION_CREDENTIAL = PREFIX + "authorization.credential";

  private final Properties props;
  private String gatewayHost;
  private int gatewayPort;
  private int gatewayPrivilegedPort;
  private boolean isGatewayTlsEnabled;
  private String gatewayTlsCaRootCert;
  private String gatewayTlsOverrideAuthority;
  private String gatewayAuthorizationCredential;
  private ClientConfig clientConfig;
  private GrpcClientConfig grpcClientConfig;
  private TargetConfig gatewayTargetConfig;

  public GatewayClientConfig(File propertiesFile) throws IOException {
    try (FileInputStream stream = new FileInputStream(propertiesFile)) {
      props = new Properties();
      props.load(stream);
    }
    load();
  }

  public GatewayClientConfig(InputStream stream) throws IOException {
    props = new Properties();
    props.load(stream);
    load();
  }

  public GatewayClientConfig(Properties properties) throws IOException {
    props = new Properties();
    props.putAll(properties);
    load();
  }

  /**
   * SpotBugs detects Bug Type "CT_CONSTRUCTOR_THROW" saying that "The object under construction
   * remains partially initialized and may be vulnerable to Finalizer attacks."
   */
  @Override
  protected final void finalize() {}

  public TargetConfig getGatewayTargetConfig() {
    return gatewayTargetConfig;
  }

  public ClientConfig getClientConfig() {
    return clientConfig;
  }

  private void load() throws IOException {
    clientConfig = new ClientConfig(props);
    grpcClientConfig = clientConfig.getGrpcClientConfig();
    gatewayHost = ConfigUtils.getString(props, GATEWAY_HOST, DEFAULT_GATEWAY_HOST);
    gatewayPort = ConfigUtils.getInt(props, GATEWAY_PORT, DEFAULT_GATEWAY_PORT);
    gatewayPrivilegedPort =
        ConfigUtils.getInt(props, GATEWAY_PRIVILEGED_PORT, DEFAULT_GATEWAY_PRIVILEGED_PORT);
    isGatewayTlsEnabled =
        ConfigUtils.getBoolean(props, GATEWAY_TLS_ENABLED, DEFAULT_GATEWAY_TLS_ENABLED);
    gatewayTlsCaRootCert = ConfigUtils.getString(props, GATEWAY_TLS_CA_ROOT_CERT_PEM, null);
    if (gatewayTlsCaRootCert == null) {
      gatewayTlsCaRootCert =
          ConfigUtils.getStringFromFilePath(props, GATEWAY_TLS_CA_ROOT_CERT_PATH, null);
    }
    gatewayTlsOverrideAuthority =
        ConfigUtils.getString(props, GATEWAY_TLS_OVERRIDE_AUTHORITY, null);
    gatewayAuthorizationCredential =
        ConfigUtils.getString(props, GATEWAY_AUTHORIZATION_CREDENTIAL, null);
    gatewayTargetConfig = createGatewayTargetConfig();
  }

  private TargetConfig createGatewayTargetConfig() {
    return TargetConfig.newBuilder()
        .host(gatewayHost)
        .port(gatewayPort)
        .privilegedPort(gatewayPrivilegedPort)
        .tlsEnabled(isGatewayTlsEnabled)
        .tlsCaRootCert(gatewayTlsCaRootCert)
        .tlsOverrideAuthority(gatewayTlsOverrideAuthority)
        .authorizationCredential(gatewayAuthorizationCredential)
        .grpcClientConfig(grpcClientConfig)
        .build();
  }
}
