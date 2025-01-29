package com.scalar.dl.ledger.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class CryptoUtilsTest {
  private static final String SOME_PRIVATE_KEY_PATH = "private_key_path";
  private static final String SOME_EC_PRIVATE_KEY_SEC1_PEM =
      "-----BEGIN EC PRIVATE KEY-----\n"
          + "MHcCAQEEIF4SjQxTArRcZaROSFjlBP2rR8fAKtL8y+kmGiSlM5hEoAoGCCqGSM49\n"
          + "AwEHoUQDQgAEY0i/iAFxIBS3etbjoSC1/aUKQV66+wiawL4bZqklu86ObIc7wrif\n"
          + "HExPmVhKFSklOyZqGoOiVZA0zf0LZeFaPA==\n"
          + "-----END EC PRIVATE KEY-----";
  private static final String SOME_EC_PRIVATE_KEY_PKCS8_PEM =
      "-----BEGIN PRIVATE KEY-----\n"
          + "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQghsSVj9WzmcShUawd\n"
          + "nwb2JzFlyxGVPYNwch/EU2RmucehRANCAAT9PMYRxgk1zB7l34GU+7G7CIiEyEqk\n"
          + "ylMxkz3gv5tGEM8WficyKBwKKOV7LuqE5/CmVWtO2c0tM0wPza23CdHu\n"
          + "-----END PRIVATE KEY-----";
  private static final String SOME_RSA_PRIVATE_KEY_PEM =
      "-----BEGIN PRIVATE KEY-----\n"
          + "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDlKRO6Xhqoipqb\n"
          + "PxM8CZUpmOuGSTD9yelg8YuAhh2c/JRqy/Nvbi++g5IFW9ij+9y6csZrjzdAithm\n"
          + "lZQvoWFBrFBcBBhu95mFilFYQkonmQnKb6KXtydZ9zEZ43jRV754NQZTPQt2RuNs\n"
          + "zG8DUBwfB4cwHUuekujNAF/y2M/UGwodDLdmUVV1+s7yXqtiqXv8oTDmXDrw1wDn\n"
          + "GTNGqsVUMDxlrJs16qFnGtlbqxbEq+s/p/yhl/zRqoC+JMbuIWQQwFZP8R+YkbbA\n"
          + "PNFzRl4YQtTuqT1o0dz9/Jjjgo/kua48mTPd1qPlSDUZMGxyBEPTr3Hg3gRaMgun\n"
          + "n2CpMcrzAgMBAAECggEAYKgHSKvpjZ5MoN+lYsTd8/oqmWzkq58O/1NSVmb7NZx5\n"
          + "k2Qr7RuqKq6/F+CKC3yvuLqxg2uYT/JgXUCJDOACMBuYl38ouSFZUTCqp9HqCjKs\n"
          + "JQUaLOjVQcaYIMXHz3C1h2lCHjjYzU4QHfNKfbi768CWnk4095EafWFDlyAo5Nqo\n"
          + "gTcvZYAfQ/J58x0R8ACaW5tr/FaWVbclpknTQJOEze/oJChPHoxWm1hQM2dFCFiC\n"
          + "4c4MfU5EwGkCEbI3oINuOD3oLibdJPa/FeKIP1c8OKev2muwOOR8EK1mvH1PheZ4\n"
          + "Flz9pT6a5NmneEDAys7E/SvkuDXjv7beFoZO1mk/kQKBgQDzsp4r21zHXgipwnT+\n"
          + "zg4d+mWxPORQmYEB94X9m1J2vcWXodTSkpuWnE2GtuuYgrBreib0601UHK1MBJCd\n"
          + "pIXiUiDzvJuzTDe27meJbfcUpe7uKw/cegnQ5OhG6+08JxrDnrBzLrauxwO1TTDe\n"
          + "Mk1g3ybxyAO8KvCTrCTLMcjNAwKBgQDwupbq9nxbfYA5tEzQUrMsluvjryGDKRFP\n"
          + "5//rs6ndsMWPIpbM+AQMiBX3a3DK/oFwuCLiOEhe+RtRkRT5v4dNMz3ppezIoz1/\n"
          + "b7l8z2kHDgWHfn4OnCzU4uu3NxKF8EUR7Cj/McAXBNc9B6IQfeghNEmn0lU/IrJU\n"
          + "t5FyN3dPUQKBgGePPhwRCipG0ZOiaSlm0yT2JYTeBYAi22nEkBInkTGYj4FSaihZ\n"
          + "2Ph1z5Qx44hZ8TKbJDbsT14xGYu2XOZA5gw5LnulaN0WxI5eXW1PZ7JEmXZookLl\n"
          + "MYqt2+9XjucDDyKWKxGnhkZbmD3RYbTq12sVKW2Ru9SYwsdeXfxjWZuVAoGBANDD\n"
          + "0Eu2NQSW4xOn9BGaFr6tp6aN448+lz/n376F0eNrAPlvsr3QvJotjndCms/ARXr+\n"
          + "hWoQC1spx3JGUp2AZxMhRCkTdzRMtPGGb6L4sImotEzb+vAqqop4vJjoTs0PIKc5\n"
          + "WhhuCbssvIi1zOtmdWAOW24J71nQej0T8TPPPE3xAoGADrTUrZHjZTswSj2a7W+G\n"
          + "+cTaT+DFMedlkvivPxyrTxZ0NBAthwKhjm2eoF16d+oQ5pSKtRbgTg+M11RIHst9\n"
          + "7qmDQ8vmvyC1KOQTB3xEy2+sJJKlfsZ3uuSGdrRBvgdJ8KjTmvxnkzRDIXKppc1b\n"
          + "3k8D33B6XL4v+7PWvxNRfYA=\n"
          + "-----END PRIVATE KEY-----";
  private static final String SOME_CERTIFICATE_PATH = "certificate_path";
  private static final String SOME_CERTIFICATE_PEM =
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
  private static final String SOME_INVALID_PEM = "invalid_pem";

  @TempDir Path folder;

  private void writeToFile(File file, String content) throws IOException {
    BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8);
    writer.write(content);
    writer.close();
  }

  @BeforeEach
  public void setUp() {}

  @Test
  void createKeyStore_EcdsaCertAndKeyGiven_ShouldReturnKeyStore()
      throws IOException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
    // Arrange
    File certificatePath = folder.resolve(SOME_CERTIFICATE_PATH).toFile();
    writeToFile(certificatePath.getCanonicalFile(), SOME_CERTIFICATE_PEM);
    File privateKeyPath = folder.resolve(SOME_PRIVATE_KEY_PATH).toFile();
    writeToFile(privateKeyPath.getCanonicalFile(), SOME_EC_PRIVATE_KEY_SEC1_PEM);

    // Act
    KeyStore actual =
        CryptoUtils.createKeyStore(
            certificatePath.getCanonicalPath(), privateKeyPath.getCanonicalPath(), "password");

    // Assert
    assertThat(actual.getKey("key", "password".toCharArray())).isInstanceOf(ECPrivateKey.class);
    assertThat(actual.getCertificate("cert")).isInstanceOf(X509Certificate.class);
  }

  @Test
  void createKeyStore_RsaCertAndKeyGiven_ShouldReturnKeyStore()
      throws IOException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
    // Arrange
    File certificatePath = folder.resolve(SOME_CERTIFICATE_PATH).toFile();
    writeToFile(certificatePath.getCanonicalFile(), SOME_CERTIFICATE_PEM);
    File privateKeyPath = folder.resolve(SOME_PRIVATE_KEY_PATH).toFile();
    writeToFile(privateKeyPath.getCanonicalFile(), SOME_RSA_PRIVATE_KEY_PEM);

    // Act
    KeyStore actual =
        CryptoUtils.createKeyStore(
            certificatePath.getCanonicalPath(), privateKeyPath.getCanonicalPath(), "password");

    // Assert
    assertThat(actual.getKey("key", "password".toCharArray())).isInstanceOf(RSAPrivateKey.class);
    assertThat(actual.getCertificate("cert")).isInstanceOf(X509Certificate.class);
  }

  @Test
  void createKeyStore_CorrectCertAndInvalidKeyGiven_ShouldThrowIllegalArgumentException()
      throws IOException {
    // Arrange
    File certificatePath = folder.resolve(SOME_CERTIFICATE_PATH).toFile();
    writeToFile(certificatePath.getCanonicalFile(), SOME_CERTIFICATE_PEM);
    File privateKeyPath = folder.resolve(SOME_PRIVATE_KEY_PATH).toFile();
    writeToFile(privateKeyPath.getCanonicalFile(), SOME_INVALID_PEM);

    // Act Assert
    assertThatThrownBy(
            () ->
                CryptoUtils.createKeyStore(
                    certificatePath.getCanonicalPath(),
                    privateKeyPath.getCanonicalPath(),
                    "password"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void createKeyStore_InvalidCertAndCorrectKeyGiven_ShouldThrowIllegalArgumentException()
      throws IOException {
    // Arrange
    File certificatePath = folder.resolve(SOME_CERTIFICATE_PATH).toFile();
    writeToFile(certificatePath.getCanonicalFile(), SOME_INVALID_PEM);
    File privateKeyPath = folder.resolve(SOME_PRIVATE_KEY_PATH).toFile();
    writeToFile(privateKeyPath.getCanonicalFile(), SOME_EC_PRIVATE_KEY_SEC1_PEM);

    // Act Assert
    assertThatThrownBy(
            () ->
                CryptoUtils.createKeyStore(
                    certificatePath.getCanonicalPath(),
                    privateKeyPath.getCanonicalPath(),
                    "password"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void getPrivateKey_EcSec1KeyGiven_ShouldReturnPrivateKey() throws IOException {
    // Arrange
    Reader reader = new StringReader(SOME_EC_PRIVATE_KEY_SEC1_PEM);

    // Act
    PrivateKey actual = CryptoUtils.getPrivateKey(reader);

    // Assert
    assertThat(actual).isInstanceOf(ECPrivateKey.class);
    assertThat(actual.getFormat()).isEqualTo("PKCS#8"); // BCECPrivateKey always return #8
  }

  @Test
  void getPrivateKey_EcPkcs8KeyGiven_ShouldReturnPrivateKey() throws IOException {
    // Arrange
    Reader reader = new StringReader(SOME_EC_PRIVATE_KEY_PKCS8_PEM);

    // Act
    PrivateKey actual = CryptoUtils.getPrivateKey(reader);

    // Assert
    assertThat(actual).isInstanceOf(ECPrivateKey.class);
    assertThat(actual.getFormat()).isEqualTo("PKCS#8");
  }
}
