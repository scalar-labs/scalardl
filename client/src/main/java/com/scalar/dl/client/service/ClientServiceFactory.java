package com.scalar.dl.client.service;

import com.google.common.annotations.VisibleForTesting;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.ClientMode;
import com.scalar.dl.client.config.DigitalSignatureIdentityConfig;
import com.scalar.dl.client.config.GatewayClientConfig;
import com.scalar.dl.client.config.HmacIdentityConfig;
import com.scalar.dl.client.util.RequestSigner;
import com.scalar.dl.ledger.config.TargetConfig;
import com.scalar.dl.ledger.crypto.DigitalSignatureSigner;
import com.scalar.dl.ledger.crypto.HmacSigner;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A factory class to instantiate {@link ClientService}. {@code ClientServiceFactory} creates a new
 * {@link ClientService} for each create method call but reuses objects such as clients and
 * connections as much as possible on the basis of a give configuration. So, {@code
 * ClientServiceFactory} object should always be reused. Please see the javadoc of {@link
 * ClientService} for how to use this.
 */
@ThreadSafe
public class ClientServiceFactory {
  private ClientConfig config;
  private final Map<TargetConfig, AbstractLedgerClient> ledgerClients = new ConcurrentHashMap<>();
  private final Map<TargetConfig, AbstractAuditorClient> auditorClients = new ConcurrentHashMap<>();
  private final Map<TargetConfig, AbstractGatewayClient> gatewayClients = new ConcurrentHashMap<>();
  private final Map<DigitalSignatureIdentityConfig, RequestSigner> dsSigners =
      new ConcurrentHashMap<>();
  private final Map<HmacIdentityConfig, RequestSigner> hmacSigners = new ConcurrentHashMap<>();

  /**
   * @param config a client config.
   * @deprecated This method will be removed in release 5.0.0.
   */
  @Deprecated
  public ClientServiceFactory(ClientConfig config) {
    this.config = config;
  }

  public ClientServiceFactory() {}

  /**
   * Returns a {@link ClientService} instance.
   *
   * @return a {@link ClientService} instance
   * @deprecated This method will be removed in release 5.0.0.
   */
  @Deprecated
  public ClientService getClientService() {
    return create(config);
  }

  /**
   * Returns a {@link ClientService} instance.
   *
   * @param config a client config
   * @return a {@link ClientService} instance
   */
  public ClientService create(ClientConfig config) {
    return create(config, true);
  }

  /**
   * Returns a {@link ClientService} instance.
   *
   * @param config a client config
   * @param autoRegistrationEnabled a boolean flag whether it performs auto registration
   * @return a {@link ClientService} instance
   */
  public ClientService create(ClientConfig config, boolean autoRegistrationEnabled) {
    // LedgerClient is reused if the specified target is the same
    AbstractLedgerClient ledgerClient =
        ledgerClients.computeIfAbsent(config.getLedgerTargetConfig(), this::createLedgerClient);

    // AuditorClient is reused if the specified target is the same
    AbstractAuditorClient auditorClient = null;
    if (config.isAuditorEnabled()) {
      auditorClient =
          auditorClients.computeIfAbsent(
              config.getAuditorTargetConfig(), this::createAuditorClient);
    }
    ClientServiceHandler handler = new DefaultClientServiceHandler(ledgerClient, auditorClient);

    return createClientService(config, handler, autoRegistrationEnabled);
  }

  /**
   * Returns a {@link ClientService} instance.
   *
   * @param config a gateway client config
   * @return a {@link ClientService} instance
   */
  public ClientService create(GatewayClientConfig config) {
    return create(config, true);
  }

  /**
   * Returns a {@link ClientService} instance.
   *
   * @param config a gateway client config
   * @param autoRegistrationEnabled a boolean flag whether it performs auto registration
   * @return a {@link ClientService} instance
   */
  public ClientService create(GatewayClientConfig config, boolean autoRegistrationEnabled) {
    // GatewayClient is reused if the specified target is the same
    AbstractGatewayClient gatewayClient =
        gatewayClients.computeIfAbsent(config.getGatewayTargetConfig(), this::createGatewayClient);
    ClientServiceHandler handler = new GatewayClientServiceHandler(gatewayClient);

    return createClientService(config.getClientConfig(), handler, autoRegistrationEnabled);
  }

  /**
   * Returns a {@link GenericContractClientService} instance.
   *
   * @param config a client config
   * @return a {@link GenericContractClientService} instance
   */
  public GenericContractClientService createForGenericContract(ClientConfig config) {
    return createForGenericContract(config, true);
  }

  /**
   * Returns a {@link GenericContractClientService} instance.
   *
   * @param config a client config
   * @param autoRegistrationEnabled a boolean flag whether it performs auto registration
   * @return a {@link GenericContractClientService} instance
   */
  public GenericContractClientService createForGenericContract(
      ClientConfig config, boolean autoRegistrationEnabled) {
    return new GenericContractClientService(create(config, autoRegistrationEnabled));
  }

  /**
   * Returns a {@link GenericContractClientService} instance.
   *
   * @param config a gateway client config
   * @return a {@link GenericContractClientService} instance
   */
  public GenericContractClientService createForGenericContract(GatewayClientConfig config) {
    return createForGenericContract(config, true);
  }

  /**
   * Returns a {@link GenericContractClientService} instance.
   *
   * @param config a gateway client config
   * @param autoRegistrationEnabled a boolean flag whether it performs auto registration
   * @return a {@link GenericContractClientService} instance
   */
  public GenericContractClientService createForGenericContract(
      GatewayClientConfig config, boolean autoRegistrationEnabled) {
    return new GenericContractClientService(create(config, autoRegistrationEnabled));
  }

  /**
   * Cleans up all the resources managed by the factory. This must be called after finishing up all
   * the interactions with the {@link ClientService}s that it creates.
   */
  public void close() {
    ledgerClients.values().forEach(Client::shutdown);
    auditorClients.values().forEach(Client::shutdown);
    gatewayClients.values().forEach(Client::shutdown);
  }

  private ClientService createClientService(
      ClientConfig config, ClientServiceHandler handler, boolean autoRegistrationEnabled) {
    // RequestSigner is reused if the specified identity is the same
    RequestSigner signer = null;
    if (config.getDigitalSignatureIdentityConfig() != null) {
      signer =
          dsSigners.computeIfAbsent(
              config.getDigitalSignatureIdentityConfig(), this::createRequestSigner);
    } else if (config.getHmacIdentityConfig() != null) {
      signer =
          hmacSigners.computeIfAbsent(config.getHmacIdentityConfig(), this::createRequestSigner);
    } else {
      assert config.getClientMode().equals(ClientMode.INTERMEDIARY);
    }

    ClientService clientService = createClientService(config, handler, signer);
    if (autoRegistrationEnabled) {
      clientService.bootstrap();
    }

    return clientService;
  }

  @VisibleForTesting
  ClientService createClientService(
      ClientConfig config, ClientServiceHandler handler, RequestSigner signer) {
    return new ClientService(config, handler, signer);
  }

  @VisibleForTesting
  AbstractLedgerClient createLedgerClient(TargetConfig config) {
    return new LedgerClient(config);
  }

  @VisibleForTesting
  AbstractAuditorClient createAuditorClient(TargetConfig config) {
    return new AuditorClient(config);
  }

  @VisibleForTesting
  AbstractGatewayClient createGatewayClient(TargetConfig config) {
    return new GatewayClient(config);
  }

  @VisibleForTesting
  RequestSigner createRequestSigner(DigitalSignatureIdentityConfig config) {
    assert config.getPrivateKey() != null;
    return new RequestSigner(new DigitalSignatureSigner(config.getPrivateKey()));
  }

  @VisibleForTesting
  RequestSigner createRequestSigner(HmacIdentityConfig config) {
    assert config.getSecretKey() != null;
    return new RequestSigner(new HmacSigner(config.getSecretKey()));
  }
}
