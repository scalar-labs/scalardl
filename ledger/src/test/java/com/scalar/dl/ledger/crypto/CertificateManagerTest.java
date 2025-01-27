package com.scalar.dl.ledger.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.scalar.dl.ledger.database.CertificateRegistry;
import com.scalar.dl.ledger.exception.DatabaseException;
import com.scalar.dl.ledger.exception.MissingCertificateException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CertificateManagerTest {
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
  private Cache<CertificateEntry.Key, DigitalSignatureValidator> cache;
  private CertificateManager manager;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    cache = CacheBuilder.newBuilder().maximumSize(128).build();
    manager = new CertificateManager(registry, cache);
    entry = new CertificateEntry(SOME_ENTITY_ID, SOME_VERSION, SOME_PEM, SOME_TIME);
  }

  @Test
  public void register_CertificateRegistrationRequestGiven_ShouldCallBind() {
    // Arrange
    when(registry.lookup(entry.getKey())).thenThrow(MissingCertificateException.class);

    // Act
    manager.register(entry);

    // Assert
    verify(registry).lookup(entry.getKey());
    verify(registry).bind(entry);
  }

  @Test
  public void register_AlreadyRegisteredCertificateGiven_ShouldThrowDatabaseException() {
    // Arrange
    when(registry.lookup(entry.getKey())).thenReturn(mock(CertificateEntry.class));

    // Act
    assertThatThrownBy(() -> manager.register(entry)).isInstanceOf(DatabaseException.class);

    // Assert
    verify(registry, never()).bind(entry);
  }

  @Test
  public void getValidator_holderIdAndVersionGivenAndValidatorNotCached_ShouldGetFromRegistry() {
    // Arrange
    when(registry.lookup(entry.getKey())).thenReturn(entry);

    // Act
    DigitalSignatureValidator actual = manager.getValidator(entry.getKey());

    // Assert
    verify(registry).lookup(entry.getKey());
    assertThat(actual).isEqualTo(new DigitalSignatureValidator(SOME_PEM));
    assertThat(cache.asMap()).containsOnly(entry(entry.getKey(), actual));
  }

  @Test
  public void getValidator_holderIdAndVersionGivenAndValidatorCached_ShouldGetFromCache() {
    // Arrange
    cache.put(entry.getKey(), validator);

    // Act
    DigitalSignatureValidator actual = manager.getValidator(entry.getKey());

    // Assert
    verify(registry, never()).lookup(entry.getKey());
    assertThat(actual).isEqualTo(validator);
    assertThat(cache.asMap()).containsOnly(entry(entry.getKey(), actual));
  }
}
