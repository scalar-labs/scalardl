package com.scalar.dl.hashstore.client.service;

import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.GatewayClientConfig;
import com.scalar.dl.client.service.ClientServiceFactory;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A factory class to instantiate {@link HashStoreClientService}. {@code
 * HashStoreClientServiceFactory} creates a new {@link HashStoreClientService} for each create
 * method call but reuses objects such as clients and connections as much as possible on the basis
 * of a give configuration. So, {@code HashStoreClientServiceFactory} object should always be
 * reused. Please see the Javadoc of {@link HashStoreClientService} for how to use this.
 */
@ThreadSafe
public class HashStoreClientServiceFactory {
  private final ClientServiceFactory clientServiceFactory;

  public HashStoreClientServiceFactory() {
    clientServiceFactory = new ClientServiceFactory();
  }

  /**
   * Returns a {@link HashStoreClientService} instance.
   *
   * @param config a client config
   * @return a {@link HashStoreClientService} instance
   */
  public HashStoreClientService create(ClientConfig config) {
    return create(config, true);
  }

  /**
   * Returns a {@link HashStoreClientService} instance.
   *
   * @param config a client config
   * @param autoRegistrationEnabled a boolean flag whether it performs auto registration
   * @return a {@link HashStoreClientService} instance
   */
  public HashStoreClientService create(ClientConfig config, boolean autoRegistrationEnabled) {
    HashStoreClientService clientService =
        new HashStoreClientService(clientServiceFactory.create(config), config);
    if (autoRegistrationEnabled) {
      clientService.bootstrap();
    }
    return clientService;
  }

  /**
   * Returns a {@link HashStoreClientService} instance.
   *
   * @param config a gateway client config
   * @return a {@link HashStoreClientService} instance
   */
  public HashStoreClientService create(GatewayClientConfig config) {
    return create(config, true);
  }

  /**
   * Returns a {@link HashStoreClientService} instance.
   *
   * @param config a gateway client config
   * @param autoRegistrationEnabled a boolean flag whether it performs auto registration
   * @return a {@link HashStoreClientService} instance
   */
  public HashStoreClientService create(
      GatewayClientConfig config, boolean autoRegistrationEnabled) {
    HashStoreClientService clientService =
        new HashStoreClientService(clientServiceFactory.create(config), config.getClientConfig());
    if (autoRegistrationEnabled) {
      clientService.bootstrap();
    }
    return clientService;
  }

  /**
   * Cleans up all the resources managed by the factory. This must be called after finishing up all
   * the interactions with the {@link HashStoreClientService}s that it creates.
   */
  public void close() {
    clientServiceFactory.close();
  }
}
