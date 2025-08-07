package com.scalar.dl.tablestore.client.service;

import com.google.common.annotations.VisibleForTesting;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.GatewayClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.ledger.config.AuthenticationMethod;
import com.scalar.dl.ledger.service.StatusCode;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A factory class to instantiate {@link ClientService}. {@code ClientServiceFactory} creates a new
 * {@link ClientService} for each create method call but reuses objects such as clients and
 * connections as much as possible on the basis of a give configuration. So, {@code
 * ClientServiceFactory} object should always be reused. Please see the Javadoc of {@link
 * ClientService} for how to use this.
 */
@ThreadSafe
public class ClientServiceFactory {
  private final com.scalar.dl.client.service.ClientServiceFactory clientServiceFactory;

  public ClientServiceFactory() {
    clientServiceFactory = new com.scalar.dl.client.service.ClientServiceFactory();
  }

  @VisibleForTesting
  ClientServiceFactory(com.scalar.dl.client.service.ClientServiceFactory factory) {
    clientServiceFactory = factory;
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
    ClientService clientService = new ClientService(clientServiceFactory.create(config));
    if (autoRegistrationEnabled) {
      register(clientService, config);
    }
    return clientService;
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
    ClientService clientService = new ClientService(clientServiceFactory.create(config));
    if (autoRegistrationEnabled) {
      register(clientService, config.getClientConfig());
    }
    return clientService;
  }

  /**
   * Cleans up all the resources managed by the factory. This must be called after finishing up all
   * the interactions with the {@link ClientService}s that it creates.
   */
  public void close() {
    clientServiceFactory.close();
  }

  /**
   * Registers the predefined contracts for the table store client, in addition to the certificate
   * (for digital signature authentication) or the secret key (HMAC authentication) based on the
   * configuration in {@code ClientConfig}. If the certificate, secret, or contracts are already
   * registered, they are simply skipped without throwing an exception.
   *
   * @throws ClientException if a request fails for some reason other than 'already exists'
   */
  private void register(ClientService clientService, ClientConfig config) {
    try {
      if (config.getAuthenticationMethod().equals(AuthenticationMethod.DIGITAL_SIGNATURE)) {
        clientService.registerCertificate();
      } else {
        clientService.registerSecret();
      }
    } catch (ClientException e) {
      if (!e.getStatusCode().equals(StatusCode.CERTIFICATE_ALREADY_REGISTERED)
          && !e.getStatusCode().equals(StatusCode.SECRET_ALREADY_REGISTERED)) {
        throw e;
      }
    }

    clientService.registerContracts();
  }
}
