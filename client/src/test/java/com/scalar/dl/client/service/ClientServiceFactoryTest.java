package com.scalar.dl.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.config.ClientMode;
import com.scalar.dl.client.config.DigitalSignatureIdentityConfig;
import com.scalar.dl.client.config.GatewayClientConfig;
import com.scalar.dl.client.config.HmacIdentityConfig;
import com.scalar.dl.client.util.RequestSigner;
import com.scalar.dl.ledger.config.TargetConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ClientServiceFactoryTest {
  @Mock private ClientConfig config;
  @Mock private GatewayClientConfig gatewayClientConfig;
  @Mock private DigitalSignatureIdentityConfig digitalSignatureIdentityConfig;
  @Mock private LedgerClient ledgerClient;
  @Mock private AuditorClient auditorClient;
  @Mock private GatewayClient gatewayClient;
  @Mock private RequestSigner requestSigner;
  @Mock private ClientService clientService;
  private ClientServiceFactory factory;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    factory = spy(new ClientServiceFactory());
  }

  @Test
  public void
      create_ClientConfigWithDigitalSignatureIdentityOnlyForLedgerGiven_ShouldCreateClientService() {
    // Arrange
    TargetConfig ledgerTargetConfig = mock(TargetConfig.class);
    DigitalSignatureIdentityConfig digitalSignatureIdentityConfig =
        mock(DigitalSignatureIdentityConfig.class);
    when(config.getLedgerTargetConfig()).thenReturn(ledgerTargetConfig);
    when(config.getDigitalSignatureIdentityConfig()).thenReturn(digitalSignatureIdentityConfig);
    when(config.isAuditorEnabled()).thenReturn(false);
    when(config.isAutoBootstrapEnabled()).thenReturn(false);
    doReturn(ledgerClient).when(factory).createLedgerClient(any());
    doReturn(requestSigner)
        .when(factory)
        .createRequestSigner(any(DigitalSignatureIdentityConfig.class));

    // Act
    ClientService service = factory.create(config);

    // Assert
    assertThat(service.getClientServiceHandler()).isInstanceOf(DefaultClientServiceHandler.class);
    assertThat(((DefaultClientServiceHandler) service.getClientServiceHandler()).getLedgerClient())
        .isEqualTo(ledgerClient);
    assertThat(((DefaultClientServiceHandler) service.getClientServiceHandler()).getAuditorClient())
        .isNull();
    assertThat(service.getRequestSigner()).isEqualTo(requestSigner);
    verify(factory).createLedgerClient(ledgerTargetConfig);
    verify(factory).createRequestSigner(digitalSignatureIdentityConfig);
    verify(factory, never()).createRequestSigner(any(HmacIdentityConfig.class));
  }

  @Test
  public void create_ClientConfigWithHmacIdentityOnlyForLedgerGiven_ShouldCreateClientService() {
    // Arrange
    TargetConfig ledgerTargetConfig = mock(TargetConfig.class);
    HmacIdentityConfig hmacIdentityConfig = mock(HmacIdentityConfig.class);
    when(config.getLedgerTargetConfig()).thenReturn(ledgerTargetConfig);
    when(config.getDigitalSignatureIdentityConfig()).thenReturn(null);
    when(config.getHmacIdentityConfig()).thenReturn(hmacIdentityConfig);
    when(config.isAuditorEnabled()).thenReturn(false);
    when(config.isAutoBootstrapEnabled()).thenReturn(false);
    doReturn(ledgerClient).when(factory).createLedgerClient(any());
    doReturn(requestSigner).when(factory).createRequestSigner(any(HmacIdentityConfig.class));

    // Act
    ClientService service = factory.create(config);

    // Assert
    assertThat(service.getClientServiceHandler()).isInstanceOf(DefaultClientServiceHandler.class);
    assertThat(((DefaultClientServiceHandler) service.getClientServiceHandler()).getLedgerClient())
        .isEqualTo(ledgerClient);
    assertThat(((DefaultClientServiceHandler) service.getClientServiceHandler()).getAuditorClient())
        .isNull();
    assertThat(service.getRequestSigner()).isEqualTo(requestSigner);
    verify(factory).createLedgerClient(ledgerTargetConfig);
    verify(factory).createRequestSigner(hmacIdentityConfig);
    verify(factory, never()).createRequestSigner(any(DigitalSignatureIdentityConfig.class));
  }

  @Test
  public void create_ClientConfigBothForLedgerAndAuditorGiven_ShouldCreateClientService() {
    // Arrange
    TargetConfig ledgerTargetConfig = mock(TargetConfig.class);
    TargetConfig auditorTargetConfig = mock(TargetConfig.class);
    DigitalSignatureIdentityConfig digitalSignatureIdentityConfig =
        mock(DigitalSignatureIdentityConfig.class);
    when(config.getLedgerTargetConfig()).thenReturn(ledgerTargetConfig);
    when(config.getAuditorTargetConfig()).thenReturn(auditorTargetConfig);
    when(config.getDigitalSignatureIdentityConfig()).thenReturn(digitalSignatureIdentityConfig);
    when(config.isAuditorEnabled()).thenReturn(true);
    when(config.isAutoBootstrapEnabled()).thenReturn(false);
    doReturn(ledgerClient).when(factory).createLedgerClient(any());
    doReturn(auditorClient).when(factory).createAuditorClient(any());
    doReturn(requestSigner)
        .when(factory)
        .createRequestSigner(any(DigitalSignatureIdentityConfig.class));

    // Act
    ClientService service = factory.create(config);

    // Assert
    assertThat(service.getClientServiceHandler()).isInstanceOf(DefaultClientServiceHandler.class);
    assertThat(((DefaultClientServiceHandler) service.getClientServiceHandler()).getLedgerClient())
        .isEqualTo(ledgerClient);
    assertThat(((DefaultClientServiceHandler) service.getClientServiceHandler()).getAuditorClient())
        .isEqualTo(auditorClient);
    assertThat(service.getRequestSigner()).isEqualTo(requestSigner);
    verify(factory).createLedgerClient(ledgerTargetConfig);
    verify(factory).createAuditorClient(auditorTargetConfig);
    verify(factory).createRequestSigner(digitalSignatureIdentityConfig);
  }

  @Test
  public void
      create_ClientConfigWithTheSameInternalConfigGiven_ShouldCreateClientServicesWithSameInternalInstances() {
    // Arrange
    TargetConfig ledgerTargetConfig = mock(TargetConfig.class);
    TargetConfig auditorTargetConfig = mock(TargetConfig.class);
    DigitalSignatureIdentityConfig digitalSignatureIdentityConfig =
        mock(DigitalSignatureIdentityConfig.class);
    when(config.getLedgerTargetConfig()).thenReturn(ledgerTargetConfig);
    when(config.getAuditorTargetConfig()).thenReturn(auditorTargetConfig);
    when(config.getDigitalSignatureIdentityConfig()).thenReturn(digitalSignatureIdentityConfig);
    when(config.isAuditorEnabled()).thenReturn(true);
    when(config.isAutoBootstrapEnabled()).thenReturn(false);
    doReturn(ledgerClient).when(factory).createLedgerClient(any());
    doReturn(auditorClient).when(factory).createAuditorClient(any());
    doReturn(requestSigner)
        .when(factory)
        .createRequestSigner(any(DigitalSignatureIdentityConfig.class));

    // Act
    ClientService service1 = factory.create(config);
    ClientService service2 = factory.create(config);

    // Assert
    assertThat(service1.getClientServiceHandler()).isInstanceOf(DefaultClientServiceHandler.class);
    assertThat(((DefaultClientServiceHandler) service1.getClientServiceHandler()).getLedgerClient())
        .isEqualTo(ledgerClient);
    assertThat(
            ((DefaultClientServiceHandler) service1.getClientServiceHandler()).getAuditorClient())
        .isEqualTo(auditorClient);
    assertThat(service1.getRequestSigner()).isEqualTo(requestSigner);
    assertThat(service2.getClientServiceHandler()).isInstanceOf(DefaultClientServiceHandler.class);
    assertThat(((DefaultClientServiceHandler) service2.getClientServiceHandler()).getLedgerClient())
        .isEqualTo(ledgerClient);
    assertThat(
            ((DefaultClientServiceHandler) service2.getClientServiceHandler()).getAuditorClient())
        .isEqualTo(auditorClient);
    assertThat(service2.getRequestSigner()).isEqualTo(requestSigner);
    verify(factory).createLedgerClient(ledgerTargetConfig);
    verify(factory).createAuditorClient(auditorTargetConfig);
    verify(factory).createRequestSigner(digitalSignatureIdentityConfig);
  }

  @Test
  public void
      create_ClientConfigWithDifferentInternalConfigGiven_ShouldCreateClientServicesWithDifferentInternalInstances() {
    // Arrange
    ClientConfig config2 = mock(ClientConfig.class);
    TargetConfig ledgerTargetConfig1 = mock(TargetConfig.class);
    TargetConfig auditorTargetConfig1 = mock(TargetConfig.class);
    DigitalSignatureIdentityConfig digitalSignatureIdentityConfig1 =
        mock(DigitalSignatureIdentityConfig.class);
    TargetConfig ledgerTargetConfig2 = mock(TargetConfig.class);
    TargetConfig auditorTargetConfig2 = mock(TargetConfig.class);
    DigitalSignatureIdentityConfig digitalSignatureIdentityConfig2 =
        mock(DigitalSignatureIdentityConfig.class);
    when(config.getLedgerTargetConfig()).thenReturn(ledgerTargetConfig1);
    when(config2.getLedgerTargetConfig()).thenReturn(ledgerTargetConfig2);
    when(config.getAuditorTargetConfig()).thenReturn(auditorTargetConfig1);
    when(config2.getAuditorTargetConfig()).thenReturn(auditorTargetConfig2);
    when(config.getDigitalSignatureIdentityConfig()).thenReturn(digitalSignatureIdentityConfig1);
    when(config2.getDigitalSignatureIdentityConfig()).thenReturn(digitalSignatureIdentityConfig2);
    when(config.isAuditorEnabled()).thenReturn(true);
    when(config2.isAuditorEnabled()).thenReturn(true);
    when(config.isAutoBootstrapEnabled()).thenReturn(false);
    when(config2.isAutoBootstrapEnabled()).thenReturn(false);
    doReturn(ledgerClient).when(factory).createLedgerClient(ledgerTargetConfig1);
    doReturn(auditorClient).when(factory).createAuditorClient(auditorTargetConfig1);
    doReturn(requestSigner).when(factory).createRequestSigner(digitalSignatureIdentityConfig1);
    LedgerClient ledgerClient2 = mock(LedgerClient.class);
    AuditorClient auditorClient2 = mock(AuditorClient.class);
    RequestSigner requestSigner2 = mock(RequestSigner.class);
    doReturn(ledgerClient2).when(factory).createLedgerClient(ledgerTargetConfig2);
    doReturn(auditorClient2).when(factory).createAuditorClient(auditorTargetConfig2);
    doReturn(requestSigner2).when(factory).createRequestSigner(digitalSignatureIdentityConfig2);

    // Act
    ClientService service1 = factory.create(config);
    ClientService service2 = factory.create(config2);

    // Assert
    assertThat(service1.getClientServiceHandler()).isInstanceOf(DefaultClientServiceHandler.class);
    assertThat(((DefaultClientServiceHandler) service1.getClientServiceHandler()).getLedgerClient())
        .isEqualTo(ledgerClient);
    assertThat(
            ((DefaultClientServiceHandler) service1.getClientServiceHandler()).getAuditorClient())
        .isEqualTo(auditorClient);
    assertThat(service1.getRequestSigner()).isEqualTo(requestSigner);
    assertThat(service2.getClientServiceHandler()).isInstanceOf(DefaultClientServiceHandler.class);
    assertThat(((DefaultClientServiceHandler) service2.getClientServiceHandler()).getLedgerClient())
        .isEqualTo(ledgerClient2);
    assertThat(
            ((DefaultClientServiceHandler) service2.getClientServiceHandler()).getAuditorClient())
        .isEqualTo(auditorClient2);
    assertThat(service2.getRequestSigner()).isEqualTo(requestSigner2);
    verify(factory).createLedgerClient(ledgerTargetConfig1);
    verify(factory).createAuditorClient(auditorTargetConfig1);
    verify(factory).createRequestSigner(digitalSignatureIdentityConfig1);
    verify(factory).createLedgerClient(ledgerTargetConfig2);
    verify(factory).createAuditorClient(auditorTargetConfig2);
    verify(factory).createRequestSigner(digitalSignatureIdentityConfig2);
  }

  @Test
  public void
      create_ClientConfigWithIntermediaryModeForLedgerAndAuditorGiven_ShouldCreateClientService() {
    // Arrange
    TargetConfig ledgerTargetConfig = mock(TargetConfig.class);
    TargetConfig auditorTargetConfig = mock(TargetConfig.class);
    when(config.getLedgerTargetConfig()).thenReturn(ledgerTargetConfig);
    when(config.getAuditorTargetConfig()).thenReturn(auditorTargetConfig);
    when(config.getClientMode()).thenReturn(ClientMode.INTERMEDIARY);
    when(config.getDigitalSignatureIdentityConfig()).thenReturn(null);
    when(config.getHmacIdentityConfig()).thenReturn(null);
    when(config.isAuditorEnabled()).thenReturn(true);
    doReturn(ledgerClient).when(factory).createLedgerClient(any());
    doReturn(auditorClient).when(factory).createAuditorClient(any());

    // Act
    ClientService service = factory.create(config);

    // Assert
    assertThat(service.getClientServiceHandler()).isInstanceOf(DefaultClientServiceHandler.class);
    assertThat(((DefaultClientServiceHandler) service.getClientServiceHandler()).getLedgerClient())
        .isEqualTo(ledgerClient);
    assertThat(((DefaultClientServiceHandler) service.getClientServiceHandler()).getAuditorClient())
        .isEqualTo(auditorClient);
    assertThat(service.getRequestSigner()).isNull();
    verify(factory).createLedgerClient(ledgerTargetConfig);
    verify(factory).createAuditorClient(auditorTargetConfig);
    verify(factory, never()).createRequestSigner(any(DigitalSignatureIdentityConfig.class));
    verify(factory, never()).createRequestSigner(any(HmacIdentityConfig.class));
  }

  @Test
  public void
      create_GatewayClientConfigWithDigitalSignatureIdentityGiven_ShouldCreateClientService() {
    // Arrange
    TargetConfig gatewayTargetConfig = mock(TargetConfig.class);
    DigitalSignatureIdentityConfig digitalSignatureIdentityConfig =
        mock(DigitalSignatureIdentityConfig.class);
    when(config.getDigitalSignatureIdentityConfig()).thenReturn(digitalSignatureIdentityConfig);
    when(config.isAutoBootstrapEnabled()).thenReturn(false);
    when(gatewayClientConfig.getClientConfig()).thenReturn(config);
    when(gatewayClientConfig.getGatewayTargetConfig()).thenReturn(gatewayTargetConfig);
    doReturn(gatewayClient).when(factory).createGatewayClient(any());
    doReturn(requestSigner)
        .when(factory)
        .createRequestSigner(any(DigitalSignatureIdentityConfig.class));

    // Act
    ClientService service = factory.create(gatewayClientConfig);

    // Assert
    assertThat(service.getClientServiceHandler()).isInstanceOf(GatewayClientServiceHandler.class);
    assertThat(((GatewayClientServiceHandler) service.getClientServiceHandler()).getGatewayClient())
        .isEqualTo(gatewayClient);
    assertThat(service.getRequestSigner()).isEqualTo(requestSigner);
    verify(factory).createGatewayClient(gatewayTargetConfig);
    verify(factory).createRequestSigner(digitalSignatureIdentityConfig);
    verify(factory, never()).createRequestSigner(any(HmacIdentityConfig.class));
  }

  @Test
  public void create_GatewayClientConfigWithHmacIdentityGiven_ShouldCreateClientService() {
    // Arrange
    TargetConfig gatewayTargetConfig = mock(TargetConfig.class);
    HmacIdentityConfig hmacIdentityConfig = mock(HmacIdentityConfig.class);
    when(config.getDigitalSignatureIdentityConfig()).thenReturn(null);
    when(config.getHmacIdentityConfig()).thenReturn(hmacIdentityConfig);
    when(config.isAutoBootstrapEnabled()).thenReturn(false);
    when(gatewayClientConfig.getClientConfig()).thenReturn(config);
    when(gatewayClientConfig.getGatewayTargetConfig()).thenReturn(gatewayTargetConfig);
    doReturn(gatewayClient).when(factory).createGatewayClient(any());
    doReturn(requestSigner).when(factory).createRequestSigner(any(HmacIdentityConfig.class));

    // Act
    ClientService service = factory.create(gatewayClientConfig);

    // Assert
    assertThat(service.getClientServiceHandler()).isInstanceOf(GatewayClientServiceHandler.class);
    assertThat(((GatewayClientServiceHandler) service.getClientServiceHandler()).getGatewayClient())
        .isEqualTo(gatewayClient);
    assertThat(service.getRequestSigner()).isEqualTo(requestSigner);
    verify(factory).createGatewayClient(gatewayTargetConfig);
    verify(factory).createRequestSigner(hmacIdentityConfig);
    verify(factory, never()).createRequestSigner(any(DigitalSignatureIdentityConfig.class));
  }

  @Test
  public void create_GatewayClientConfigWithIntermediaryModeGiven_ShouldCreateClientService() {
    // Arrange
    TargetConfig gatewayTargetConfig = mock(TargetConfig.class);
    when(config.getClientMode()).thenReturn(ClientMode.INTERMEDIARY);
    when(config.getDigitalSignatureIdentityConfig()).thenReturn(null);
    when(config.getHmacIdentityConfig()).thenReturn(null);
    when(config.isAutoBootstrapEnabled()).thenReturn(false);
    when(gatewayClientConfig.getClientConfig()).thenReturn(config);
    when(gatewayClientConfig.getGatewayTargetConfig()).thenReturn(gatewayTargetConfig);
    doReturn(gatewayClient).when(factory).createGatewayClient(any());

    // Act
    ClientService service = factory.create(gatewayClientConfig);

    // Assert
    assertThat(service.getClientServiceHandler()).isInstanceOf(GatewayClientServiceHandler.class);
    assertThat(((GatewayClientServiceHandler) service.getClientServiceHandler()).getGatewayClient())
        .isEqualTo(gatewayClient);
    assertThat(service.getRequestSigner()).isNull();
    verify(factory, never()).createRequestSigner(any(DigitalSignatureIdentityConfig.class));
    verify(factory, never()).createRequestSigner(any(HmacIdentityConfig.class));
  }

  @Test
  public void
      create_GatewayClientConfigWithTheSameInternalConfigGiven_ShouldCreateClientServicesWithSameInternalInstances() {
    // Arrange
    TargetConfig gatewayTargetConfig = mock(TargetConfig.class);
    DigitalSignatureIdentityConfig digitalSignatureIdentityConfig =
        mock(DigitalSignatureIdentityConfig.class);
    when(config.getDigitalSignatureIdentityConfig()).thenReturn(digitalSignatureIdentityConfig);
    when(config.isAutoBootstrapEnabled()).thenReturn(false);
    when(gatewayClientConfig.getClientConfig()).thenReturn(config);
    when(gatewayClientConfig.getGatewayTargetConfig()).thenReturn(gatewayTargetConfig);
    doReturn(gatewayClient).when(factory).createGatewayClient(any());
    doReturn(requestSigner)
        .when(factory)
        .createRequestSigner(any(DigitalSignatureIdentityConfig.class));

    // Act
    ClientService service1 = factory.create(gatewayClientConfig);
    ClientService service2 = factory.create(gatewayClientConfig);

    // Assert
    assertThat(service1.getClientServiceHandler()).isInstanceOf(GatewayClientServiceHandler.class);
    assertThat(
            ((GatewayClientServiceHandler) service1.getClientServiceHandler()).getGatewayClient())
        .isEqualTo(gatewayClient);
    assertThat(service1.getRequestSigner()).isEqualTo(requestSigner);
    assertThat(service2.getClientServiceHandler()).isInstanceOf(GatewayClientServiceHandler.class);
    assertThat(
            ((GatewayClientServiceHandler) service2.getClientServiceHandler()).getGatewayClient())
        .isEqualTo(gatewayClient);
    assertThat(service2.getRequestSigner()).isEqualTo(requestSigner);
    verify(factory).createGatewayClient(gatewayTargetConfig);
    verify(factory).createRequestSigner(digitalSignatureIdentityConfig);
  }

  @Test
  public void
      create_GatewayClientConfigWithDifferentInternalConfigGiven_ShouldCreateClientServicesWithDifferentInternalInstances() {
    // Arrange
    GatewayClientConfig gatewayClientConfig2 = mock(GatewayClientConfig.class);
    ClientConfig config2 = mock(ClientConfig.class);
    TargetConfig gatewayTargetConfig1 = mock(TargetConfig.class);
    DigitalSignatureIdentityConfig digitalSignatureIdentityConfig1 =
        mock(DigitalSignatureIdentityConfig.class);
    TargetConfig gatewayTargetConfig2 = mock(TargetConfig.class);
    DigitalSignatureIdentityConfig digitalSignatureIdentityConfig2 =
        mock(DigitalSignatureIdentityConfig.class);
    when(config.getDigitalSignatureIdentityConfig()).thenReturn(digitalSignatureIdentityConfig1);
    when(config2.getDigitalSignatureIdentityConfig()).thenReturn(digitalSignatureIdentityConfig2);
    when(config.isAutoBootstrapEnabled()).thenReturn(false);
    when(config2.isAutoBootstrapEnabled()).thenReturn(false);
    when(gatewayClientConfig.getClientConfig()).thenReturn(config);
    when(gatewayClientConfig2.getClientConfig()).thenReturn(config2);
    when(gatewayClientConfig.getGatewayTargetConfig()).thenReturn(gatewayTargetConfig1);
    when(gatewayClientConfig2.getGatewayTargetConfig()).thenReturn(gatewayTargetConfig2);
    doReturn(gatewayClient).when(factory).createGatewayClient(gatewayTargetConfig1);
    doReturn(requestSigner).when(factory).createRequestSigner(digitalSignatureIdentityConfig1);
    GatewayClient gatewayClient2 = mock(GatewayClient.class);
    RequestSigner requestSigner2 = mock(RequestSigner.class);
    doReturn(gatewayClient2).when(factory).createGatewayClient(gatewayTargetConfig2);
    doReturn(requestSigner2).when(factory).createRequestSigner(digitalSignatureIdentityConfig2);

    // Act
    ClientService service1 = factory.create(gatewayClientConfig);
    ClientService service2 = factory.create(gatewayClientConfig2);

    // Assert
    assertThat(service1.getClientServiceHandler()).isInstanceOf(GatewayClientServiceHandler.class);
    assertThat(
            ((GatewayClientServiceHandler) service1.getClientServiceHandler()).getGatewayClient())
        .isEqualTo(gatewayClient);
    assertThat(service1.getRequestSigner()).isEqualTo(requestSigner);
    assertThat(service2.getClientServiceHandler()).isInstanceOf(GatewayClientServiceHandler.class);
    assertThat(
            ((GatewayClientServiceHandler) service2.getClientServiceHandler()).getGatewayClient())
        .isEqualTo(gatewayClient2);
    assertThat(service2.getRequestSigner()).isEqualTo(requestSigner2);
    verify(factory).createGatewayClient(gatewayTargetConfig1);
    verify(factory).createRequestSigner(digitalSignatureIdentityConfig1);
    verify(factory).createGatewayClient(gatewayTargetConfig2);
    verify(factory).createRequestSigner(digitalSignatureIdentityConfig2);
  }

  @Test
  public void create_ClientConfigWithoutAutoBootstrapConfigurationGiven_ShouldCallBootstrap() {
    // Arrange
    TargetConfig ledgerTargetConfig = mock(TargetConfig.class);
    when(config.isAutoBootstrapEnabled()).thenReturn(true);
    when(config.getLedgerTargetConfig()).thenReturn(ledgerTargetConfig);
    when(config.getDigitalSignatureIdentityConfig()).thenReturn(digitalSignatureIdentityConfig);
    when(config.isAuditorEnabled()).thenReturn(false);
    doReturn(ledgerClient).when(factory).createLedgerClient(any());
    doReturn(requestSigner)
        .when(factory)
        .createRequestSigner(any(DigitalSignatureIdentityConfig.class));
    doReturn(clientService)
        .when(factory)
        .createClientService(any(ClientConfig.class), any(ClientServiceHandler.class), any());

    // Act
    factory.create(config);

    // Assert
    verify(clientService).bootstrap();
  }

  @Test
  public void create_ClientConfigGivenAndAutoBootstrapDisabledOverride_ShouldNotCallBootstrap() {
    // Arrange
    TargetConfig ledgerTargetConfig = mock(TargetConfig.class);
    when(config.isAutoBootstrapEnabled()).thenReturn(true);
    when(config.getLedgerTargetConfig()).thenReturn(ledgerTargetConfig);
    when(config.getDigitalSignatureIdentityConfig()).thenReturn(digitalSignatureIdentityConfig);
    when(config.isAuditorEnabled()).thenReturn(false);
    doReturn(ledgerClient).when(factory).createLedgerClient(any());
    doReturn(requestSigner)
        .when(factory)
        .createRequestSigner(any(DigitalSignatureIdentityConfig.class));
    doReturn(clientService)
        .when(factory)
        .createClientService(any(ClientConfig.class), any(ClientServiceHandler.class), any());

    // Act
    factory.create(config, false);

    // Assert
    verify(clientService, never()).bootstrap();
  }

  @Test
  public void create_ClientConfigGivenAndAutoBootstrapEnabledOverride_ShouldCallBootstrap() {
    // Arrange
    TargetConfig ledgerTargetConfig = mock(TargetConfig.class);
    when(config.isAutoBootstrapEnabled()).thenReturn(false);
    when(config.getLedgerTargetConfig()).thenReturn(ledgerTargetConfig);
    when(config.getDigitalSignatureIdentityConfig()).thenReturn(digitalSignatureIdentityConfig);
    when(config.isAuditorEnabled()).thenReturn(false);
    doReturn(ledgerClient).when(factory).createLedgerClient(any());
    doReturn(requestSigner)
        .when(factory)
        .createRequestSigner(any(DigitalSignatureIdentityConfig.class));
    doReturn(clientService)
        .when(factory)
        .createClientService(any(ClientConfig.class), any(ClientServiceHandler.class), any());

    // Act
    factory.create(config, true);

    // Assert
    verify(clientService).bootstrap();
  }

  @Test
  public void
      create_GatewayClientConfigGivenAndAutoBootstrapDisabledOverride_ShouldNotCallBootstrap() {
    // Arrange
    TargetConfig gatewayTargetConfig = mock(TargetConfig.class);
    when(config.isAutoBootstrapEnabled()).thenReturn(true);
    when(config.getDigitalSignatureIdentityConfig()).thenReturn(digitalSignatureIdentityConfig);
    when(gatewayClientConfig.getClientConfig()).thenReturn(config);
    when(gatewayClientConfig.getGatewayTargetConfig()).thenReturn(gatewayTargetConfig);
    doReturn(gatewayClient).when(factory).createGatewayClient(any());
    doReturn(requestSigner)
        .when(factory)
        .createRequestSigner(any(DigitalSignatureIdentityConfig.class));
    doReturn(clientService)
        .when(factory)
        .createClientService(any(ClientConfig.class), any(ClientServiceHandler.class), any());

    // Act
    factory.create(gatewayClientConfig, false);

    // Assert
    verify(clientService, never()).bootstrap();
  }

  @Test
  public void create_GatewayClientConfigGivenAndAutoBootstrapEnabledOverride_ShouldCallBootstrap() {
    // Arrange
    TargetConfig gatewayTargetConfig = mock(TargetConfig.class);
    when(config.isAutoBootstrapEnabled()).thenReturn(false);
    when(config.getDigitalSignatureIdentityConfig()).thenReturn(digitalSignatureIdentityConfig);
    when(gatewayClientConfig.getClientConfig()).thenReturn(config);
    when(gatewayClientConfig.getGatewayTargetConfig()).thenReturn(gatewayTargetConfig);
    doReturn(gatewayClient).when(factory).createGatewayClient(any());
    doReturn(requestSigner)
        .when(factory)
        .createRequestSigner(any(DigitalSignatureIdentityConfig.class));
    doReturn(clientService)
        .when(factory)
        .createClientService(any(ClientConfig.class), any(ClientServiceHandler.class), any());

    // Act
    factory.create(gatewayClientConfig, true);

    // Assert
    verify(clientService).bootstrap();
  }
}
