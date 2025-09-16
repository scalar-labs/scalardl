package com.scalar.dl.tablestore.client.service;

import com.google.common.annotations.VisibleForTesting;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.GatewayClientConfig;
import com.scalar.dl.client.service.ClientServiceFactory;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A factory class to instantiate {@link TableStoreClientService}. {@code
 * TableStoreClientServiceFactory} creates a new {@link TableStoreClientService} for each create
 * method call but reuses objects such as clients and connections as much as possible on the basis
 * of a give configuration. So, {@code TableStoreClientServiceFactory} object should always be
 * reused. Please see the Javadoc of {@link TableStoreClientService} for how to use this.
 */
@ThreadSafe
public class TableStoreClientServiceFactory {
  private final ClientServiceFactory clientServiceFactory;

  public TableStoreClientServiceFactory() {
    clientServiceFactory = new ClientServiceFactory();
  }

  @VisibleForTesting
  TableStoreClientServiceFactory(ClientServiceFactory factory) {
    clientServiceFactory = factory;
  }

  /**
   * Returns a {@link TableStoreClientService} instance.
   *
   * @param config a client config
   * @return a {@link TableStoreClientService} instance
   */
  public TableStoreClientService create(ClientConfig config) {
    return create(config, true);
  }

  /**
   * Returns a {@link TableStoreClientService} instance.
   *
   * @param config a client config
   * @param autoRegistrationEnabled a boolean flag whether it performs auto registration
   * @return a {@link TableStoreClientService} instance
   */
  public TableStoreClientService create(ClientConfig config, boolean autoRegistrationEnabled) {
    TableStoreClientService clientService =
        new TableStoreClientService(clientServiceFactory.create(config), config);
    if (autoRegistrationEnabled) {
      clientService.bootstrap();
    }
    return clientService;
  }

  /**
   * Returns a {@link TableStoreClientService} instance.
   *
   * @param config a gateway client config
   * @return a {@link TableStoreClientService} instance
   */
  public TableStoreClientService create(GatewayClientConfig config) {
    return create(config, true);
  }

  /**
   * Returns a {@link TableStoreClientService} instance.
   *
   * @param config a gateway client config
   * @param autoRegistrationEnabled a boolean flag whether it performs auto registration
   * @return a {@link TableStoreClientService} instance
   */
  public TableStoreClientService create(
      GatewayClientConfig config, boolean autoRegistrationEnabled) {
    TableStoreClientService clientService =
        new TableStoreClientService(clientServiceFactory.create(config), config.getClientConfig());
    if (autoRegistrationEnabled) {
      clientService.bootstrap();
    }
    return clientService;
  }

  /**
   * Cleans up all the resources managed by the factory. This must be called after finishing up all
   * the interactions with the {@link TableStoreClientService}s that it creates.
   */
  public void close() {
    clientServiceFactory.close();
  }
}
