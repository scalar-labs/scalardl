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
import com.scalar.dl.client.config.HmacIdentityConfig;
import com.scalar.dl.client.util.RequestSigner;
import com.scalar.dl.ledger.config.TargetConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ClientServiceFactoryTest {
  @Mock private ClientConfig config;
  @Mock private LedgerClient ledgerClient;
  @Mock private AuditorClient auditorClient;
  @Mock private RequestSigner requestSigner;
  private ClientServiceFactory factory;

  @Before
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
    doReturn(ledgerClient).when(factory).createLedgerClient(any());
    doReturn(requestSigner)
        .when(factory)
        .createRequestSigner(any(DigitalSignatureIdentityConfig.class));

    // Act
    ClientService service = factory.create(config);

    // Assert
    assertThat(service.getLedgerClient()).isEqualTo(ledgerClient);
    assertThat(service.getRequestSigner()).isEqualTo(requestSigner);
    assertThat(service.getAuditorClient()).isNull();
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
    doReturn(ledgerClient).when(factory).createLedgerClient(any());
    doReturn(requestSigner).when(factory).createRequestSigner(any(HmacIdentityConfig.class));

    // Act
    ClientService service = factory.create(config);

    // Assert
    assertThat(service.getLedgerClient()).isEqualTo(ledgerClient);
    assertThat(service.getRequestSigner()).isEqualTo(requestSigner);
    assertThat(service.getAuditorClient()).isNull();
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
    doReturn(ledgerClient).when(factory).createLedgerClient(any());
    doReturn(auditorClient).when(factory).createAuditorClient(any());
    doReturn(requestSigner)
        .when(factory)
        .createRequestSigner(any(DigitalSignatureIdentityConfig.class));

    // Act
    ClientService service = factory.create(config);

    // Assert
    assertThat(service.getLedgerClient()).isEqualTo(ledgerClient);
    assertThat(service.getAuditorClient()).isEqualTo(auditorClient);
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
    doReturn(ledgerClient).when(factory).createLedgerClient(any());
    doReturn(auditorClient).when(factory).createAuditorClient(any());
    doReturn(requestSigner)
        .when(factory)
        .createRequestSigner(any(DigitalSignatureIdentityConfig.class));

    // Act
    ClientService service1 = factory.create(config);
    ClientService service2 = factory.create(config);

    // Assert
    assertThat(service1.getLedgerClient()).isEqualTo(ledgerClient);
    assertThat(service1.getAuditorClient()).isEqualTo(auditorClient);
    assertThat(service1.getRequestSigner()).isEqualTo(requestSigner);
    assertThat(service2.getLedgerClient()).isEqualTo(ledgerClient);
    assertThat(service2.getAuditorClient()).isEqualTo(auditorClient);
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
    assertThat(service1.getLedgerClient()).isEqualTo(ledgerClient);
    assertThat(service1.getAuditorClient()).isEqualTo(auditorClient);
    assertThat(service1.getRequestSigner()).isEqualTo(requestSigner);
    assertThat(service2.getLedgerClient()).isEqualTo(ledgerClient2);
    assertThat(service2.getAuditorClient()).isEqualTo(auditorClient2);
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
    assertThat(service.getLedgerClient()).isEqualTo(ledgerClient);
    assertThat(service.getAuditorClient()).isEqualTo(auditorClient);
    assertThat(service.getRequestSigner()).isNull();
    verify(factory).createLedgerClient(ledgerTargetConfig);
    verify(factory).createAuditorClient(auditorTargetConfig);
    verify(factory, never()).createRequestSigner(any(DigitalSignatureIdentityConfig.class));
    verify(factory, never()).createRequestSigner(any(HmacIdentityConfig.class));
  }
}
