package com.scalar.dl.tablestore.client.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.GatewayClientConfig;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.client.service.ClientServiceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TableStoreClientServiceFactoryTest {
  @Mock private ClientConfig config;
  @Mock private GatewayClientConfig gatewayClientConfig;
  @Mock private ClientServiceFactory clientServiceFactory;
  @Mock private ClientService clientService;
  private TableStoreClientServiceFactory factory;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    factory = new TableStoreClientServiceFactory(clientServiceFactory);
  }

  @Test
  public void create_ClientConfigGivenAndAutoBootstrapDisabled_ShouldCreateClientService() {
    // Arrange
    when(clientServiceFactory.create(any(ClientConfig.class))).thenReturn(clientService);

    // Act
    factory.create(config, false);

    // Assert
    verify(clientServiceFactory).create(config);
    verify(clientService, never()).bootstrap();
  }

  @Test
  public void create_ClientConfigAndAutoBootstrapEnabled_ShouldCreateClientServiceWithBootstrap() {
    // Arrange
    when(clientServiceFactory.create(any(ClientConfig.class))).thenReturn(clientService);

    // Act
    factory.create(config, true);

    // Assert
    verify(clientServiceFactory).create(config);
    verify(clientService).bootstrap();
  }

  @Test
  public void create_ClientConfigWithAutoBootstrapEnabled_ShouldCreateClientServiceWithBootstrap() {
    // Arrange
    when(config.isAutoBootstrapEnabled()).thenReturn(true);
    when(clientServiceFactory.create(any(ClientConfig.class))).thenReturn(clientService);

    // Act
    factory.create(config);

    // Assert
    verify(clientServiceFactory).create(config);
    verify(clientService).bootstrap();
  }

  @Test
  public void
      create_ClientConfigWithAutoBootstrapDisabled_ShouldCreateClientServiceWithoutBootstrap() {
    // Arrange
    when(config.isAutoBootstrapEnabled()).thenReturn(false);
    when(clientServiceFactory.create(any(ClientConfig.class))).thenReturn(clientService);

    // Act
    factory.create(config);

    // Assert
    verify(clientServiceFactory).create(config);
    verify(clientService, never()).bootstrap();
  }

  @Test
  public void create_GatewayClientConfigGivenAndAutoBootstrapDisabled_ShouldCreateClientService() {
    // Arrange
    when(clientServiceFactory.create(any(GatewayClientConfig.class))).thenReturn(clientService);

    // Act
    factory.create(gatewayClientConfig, false);

    // Assert
    verify(clientServiceFactory).create(gatewayClientConfig);
    verify(clientService, never()).bootstrap();
  }

  @Test
  public void
      create_GatewayClientConfigAndAutoBootstrapEnabled_ShouldCreateClientServiceWithBootstrap() {
    // Arrange
    when(clientServiceFactory.create(any(GatewayClientConfig.class))).thenReturn(clientService);

    // Act
    factory.create(gatewayClientConfig, true);

    // Assert
    verify(clientServiceFactory).create(gatewayClientConfig);
    verify(clientService).bootstrap();
  }

  @Test
  public void
      create_GatewayClientConfigWithAutoBootstrapEnabled_ShouldCreateClientServiceWithBootstrap() {
    // Arrange
    when(gatewayClientConfig.getClientConfig()).thenReturn(config);
    when(config.isAutoBootstrapEnabled()).thenReturn(true);
    when(clientServiceFactory.create(any(GatewayClientConfig.class))).thenReturn(clientService);

    // Act
    factory.create(gatewayClientConfig);

    // Assert
    verify(clientServiceFactory).create(gatewayClientConfig);
    verify(clientService).bootstrap();
  }

  @Test
  public void
      create_GatewayClientConfigWithAutoBootstrapDisabled_ShouldCreateClientServiceWithoutBootstrap() {
    // Arrange
    when(gatewayClientConfig.getClientConfig()).thenReturn(config);
    when(config.isAutoBootstrapEnabled()).thenReturn(false);
    when(clientServiceFactory.create(any(GatewayClientConfig.class))).thenReturn(clientService);

    // Act
    factory.create(gatewayClientConfig);

    // Assert
    verify(clientServiceFactory).create(gatewayClientConfig);
    verify(clientService, never()).bootstrap();
  }
}
