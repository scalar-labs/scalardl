package com.scalar.dl.tablestore.client.service;

import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.GatewayClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.client.service.ClientServiceFactory;
import com.scalar.dl.ledger.config.AuthenticationMethod;
import com.scalar.dl.ledger.service.StatusCode;
import org.assertj.core.api.Assertions;
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
  public void create_ClientConfigGivenAndAutoRegistrationDisabled_ShouldCreateClientService() {
    // Arrange
    when(clientServiceFactory.create(any(ClientConfig.class))).thenReturn(clientService);

    // Act
    factory.create(config, false);

    // Assert
    verify(clientServiceFactory).create(config);
    verify(clientService, never()).registerCertificate();
    verify(clientService, never()).registerSecret();
  }

  @Test
  public void
      create_ClientConfigWithDigitalSignatureGivenAndAutoRegistrationEnabled_ShouldCreateClientServiceAndRegisterCertificate() {
    // Arrange
    when(config.getAuthenticationMethod()).thenReturn(AuthenticationMethod.DIGITAL_SIGNATURE);
    when(clientServiceFactory.create(any(ClientConfig.class))).thenReturn(clientService);

    // Act
    factory.create(config, true);

    // Assert
    verify(clientServiceFactory).create(config);
    verify(clientService).registerCertificate();
    verify(clientService, never()).registerSecret();
  }

  @Test
  public void
      create_ClientConfigWithHmacGivenAndAutoRegistrationEnabled_ShouldCreateClientServiceAndRegisterSecret() {
    // Arrange
    when(config.getAuthenticationMethod()).thenReturn(AuthenticationMethod.HMAC);
    when(clientServiceFactory.create(any(ClientConfig.class))).thenReturn(clientService);

    // Act
    factory.create(config, true);

    // Assert
    verify(clientServiceFactory).create(config);
    verify(clientService, never()).registerCertificate();
    verify(clientService).registerSecret();
  }

  @Test
  public void
      create_ClientConfigWithDigitalSignatureGivenAndAutoRegistrationNotSpecified_ShouldCreateClientServiceAndRegisterCertificate() {
    // Arrange
    when(config.getAuthenticationMethod()).thenReturn(AuthenticationMethod.DIGITAL_SIGNATURE);
    when(clientServiceFactory.create(any(ClientConfig.class))).thenReturn(clientService);

    // Act
    factory.create(config);

    // Assert
    verify(clientServiceFactory).create(config);
    verify(clientService).registerCertificate();
    verify(clientService, never()).registerSecret();
  }

  @Test
  public void
      create_ClientExceptionThrownDueToCertificateAlreadyRegistered_ShouldCreateClientService() {
    // Arrange
    when(config.getAuthenticationMethod()).thenReturn(AuthenticationMethod.DIGITAL_SIGNATURE);
    when(clientServiceFactory.create(any(ClientConfig.class))).thenReturn(clientService);
    ClientException exception =
        new ClientException("cert already registered", StatusCode.CERTIFICATE_ALREADY_REGISTERED);
    doThrow(exception).when(clientService).registerCertificate();

    // Act
    factory.create(config);

    // Assert
    verify(clientServiceFactory).create(config);
    verify(clientService).registerCertificate();
    verify(clientService, never()).registerSecret();
  }

  @Test
  public void create_ClientExceptionThrownDueToSecretAlreadyRegistered_ShouldCreateClientService() {
    // Arrange
    when(config.getAuthenticationMethod()).thenReturn(AuthenticationMethod.HMAC);
    when(clientServiceFactory.create(any(ClientConfig.class))).thenReturn(clientService);
    ClientException exception =
        new ClientException("secret already registered", StatusCode.SECRET_ALREADY_REGISTERED);
    doThrow(exception).when(clientService).registerSecret();

    // Act
    factory.create(config);

    // Assert
    verify(clientServiceFactory).create(config);
    verify(clientService, never()).registerCertificate();
    verify(clientService).registerSecret();
  }

  @Test
  public void create_ClientExceptionThrownDueToInvalidRequest_ShouldCreateClientService() {
    // Arrange
    when(config.getAuthenticationMethod()).thenReturn(AuthenticationMethod.DIGITAL_SIGNATURE);
    when(clientServiceFactory.create(any(ClientConfig.class))).thenReturn(clientService);
    ClientException exception = new ClientException("invalid request", StatusCode.INVALID_REQUEST);
    doThrow(exception).when(clientService).registerCertificate();

    // Act
    Throwable thrown = catchThrowable(() -> factory.create(config));

    // Assert
    Assertions.assertThat(thrown).isEqualTo(exception);
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
    verify(clientService, never()).registerCertificate();
    verify(clientService, never()).registerSecret();
  }

  @Test
  public void
      create_GatewayClientConfigWithDigitalSignatureGivenAndAutoRegistrationNotSpecified_ShouldCreateClientServiceAndRegisterCertificate() {
    // Arrange
    when(config.getAuthenticationMethod()).thenReturn(AuthenticationMethod.DIGITAL_SIGNATURE);
    when(gatewayClientConfig.getClientConfig()).thenReturn(config);
    when(clientServiceFactory.create(any(GatewayClientConfig.class))).thenReturn(clientService);

    // Act
    factory.create(gatewayClientConfig);

    // Assert
    verify(clientServiceFactory).create(gatewayClientConfig);
    verify(clientService).registerCertificate();
    verify(clientService, never()).registerSecret();
  }
}
