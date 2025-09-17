package com.scalar.dl.hashstore.client.service;

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

public class HashStoreClientServiceFactoryTest {
  @Mock private ClientConfig config;
  @Mock private GatewayClientConfig gatewayClientConfig;
  @Mock private ClientServiceFactory clientServiceFactory;
  @Mock private ClientService clientService;
  private HashStoreClientServiceFactory factory;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    factory = new HashStoreClientServiceFactory(clientServiceFactory);
  }

  @Test
  public void create_ClientConfigGivenAndAutoRegistrationDisabled_ShouldCreateClientService() {
    // Arrange
    when(clientServiceFactory.create(any(ClientConfig.class))).thenReturn(clientService);

    // Act
    factory.create(config, false);

    // Assert
    verify(clientServiceFactory).create(config);
    verify(clientService, never()).bootstrap();
  }

  @Test
  public void
      create_ClientConfigAndAutoRegistrationEnabled_ShouldCreateClientServiceWithBootstrap() {
    // Arrange
    when(clientServiceFactory.create(any(ClientConfig.class))).thenReturn(clientService);

    // Act
    factory.create(config, true);

    // Assert
    verify(clientServiceFactory).create(config);
    verify(clientService).bootstrap();
  }

  @Test
  public void
      create_ClientConfigWithDigitalSignatureGivenAndAutoRegistrationNotSpecified_ShouldCreateClientServiceWithBootstrap() {
    // Arrange
    when(clientServiceFactory.create(any(ClientConfig.class))).thenReturn(clientService);

    // Act
    factory.create(config);

    // Assert
    verify(clientServiceFactory).create(config);
    verify(clientService).bootstrap();
  }

  @Test
  public void
      create_GatewayClientConfigGivenAndAutoRegistrationDisabled_ShouldCreateClientService() {
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
      create_GatewayClientConfigAndAutoRegistrationNotSpecified_ShouldCreateClientServiceWithBootstrap() {
    // Arrange
    when(gatewayClientConfig.getClientConfig()).thenReturn(config);
    when(clientServiceFactory.create(any(GatewayClientConfig.class))).thenReturn(clientService);

    // Act
    factory.create(gatewayClientConfig);

    // Assert
    verify(clientServiceFactory).create(gatewayClientConfig);
    verify(clientService).bootstrap();
  }
}
