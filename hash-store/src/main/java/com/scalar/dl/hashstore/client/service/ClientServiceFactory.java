package com.scalar.dl.hashstore.client.service;

import com.google.common.annotations.VisibleForTesting;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.GatewayClientConfig;
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
    ClientService clientService = new ClientService(clientServiceFactory.create(config), config);
    if (autoRegistrationEnabled) {
      clientService.registerIdentity();
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
    ClientService clientService =
        new ClientService(clientServiceFactory.create(config), config.getClientConfig());
    if (autoRegistrationEnabled) {
      clientService.registerIdentity();
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
}
