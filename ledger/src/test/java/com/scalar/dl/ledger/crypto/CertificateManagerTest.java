package com.scalar.dl.ledger.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.scalar.dl.ledger.database.CertificateRegistry;
import com.scalar.dl.ledger.exception.DatabaseException;
import com.scalar.dl.ledger.exception.MissingCertificateException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CertificateManagerTest {
  private static final String SOME_NAMESPACE = "some_namespace";
  private static final String SOME_OTHER_NAMESPACE = "some_other_namespace";
  private static final String SOME_ENTITY_ID = "some_id";
  private static final int SOME_VERSION = 1;
  private static final String SOME_PEM =
      "-----BEGIN CERTIFICATE-----\n"
          + "MIICQTCCAeagAwIBAgIUEKARigcZQ3sLEXdlEtjYissVx0cwCgYIKoZIzj0EAwIw\n"
          + "QTELMAkGA1UEBhMCSlAxDjAMBgNVBAgTBVRva3lvMQ4wDAYDVQQHEwVUb2t5bzES\n"
          + "MBAGA1UEChMJU2FtcGxlIENBMB4XDTE4MDYyMTAyMTUwMFoXDTE5MDYyMTAyMTUw\n"
          + "MFowRTELMAkGA1UEBhMCSlAxDjAMBgNVBAgTBVRva3lvMQ4wDAYDVQQHEwVUb2t5\n"
          + "bzEWMBQGA1UEChMNU2FtcGxlIENsaWVudDBZMBMGByqGSM49AgEGCCqGSM49AwEH\n"
          + "A0IABGNIv4gBcSAUt3rW46Egtf2lCkFeuvsImsC+G2apJbvOjmyHO8K4nxxMT5lY\n"
          + "ShUpJTsmahqDolWQNM39C2XhWjyjgbcwgbQwDgYDVR0PAQH/BAQDAgWgMB0GA1Ud\n"
          + "JQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjAMBgNVHRMBAf8EAjAAMB0GA1UdDgQW\n"
          + "BBTpBQl/JxB7yr77uMVT9mMicPeVJTAfBgNVHSMEGDAWgBQrJo3N3/0j3oPS6F6m\n"
          + "wunHe8xLpzA1BgNVHREELjAsghJjbGllbnQuZXhhbXBsZS5jb22CFnd3dy5jbGll\n"
          + "bnQuZXhhbXBsZS5jb20wCgYIKoZIzj0EAwIDSQAwRgIhAJPtXSzuncDJXnM+7us8\n"
          + "46MEVjGHJy70bRY1My23RkxbAiEA5oFgTKMvls8e4UpnmUgFNP+FH8a5bF4tUPaV\n"
          + "BQiBbgk=\n"
          + "-----END CERTIFICATE-----";
  private static final long SOME_TIME = 1;
  private CertificateEntry entry;
  @Mock private CertificateRegistry registry;
  @Mock private DigitalSignatureValidator validator;
  private ConcurrentMap<String, Cache<CertificateEntry.Key, DigitalSignatureValidator>> caches;
  private CertificateManager manager;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    caches = new ConcurrentHashMap<>();
    manager = new CertificateManager(registry, caches);
    entry = new CertificateEntry(SOME_ENTITY_ID, SOME_VERSION, SOME_PEM, SOME_TIME);
  }

  @Test
  public void register_CertificateRegistrationRequestGiven_ShouldCallBind() {
    // Arrange
    when(registry.lookup(SOME_NAMESPACE, entry.getKey()))
        .thenThrow(MissingCertificateException.class);

    // Act
    manager.register(SOME_NAMESPACE, entry);

    // Assert
    verify(registry).lookup(SOME_NAMESPACE, entry.getKey());
    verify(registry).bind(SOME_NAMESPACE, entry);
  }

  @Test
  public void register_AlreadyRegisteredCertificateGiven_ShouldThrowDatabaseException() {
    // Arrange
    when(registry.lookup(SOME_NAMESPACE, entry.getKey())).thenReturn(mock(CertificateEntry.class));

    // Act
    assertThatThrownBy(() -> manager.register(SOME_NAMESPACE, entry))
        .isInstanceOf(DatabaseException.class);

    // Assert
    verify(registry, never()).bind(eq(SOME_NAMESPACE), eq(entry));
  }

  @Test
  public void getValidator_holderIdAndVersionGivenAndValidatorNotCached_ShouldGetFromRegistry() {
    // Arrange
    when(registry.lookup(SOME_NAMESPACE, entry.getKey())).thenReturn(entry);

    // Act
    DigitalSignatureValidator actual = manager.getValidator(SOME_NAMESPACE, entry.getKey());

    // Assert
    verify(registry).lookup(SOME_NAMESPACE, entry.getKey());
    assertThat(actual).isEqualTo(new DigitalSignatureValidator(SOME_PEM));
    assertThat(caches.get(SOME_NAMESPACE).asMap()).containsOnly(entry(entry.getKey(), actual));
  }

  @Test
  public void getValidator_holderIdAndVersionGivenAndValidatorCached_ShouldGetFromCache() {
    // Arrange
    Cache<CertificateEntry.Key, DigitalSignatureValidator> cache =
        CacheBuilder.newBuilder().maximumSize(128).build();
    cache.put(entry.getKey(), validator);
    caches.put(SOME_NAMESPACE, cache);

    // Act
    DigitalSignatureValidator actual = manager.getValidator(SOME_NAMESPACE, entry.getKey());

    // Assert
    verify(registry, never()).lookup(SOME_NAMESPACE, entry.getKey());
    assertThat(actual).isEqualTo(validator);
    assertThat(caches.get(SOME_NAMESPACE).asMap()).containsOnly(entry(entry.getKey(), actual));
  }

  @Test
  public void getValidator_DifferentNamespacesGiven_ShouldUseSeparateCaches() {
    // Arrange
    CertificateEntry otherEntry =
        new CertificateEntry(SOME_ENTITY_ID, SOME_VERSION, SOME_PEM, SOME_TIME);
    when(registry.lookup(SOME_NAMESPACE, entry.getKey())).thenReturn(entry);
    when(registry.lookup(SOME_OTHER_NAMESPACE, otherEntry.getKey())).thenReturn(otherEntry);

    // Act
    DigitalSignatureValidator validator1 = manager.getValidator(SOME_NAMESPACE, entry.getKey());
    DigitalSignatureValidator validator2 =
        manager.getValidator(SOME_OTHER_NAMESPACE, otherEntry.getKey());

    // Assert
    verify(registry).lookup(SOME_NAMESPACE, entry.getKey());
    verify(registry).lookup(SOME_OTHER_NAMESPACE, otherEntry.getKey());
    assertThat(caches).hasSize(2);
    assertThat(caches.get(SOME_NAMESPACE).asMap()).containsOnly(entry(entry.getKey(), validator1));
    assertThat(caches.get(SOME_OTHER_NAMESPACE).asMap())
        .containsOnly(entry(otherEntry.getKey(), validator2));
  }
}
