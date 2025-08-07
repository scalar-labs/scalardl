package com.scalar.dl.client.config;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.VisibleForTesting;
import com.scalar.dl.client.error.ClientError;
import com.scalar.dl.ledger.config.AuthenticationMethod;
import com.scalar.dl.ledger.config.ConfigUtils;
import com.scalar.dl.ledger.config.GrpcClientConfig;
import com.scalar.dl.ledger.config.TargetConfig;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
@SuppressFBWarnings("JCIP_FIELD_ISNT_FINAL_IN_IMMUTABLE_CLASS")
public class ClientConfig {
  @VisibleForTesting static final String DEFAULT_SERVER_HOST = "localhost";
  @VisibleForTesting static final int DEFAULT_SERVER_PORT = 50051;
  @VisibleForTesting static final int DEFAULT_SERVER_PRIVILEGED_PORT = 50052;
  @VisibleForTesting static final ClientMode DEFAULT_CLIENT_MODE = ClientMode.CLIENT;
  @VisibleForTesting static final int DEFAULT_CERT_VERSION = 1;
  @VisibleForTesting static final int DEFAULT_SECRET_KEY_VERSION = 1;
  @VisibleForTesting static final boolean DEFAULT_TLS_ENABLED = false;
  @VisibleForTesting static final long DEFAULT_DEADLINE_DURATION_MILLIS = 60000;
  @VisibleForTesting static final boolean DEFAULT_AUDITOR_ENABLED = false;
  @VisibleForTesting static final String DEFAULT_AUDITOR_HOST = "localhost";
  @VisibleForTesting static final int DEFAULT_AUDITOR_PORT = 40051;
  @VisibleForTesting static final int DEFAULT_AUDITOR_PRIVILEGED_PORT = 40052;
  @VisibleForTesting static final boolean DEFAULT_AUDITOR_TLS_ENABLED = false;

  @VisibleForTesting
  static final AuthenticationMethod DEFAULT_AUTHENTICATION_METHOD =
      AuthenticationMethod.DIGITAL_SIGNATURE;

  @VisibleForTesting
  static final String DEFAULT_AUDITOR_LINEARIZABLE_VALIDATION_CONTRACT_ID = "validate-ledger";

  private static final String PREFIX = "scalar.dl.client.";
  /**
   * <code>scalar.dl.client.server.host</code> (Optional)<br>
   * A hostname or an IP address of the server ("localhost" by default). It assumes that there is a
   * single endpoint that is given by DNS or a load balancer.
   */
  public static final String SERVER_HOST = PREFIX + "server.host";
  /**
   * <code>scalar.dl.client.server.port</code> (Optional)<br>
   * A port number of the server (50051 by default).
   */
  public static final String SERVER_PORT = PREFIX + "server.port";
  /**
   * <code>scalar.dl.client.server.privileged_port</code> (Optional)<br>
   * A port number of the server for privileged services (50052 by default).
   */
  public static final String SERVER_PRIVILEGED_PORT = PREFIX + "server.privileged_port";
  /**
   * <code>scalar.dl.client.cert_holder_id</code><br>
   * If both {@code scalar.dl.client.cert_holder_id} and {@code scalar.dl.client.entity_id} are
   * specified, {@code scalar.dl.client.entity_id} will be used.
   *
   * @deprecated This variable will be deleted in release 5.0.0. Use {@code
   *     scalar.dl.client.entity_id} instead.
   */
  @Deprecated public static final String CERT_HOLDER_ID = PREFIX + "cert_holder_id";
  /**
   * <code>scalar.dl.client.cert_version</code><br>
   *
   * @deprecated This variable will be deleted in release 5.0.0. Use {@code
   *     scalar.dl.client.entity.identity.digital_signature.cert_version} instead.
   */
  @Deprecated public static final String CERT_VERSION = PREFIX + "cert_version";
  /**
   * <code>scalar.dl.client.cert_path</code><br>
   *
   * @deprecated This variable will be deleted in release 5.0.0. Use {@code
   *     scalar.dl.client.entity.identity.digital_signature.cert_path} instead.
   */
  @Deprecated public static final String CERT_PATH = PREFIX + "cert_path";
  /**
   * <code>scalar.dl.client.cert_pem</code><br>
   *
   * @deprecated This variable will be deleted in release 5.0.0. Use {@code
   *     scalar.dl.client.entity.identity.digital_signature.cert_pem} instead.
   */
  @Deprecated public static final String CERT_PEM = PREFIX + "cert_pem";
  /**
   * <code>scalar.dl.client.private_key_path</code><br>
   *
   * @deprecated This variable will be deleted in release 5.0.0. Use {@code
   *     scalar.dl.client.entity.identity.digital_signature.private_key_path} instead.
   */
  @Deprecated public static final String PRIVATE_KEY_PATH = PREFIX + "private_key_path";
  /**
   * <code>scalar.dl.client.private_key_pem</code><br>
   *
   * @deprecated This variable will be deleted in release 5.0.0. Use {@code
   *     scalar.dl.client.entity.identity.digital_signature.private_key_pem} instead.
   */
  @Deprecated public static final String PRIVATE_KEY_PEM = PREFIX + "private_key_pem";
  /**
   * <code>scalar.dl.client.entity.id</code><br>
   * A unique ID of a requester (e.g., a user or a device).
   */
  public static final String ENTITY_ID = PREFIX + "entity.id";

  private static final String HMAC_IDENTITY_PREFIX = PREFIX + "entity.identity.hmac.";
  /**
   * <code>scalar.dl.client.entity.identity.hmac.secret_key</code><br>
   * A secret key for HMAC, which is required if HMAC is used for authentication.
   */
  public static final String HMAC_SECRET_KEY = HMAC_IDENTITY_PREFIX + "secret_key";
  /**
   * <code>scalar.dl.client.entity.identity.hmac.secret_key_version</code> (Optional)<br>
   * The version of the HMAC key. 1 by default.
   */
  public static final String HMAC_SECRET_KEY_VERSION = HMAC_IDENTITY_PREFIX + "secret_key_version";

  private static final String DS_IDENTITY_PREFIX = PREFIX + "entity.identity.digital_signature.";
  /**
   * <code>scalar.dl.client.entity.identity.digital_signature.cert_path</code><br>
   * PEM-encoded certificate data, which is required if {@code
   * scalar.dl.client.entity.identity.digital_signature.cert_pem} is empty.
   */
  public static final String DS_CERT_PATH = DS_IDENTITY_PREFIX + "cert_path";
  /**
   * <code>scalar.dl.client.entity.identity.digital_signature.cert_pem</code><br>
   * The path of a certificate file in PEM format, which is required if {@code
   * scalar.dl.client.entity.identity.digital_signature.cert_path} is empty.
   */
  public static final String DS_CERT_PEM = DS_IDENTITY_PREFIX + "cert_pem";
  /**
   * <code>scalar.dl.client.entity.identity.digital_signature.cert_version</code> (Optional)<br>
   * The version of the certificate 1 by default.
   */
  public static final String DS_CERT_VERSION = DS_IDENTITY_PREFIX + "cert_version";
  /**
   * <code>scalar.dl.client.entity.identity.digital_signature.private_key_path</code><br>
   * The path of a private key file in PEM format, which corresponds to the specified certificate.
   * Required if {@code scalar.dl.client.entity.identity.digital_signature.private_key_pem} is
   * empty.
   */
  public static final String DS_PRIVATE_KEY_PATH = DS_IDENTITY_PREFIX + "private_key_path";
  /**
   * <code>scalar.dl.client.entity.identity.digital_signature.private_key_pem</code><br>
   * PEM-encoded private key data. Required if {@code
   * scalar.dl.client.entity.identity.digital_signature.private_key_path} is empty.
   */
  public static final String DS_PRIVATE_KEY_PEM = DS_IDENTITY_PREFIX + "private_key_pem";
  /**
   * <code>scalar.dl.client.authentication_method</code> (Optional)<br>
   * The authentication method for clients and Ledger/Auditor servers. {@code "digital-signature"}
   * (default) or {@code "hmac"} can be specified. This must be consistent with the Ledger/Auditor
   * configuration.
   *
   * @deprecated This variable will be deleted in release 5.0.0. Use {@code
   *     scalar.dl.client.authentication.method} instead.
   */
  public static final String DEPRECATED_AUTHENTICATION_METHOD = PREFIX + "authentication_method";
  /**
   * <code>scalar.dl.client.authentication.method</code> (Optional)<br>
   * The authentication method for clients and Ledger/Auditor servers. {@code "digital-signature"}
   * (default) or {@code "hmac"} can be specified. This must be consistent with the Ledger/Auditor
   * configuration.
   */
  public static final String AUTHENTICATION_METHOD = PREFIX + "authentication.method";
  /**
   * <code>scalar.dl.client.tls.enabled</code> (Optional)<br>
   * A flag to enable TLS communication for Ledger (false by default).
   */
  public static final String TLS_ENABLED = PREFIX + "tls.enabled";
  /**
   * <code>scalar.dl.client.tls.ca_root_cert_path</code> (Optional)<br>
   * A custom CA root certificate (file path) for TLS communication for Ledger. If the issuing
   * certificate authority is known to the client, it can be empty.
   */
  public static final String TLS_CA_ROOT_CERT_PATH = PREFIX + "tls.ca_root_cert_path";
  /**
   * <code>scalar.dl.client.tls.ca_root_cert_pem</code> (Optional)<br>
   * A custom CA root certificate (PEM data) for TLS communication for Ledger. If the issuing
   * certificate authority is known to the client, it can be empty.
   */
  public static final String TLS_CA_ROOT_CERT_PEM = PREFIX + "tls.ca_root_cert_pem";
  /**
   * <code>scalar.dl.client.tls.override_authority</code> (Optional)<br>
   * A custom authority for TLS communication for Ledger. This doesn't change what the host is
   * actually connected to. This is intended for testing, but may safely be used outside of tests as
   * an alternative to DNS overrides. For example, you can specify the hostname presented in the
   * certificate chain file that you set for `scalar.dl.ledger.server.tls.cert_chain_path`.
   */
  public static final String TLS_OVERRIDE_AUTHORITY = PREFIX + "tls.override_authority";
  /**
   * <code>scalar.dl.client.authorization.credential</code> (Optional)<br>
   * An authorization credential for Ledger. (e.g. authorization: Bearer token) If this is given,
   * clients will add "authorization: [credential]" http/2 header.
   */
  public static final String AUTHORIZATION_CREDENTIAL = PREFIX + "authorization.credential";
  /**
   * <code>scalar.dl.client.grpc.deadline_duration_millis</code> (Optional)<br>
   * A deadline that is after the given duration from now for each request.
   */
  public static final String GRPC_DEADLINE_DURATION_MILLIS =
      PREFIX + "grpc.deadline_duration_millis";
  /**
   * <code>scalar.dl.client.grpc.max_inbound_message_size</code> (Optional)<br>
   * The maximum message size allowed for a single gRPC frame. If an inbound message larger than
   * this limit is received, it will not be processed, and the RPC will fail with
   * RESOURCE_EXHAUSTED.
   */
  public static final String GRPC_MAX_INBOUND_MESSAGE_SIZE =
      PREFIX + "grpc.max_inbound_message_size";
  /**
   * <code>scalar.dl.client.grpc.max_inbound_metadata_size</code> (Optional)<br>
   * The maximum size of metadata allowed to be received. This is cumulative size of the entries
   * with some overhead, as defined for HTTP/2's SETTINGS_MAX_HEADER_LIST_SIZE. The default is 8
   * KiB.
   */
  public static final String GRPC_MAX_INBOUND_METADATA_SIZE =
      PREFIX + "grpc.max_inbound_metadata_size";
  /**
   * <code>scalar.dl.client.mode</code> (Optional)<br>
   * A client mode. CLIENT OR INTERMEDIARY. CLIENT by default. In INTERMEDIARY mode, this client
   * receives a signed serialized request from another client, and sends it to a server.
   */
  public static final String MODE = PREFIX + "mode";
  /** Optional. A flag to enable auditor (false by default). */
  public static final String AUDITOR_ENABLED = PREFIX + "auditor.enabled";
  /**
   * <code>scalar.dl.client.auditor.host</code> (Optional)<br>
   * A hostname or an IP address of the auditor ("localhost" by default). It assumes that there is a
   * single endpoint that is given by DNS or a load balancer.
   */
  public static final String AUDITOR_HOST = PREFIX + "auditor.host";
  /**
   * <code>scalar.dl.client.auditor.port</code> (Optional)<br>
   * A hostname or an IP address of the auditor ("localhost" by default). It assumes that Optional.
   * A port number of the auditor (40051 by default).
   */
  public static final String AUDITOR_PORT = PREFIX + "auditor.port";
  /**
   * <code>scalar.dl.client.auditor.privileged_port</code> (Optional)<br>
   * A port number of the auditor for privileged services (40052 by default).
   */
  public static final String AUDITOR_PRIVILEGED_PORT = PREFIX + "auditor.privileged_port";
  /**
   * <code>scalar.dl.client.auditor.tls.enabled</code> (Optional)<br>
   * A flag to enable TLS communication for Auditor (false by default).
   */
  public static final String AUDITOR_TLS_ENABLED = PREFIX + "auditor.tls.enabled";
  /**
   * <code>scalar.dl.client.auditor.tls.ca_root_cert_path</code> (Optional)<br>
   * A custom CA root certificate (file path) for TLS communication for Auditor. If the issuing
   * certificate authority is known to the client, it can be empty.
   */
  public static final String AUDITOR_TLS_CA_ROOT_CERT_PATH =
      PREFIX + "auditor.tls.ca_root_cert_path";
  /**
   * <code>scalar.dl.client.auditor.tls.ca_root_cert_pem</code> (Optional)<br>
   * A custom CA root certificate (PEM data) for TLS communication for Auditor. If the issuing
   * certificate authority is known to the client, it can be empty.
   */
  public static final String AUDITOR_TLS_CA_ROOT_CERT_PEM = PREFIX + "auditor.tls.ca_root_cert_pem";
  /**
   * <code>scalar.dl.client.auditor.tls.override_authority</code> (Optional)<br>
   * A custom authority for TLS communication for Auditor. This doesn't change what the host is
   * actually connected to. This is intended for testing, but may safely be used outside of tests as
   * an alternative to DNS overrides. For example, you can specify the hostname presented in the
   * certificate chain file that you set for `scalar.dl.auditor.server.tls.cert_chain_path`.
   */
  public static final String AUDITOR_TLS_OVERRIDE_AUTHORITY =
      PREFIX + "auditor.tls.override_authority";
  /**
   * <code>scalar.dl.client.auditor.authorization.credential</code> (Optional)<br>
   * An authorization credential for Auditor. (e.g. authorization: Bearer token) If this is given,
   * clients will add "authorization: [credential]" http/2 header.
   */
  public static final String AUDITOR_AUTHORIZATION_CREDENTIAL =
      PREFIX + "auditor.authorization.credential";
  /**
   * <code>scalar.dl.client.auditor.linearizable_validation.contract_id</code> (Optional)<br>
   * The ID of ValidateLedger contract ("validate-ledger" by default). It is used for the
   * linearizable validation.
   */
  public static final String AUDITOR_LINEARIZABLE_VALIDATION_CONTRACT_ID =
      PREFIX + "auditor.linearizable_validation.contract_id";

  private final Properties props;
  private String serverHost;
  private int serverPort;
  private int serverPrivilegedPort;
  private String entityId;
  private int certVersion;
  private String cert;
  private String privateKey;
  private int secretKeyVersion;
  private String secretKey;
  private AuthenticationMethod authenticationMethod;
  private boolean isTlsEnabled;
  private String tlsCaRootCert;
  private String tlsOverrideAuthority;
  private String authorizationCredential;
  private GrpcClientConfig grpcClientConfig;
  private ClientMode clientMode;
  private boolean isAuditorEnabled;
  private String auditorHost;
  private int auditorPort;
  private int auditorPrivilegedPort;
  private boolean isAuditorTlsEnabled;
  private String auditorTlsCaRootCert;
  private String auditorTlsOverrideAuthority;
  private String auditorAuthorizationCredential;
  private String auditorLinearizableValidationContractId;
  private DigitalSignatureIdentityConfig digitalSignatureIdentityConfig;
  private HmacIdentityConfig hmacIdentityConfig;
  private TargetConfig ledgerTargetConfig;
  private TargetConfig auditorTargetConfig;

  public ClientConfig(File propertiesFile) throws IOException {
    try (FileInputStream stream = new FileInputStream(propertiesFile)) {
      props = new Properties();
      props.load(stream);
    }
    load();
  }

  public ClientConfig(InputStream stream) throws IOException {
    props = new Properties();
    props.load(stream);
    load();
  }

  public ClientConfig(Properties properties) throws IOException {
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

  public AuthenticationMethod getAuthenticationMethod() {
    return authenticationMethod;
  }

  @Nullable
  public DigitalSignatureIdentityConfig getDigitalSignatureIdentityConfig() {
    return digitalSignatureIdentityConfig;
  }

  @Nullable
  public HmacIdentityConfig getHmacIdentityConfig() {
    return hmacIdentityConfig;
  }

  @Nonnull
  public TargetConfig getLedgerTargetConfig() {
    return ledgerTargetConfig;
  }

  @Nullable
  public TargetConfig getAuditorTargetConfig() {
    return auditorTargetConfig;
  }

  public ClientMode getClientMode() {
    return clientMode;
  }

  public boolean isAuditorEnabled() {
    return isAuditorEnabled;
  }

  public String getAuditorLinearizableValidationContractId() {
    return auditorLinearizableValidationContractId;
  }

  public GrpcClientConfig getGrpcClientConfig() {
    return grpcClientConfig;
  }

  private void load() {
    serverHost = ConfigUtils.getString(props, SERVER_HOST, DEFAULT_SERVER_HOST);
    serverPort = ConfigUtils.getInt(props, SERVER_PORT, DEFAULT_SERVER_PORT);
    serverPrivilegedPort =
        ConfigUtils.getInt(props, SERVER_PRIVILEGED_PORT, DEFAULT_SERVER_PRIVILEGED_PORT);
    clientMode =
        ClientMode.valueOf(
            ConfigUtils.getString(props, MODE, DEFAULT_CLIENT_MODE.toString()).toUpperCase());
    if (clientMode.equals(ClientMode.CLIENT)) {
      entityId = ConfigUtils.getString(props, ENTITY_ID, null);
      if (entityId == null) {
        entityId = ConfigUtils.getString(props, CERT_HOLDER_ID, null);
      }
      checkArgument(
          entityId != null,
          ClientError.CONFIG_ENTITY_ID_OR_CERT_HOLDER_ID_REQUIRED.buildMessage(
              ENTITY_ID, CERT_HOLDER_ID));

      // identity based on digital signature
      certVersion = ConfigUtils.getInt(props, CERT_VERSION, DEFAULT_CERT_VERSION);
      certVersion = ConfigUtils.getInt(props, DS_CERT_VERSION, certVersion);
      cert = ConfigUtils.getString(props, DS_CERT_PEM, null);
      if (cert == null) {
        cert = ConfigUtils.getStringFromFilePath(props, DS_CERT_PATH, null);
      }
      if (cert == null) {
        cert = ConfigUtils.getString(props, CERT_PEM, null);
      }
      if (cert == null) {
        cert = ConfigUtils.getStringFromFilePath(props, CERT_PATH, null);
      }
      privateKey = ConfigUtils.getString(props, DS_PRIVATE_KEY_PEM, null);
      if (privateKey == null) {
        privateKey = ConfigUtils.getStringFromFilePath(props, DS_PRIVATE_KEY_PATH, null);
      }
      if (privateKey == null) {
        privateKey = ConfigUtils.getString(props, PRIVATE_KEY_PEM, null);
      }
      if (privateKey == null) {
        privateKey = ConfigUtils.getStringFromFilePath(props, PRIVATE_KEY_PATH, null);
      }

      // identity based on HMAC
      secretKeyVersion =
          ConfigUtils.getInt(props, HMAC_SECRET_KEY_VERSION, DEFAULT_SECRET_KEY_VERSION);
      secretKey = ConfigUtils.getString(props, HMAC_SECRET_KEY, null);
      authenticationMethod = getAuthenticationMethod(DEFAULT_AUTHENTICATION_METHOD);

      // validate and create identity config
      validateAuthentication();
      if (authenticationMethod == AuthenticationMethod.DIGITAL_SIGNATURE) {
        digitalSignatureIdentityConfig = createDigitalSignatureIdentityConfig();
      } else if (authenticationMethod == AuthenticationMethod.HMAC) {
        hmacIdentityConfig = createHmacIdentityConfig();
      } else {
        throw new IllegalArgumentException(
            ClientError.CONFIG_INVALID_AUTHENTICATION_METHOD_FOR_CLIENT_MODE.buildMessage());
      }
    } else {
      // for intermediary mode
      authenticationMethod = getAuthenticationMethod(AuthenticationMethod.PASS_THROUGH);
      if (authenticationMethod != AuthenticationMethod.PASS_THROUGH) {
        throw new IllegalArgumentException(
            ClientError.CONFIG_INVALID_AUTHENTICATION_METHOD_FOR_INTERMEDIARY_MODE.buildMessage());
      }
    }
    isTlsEnabled = ConfigUtils.getBoolean(props, TLS_ENABLED, DEFAULT_TLS_ENABLED);
    tlsCaRootCert = ConfigUtils.getString(props, TLS_CA_ROOT_CERT_PEM, null);
    if (tlsCaRootCert == null) {
      tlsCaRootCert = ConfigUtils.getStringFromFilePath(props, TLS_CA_ROOT_CERT_PATH, null);
    }
    tlsOverrideAuthority = ConfigUtils.getString(props, TLS_OVERRIDE_AUTHORITY, null);
    authorizationCredential = ConfigUtils.getString(props, AUTHORIZATION_CREDENTIAL, null);
    grpcClientConfig =
        GrpcClientConfig.newBuilder()
            .deadlineDurationMillis(
                ConfigUtils.getLong(
                    props, GRPC_DEADLINE_DURATION_MILLIS, DEFAULT_DEADLINE_DURATION_MILLIS))
            .maxInboundMessageSize(ConfigUtils.getInt(props, GRPC_MAX_INBOUND_MESSAGE_SIZE, 0))
            .maxInboundMetadataSize(ConfigUtils.getInt(props, GRPC_MAX_INBOUND_METADATA_SIZE, 0))
            .build();
    isAuditorEnabled = ConfigUtils.getBoolean(props, AUDITOR_ENABLED, DEFAULT_AUDITOR_ENABLED);
    if (isAuditorEnabled) {
      auditorHost = ConfigUtils.getString(props, AUDITOR_HOST, DEFAULT_AUDITOR_HOST);
      auditorPort = ConfigUtils.getInt(props, AUDITOR_PORT, DEFAULT_AUDITOR_PORT);
      auditorPrivilegedPort =
          ConfigUtils.getInt(props, AUDITOR_PRIVILEGED_PORT, DEFAULT_AUDITOR_PRIVILEGED_PORT);
      isAuditorTlsEnabled =
          ConfigUtils.getBoolean(props, AUDITOR_TLS_ENABLED, DEFAULT_AUDITOR_TLS_ENABLED);
      auditorTlsCaRootCert = ConfigUtils.getString(props, AUDITOR_TLS_CA_ROOT_CERT_PEM, null);
      if (auditorTlsCaRootCert == null) {
        auditorTlsCaRootCert =
            ConfigUtils.getStringFromFilePath(props, AUDITOR_TLS_CA_ROOT_CERT_PATH, null);
      }
      auditorTlsOverrideAuthority =
          ConfigUtils.getString(props, AUDITOR_TLS_OVERRIDE_AUTHORITY, null);
      auditorAuthorizationCredential =
          ConfigUtils.getString(props, AUDITOR_AUTHORIZATION_CREDENTIAL, null);
      auditorLinearizableValidationContractId =
          ConfigUtils.getString(
              props,
              AUDITOR_LINEARIZABLE_VALIDATION_CONTRACT_ID,
              DEFAULT_AUDITOR_LINEARIZABLE_VALIDATION_CONTRACT_ID);
    }

    ledgerTargetConfig = createLedgerTargetConfig();
    auditorTargetConfig = createAuditorTargetConfig();
  }

  private AuthenticationMethod getAuthenticationMethod(AuthenticationMethod defaultMethod) {
    String authenticationMethod =
        ConfigUtils.getString(props, DEPRECATED_AUTHENTICATION_METHOD, defaultMethod.getMethod());
    return AuthenticationMethod.get(
        Objects.requireNonNull(
                ConfigUtils.getString(props, AUTHENTICATION_METHOD, authenticationMethod))
            .toLowerCase());
  }

  private void validateAuthentication() {
    if (authenticationMethod == AuthenticationMethod.DIGITAL_SIGNATURE
        && (cert == null || privateKey == null)) {
      throw new IllegalArgumentException(
          ClientError.CONFIG_CERT_AND_KEY_REQUIRED_FOR_DIGITAL_SIGNATURE.buildMessage());
    }
    if (authenticationMethod == AuthenticationMethod.HMAC && secretKey == null) {
      throw new IllegalArgumentException(
          ClientError.CONFIG_SECRET_KEY_REQUIRED_FOR_HMAC.buildMessage());
    }
  }

  @Nonnull
  private DigitalSignatureIdentityConfig createDigitalSignatureIdentityConfig() {
    return DigitalSignatureIdentityConfig.newBuilder()
        .entityId(entityId)
        .certVersion(certVersion)
        .cert(cert)
        .privateKey(privateKey)
        .build();
  }

  @Nonnull
  private HmacIdentityConfig createHmacIdentityConfig() {
    return HmacIdentityConfig.newBuilder()
        .entityId(entityId)
        .secretKeyVersion(secretKeyVersion)
        .secretKey(secretKey)
        .build();
  }

  @Nonnull
  private TargetConfig createLedgerTargetConfig() {
    return TargetConfig.newBuilder()
        .host(serverHost)
        .port(serverPort)
        .privilegedPort(serverPrivilegedPort)
        .tlsEnabled(isTlsEnabled)
        .tlsCaRootCert(tlsCaRootCert)
        .tlsOverrideAuthority(tlsOverrideAuthority)
        .authorizationCredential(authorizationCredential)
        .grpcClientConfig(grpcClientConfig)
        .build();
  }

  @Nullable
  private TargetConfig createAuditorTargetConfig() {
    if (!isAuditorEnabled) {
      return null;
    }
    return TargetConfig.newBuilder()
        .host(auditorHost)
        .port(auditorPort)
        .privilegedPort(auditorPrivilegedPort)
        .tlsEnabled(isAuditorTlsEnabled)
        .tlsCaRootCert(auditorTlsCaRootCert)
        .tlsOverrideAuthority(auditorTlsOverrideAuthority)
        .authorizationCredential(auditorAuthorizationCredential)
        .grpcClientConfig(grpcClientConfig)
        .build();
  }
}
