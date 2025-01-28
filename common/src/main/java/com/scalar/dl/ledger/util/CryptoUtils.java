package com.scalar.dl.ledger.util;

import com.scalar.dl.ledger.error.CommonError;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

public final class CryptoUtils {

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  private CryptoUtils() {}

  public static KeyStore createKeyStore(
      String certChainPath, String privateKeyPath, String keyPassword) {
    X509Certificate cert = getCertificate(certChainPath);
    PrivateKey key = getPrivateKey(privateKeyPath);
    return createKeyStore(cert, key, keyPassword);
  }

  private static X509Certificate getCertificate(String certChainPath) {
    try (PEMParser pemParser = new PEMParser(Files.newBufferedReader(Paths.get(certChainPath)))) {
      Object data = pemParser.readObject();
      if (!(data instanceof X509CertificateHolder)) {
        throw new IllegalArgumentException(
            CommonError.INVALID_CERTIFICATE.buildMessage(certChainPath));
      }
      return new JcaX509CertificateConverter().getCertificate((X509CertificateHolder) data);
    } catch (IOException | CertificateException e) {
      throw new IllegalArgumentException(
          CommonError.READING_CERTIFICATE_FAILED.buildMessage(certChainPath, e.getMessage()), e);
    }
  }

  private static PrivateKey getPrivateKey(String privateKeyPath) {
    try (Reader reader = Files.newBufferedReader(Paths.get(privateKeyPath))) {
      return getPrivateKey(reader);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          CommonError.INVALID_PRIVATE_KEY.buildMessage(privateKeyPath));
    } catch (IOException e) {
      throw new IllegalArgumentException(
          CommonError.READING_CERTIFICATE_FAILED.buildMessage(privateKeyPath, e.getMessage()), e);
    }
  }

  public static PrivateKey getPrivateKey(Reader reader) throws IOException {
    try (PEMParser pemParser = new PEMParser(reader)) {
      Object data = pemParser.readObject();

      PrivateKeyInfo privateKeyInfo;
      if (data instanceof PrivateKeyInfo) {
        privateKeyInfo = (PrivateKeyInfo) data;
      } else if (data instanceof PEMKeyPair) {
        PEMKeyPair pemKeyPair = (PEMKeyPair) data;
        privateKeyInfo = pemKeyPair.getPrivateKeyInfo();
      } else {
        throw new IllegalArgumentException();
      }

      return new JcaPEMKeyConverter().getPrivateKey(privateKeyInfo);
    }
  }

  private static KeyStore createKeyStore(X509Certificate cert, PrivateKey key, String keyPassword) {
    try {
      KeyStore keystore = KeyStore.getInstance("PKCS12");
      keystore.load(null);
      keystore.setCertificateEntry("cert", cert);
      keystore.setKeyEntry("key", key, keyPassword.toCharArray(), new X509Certificate[] {cert});
      return keystore;
    } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
      throw new IllegalArgumentException(
          CommonError.CREATING_KEY_STORE_FAILED.buildMessage(e.getMessage()), e);
    }
  }
}
